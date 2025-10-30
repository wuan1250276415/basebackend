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
 */
export const presetColors = [
  { name: '拂晓蓝', value: '#1890ff' },
  { name: '薄暮红', value: '#f5222d' },
  { name: '火山橙', value: '#fa541c' },
  { name: '日暮黄', value: '#faad14' },
  { name: '极光绿', value: '#52c41a' },
  { name: '明青', value: '#13c2c2' },
  { name: '极客蓝', value: '#2f54eb' },
  { name: '酱紫', value: '#722ed1' },
];

/**
 * 浅色主题配置
 */
export const lightTheme: ThemeConfig = {
  token: {
    colorPrimary: '#1890ff',
    borderRadius: 6,
    colorBgContainer: '#ffffff',
    colorBgElevated: '#ffffff',
    colorBgLayout: '#f5f5f5',
    colorText: '#000000d9',
    colorTextSecondary: '#00000073',
    colorBorder: '#d9d9d9',
  },
  components: {
    Layout: {
      headerBg: '#001529',
      headerHeight: 64,
      siderBg: '#001529',
      bodyBg: '#f0f2f5',
    },
    Menu: {
      darkItemBg: '#001529',
      darkItemSelectedBg: '#1890ff',
      darkItemColor: 'rgba(255, 255, 255, 0.65)',
      darkItemSelectedColor: '#fff',
    },
  },
};

/**
 * 深色主题配置
 */
export const darkTheme: ThemeConfig = {
  token: {
    colorPrimary: '#1890ff',
    borderRadius: 6,
    colorBgContainer: '#141414',
    colorBgElevated: '#1f1f1f',
    colorBgLayout: '#000000',
    colorText: '#ffffffd9',
    colorTextSecondary: '#ffffff73',
    colorBorder: '#434343',
  },
  components: {
    Layout: {
      headerBg: '#141414',
      headerHeight: 64,
      siderBg: '#141414',
      bodyBg: '#000000',
    },
    Menu: {
      darkItemBg: '#141414',
      darkItemSelectedBg: '#1890ff',
      darkItemColor: 'rgba(255, 255, 255, 0.65)',
      darkItemSelectedColor: '#fff',
    },
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
