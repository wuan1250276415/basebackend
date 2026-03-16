import axios, { type InternalAxiosRequestConfig, type AxiosResponse } from 'axios';
import type { ApiResult } from '@/types';

/** Axios 实例，自动附加 JWT Token 和租户 ID */
const http = axios.create({
  baseURL: '/api/chat',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

/** 请求拦截器：注入 Authorization 和 X-Tenant-Id */
http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  const tenantId = localStorage.getItem('tenantId') || '0';
  config.headers['X-Tenant-Id'] = tenantId;

  const userId = localStorage.getItem('userId');
  if (userId) {
    config.headers['X-User-Id'] = userId;
  }
  return config;
});

/** 响应拦截器：统一错误处理 */
http.interceptors.response.use(
  (response: AxiosResponse<ApiResult<unknown>>) => response,
  (error) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('userId');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  },
);

/** 提取 data 字段的便捷方法 */
export async function request<T>(promise: Promise<AxiosResponse<ApiResult<T>>>): Promise<T> {
  const resp = await promise;
  if (resp.data.code !== 200) {
    throw new Error(resp.data.message || '请求失败');
  }
  return resp.data.data;
}

export default http;
