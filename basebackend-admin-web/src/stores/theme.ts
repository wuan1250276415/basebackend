import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { ThemeMode, LayoutMode, getThemeConfig, watchSystemTheme } from '@/config/theme';
import { ThemeConfig } from 'antd';

/**
 * 主题状态接口
 */
interface ThemeState {
  // 主题模式
  mode: ThemeMode;
  // 主题色
  primaryColor: string;
  // 布局模式
  layout: LayoutMode;
  // 菜单是否折叠
  menuCollapsed: boolean;
  // Ant Design 主题配置
  themeConfig: ThemeConfig;
  // 实际使用的主题模式（考虑 auto 的情况）
  actualMode: 'light' | 'dark';
}

/**
 * 主题操作接口
 */
interface ThemeActions {
  // 设置主题模式
  setMode: (mode: ThemeMode) => void;
  // 设置主题色
  setPrimaryColor: (color: string) => void;
  // 设置布局模式
  setLayout: (layout: LayoutMode) => void;
  // 切换菜单折叠状态
  toggleMenuCollapsed: () => void;
  // 设置菜单折叠状态
  setMenuCollapsed: (collapsed: boolean) => void;
  // 更新主题配置
  updateThemeConfig: () => void;
}

/**
 * 计算实际主题模式
 */
const calculateActualMode = (mode: ThemeMode): 'light' | 'dark' => {
  if (mode === 'auto') {
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  }
  return mode;
};

/**
 * 主题 Store
 */
export const useThemeStore = create<ThemeState & ThemeActions>()(
  persist(
    (set, get) => {
      // 监听系统主题变化
      const cleanup = watchSystemTheme(() => {
        const { mode } = get();
        if (mode === 'auto') {
          get().updateThemeConfig();
        }
      });

      // 清理监听器（虽然在浏览器环境中很少需要）
      if (typeof window !== 'undefined') {
        window.addEventListener('beforeunload', cleanup);
      }

      return {
        // 初始状态
        mode: 'light',
        primaryColor: '#1890ff',
        layout: 'side',
        menuCollapsed: false,
        actualMode: 'light',
        themeConfig: getThemeConfig('light', '#1890ff'),

        // Actions
        setMode: (mode) => {
          set({ mode });
          get().updateThemeConfig();
        },

        setPrimaryColor: (color) => {
          set({ primaryColor: color });
          get().updateThemeConfig();
        },

        setLayout: (layout) => {
          set({ layout });
        },

        toggleMenuCollapsed: () => {
          set((state) => ({ menuCollapsed: !state.menuCollapsed }));
        },

        setMenuCollapsed: (collapsed) => {
          set({ menuCollapsed: collapsed });
        },

        updateThemeConfig: () => {
          const { mode, primaryColor } = get();
          const actualMode = calculateActualMode(mode);
          const themeConfig = getThemeConfig(mode, primaryColor);

          set({
            actualMode,
            themeConfig,
          });

          // 更新 HTML 根元素的 data-theme 属性，方便 CSS 使用
          document.documentElement.setAttribute('data-theme', actualMode);
        },
      };
    },
    {
      name: 'theme-storage',
      // 只持久化部分状态
      partialize: (state) => ({
        mode: state.mode,
        primaryColor: state.primaryColor,
        layout: state.layout,
        menuCollapsed: state.menuCollapsed,
      }),
      // 恢复状态后的回调
      onRehydrateStorage: () => (state) => {
        if (state) {
          state.updateThemeConfig();
        }
      },
    }
  )
);
