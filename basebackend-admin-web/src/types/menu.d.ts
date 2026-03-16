/**
 * 权限/菜单类型定义
 * 对应后端 PermissionDTO 和前端菜单树节点
 */

/** 权限数据传输对象，对应后端 PermissionDTO */
export interface PermissionDTO {
  /** 权限ID */
  id: number;
  /** 权限名称 */
  permissionName: string;
  /** 权限标识 */
  permissionKey: string;
  /** API路径 */
  apiPath: string;
  /** HTTP方法 */
  httpMethod: string;
  /** 权限类型（1-菜单权限, 2-按钮权限, 3-API权限） */
  permissionType: number;
  /** 状态（0-禁用, 1-启用） */
  status: number;
  /** 备注 */
  remark: string;
}

/** 前端菜单树节点，由后端权限数据 + 前端路由配置组合 */
export interface MenuItem {
  /** 菜单ID */
  id: number;
  /** 父菜单ID */
  parentId: number;
  /** 菜单名称 */
  name: string;
  /** 图标 */
  icon: string;
  /** 路由路径 */
  path: string;
  /** 权限标识 */
  permissionKey: string;
  /** 类型（0-目录, 1-菜单, 2-按钮） */
  type: number;
  /** 排序号 */
  orderNum: number;
  /** 状态（0-禁用, 1-启用） */
  status: number;
  /** 子菜单 */
  children?: MenuItem[];
}
