import { create } from 'zustand';

/**
 * 标签页项接口
 */
export interface TabItem {
  /** 路由 path，作为唯一标识 */
  key: string;
  /** 标签标题 */
  title: string;
  /** 是否可关闭（首页不可关闭） */
  closable: boolean;
}

/**
 * 标签页状态接口
 */
export interface TabState {
  /** 已打开的标签页列表 */
  tabs: TabItem[];
  /** 当前激活的标签页 key */
  activeKey: string;

  /** 添加标签页：已存在则仅激活，不存在则添加并激活 */
  addTab: (tab: TabItem) => void;
  /** 移除标签页：关闭后激活最近的标签页 */
  removeTab: (key: string) => void;
  /** 设置当前激活的标签页 */
  setActiveTab: (key: string) => void;
}

/** 首页标签（不可关闭） */
const dashboardTab: TabItem = {
  key: '/',
  title: '首页',
  closable: false,
};

/**
 * 多标签页状态管理 Store
 * 不使用 persist 中间件，页面刷新后标签页重置
 */
export const useTabStore = create<TabState>()((set, get) => ({
  tabs: [dashboardTab],
  activeKey: '/',

  addTab: (tab: TabItem) => {
    const { tabs } = get();
    const exists = tabs.some((t) => t.key === tab.key);
    if (exists) {
      // 已存在，仅切换激活
      set({ activeKey: tab.key });
    } else {
      // 不存在，添加并激活
      set({ tabs: [...tabs, tab], activeKey: tab.key });
    }
  },

  removeTab: (key: string) => {
    const { tabs, activeKey } = get();

    // 找到要移除的标签
    const targetTab = tabs.find((t) => t.key === key);
    // 不可关闭的标签不允许移除
    if (!targetTab || !targetTab.closable) {
      return;
    }

    const targetIndex = tabs.findIndex((t) => t.key === key);
    const newTabs = tabs.filter((t) => t.key !== key);

    // 如果关闭的是当前激活的标签，需要激活最近的标签
    if (key === activeKey) {
      // 优先激活前一个标签，没有则激活后一个
      const newActiveIndex = targetIndex > 0 ? targetIndex - 1 : 0;
      const newActiveKey = newTabs.length > 0 ? newTabs[newActiveIndex].key : '/';
      set({ tabs: newTabs, activeKey: newActiveKey });
    } else {
      set({ tabs: newTabs });
    }
  },

  setActiveTab: (key: string) => {
    set({ activeKey: key });
  },
}));
