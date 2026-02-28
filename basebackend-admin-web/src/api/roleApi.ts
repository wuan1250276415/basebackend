import request from '@/api/request';
import type { RoleDTO, PageResult } from '@/types';

/**
 * 角色管理 API
 * 对接后端 /api/user/roles/* 接口
 */
export const roleApi = {
  /** 分页查询角色列表 */
  page: (params: { current: number; size: number; roleName?: string; roleKey?: string; status?: number }): Promise<PageResult<RoleDTO>> =>
    request.get('/api/user/roles', { params }),

  /** 根据 ID 获取角色详情 */
  getById: (id: number): Promise<RoleDTO> =>
    request.get(`/api/user/roles/${id}`),

  /** 创建角色 */
  create: (data: Partial<RoleDTO>): Promise<void> =>
    request.post('/api/user/roles', data),

  /** 更新角色 */
  update: (id: number, data: Partial<RoleDTO>): Promise<void> =>
    request.put(`/api/user/roles/${id}`, data),

  /** 删除角色 */
  delete: (id: number): Promise<void> =>
    request.delete(`/api/user/roles/${id}`),

  /** 分配菜单 */
  assignMenus: (id: number, menuIds: number[]): Promise<void> =>
    request.put(`/api/user/roles/${id}/menus`, menuIds),

  /** 分配权限 */
  assignPermissions: (id: number, permissionIds: number[]): Promise<void> =>
    request.put(`/api/user/roles/${id}/permissions`, permissionIds),

  /** 管理数据权限 */
  manageDataPermissions: (id: number, dataScope: number): Promise<void> =>
    request.put(`/api/user/roles/${id}/data-permissions`, null, { params: { dataScope } }),
};
