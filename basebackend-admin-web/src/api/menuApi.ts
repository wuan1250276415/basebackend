import request from '@/api/request';
import type { MenuItem } from '@/types';

/**
 * 菜单/资源管理 API
 * 对接后端 /api/system/application/resource/* 接口
 */
export const menuApi = {
  /** 当前用户菜单 */
  getCurrentUserMenus: (): Promise<MenuItem[]> =>
    request.get('/api/system/application/resource/current-user'),

  /** 菜单树 */
  getTree: (): Promise<MenuItem[]> =>
    request.get('/api/system/application/resource/tree'),

  /** 资源列表 */
  list: (): Promise<any[]> =>
    request.get('/api/system/application/resource'),

  /** 创建菜单 */
  create: (data: any): Promise<void> =>
    request.post('/api/system/application/resource', data),

  /** 更新菜单 */
  update: (id: number, data: any): Promise<void> =>
    request.put(`/api/system/application/resource/${id}`, data),

  /** 删除菜单 */
  delete: (id: number): Promise<void> =>
    request.delete(`/api/system/application/resource/${id}`),

  /** 根据用户ID查询菜单列表 (用于兼容或其他需要) */
  getByUser: (userId: number): Promise<MenuItem[]> =>
    request.get(`/api/system/application/resource/user/${userId}`),

  /** 根据角色ID查询菜单列表 */
  getByRole: (roleId: number): Promise<any[]> =>
    request.get(`/api/system/application/resource/role/${roleId}`),
};
