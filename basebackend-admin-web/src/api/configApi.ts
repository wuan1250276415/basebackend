import request from '@/api/request';
import type { ConfigDTO, PageResult } from '@/types';

/**
 * 系统配置管理 API
 * 对接后端 /api/system/configs/* 接口
 */
export const configApi = {
  /** 分页查询配置参数列表 */
  page: (params: {
    current: number;
    size: number;
    configName?: string;
    configKey?: string;
    configType?: number;
  }): Promise<PageResult<ConfigDTO>> =>
    request.get('/basebackend-system-api/api/system/configs', { params }),

  /** 根据ID查询配置详情 */
  getById: (id: number): Promise<ConfigDTO> =>
    request.get(`/basebackend-system-api/api/system/configs/${id}`),

  /** 创建配置参数 */
  create: (data: Partial<ConfigDTO>): Promise<void> =>
    request.post('/basebackend-system-api/api/system/configs', data),

  /** 更新配置参数 */
  update: (id: number, data: Partial<ConfigDTO>): Promise<void> =>
    request.put(`/basebackend-system-api/api/system/configs/${id}`, data),

  /** 删除配置参数 */
  delete: (id: number): Promise<void> =>
    request.delete(`/basebackend-system-api/api/system/configs/${id}`),
};
