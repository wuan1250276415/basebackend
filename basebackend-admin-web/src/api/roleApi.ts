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

  /** 获取角色权限 */
  permissions: (id: number | string): Promise<number[]> =>
    request.get(`/api/user/roles/${id}/permissions`),

  /** 管理数据权限 */
  manageDataPermissions: (id: number, dataScope: number): Promise<void> =>
    request.put(`/api/user/roles/${id}/data-permissions`, null, { params: { dataScope } }),

  /** 获取角色树 */
  tree: (appId?: number | string): Promise<RoleDTO[]> =>
    request.get('/api/user/roles/tree', { params: appId ? { appId } : undefined }),

  /** 获取角色关联用户 */
  users: (id: number | string, username?: string): Promise<any[]> =>
    request.get(`/api/user/roles/${id}/users`, { params: username ? { username } : undefined }),

  /** 关联用户到角色 */
  assignUsers: (id: number | string, userIds: Array<number | string>): Promise<void> =>
    request.post(`/api/user/roles/${id}/users`, userIds),

  /** 取消用户角色关联 */
  removeUser: (roleId: number | string, userId: number | string): Promise<void> =>
    request.delete(`/api/user/roles/${roleId}/users/${userId}`),

  /** 获取角色资源 */
  resources: (id: number | string): Promise<number[]> =>
    request.get(`/api/user/roles/${id}/resources`),

  /** 分配角色资源 */
  assignResources: (id: number | string, resourceIds: Array<number | string>): Promise<void> =>
    request.put(`/api/user/roles/${id}/resources`, resourceIds),

  /** 获取角色列表操作权限 */
  listOperations: (id: number | string, resourceType: string): Promise<number[]> =>
    request.get(`/api/user/roles/${id}/list-operations`, { params: { resourceType } }),

  /** 配置列表操作权限 */
  configureListOperations: (
    id: number | string,
    payload: { resourceType: string; operationIds: Array<number | string> }
  ): Promise<void> => request.put(`/api/user/roles/${id}/list-operations`, payload),

  /** 配置数据权限 */
  configureDataPermissions: (id: number | string, filterRule: string): Promise<void> =>
    request.put(`/api/user/roles/${id}/data-permissions`, { filterRule }),
};

export const getRolePage = roleApi.page;
export const getRoleById = roleApi.getById;
export const createRole = roleApi.create;
export const updateRole = roleApi.update;
export const deleteRole = roleApi.delete;
export const assignRoleMenus = roleApi.assignMenus;
export const assignRolePermissions = roleApi.assignPermissions;
export const getRolePermissions = roleApi.permissions;
export const getRoleTree = roleApi.tree;
export const getRoleUsers = roleApi.users;
export const assignUsersToRole = roleApi.assignUsers;
export const removeUserFromRole = roleApi.removeUser;
export const getRoleResources = roleApi.resources;
export const assignRoleResources = roleApi.assignResources;
export const getRoleListOperations = roleApi.listOperations;
export const configureListOperations = roleApi.configureListOperations;
export const configureDataPermissions = roleApi.configureDataPermissions;
