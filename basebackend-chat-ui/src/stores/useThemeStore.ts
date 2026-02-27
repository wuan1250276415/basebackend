import { create } from 'zustand';

interface ThemeState {
  /** 当前主题 */
  theme: 'light' | 'dark';
  /** 切换主题 */
  toggleTheme: () => void;
}

export const useThemeStore = create<ThemeState>((set) => ({
  theme: (localStorage.getItem('theme') as 'light' | 'dark') || 'light',

  toggleTheme: () =>
    set((state) => {
      const next = state.theme === 'light' ? 'dark' : 'light';
      localStorage.setItem('theme', next);
      return { theme: next };
    }),
}));
