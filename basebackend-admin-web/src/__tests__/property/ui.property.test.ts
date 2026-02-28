/**
 * UI 相关属性基测试
 * 使用 fast-check 验证主题持久化和响应式侧边栏等 UI 属性
 */
import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import fc from 'fast-check';
import { useSettingStore } from '@/stores/settingStore';

describe('SettingStore 属性基测试', () => {
  beforeEach(() => {
    // 清除 localStorage，确保每次测试从干净状态开始
    localStorage.clear();
    // 重置 zustand store 到初始状态
    useSettingStore.setState({
      theme: 'light',
      primaryColor: '#1677ff',
      collapsed: false,
    });
  });

  afterEach(() => {
    localStorage.clear();
  });

  // Feature: admin-web-rebuild, Property 13: Theme preference persistence round-trip
  // **Validates: Requirements 20.5**
  describe('Property 13: Theme preference persistence round-trip', () => {
    it('对于任意主题值（light 或 dark），设置到 SettingStore 后从 localStorage 恢复应得到相同值', async () => {
      await fc.assert(
        fc.asyncProperty(
          // 生成随机主题值：'light' 或 'dark'
          fc.oneof(fc.constant('light' as const), fc.constant('dark' as const)),
          async (theme) => {
            // 清理上一次迭代的状态
            localStorage.clear();
            useSettingStore.setState({
              theme: 'light',
              primaryColor: '#1677ff',
              collapsed: false,
            });

            // 通过 setState 设置主题值
            useSettingStore.setState({ theme });

            // 等待 zustand persist 中间件将状态写入 localStorage
            await new Promise((resolve) => setTimeout(resolve, 0));

            // 从 localStorage 读取持久化的数据
            const raw = localStorage.getItem('setting-storage');
            expect(raw).not.toBeNull();

            // 解析 localStorage 中的数据
            const parsed = JSON.parse(raw!);

            // 验证主题值往返一致性：store → localStorage → 读取 = 相同值
            expect(parsed.state.theme).toBe(theme);
          },
        ),
        { numRuns: 20 },
      );
    });
  });
});


/**
 * 响应式侧边栏折叠的纯逻辑函数
 * 根据视口宽度和用户手动折叠偏好，决定侧边栏最终的折叠状态
 * - 视口宽度 < 768px 时，强制折叠（图标模式）
 * - 视口宽度 >= 768px 时，尊重用户的手动折叠偏好
 */
export function computeCollapsed(viewportWidth: number, userPreference: boolean): boolean {
  if (viewportWidth < 768) {
    return true;
  }
  return userPreference;
}

// Feature: admin-web-rebuild, Property 12: Responsive sidebar collapse
// **Validates: Requirements 20.4**
describe('Property 12: Responsive sidebar collapse', () => {
  beforeEach(() => {
    localStorage.clear();
    useSettingStore.setState({
      theme: 'light',
      primaryColor: '#1677ff',
      collapsed: false,
    });
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('视口宽度 < 768px 时，侧边栏应始终处于折叠（图标模式）状态', () => {
    fc.assert(
      fc.property(
        // 生成 100-767 范围内的随机视口宽度
        fc.integer({ min: 100, max: 767 }),
        // 生成随机的用户手动折叠偏好
        fc.boolean(),
        (viewportWidth, userPreference) => {
          // 无论用户偏好如何，窄视口下侧边栏必须折叠
          const result = computeCollapsed(viewportWidth, userPreference);
          expect(result).toBe(true);
        },
      ),
      { numRuns: 100 },
    );
  });

  it('视口宽度 >= 768px 时，侧边栏应尊重用户的手动折叠偏好', () => {
    fc.assert(
      fc.property(
        // 生成 768-2000 范围内的随机视口宽度
        fc.integer({ min: 768, max: 2000 }),
        // 生成随机的用户手动折叠偏好
        fc.boolean(),
        (viewportWidth, userPreference) => {
          // 宽视口下，折叠状态应与用户偏好一致
          const result = computeCollapsed(viewportWidth, userPreference);
          expect(result).toBe(userPreference);
        },
      ),
      { numRuns: 100 },
    );
  });

  it('对于任意视口宽度（100-2000px），768px 是折叠行为的分界点', () => {
    fc.assert(
      fc.property(
        // 生成 100-2000 范围内的随机视口宽度
        fc.integer({ min: 100, max: 2000 }),
        // 生成随机的用户手动折叠偏好
        fc.boolean(),
        (viewportWidth, userPreference) => {
          const result = computeCollapsed(viewportWidth, userPreference);

          if (viewportWidth < 768) {
            // 窄视口：强制折叠
            expect(result).toBe(true);
          } else {
            // 宽视口：尊重用户偏好
            expect(result).toBe(userPreference);
          }
        },
      ),
      { numRuns: 100 },
    );
  });

  it('结合 SettingStore 验证：窄视口下 collapsed 偏好不影响最终折叠状态', () => {
    fc.assert(
      fc.property(
        // 生成 100-767 范围内的随机视口宽度
        fc.integer({ min: 100, max: 767 }),
        (viewportWidth) => {
          // 用户偏好为展开（collapsed = false）
          useSettingStore.setState({ collapsed: false });
          const storeCollapsed = useSettingStore.getState().collapsed;
          const result = computeCollapsed(viewportWidth, storeCollapsed);

          // 即使用户偏好为展开，窄视口下仍应折叠
          expect(result).toBe(true);
        },
      ),
      { numRuns: 100 },
    );
  });

  it('结合 SettingStore 验证：宽视口下 toggleCollapsed 正确切换折叠状态', () => {
    fc.assert(
      fc.property(
        // 生成 768-2000 范围内的随机视口宽度
        fc.integer({ min: 768, max: 2000 }),
        // 生成随机的初始折叠状态
        fc.boolean(),
        (viewportWidth, initialCollapsed) => {
          // 设置初始折叠状态
          useSettingStore.setState({ collapsed: initialCollapsed });

          // 切换折叠状态
          useSettingStore.getState().toggleCollapsed();
          const newCollapsed = useSettingStore.getState().collapsed;

          // 验证 toggleCollapsed 正确取反
          expect(newCollapsed).toBe(!initialCollapsed);

          // 宽视口下，computeCollapsed 应尊重切换后的偏好
          const result = computeCollapsed(viewportWidth, newCollapsed);
          expect(result).toBe(!initialCollapsed);
        },
      ),
      { numRuns: 100 },
    );
  });
});
