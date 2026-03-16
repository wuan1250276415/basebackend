/**
 * AuthStore 属性基测试
 * 使用 fast-check 验证认证状态管理的核心属性
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import fc from 'fast-check';

// mock authApi 和 menuApi 模块，在导入 authStore 之前
vi.mock('@/api/authApi', () => ({
  authApi: {
    login: vi.fn(),
    logout: vi.fn(),
    getUserInfo: vi.fn(),
    refreshToken: vi.fn(),
    changePassword: vi.fn(),
  },
}));

vi.mock('@/api/menuApi', () => ({
  menuApi: {
    list: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
    getByUser: vi.fn(),
    getByRole: vi.fn(),
  },
}));

import { useAuthStore } from '@/stores/authStore';
import { authApi } from '@/api/authApi';

describe('AuthStore 属性基测试', () => {
  beforeEach(() => {
    // 清除 localStorage，确保每次测试从干净状态开始
    localStorage.clear();
    // 重置 zustand store 到初始状态
    useAuthStore.getState().reset();
    // 清除所有 mock 调用记录
    vi.clearAllMocks();
  });

  afterEach(() => {
    localStorage.clear();
  });

  // Feature: admin-web-rebuild, Property 3: Login error message propagation
  // **Validates: Requirements 3.5**
  describe('Property 3: Login error message propagation', () => {
    it('对于任意登录错误消息，登录失败后 AuthStore 应保持不变（无 token 存储）', async () => {
      await fc.assert(
        fc.asyncProperty(
          // 生成随机非空错误消息字符串
          fc.string({ minLength: 1, maxLength: 200 }),
          async (errorMessage) => {
            // 记录登录前的 store 状态
            const stateBefore = useAuthStore.getState();
            const tokenBefore = stateBefore.token;
            const userInfoBefore = stateBefore.userInfo;

            // mock authApi.login 抛出包含错误消息的异常
            vi.mocked(authApi.login).mockRejectedValueOnce(new Error(errorMessage));

            // 执行登录，预期会抛出错误
            let caughtError: Error | null = null;
            try {
              await useAuthStore.getState().login('testuser', 'testpass');
            } catch (err) {
              caughtError = err as Error;
            }

            // 验证错误被正确传播
            expect(caughtError).not.toBeNull();
            expect(caughtError!.message).toBe(errorMessage);

            // 验证 AuthStore 状态未改变（无 token 存储）
            const stateAfter = useAuthStore.getState();
            expect(stateAfter.token).toBe(tokenBefore);
            expect(stateAfter.userInfo).toBe(userInfoBefore);
          },
        ),
        { numRuns: 20 },
      );
    });
  });

  // Feature: admin-web-rebuild, Property 4: Token persistence round-trip
  // **Validates: Requirements 3.7**
  describe('Property 4: Token persistence round-trip', () => {
    it('对于任意有效 token 字符串，存储到 AuthStore 后从 localStorage 恢复应得到相同值', async () => {
      await fc.assert(
        fc.asyncProperty(
          // 生成随机非空 token 字符串，过滤掉会破坏 JSON 序列化的字符
          fc.string({ minLength: 1, maxLength: 500 }).filter((s) => {
            try {
              const serialized = JSON.stringify({ state: { token: s }, version: 0 });
              const parsed = JSON.parse(serialized);
              return parsed.state.token === s;
            } catch {
              return false;
            }
          }),
          async (token) => {
            // 通过 zustand setState 直接设置 token（模拟登录成功后的状态）
            useAuthStore.setState({ token });

            // 等待 zustand persist 中间件将状态写入 localStorage
            // zustand persist 是同步写入的，但需要确保 setState 完成
            await new Promise((resolve) => setTimeout(resolve, 0));

            // 从 localStorage 读取持久化的数据
            const raw = localStorage.getItem('auth-storage');
            expect(raw).not.toBeNull();

            // 解析 localStorage 中的数据
            const parsed = JSON.parse(raw!);

            // 验证 token 往返一致性：store → localStorage → 读取 = 相同值
            expect(parsed.state.token).toBe(token);

            // 清理，为下一次迭代做准备
            useAuthStore.getState().reset();
            localStorage.clear();
          },
        ),
        { numRuns: 20 },
      );
    });
  });
});
