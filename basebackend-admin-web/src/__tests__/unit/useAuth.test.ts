import { describe, it, expect, beforeEach } from 'vitest';
import { useAuthStore } from '@/stores/authStore';
import { useAuth } from '@/hooks/useAuth';
import { renderHook } from '@testing-library/react';

/**
 * useAuth Hook 单元测试
 * 验证权限判断逻辑：单权限、多权限（any/all 模式）、通配符
 */
describe('useAuth', () => {
  // 每个测试前重置 store 权限
  beforeEach(() => {
    useAuthStore.setState({
      token: 'test-token',
      permissions: new Set(['system:user:add', 'system:user:edit', 'system:role:list']),
      roles: new Set(['admin']),
      userInfo: null,
      menus: [],
      dynamicRoutes: [],
    });
  });

  describe('单权限模式', () => {
    it('用户拥有该权限时返回 true', () => {
      const { result } = renderHook(() => useAuth('system:user:add'));
      expect(result.current).toBe(true);
    });

    it('用户没有该权限时返回 false', () => {
      const { result } = renderHook(() => useAuth('system:user:delete'));
      expect(result.current).toBe(false);
    });
  });

  describe('多权限 any 模式（默认）', () => {
    it('拥有其中一个权限时返回 true', () => {
      const { result } = renderHook(() =>
        useAuth(['system:user:add', 'system:user:delete']),
      );
      expect(result.current).toBe(true);
    });

    it('一个都没有时返回 false', () => {
      const { result } = renderHook(() =>
        useAuth(['system:user:delete', 'system:dept:add']),
      );
      expect(result.current).toBe(false);
    });

    it('显式传入 any 模式与默认行为一致', () => {
      const { result } = renderHook(() =>
        useAuth(['system:user:add', 'system:user:delete'], 'any'),
      );
      expect(result.current).toBe(true);
    });
  });

  describe('多权限 all 模式', () => {
    it('全部拥有时返回 true', () => {
      const { result } = renderHook(() =>
        useAuth(['system:user:add', 'system:user:edit'], 'all'),
      );
      expect(result.current).toBe(true);
    });

    it('缺少任一权限时返回 false', () => {
      const { result } = renderHook(() =>
        useAuth(['system:user:add', 'system:user:delete'], 'all'),
      );
      expect(result.current).toBe(false);
    });
  });

  describe('通配符权限', () => {
    it('拥有 *:*:* 通配符时任何权限都返回 true', () => {
      useAuthStore.setState({
        permissions: new Set(['*:*:*']),
      });
      const { result } = renderHook(() => useAuth('system:any:permission'));
      expect(result.current).toBe(true);
    });

    it('拥有 * 通配符时任何权限都返回 true', () => {
      useAuthStore.setState({
        permissions: new Set(['*']),
      });
      const { result } = renderHook(() => useAuth('system:any:permission'));
      expect(result.current).toBe(true);
    });

    it('通配符对多权限 all 模式也生效', () => {
      useAuthStore.setState({
        permissions: new Set(['*:*:*']),
      });
      const { result } = renderHook(() =>
        useAuth(['system:user:add', 'system:role:delete'], 'all'),
      );
      expect(result.current).toBe(true);
    });
  });

  describe('空权限集合', () => {
    it('权限集合为空时返回 false', () => {
      useAuthStore.setState({
        permissions: new Set(),
      });
      const { result } = renderHook(() => useAuth('system:user:add'));
      expect(result.current).toBe(false);
    });

    it('空权限数组在 any 模式下返回 false', () => {
      const { result } = renderHook(() => useAuth([], 'any'));
      expect(result.current).toBe(false);
    });

    it('空权限数组在 all 模式下返回 true（vacuous truth）', () => {
      const { result } = renderHook(() => useAuth([], 'all'));
      expect(result.current).toBe(true);
    });
  });
});
