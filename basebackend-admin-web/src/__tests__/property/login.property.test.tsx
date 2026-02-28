/**
 * 登录页面集成属性基测试
 * 验证 Login 页面组件在登录失败时正确显示服务端错误消息
 * 与 auth.property.test.ts 中的 Property 3 不同，本测试验证的是页面级集成行为
 */
import { describe, it, expect, vi, beforeAll, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent, cleanup, act } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import fc from 'fast-check';

/**
 * jsdom 不提供 window.matchMedia，Ant Design 的响应式组件需要它
 * 在所有测试之前 mock 该方法
 */
beforeAll(() => {
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: vi.fn().mockImplementation((query: string) => ({
      matches: false,
      media: query,
      onchange: null,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    })),
  });
});

// 在导入组件之前 mock 依赖模块
const mockLogin = vi.fn();

vi.mock('@/stores/authStore', () => ({
  useAuthStore: (selector: (state: Record<string, unknown>) => unknown) =>
    selector({ login: mockLogin }),
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => vi.fn(),
  };
});

import Login from '@/pages/Login/index';

/**
 * 辅助函数：渲染登录页面并填写表单后提交
 * 使用 fireEvent.change 填充用户名和密码，然后点击提交按钮
 */
async function renderAndSubmitLogin() {
  const result = render(
    <MemoryRouter initialEntries={['/login']}>
      <Login />
    </MemoryRouter>,
  );

  // 填写用户名和密码（Ant Design Form 需要 change 事件触发值更新）
  const usernameInput = screen.getByPlaceholderText('用户名');
  const passwordInput = screen.getByPlaceholderText('密码');

  await act(async () => {
    fireEvent.change(usernameInput, { target: { value: 'testuser' } });
    fireEvent.change(passwordInput, { target: { value: 'testpass' } });
  });

  // 点击登录按钮触发表单提交
  const submitButton = screen.getByRole('button', { name: /登\s*录/ });
  await act(async () => {
    fireEvent.click(submitButton);
  });

  return result;
}

// Feature: admin-web-rebuild, Property 3: Login error message propagation
// **Validates: Requirements 3.5**
describe('Property 3 集成测试: Login 页面错误消息传播', () => {
  beforeEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  it('对于任意服务端错误消息，登录失败后应在页面上显示该错误消息', async () => {
    await fc.assert(
      fc.asyncProperty(
        // 生成随机非空错误消息，过滤掉纯空白和包含特殊 HTML 字符的字符串
        fc
          .string({ minLength: 1, maxLength: 80 })
          .filter((s) => s.trim().length > 0)
          .map((s) => s.trim()),
        async (errorMessage) => {
          cleanup();
          vi.clearAllMocks();

          // mock login 抛出包含随机错误消息的异常
          mockLogin.mockRejectedValueOnce(new Error(errorMessage));

          await renderAndSubmitLogin();

          // 等待 mockLogin 被调用（表单验证通过后异步调用）
          await waitFor(() => {
            expect(mockLogin).toHaveBeenCalledWith('testuser', 'testpass');
          });

          // 等待错误消息出现在 DOM 中（通过 Alert 组件渲染）
          await waitFor(() => {
            const alert = screen.getByRole('alert');
            expect(alert).toBeInTheDocument();
            expect(alert.textContent).toContain(errorMessage);
          });
        },
      ),
      { numRuns: 100 },
    );
  }, 120000);
});
