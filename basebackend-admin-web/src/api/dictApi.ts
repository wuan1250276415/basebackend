import request from '@/api/request';
import type { DictTypeDTO, DictDataDTO, PageResult } from '@/types';

/**
 * 字典管理 API
 * 对接后端 /api/system/dicts/* 接口
 */
export const dictApi = {
  /** 分页查询字典类型列表 */
  typePage: (params: { current: number; size: number; dictName?: string; dictType?: string; status?: number }): Promise<PageResult<DictTypeDTO>> =>
    request.get('/basebackend-system-api/api/system/dicts', { params }),

  /** 根据字典类型标识查询字典数据列表 */
  dataByType: (dictType: string): Promise<DictDataDTO[]> =>
    request.get(`/basebackend-system-api/api/system/dicts/data/type/${dictType}`),

  /** 创建字典类型 */
  createType: (data: Partial<DictTypeDTO>): Promise<void> =>
    request.post('/basebackend-system-api/api/system/dicts', data),

  /** 更新字典类型 */
  updateType: (id: number, data: Partial<DictTypeDTO>): Promise<void> =>
    request.put(`/basebackend-system-api/api/system/dicts/${id}`, data),

  /** 删除字典类型 */
  deleteType: (id: number): Promise<void> =>
    request.delete(`/basebackend-system-api/api/system/dicts/${id}`),

  /** 创建字典数据 */
  createData: (data: Partial<DictDataDTO>): Promise<void> =>
    request.post('/basebackend-system-api/api/system/dicts/data', data),

  /** 更新字典数据 */
  updateData: (id: number, data: Partial<DictDataDTO>): Promise<void> =>
    request.put(`/basebackend-system-api/api/system/dicts/data/${id}`, data),

  /** 删除字典数据 */
  deleteData: (id: number): Promise<void> =>
    request.delete(`/basebackend-system-api/api/system/dicts/data/${id}`),

  /** 刷新字典缓存 */
  refreshCache: (): Promise<void> =>
    request.post('/basebackend-system-api/api/system/dicts/refresh-cache'),
};
