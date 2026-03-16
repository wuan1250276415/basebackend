/**
 * 类型定义统一导出
 * 从各模块 .d.ts 文件重新导出所有类型
 */

// 通用 API 响应类型
export type { Result, PageResult, SimplePageResult } from './api';

// 认证相关类型
export type {
  LoginParams,
  LoginResult,
  UserInfo,
  UserContext,
  ProfileDetail,
  UpdateProfileParams,
  ChangePasswordParams,
} from './auth';

// 用户管理类型
export type { UserDTO, UserCreateDTO, UserQueryDTO } from './user';

// 角色管理类型
export type { RoleDTO } from './role';

// 权限/菜单类型
export type { PermissionDTO, MenuItem } from './menu';

// 部门管理类型
export type { DeptDTO } from './dept';

// 字典管理类型
export type { DictTypeDTO, DictDataDTO } from './dict';

// 系统配置类型
export type { ConfigDTO } from './config';

// 日志类型
export type { OperationLogDTO, LoginLogDTO } from './log';

// 监控类型
export type { OnlineUserDTO, ServerInfoDTO, CacheInfoDTO } from './monitor';

// 聊天管理类型
export type { ChatMessage, ChatGroup, ChatGroupMember } from './chat';

// ============================================================
// 向后兼容别名：保留旧代码使用的类型名称
// 后续重构时可逐步迁移到新命名
// ============================================================

import type { LoginParams as _LoginParams } from './auth';
import type { LoginResult as _LoginResult } from './auth';
import type { ChangePasswordParams as _ChangePasswordParams } from './auth';
import type { UpdateProfileParams as _UpdateProfileParams } from './auth';
import type { UserDTO as _UserDTO } from './user';
import type { RoleDTO as _RoleDTO } from './role';
import type { PermissionDTO as _PermissionDTO } from './menu';
import type { MenuItem as _MenuItem } from './menu';
import type { DeptDTO as _DeptDTO } from './dept';
import type { DictTypeDTO as _DictTypeDTO } from './dict';
import type { DictDataDTO as _DictDataDTO } from './dict';
import type { OperationLogDTO as _OperationLogDTO } from './log';
import type { LoginLogDTO as _LoginLogDTO } from './log';
import type { OnlineUserDTO as _OnlineUserDTO } from './monitor';
import type { ServerInfoDTO as _ServerInfoDTO } from './monitor';

/** @deprecated 请使用 LoginParams */
export type LoginRequest = _LoginParams;
/** @deprecated 请使用 LoginResult */
export type LoginResponse = _LoginResult;
/** @deprecated 请使用 ChangePasswordParams */
export type ChangePasswordRequest = _ChangePasswordParams;
/** @deprecated 请使用 UpdateProfileParams */
export type UpdateProfileRequest = _UpdateProfileParams;
/** @deprecated 请使用 UserDTO */
export type User = _UserDTO;
/** @deprecated 请使用 RoleDTO */
export type Role = _RoleDTO;
/** @deprecated 请使用 PermissionDTO */
export type Permission = _PermissionDTO;
/** @deprecated 请使用 MenuItem */
export type Menu = _MenuItem;
/** @deprecated 请使用 DeptDTO */
export type Dept = _DeptDTO;
/** @deprecated 请使用 DictTypeDTO */
export type Dict = _DictTypeDTO;
/** @deprecated 请使用 DictDataDTO */
export type DictData = _DictDataDTO;
/** @deprecated 请使用 OperationLogDTO */
export type OperationLog = _OperationLogDTO;
/** @deprecated 请使用 LoginLogDTO */
export type LoginLog = _LoginLogDTO;
/** @deprecated 请使用 OnlineUserDTO */
export type OnlineUser = _OnlineUserDTO;
/** @deprecated 请使用 ServerInfoDTO */
export type ServerInfo = _ServerInfoDTO;

// 保留旧代码使用的其他类型（非新 spec 范围，但需要向后兼容）

/** 应用信息类型 */
export interface Application {
  id?: string;
  appName: string;
  appCode: string;
  appType: string;
  appIcon?: string;
  appUrl?: string;
  status?: number;
  orderNum?: number;
  remark?: string;
  createTime?: string;
  updateTime?: string;
}

/** 应用资源类型 */
export interface ApplicationResource {
  id?: string;
  appId: string;
  resourceName: string;
  parentId?: string;
  resourceType: string;
  path?: string;
  component?: string;
  perms?: string;
  icon?: string;
  visible?: number;
  openType?: string;
  orderNum?: number;
  status?: number;
  remark?: string;
  children?: ApplicationResource[];
  appName?: string;
  createTime?: string;
  updateTime?: string;
}

/** 列表操作类型 */
export interface ListOperation {
  id?: string;
  operationCode: string;
  operationName: string;
  operationType: string;
  resourceType?: string;
  icon?: string;
  orderNum?: number;
  status?: number;
  remark?: string;
  createTime?: string;
  updateTime?: string;
}

/** 数据权限规则类型 */
export interface DataPermissionRule {
  id?: string;
  roleId: string;
  resourceType: string;
  permissionName: string;
  filterType: string;
  filterRule: string;
  status?: number;
  remark?: string;
  createTime?: string;
  updateTime?: string;
}
