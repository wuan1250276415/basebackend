import request from '@/api/request';
import type { PermissionDTO, MenuItem } from '@/types';

/**
 * 权限/菜单管理 API
 * 对接后端 /api/system/permissions/* 接口
 */
export const menuApi = {
  /** 查询权限列表 */
  list: (): Promise<PermissionDTO[]> =>
    request.get('/api/system/permissions'),

  /** 创建权限 */
  create: (data: Partial<PermissionDTO>): Promise<void> =>
    request.post('/api/system/permissions', data),

  /** 更新权限 */
  update: (id: number, data: Partial<PermissionDTO>): Promise<void> =>
    request.put(`/api/system/permissions/${id}`, data),

  /** 删除权限 */
  delete: (id: number): Promise<void> =>
    request.delete(`/api/system/permissions/${id}`),

  /** 根据用户ID查询菜单列表 */
  getByUser: (userId: number): Promise<MenuItem[]> =>
    request.get(`/api/system/permissions/user/${userId}`),

  /** 根据角色ID查询权限列表 */
  getByRole: (roleId: number): Promise<PermissionDTO[]> =>
    request.get(`/api/system/permissions/role/${roleId}`),
};
