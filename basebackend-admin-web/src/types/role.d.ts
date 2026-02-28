/**
 * 角色管理类型定义
 * 对应后端 RoleDTO
 */

/** 角色数据传输对象，对应后端 RoleDTO */
export interface RoleDTO {
  /** 角色ID */
  id: number;
  /** 应用ID */
  appId: number;
  /** 角色名称 */
  roleName: string;
  /** 角色标识 */
  roleKey: string;
  /** 排序号 */
  roleSort: number;
  /** 数据范围（1-全部, 2-自定义, 3-本部门, 4-本部门及以下） */
  dataScope: number;
  /** 状态（0-禁用, 1-启用） */
  status: number;
  /** 备注 */
  remark: string;
  /** 菜单ID列表 */
  menuIds: number[];
  /** 权限ID列表 */
  permissionIds: number[];
}
