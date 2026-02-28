import { create } from 'zustand';
import { persist } from 'zustand/middleware';

/**
 * 应用设置状态接口
 * 管理主题模式、主色调、侧边栏折叠状态
 */
export interface SettingState {
  /** 主题模式：亮色 / 暗色 */
  theme: 'light' | 'dark';
  /** 主色调，默认 #1677ff */
  primaryColor: string;
  /** 侧边栏是否折叠 */
  collapsed: boolean;

  /** 切换主题模式（light ↔ dark） */
  toggleTheme: () => void;
  /** 切换侧边栏折叠状态 */
  toggleCollapsed: () => void;
  /** 直接设置侧边栏折叠状态 */
  setCollapsed: (collapsed: boolean) => void;
}

/**
 * 应用设置 Store
 * 使用 zustand persist 中间件持久化到 localStorage
 * key: 'setting-storage'
 */
export const useSettingStore = create<SettingState>()(
  persist(
    (set) => ({
      theme: 'light',
      primaryColor: '#1677ff',
      collapsed: false,

      toggleTheme: () =>
        set((state) => ({
          theme: state.theme === 'light' ? 'dark' : 'light',
        })),

      toggleCollapsed: () =>
        set((state) => ({
          collapsed: !state.collapsed,
        })),

      setCollapsed: (collapsed: boolean) =>
        set({ collapsed }),
    }),
    {
      name: 'setting-storage',
    },
  ),
);
