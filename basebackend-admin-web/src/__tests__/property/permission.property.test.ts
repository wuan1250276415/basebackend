/**
 * useAuth 权限判断属性基测试
 * 使用 fast-check 验证 useAuth hook 的权限检查正确性
 *
 * Property 10: useAuth permission check correctness
 * 对于任意权限字符串 p 和任意用户权限集合 S，
 * useAuth(p) 应当返回 true 当且仅当 p ∈ S 或 S 包含通配符（*:*:* 或 *）
 */
import { describe, it, expect, beforeEach } from 'vitest';
import { renderHook } from '@testing-library/react';
import fc from 'fast-check';
import { useAuth } from '@/hooks/useAuth';
import { useAuthStore } from '@/stores/authStore';

// Feature: admin-web-rebuild, Property 10: useAuth permission check correctness
// **Validates: Requirements 5.4**

/**
 * 生成随机权限段的 arbitrary（模块:资源:操作 格式）
 * 每段为 2-8 个小写字母
 */
const permSegmentArb = fc.stringMatching(/^[a-z]{2,8}$/);

/**
 * 生成 module:resource:action 格式的权限字符串 arbitrary
 */
const permissionArb = fc
  .tuple(permSegmentArb, permSegmentArb, permSegmentArb)
  .map(([mod, res, act]) => `${mod}:${res}:${act}`);

/**
 * 生成随机权限集合的 arbitrary（1-10 个权限字符串）
 */
const permissionSetArb = fc
  .array(permissionArb, { minLength: 1, maxLength: 10 })
  .map((perms) => [...new Set(perms)]);

/**
 * 每个测试前重置 authStore 状态
 */
beforeEach(() => {
  localStorage.clear();
  useAuthStore.setState({
    token: null,
    userInfo: null,
    permissions: new Set<string>(),
    roles: new Set<string>(),
    menus: [],
    dynamicRoutes: [],
  });
});

describe('useAuth 权限判断属性基测试', () => {
  // Feature: admin-web-rebuild, Property 10: useAuth permission check correctness
  describe('Property 10: 权限存在于集合中时返回 true', () => {
    it('对于任意权限 p 和包含 p 的权限集合 S，useAuth(p) 应返回 true', () => {
      fc.assert(
        fc.property(permissionSetArb, (perms) => {
          // 从集合中随机选一个权限作为待检查权限
          const targetPerm = perms[0];

          // 设置 authStore 的权限集合
          useAuthStore.setState({ permissions: new Set(perms) });

          // 使用 renderHook 测试 useAuth hook
          const { result } = renderHook(() => useAuth(targetPerm));

          // p ∈ S → useAuth(p) 应返回 true
          expect(result.current).toBe(true);
        }),
        { numRuns: 20 },
      );
    });
  });

  // Feature: admin-web-rebuild, Property 10: useAuth permission check correctness
  describe('Property 10: 权限不在集合中时返回 false', () => {
    it('对于任意权限 p 不在权限集合 S 中，useAuth(p) 应返回 false', () => {
      fc.assert(
        fc.property(permissionSetArb, permissionArb, (perms, extraPerm) => {
          // 确保 extraPerm 不在 perms 中
          const filteredPerms = perms.filter((p) => p !== extraPerm);

          // 如果过滤后为空，使用一个固定的不同权限
          if (filteredPerms.length === 0) {
            return; // 跳过此用例
          }

          // 设置不包含 extraPerm 的权限集合
          useAuthStore.setState({ permissions: new Set(filteredPerms) });

          // 使用 renderHook 测试 useAuth hook
          const { result } = renderHook(() => useAuth(extraPerm));

          // p ∉ S → useAuth(p) 应返回 false
          expect(result.current).toBe(false);
        }),
        { numRuns: 20 },
      );
    });
  });

  // Feature: admin-web-rebuild, Property 10: useAuth permission check correctness
  describe('Property 10: 通配符 *:*:* 使任意权限返回 true', () => {
    it('当权限集合包含 *:*:* 时，任意权限字符串 useAuth(p) 应返回 true', () => {
      fc.assert(
        fc.property(permissionArb, (perm) => {
          // 设置包含通配符 *:*:* 的权限集合
          useAuthStore.setState({ permissions: new Set(['*:*:*']) });

          const { result } = renderHook(() => useAuth(perm));

          // S 包含 *:*:* → 任意 p 都应返回 true
          expect(result.current).toBe(true);
        }),
        { numRuns: 20 },
      );
    });
  });

  // Feature: admin-web-rebuild, Property 10: useAuth permission check correctness
  describe('Property 10: 通配符 * 使任意权限返回 true', () => {
    it('当权限集合包含 * 时，任意权限字符串 useAuth(p) 应返回 true', () => {
      fc.assert(
        fc.property(permissionArb, (perm) => {
          // 设置包含通配符 * 的权限集合
          useAuthStore.setState({ permissions: new Set(['*']) });

          const { result } = renderHook(() => useAuth(perm));

          // S 包含 * → 任意 p 都应返回 true
          expect(result.current).toBe(true);
        }),
        { numRuns: 20 },
      );
    });
  });

  // Feature: admin-web-rebuild, Property 10: useAuth permission check correctness
  describe('Property 10: useAuth 返回值的充要条件', () => {
    it('useAuth(p) 返回 true 当且仅当 p ∈ S 或 S 包含通配符', () => {
      // 生成权限集合和待检查权限，可能包含通配符
      const permSetWithOptionalWildcard = fc.oneof(
        // 普通权限集合
        permissionSetArb,
        // 包含 *:*:* 通配符的集合
        permissionSetArb.map((perms) => ['*:*:*', ...perms]),
        // 包含 * 通配符的集合
        permissionSetArb.map((perms) => ['*', ...perms]),
      );

      fc.assert(
        fc.property(permSetWithOptionalWildcard, permissionArb, (perms, perm) => {
          const permSet = new Set(perms);
          useAuthStore.setState({ permissions: permSet });

          const { result } = renderHook(() => useAuth(perm));

          // 计算期望值：p ∈ S 或 S 包含通配符
          const hasWildcard = permSet.has('*:*:*') || permSet.has('*');
          const expected = hasWildcard || permSet.has(perm);

          expect(result.current).toBe(expected);
        }),
        { numRuns: 20 },
      );
    });
  });
});
