import axios from 'axios';
import type { AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios';
import { message, notification } from 'antd';
import type { Result } from '@/types/api';

/**
 * 扩展 Axios 请求配置，支持跳过认证
 */
export interface RequestConfig extends AxiosRequestConfig {
  /** 跳过 token 附加（如登录接口） */
  skipAuth?: boolean;
}

/**
 * 从 localStorage 中读取 token
 * 使用 zustand persist 的存储 key 'auth-storage'
 * 避免直接依赖 authStore 模块，防止循环引用
 */
function getToken(): string | null {
  try {
    const raw = localStorage.getItem('auth-storage');
    if (!raw) return null;
    const parsed = JSON.parse(raw);
    return parsed?.state?.token ?? null;
  } catch {
    return null;
  }
}

/**
 * 清除认证状态
 * 同时清理 zustand persist 存储和旧版 token 存储
 */
function clearAuth(): void {
  localStorage.removeItem('auth-storage');
  localStorage.removeItem('token');
  localStorage.removeItem('userInfo');
}

// 创建 Axios 实例
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL as string,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8',
  },
});

// ==================== 请求拦截器 ====================
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig & { skipAuth?: boolean }) => {
    // 如果配置了 skipAuth，跳过 token 附加（如登录接口）
    if (!config.skipAuth) {
      const token = getToken();
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  },
);

// ==================== 响应拦截器 ====================
request.interceptors.response.use(
  (response: AxiosResponse<Result>) => {
    if (response.config.responseType === 'blob' || response.config.responseType === 'arraybuffer') {
      return response.data as any;
    }

    const res = response.data;

    // 业务成功：code === 200，解包返回 data 字段
    if (res.code === 200) {
      return res.data as any;
    }

    // 业务失败：显示后端返回的错误消息
    message.error(res.message || '操作失败');
    return Promise.reject(new Error(res.message || '操作失败'));
  },
  (error) => {
    // HTTP 层错误处理
    if (error.response) {
      const { status, data } = error.response;

      switch (status) {
        case 401:
          // 清除认证状态，重定向到登录页
          clearAuth();
          window.location.href = '/login';
          break;

        case 403:
          notification.error({
            message: '权限不足',
            description: '您没有权限执行此操作，请联系管理员',
          });
          break;

        case 404:
          notification.error({
            message: '资源不存在',
            description: data?.message || '请求的资源未找到',
          });
          break;

        case 500:
          notification.error({
            message: '服务器错误',
            description: data?.message || '服务器内部错误，请稍后重试',
          });
          break;

        default:
          notification.error({
            message: '请求失败',
            description: data?.message || `请求异常（状态码: ${status}）`,
          });
      }
    } else if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
      // 请求超时
      notification.error({
        message: '请求超时',
        description: '服务器响应超时，请检查网络后重试',
      });
    } else {
      // 网络错误（断网、DNS 解析失败等）
      notification.error({
        message: '网络错误',
        description: '网络连接失败，请检查网络设置',
      });
    }

    return Promise.reject(error);
  },
);

export default request;
