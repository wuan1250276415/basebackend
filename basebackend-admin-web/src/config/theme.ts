import { ThemeConfig } from 'antd';

/**
 * 主题模式
 */
export type ThemeMode = 'light' | 'dark' | 'auto';

/**
 * 布局模式
 */
export type LayoutMode = 'side' | 'top' | 'mix';

/**
 * 预设主题色
 * Fluent 2 palettes
 */
export const presetColors = [
  { name: 'Modern Dark', value: '#0f172a' },
  { name: 'Indigo', value: '#6366f1' },
  { name: 'Fluent Blue', value: '#0078D4' },
  { name: 'Teal', value: '#008272' },
  { name: 'Green', value: '#107C10' },
  { name: 'Purple', value: '#5C2D91' },
  { name: 'Magenta', value: '#B4009E' },
  { name: 'Red', value: '#D13438' },
  { name: 'Orange', value: '#D83B01' },
];

/**
 * 浅色主题配置 - Fluent Style
 */
export const lightTheme: ThemeConfig = {
  token: {
    colorPrimary: '#0f172a', // Modern Dark
    borderRadius: 8,         // Micro border radius
    borderRadiusSM: 4,
    borderRadiusLG: 12,
    colorBgContainer: '#ffffff',
    colorBgElevated: '#ffffff',
    colorBgLayout: '#f8fafc', // Very light grey blue background
    colorText: '#0f172a',
    colorTextSecondary: '#64748b',
    colorBorder: '#e2e8f0',
    fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, 'Noto Sans', sans-serif",
    boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -2px rgba(0, 0, 0, 0.05)', // Soft shadow
    fontSizeBase: 14,
  },
  components: {
    Layout: {
      headerBg: 'rgba(255, 255, 255, 0.6)', // Glassmorphism
      headerHeight: 60,
      siderBg: '#ffffff',
      bodyBg: 'transparent',
    },
    Menu: {
      itemBorderRadius: 6,
      itemHeight: 40,
      colorItemBg: 'transparent',
      itemSelectedColor: '#0f172a',
      itemSelectedBg: '#f1f5f9', // Slate 100
      activeBarBorderWidth: 3,
    },
    Button: {
      borderRadius: 6,
      controlHeight: 36,
      defaultShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.05)', // Subtle shadow
      primaryShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1)',
    },
    Card: {
      borderRadiusLG: 12,
      boxShadowTertiary: '0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -2px rgba(0, 0, 0, 0.05)',
      colorBorderSecondary: 'transparent', // Borderless
    },
    Table: {
      colorBgContainer: '#ffffff',
      headerBg: '#f8fafc',
      headerBorderRadius: 8,
    },
    Input: {
      borderRadius: 6,
      activeBorderColor: '#0f172a',
      hoverBorderColor: '#334155',
      controlHeight: 36,
    }
  },
};

/**
 * 深色主题配置 - Fluent Style
 */
export const darkTheme: ThemeConfig = {
  token: {
    colorPrimary: '#479EF5', // Lighter blue for dark mode
    borderRadius: 8,
    colorBgContainer: '#292929',
    colorBgElevated: '#333333',
    colorBgLayout: '#202020', // Mica dark
    colorText: '#ffffff',
    colorTextSecondary: '#d1d1d1',
    colorBorder: '#424242',
    fontFamily: "'Segoe UI', 'Segoe UI Web (West European)', -apple-system, BlinkMacSystemFont, Roboto, 'Helvetica Neue', sans-serif",
  },
  components: {
    Layout: {
      headerBg: 'rgba(32, 32, 32, 0.8)',
      headerHeight: 60,
      siderBg: 'rgba(32, 32, 32, 0.8)',
      bodyBg: 'transparent',
    },
    Menu: {
      itemBorderRadius: 6,
      itemHeight: 40,
      colorItemBg: 'transparent',
      itemSelectedColor: '#ffffff',
      itemSelectedBg: 'rgba(255, 255, 255, 0.1)',
      activeBarBorderWidth: 3,
    },
    Button: {
      borderRadius: 4,
    },
    Card: {
      borderRadiusLG: 12,
    }
  },
};

/**
 * 根据主题模式获取主题配置
 */
export const getThemeConfig = (mode: ThemeMode, primaryColor?: string): ThemeConfig => {
  // 检测系统主题（当模式为 auto 时）
  const systemDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
  const isDark = mode === 'dark' || (mode === 'auto' && systemDark);

  const baseTheme = isDark ? darkTheme : lightTheme;

  // 更新 data-theme 属性以便 CSS 变量生效
  if (typeof document !== 'undefined') {
    document.documentElement.setAttribute('data-theme', isDark ? 'dark' : 'light');
  }

  // 如果指定了主题色，覆盖默认主题色
  if (primaryColor) {
    return {
      ...baseTheme,
      token: {
        ...baseTheme.token,
        colorPrimary: primaryColor,
      },
    };
  }

  return baseTheme;
};

/**
 * 监听系统主题变化
 */
export const watchSystemTheme = (callback: (isDark: boolean) => void) => {
  const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');

  const handler = (e: MediaQueryListEvent) => {
    callback(e.matches);
  };

  // 添加监听器
  mediaQuery.addEventListener('change', handler);

  // 返回清理函数
  return () => {
    mediaQuery.removeEventListener('change', handler);
  };
};
