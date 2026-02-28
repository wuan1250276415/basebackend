import request from '@/api/request';
import type { LoginLogDTO, OperationLogDTO, PageResult } from '@/types';

/**
 * 日志管理 API
 * 对接后端 /api/system/logs/* 接口
 */
export const logApi = {
  /** 分页查询登录日志 */
  loginLogPage: (params: {
    current: number;
    size: number;
    username?: string;
    ipAddress?: string;
    status?: number;
    beginTime?: string;
    endTime?: string;
  }): Promise<PageResult<LoginLogDTO>> =>
    request.get('/basebackend-system-api/api/system/logs/login', { params }),

  /** 分页查询操作日志 */
  operationLogPage: (params: {
    current: number;
    size: number;
    username?: string;
    operation?: string;
    status?: number;
    beginTime?: string;
    endTime?: string;
  }): Promise<PageResult<OperationLogDTO>> =>
    request.get('/basebackend-system-api/api/system/logs/operation', { params }),

  /** 获取操作日志详情 */
  logDetail: (id: string): Promise<OperationLogDTO> =>
    request.get(`/basebackend-system-api/api/system/logs/operation/${id}`),

  /** 删除操作日志 */
  deleteOperationLog: (id: string): Promise<void> =>
    request.delete(`/basebackend-system-api/api/system/logs/operation/${id}`),

  /** 批量删除操作日志 */
  batchDeleteOperationLog: (ids: string[]): Promise<void> =>
    request.delete('/basebackend-system-api/api/system/logs/operation/batch', { data: ids }),

  /** 清空操作日志 */
  cleanOperationLog: (): Promise<void> =>
    request.delete('/basebackend-system-api/api/system/logs/operation/clean'),

  /** 批量删除登录日志 */
  batchDeleteLoginLog: (ids: string[]): Promise<void> =>
    request.delete('/basebackend-system-api/api/system/logs/login/batch', { data: ids }),

  /** 清空登录日志 */
  cleanLoginLog: (): Promise<void> =>
    request.delete('/basebackend-system-api/api/system/logs/login/clean'),
};
