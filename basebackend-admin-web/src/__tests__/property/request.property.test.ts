/**
 * HTTP 客户端属性基测试
 * 使用 fast-check 验证 request.ts 中的拦截器行为
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import fc from 'fast-check';
import MockAdapter from 'axios-mock-adapter';

// 在导入 request 之前，先 mock antd 的 message 和 notification
vi.mock('antd', () => ({
  message: {
    error: vi.fn(),
    success: vi.fn(),
    warning: vi.fn(),
    info: vi.fn(),
  },
  notification: {
    error: vi.fn(),
    success: vi.fn(),
    warning: vi.fn(),
    info: vi.fn(),
  },
}));

// mock import.meta.env
vi.stubEnv('VITE_API_BASE_URL', '/api');

// 导入被测模块和 mock 模块
import request from '@/api/request';
import { notification } from 'antd';

describe('HTTP Client 属性基测试', () => {
  let mock: MockAdapter;

  beforeEach(() => {
    // 为 request 实例创建 mock adapter
    mock = new MockAdapter(request);
    // 清除 localStorage 中的认证相关数据
    localStorage.removeItem('auth-storage');
    localStorage.removeItem('token');
    localStorage.removeItem('userInfo');
    // 重置所有 mock 调用记录
    vi.clearAllMocks();
    // mock window.location.href（防止 401 时真正跳转）
    Object.defineProperty(window, 'location', {
      writable: true,
      value: { href: '/' },
    });
  });

  afterEach(() => {
    mock.restore();
  });

  // Feature: admin-web-rebuild, Property 1: Token attached to every request
  // **Validates: Requirements 2.1**
  describe('Property 1: Token attached to every request', () => {
    it('对于任意非空 token，请求头应包含 Bearer <token>', async () => {
      await fc.assert(
        fc.asyncProperty(
          fc.string({ minLength: 1 }).filter((s) => {
            // 过滤掉包含会破坏 JSON 解析的字符的字符串
            try {
              JSON.parse(JSON.stringify({ state: { token: s }, version: 0 }));
              return true;
            } catch {
              return false;
            }
          }),
          async (token) => {
            // 设置 localStorage 中的 token（zustand persist 格式）
            localStorage.setItem(
              'auth-storage',
              JSON.stringify({ state: { token }, version: 0 }),
            );

            // 记录请求头
            let capturedAuth: string | undefined;
            mock.onGet('/api/test').reply((config) => {
              capturedAuth = config.headers?.Authorization as string | undefined;
              return [200, { code: 200, message: 'ok', data: null, success: true }];
            });

            await request.get('/api/test');

            // 验证 Authorization 头等于 Bearer <token>
            expect(capturedAuth).toBe(`Bearer ${token}`);

            // 清理，为下一次迭代做准备
            mock.reset();
          },
        ),
        { numRuns: 20 },
      );
    });
  });

  // Feature: admin-web-rebuild, Property 2: HTTP error handling by status code
  // **Validates: Requirements 2.2, 2.3, 2.4**
  describe('Property 2: HTTP error handling by status code', () => {
    it('401 响应应清除 localStorage 中的 auth-storage', async () => {
      await fc.assert(
        fc.asyncProperty(
          fc.string({ minLength: 1 }),
          async (token) => {
            // 设置认证状态
            localStorage.setItem(
              'auth-storage',
              JSON.stringify({ state: { token }, version: 0 }),
            );

            mock.onGet('/api/test-401').reply(401, { message: 'Unauthorized' });

            try {
              await request.get('/api/test-401');
            } catch {
              // 预期会抛出错误
            }

            // 验证 auth-storage 已被清除
            expect(localStorage.getItem('auth-storage')).toBeNull();

            // 清理
            mock.reset();
          },
        ),
        { numRuns: 20 },
      );
    });

    it('403 响应应调用 notification.error', async () => {
      await fc.assert(
        fc.asyncProperty(
          fc.constant(403),
          async () => {
            vi.clearAllMocks();
            mock.onGet('/api/test-403').reply(403, { message: '权限不足' });

            try {
              await request.get('/api/test-403');
            } catch {
              // 预期会抛出错误
            }

            // 验证 notification.error 被调用
            expect(notification.error).toHaveBeenCalled();
            // 验证调用参数包含权限相关信息
            const callArgs = vi.mocked(notification.error).mock.calls[0][0] as {
              message: string;
            };
            expect(callArgs.message).toBe('权限不足');

            // 清理
            mock.reset();
          },
        ),
        { numRuns: 20 },
      );
    });

    it('网络错误应调用 notification.error', async () => {
      await fc.assert(
        fc.asyncProperty(
          fc.constant('Network Error'),
          async () => {
            vi.clearAllMocks();
            mock.onGet('/api/test-network').networkError();

            try {
              await request.get('/api/test-network');
            } catch {
              // 预期会抛出错误
            }

            // 验证 notification.error 被调用（网络错误提示）
            expect(notification.error).toHaveBeenCalled();

            // 清理
            mock.reset();
          },
        ),
        { numRuns: 20 },
      );
    });
  });
});
