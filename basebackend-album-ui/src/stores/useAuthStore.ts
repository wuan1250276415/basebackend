import {create} from 'zustand';
export const useAuthStore = create((set) => ({
  token: localStorage.getItem('token'),
  user: null,
  login: (token: string, user: any) => {
    localStorage.setItem('token', token);
    set({ token, user });
  },
  logout: () => {
    localStorage.removeItem('token');
    set({ token: null, user: null });
  }
}));
