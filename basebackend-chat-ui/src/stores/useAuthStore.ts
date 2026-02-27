import { create } from 'zustand';

/** 登录用户信息 */
export interface AuthUser {
  userId: number;
  nickname: string;
  avatar: string;
  tenantId: string;
}

interface AuthState {
  /** 当前用户 */
  user: AuthUser | null;
  /** JWT Token */
  token: string | null;
  /** 是否已登录 */
  isLoggedIn: boolean;
  /** 登录 */
  login: (user: AuthUser, token: string) => void;
  /** 登出 */
  logout: () => void;
  /** 从 localStorage 恢复会话 */
  restore: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  token: null,
  isLoggedIn: false,

  login: (user, token) => {
    localStorage.setItem('token', token);
    localStorage.setItem('userId', String(user.userId));
    localStorage.setItem('tenantId', user.tenantId);
    localStorage.setItem('user', JSON.stringify(user));
    set({ user, token, isLoggedIn: true });
  },

  logout: () => {
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
    localStorage.removeItem('tenantId');
    localStorage.removeItem('user');
    set({ user: null, token: null, isLoggedIn: false });
  },

  restore: () => {
    const token = localStorage.getItem('token');
    const userJson = localStorage.getItem('user');
    if (token && userJson) {
      try {
        const user = JSON.parse(userJson) as AuthUser;
        set({ user, token, isLoggedIn: true });
      } catch {
        // 数据损坏，清除
        localStorage.removeItem('token');
        localStorage.removeItem('user');
      }
    }
  },
}));
