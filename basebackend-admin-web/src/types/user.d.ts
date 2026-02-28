/**
 * 用户管理类型定义
 * 对应后端 UserDTO、UserCreateDTO、UserQueryDTO
 */

/** 用户数据传输对象，对应后端 UserDTO */
export interface UserDTO {
  /** 用户ID */
  id: number;
  /** 用户名 */
  username: string;
  /** 昵称 */
  nickname: string;
  /** 邮箱 */
  email: string;
  /** 手机号 */
  phone: string;
  /** 头像地址 */
  avatar: string;
  /** 性别（0-未知, 1-男, 2-女） */
  gender: number;
  /** 生日 */
  birthday: string;
  /** 部门ID */
  deptId: number;
  /** 部门名称 */
  deptName: string;
  /** 用户类型 */
  userType: number;
  /** 状态（0-禁用, 1-启用） */
  status: number;
  /** 角色ID列表 */
  roleIds: number[];
  /** 角色名称列表 */
  roleNames: string[];
  /** 备注 */
  remark: string;
}

/** 创建用户参数，对应后端 UserCreateDTO */
export interface UserCreateDTO {
  /** 用户名 */
  username: string;
  /** 密码 */
  password: string;
  /** 昵称 */
  nickname: string;
  /** 邮箱 */
  email?: string;
  /** 手机号 */
  phone?: string;
  /** 头像地址 */
  avatar?: string;
  /** 性别 */
  gender?: number;
  /** 生日 */
  birthday?: string;
  /** 部门ID */
  deptId?: number;
  /** 用户类型 */
  userType?: number;
  /** 状态 */
  status?: number;
  /** 角色ID列表 */
  roleIds?: number[];
  /** 备注 */
  remark?: string;
}

/** 用户查询参数，对应后端 UserQueryDTO */
export interface UserQueryDTO {
  /** 用户名（模糊查询） */
  username?: string;
  /** 昵称（模糊查询） */
  nickname?: string;
  /** 手机号 */
  phone?: string;
  /** 状态 */
  status?: number;
  /** 部门ID */
  deptId?: number;
}
