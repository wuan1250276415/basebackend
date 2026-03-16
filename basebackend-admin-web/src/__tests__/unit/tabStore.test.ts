import { describe, it, expect, beforeEach } from 'vitest';
import { useTabStore } from '@/stores/tabStore';
import type { TabItem } from '@/stores/tabStore';

/**
 * TabStore 单元测试
 * 验证多标签页状态管理的核心逻辑
 */
describe('TabStore', () => {
  // 每个测试前重置 store 状态
  beforeEach(() => {
    useTabStore.setState({
      tabs: [{ key: '/', title: '首页', closable: false }],
      activeKey: '/',
    });
  });

  describe('初始状态', () => {
    it('应包含首页标签且为激活状态', () => {
      const state = useTabStore.getState();
      expect(state.tabs).toHaveLength(1);
      expect(state.tabs[0]).toEqual({ key: '/', title: '首页', closable: false });
      expect(state.activeKey).toBe('/');
    });
  });

  describe('addTab', () => {
    it('添加新标签并激活', () => {
      const tab: TabItem = { key: '/system/user', title: '用户管理', closable: true };
      useTabStore.getState().addTab(tab);

      const state = useTabStore.getState();
      expect(state.tabs).toHaveLength(2);
      expect(state.tabs[1]).toEqual(tab);
      expect(state.activeKey).toBe('/system/user');
    });

    it('添加已存在的标签仅切换激活，不重复添加', () => {
      const tab: TabItem = { key: '/system/user', title: '用户管理', closable: true };
      useTabStore.getState().addTab(tab);
      useTabStore.getState().addTab({ key: '/system/role', title: '角色管理', closable: true });

      // 再次添加已存在的标签
      useTabStore.getState().addTab(tab);

      const state = useTabStore.getState();
      expect(state.tabs).toHaveLength(3); // 首页 + 用户管理 + 角色管理
      expect(state.activeKey).toBe('/system/user');
    });

    it('添加多个不同标签', () => {
      useTabStore.getState().addTab({ key: '/a', title: 'A', closable: true });
      useTabStore.getState().addTab({ key: '/b', title: 'B', closable: true });
      useTabStore.getState().addTab({ key: '/c', title: 'C', closable: true });

      const state = useTabStore.getState();
      expect(state.tabs).toHaveLength(4);
      expect(state.activeKey).toBe('/c');
    });
  });

  describe('removeTab', () => {
    it('移除当前激活的标签，激活前一个标签', () => {
      useTabStore.getState().addTab({ key: '/a', title: 'A', closable: true });
      useTabStore.getState().addTab({ key: '/b', title: 'B', closable: true });
      // 当前激活 /b

      useTabStore.getState().removeTab('/b');

      const state = useTabStore.getState();
      expect(state.tabs).toHaveLength(2); // 首页 + A
      expect(state.activeKey).toBe('/a');
    });

    it('移除当前激活的标签（第一个可关闭标签），激活后一个标签', () => {
      useTabStore.getState().addTab({ key: '/a', title: 'A', closable: true });
      useTabStore.getState().addTab({ key: '/b', title: 'B', closable: true });
      useTabStore.getState().setActiveTab('/a');

      useTabStore.getState().removeTab('/a');

      const state = useTabStore.getState();
      expect(state.tabs).toHaveLength(2); // 首页 + B
      // /a 在 index 1，移除后 newTabs = [首页, B]，targetIndex=1, newActiveIndex=0 → 首页
      expect(state.activeKey).toBe('/');
    });

    it('移除非激活的标签，不改变激活状态', () => {
      useTabStore.getState().addTab({ key: '/a', title: 'A', closable: true });
      useTabStore.getState().addTab({ key: '/b', title: 'B', closable: true });
      // 当前激活 /b

      useTabStore.getState().removeTab('/a');

      const state = useTabStore.getState();
      expect(state.tabs).toHaveLength(2); // 首页 + B
      expect(state.activeKey).toBe('/b');
    });

    it('不可关闭的标签（首页）不能被移除', () => {
      useTabStore.getState().removeTab('/');

      const state = useTabStore.getState();
      expect(state.tabs).toHaveLength(1);
      expect(state.tabs[0].key).toBe('/');
    });

    it('移除不存在的标签不产生影响', () => {
      useTabStore.getState().addTab({ key: '/a', title: 'A', closable: true });
      useTabStore.getState().removeTab('/nonexistent');

      const state = useTabStore.getState();
      expect(state.tabs).toHaveLength(2);
    });
  });

  describe('setActiveTab', () => {
    it('切换激活标签', () => {
      useTabStore.getState().addTab({ key: '/a', title: 'A', closable: true });
      useTabStore.getState().addTab({ key: '/b', title: 'B', closable: true });

      useTabStore.getState().setActiveTab('/a');
      expect(useTabStore.getState().activeKey).toBe('/a');

      useTabStore.getState().setActiveTab('/');
      expect(useTabStore.getState().activeKey).toBe('/');
    });
  });

  describe('首页标签保护', () => {
    it('首页标签始终存在且不可关闭', () => {
      const state = useTabStore.getState();
      const dashboardTab = state.tabs.find((t) => t.key === '/');
      expect(dashboardTab).toBeDefined();
      expect(dashboardTab!.closable).toBe(false);
    });
  });
});
