import request from '@/api/request';
import type { RequestConfig } from '@/api/request';
import type { LoginParams, LoginResult, UserContext, ChangePasswordParams } from '@/types';

/**
 * 认证相关 API
 * 对接后端 /api/user/auth/* 接口
 */
export const authApi = {
  /** 用户登录 */
  login: (params: LoginParams): Promise<LoginResult> =>
    request.post('/api/user/auth/login', params, { skipAuth: true } as RequestConfig),

  /** 用户登出 */
  logout: (): Promise<void> => request.post('/api/user/auth/logout'),

  /** 刷新 Token */
  refreshToken: (): Promise<LoginResult> => request.post('/api/user/auth/refresh'),

  /** 获取当前用户信息（含权限、角色、菜单） */
  getUserInfo: (): Promise<UserContext> => request.get('/api/user/auth/info'),

  /** 修改密码 */
  changePassword: (params: ChangePasswordParams): Promise<void> =>
    request.put('/api/user/auth/password', params),
};

export const login = authApi.login;
export const logout = authApi.logout;
export const refreshToken = authApi.refreshToken;
export const getCurrentUserInfo = authApi.getUserInfo;
export const changePassword = authApi.changePassword;
