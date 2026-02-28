/**
 * TabStore 属性基测试
 * 使用 fast-check 验证多标签页状态管理的核心属性
 */
import { describe, it, expect, beforeEach } from 'vitest';
import fc from 'fast-check';
import { useTabStore } from '@/stores/tabStore';

// Feature: admin-web-rebuild, Property 6: Tab bar reflects navigation history
// **Validates: Requirements 4.5**

/**
 * 生成随机路由路径的 arbitrary
 * 路径以 / 开头，包含 1-3 段字母数字路径段
 * 排除 '/' 以避免与首页标签冲突
 */
const routePathArb = fc
  .array(fc.stringMatching(/^[a-z][a-z0-9]{0,9}$/), { minLength: 1, maxLength: 3 })
  .map((segments) => '/' + segments.join('/'));

/**
 * 生成随机标签页项的 arbitrary（排除首页路径）
 */
const tabItemArb = fc.record({
  key: routePathArb,
  title: fc.stringMatching(/^[\u4e00-\u9fa5a-zA-Z]{1,10}$/),
  closable: fc.constant(true),
});

describe('TabStore 属性基测试', () => {
  beforeEach(() => {
    // 重置 store 到初始状态（仅包含首页标签）
    useTabStore.setState({
      tabs: [{ key: '/', title: '首页', closable: false }],
      activeKey: '/',
    });
  });

  // Feature: admin-web-rebuild, Property 6: Tab bar reflects navigation history
  // 子属性 1：添加 N 个唯一标签后，store 中应有 N+1 个标签（包含首页）
  describe('子属性 1: 唯一标签数量正确', () => {
    it('添加 N 个不同路由的标签后，store 应包含 N+1 个标签（含首页）', () => {
      fc.assert(
        fc.property(
          // 生成 1-20 个随机标签页项
          fc.array(tabItemArb, { minLength: 1, maxLength: 20 }),
          (tabItems) => {
            // 重置 store
            useTabStore.setState({
              tabs: [{ key: '/', title: '首页', closable: false }],
              activeKey: '/',
            });

            // 收集唯一路径（排除首页 '/'）
            const uniqueKeys = new Set<string>();

            // 依次添加标签
            for (const tab of tabItems) {
              useTabStore.getState().addTab(tab);
              if (tab.key !== '/') {
                uniqueKeys.add(tab.key);
              }
            }

            const { tabs } = useTabStore.getState();

            // 验证：标签数 = 唯一非首页路径数 + 1（首页）
            expect(tabs.length).toBe(uniqueKeys.size + 1);

            // 验证：首页标签始终存在
            expect(tabs.some((t) => t.key === '/')).toBe(true);

            // 验证：每个唯一路径都有对应标签
            for (const key of uniqueKeys) {
              expect(tabs.some((t) => t.key === key)).toBe(true);
            }
          },
        ),
        { numRuns: 20 },
      );
    });
  });

  // Feature: admin-web-rebuild, Property 6: Tab bar reflects navigation history
  // 子属性 2：激活标签始终匹配最后添加/设置的标签
  describe('子属性 2: 激活标签匹配最后操作', () => {
    it('每次 addTab 或 setActiveTab 后，activeKey 应匹配操作的目标', () => {
      fc.assert(
        fc.property(
          // 生成操作序列：addTab 或 setActiveTab
          fc.array(
            fc.oneof(
              tabItemArb.map((tab) => ({ type: 'add' as const, tab })),
              routePathArb.map((key) => ({ type: 'setActive' as const, key })),
            ),
            { minLength: 1, maxLength: 30 },
          ),
          (operations) => {
            // 重置 store
            useTabStore.setState({
              tabs: [{ key: '/', title: '首页', closable: false }],
              activeKey: '/',
            });

            for (const op of operations) {
              if (op.type === 'add') {
                useTabStore.getState().addTab(op.tab);
                // addTab 后，activeKey 应为刚添加的标签
                expect(useTabStore.getState().activeKey).toBe(op.tab.key);
              } else {
                // setActiveTab 仅设置 activeKey，不检查标签是否存在
                useTabStore.getState().setActiveTab(op.key);
                expect(useTabStore.getState().activeKey).toBe(op.key);
              }
            }
          },
        ),
        { numRuns: 20 },
      );
    });
  });

  // Feature: admin-web-rebuild, Property 6: Tab bar reflects navigation history
  // 子属性 3：关闭激活标签后，应激活最近的剩余标签
  describe('子属性 3: 关闭激活标签后激活最近标签', () => {
    it('关闭当前激活的可关闭标签后，应激活前一个标签（或后一个）', () => {
      fc.assert(
        fc.property(
          // 生成 2-10 个唯一路径的标签（确保有足够标签可关闭）
          fc
            .uniqueArray(routePathArb, { minLength: 2, maxLength: 10 })
            .map((keys) =>
              keys.map((key) => ({
                key,
                title: `页面${key}`,
                closable: true,
              })),
            ),
          // 随机选择要关闭的标签索引
          fc.nat(),
          (tabItems, closeIndexSeed) => {
            // 重置 store
            useTabStore.setState({
              tabs: [{ key: '/', title: '首页', closable: false }],
              activeKey: '/',
            });

            // 添加所有标签
            for (const tab of tabItems) {
              useTabStore.getState().addTab(tab);
            }

            const { tabs: tabsBefore } = useTabStore.getState();
            // 从可关闭标签中选择一个来关闭
            const closableTabs = tabsBefore.filter((t) => t.closable);
            if (closableTabs.length === 0) return;

            const closeIndex = closeIndexSeed % closableTabs.length;
            const tabToClose = closableTabs[closeIndex];

            // 先激活要关闭的标签
            useTabStore.getState().setActiveTab(tabToClose.key);
            expect(useTabStore.getState().activeKey).toBe(tabToClose.key);

            // 记录关闭前的标签列表和目标标签的位置
            const tabsBeforeClose = useTabStore.getState().tabs;
            const targetIndex = tabsBeforeClose.findIndex((t) => t.key === tabToClose.key);

            // 关闭标签
            useTabStore.getState().removeTab(tabToClose.key);

            const { tabs: tabsAfter, activeKey } = useTabStore.getState();

            // 验证：标签已被移除
            expect(tabsAfter.some((t) => t.key === tabToClose.key)).toBe(false);
            expect(tabsAfter.length).toBe(tabsBeforeClose.length - 1);

            // 验证：激活了最近的标签
            // 优先激活前一个标签，没有则激活后一个
            const expectedTabs = tabsBeforeClose.filter((t) => t.key !== tabToClose.key);
            const expectedActiveIndex = targetIndex > 0 ? targetIndex - 1 : 0;
            const expectedActiveKey = expectedTabs[expectedActiveIndex].key;
            expect(activeKey).toBe(expectedActiveKey);
          },
        ),
        { numRuns: 20 },
      );
    });

    it('不可关闭的标签（首页）不能被移除', () => {
      fc.assert(
        fc.property(
          // 生成 0-5 个额外标签
          fc.array(tabItemArb, { minLength: 0, maxLength: 5 }),
          (tabItems) => {
            // 重置 store
            useTabStore.setState({
              tabs: [{ key: '/', title: '首页', closable: false }],
              activeKey: '/',
            });

            // 添加额外标签
            for (const tab of tabItems) {
              useTabStore.getState().addTab(tab);
            }

            const tabsBefore = useTabStore.getState().tabs;

            // 尝试移除首页标签
            useTabStore.getState().removeTab('/');

            const tabsAfter = useTabStore.getState().tabs;

            // 验证：首页标签仍然存在，标签数量不变
            expect(tabsAfter.some((t) => t.key === '/')).toBe(true);
            expect(tabsAfter.length).toBe(tabsBefore.length);
          },
        ),
        { numRuns: 20 },
      );
    });
  });
});
