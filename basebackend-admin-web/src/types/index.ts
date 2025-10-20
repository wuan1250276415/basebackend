// API响应类型
export interface Result<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
}

// 分页响应类型
export interface PageResult<T = any> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

// 用户信息类型
export interface UserInfo {
  userId: string
  username: string
  nickname: string
  email?: string
  phone?: string
  avatar?: string
  gender?: number
  deptId?: string
  deptName?: string
  userType?: number
  status?: number
}

// 登录请求类型
export interface LoginRequest {
  username: string
  password: string
  captcha?: string
  captchaId?: string
  rememberMe?: boolean
}

// 登录响应类型
export interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  userInfo: UserInfo
  permissions: string[]
  roles: string[]
}

// 用户类型
export interface User {
  id?: string
  username: string
  nickname: string
  email?: string
  phone?: string
  avatar?: string
  gender?: number
  birthday?: string
  deptId?: string
  deptName?: string
  userType?: number
  status?: number
  roleIds?: string[]
  roleNames?: string[]
  remark?: string
  createTime?: string
  updateTime?: string
}

// 角色类型
export interface Role {
  id?: string
  appId?: string
  roleName: string
  roleKey: string
  roleSort?: number
  dataScope?: number
  status?: number
  remark?: string
  menuIds?: string[]
  permissionIds?: string[]
  resourceIds?: string[]
  createTime?: string
  updateTime?: string
}

// 菜单类型
export interface Menu {
  id?: string
  appId?: string
  menuName: string
  parentId?: string
  orderNum?: number
  path?: string
  component?: string
  query?: string
  isFrame?: number
  isCache?: number
  menuType: string
  visible?: number
  status?: number
  perms?: string
  icon?: string
  remark?: string
  children?: Menu[]
  createTime?: string
  updateTime?: string
}

// 权限类型
export interface Permission {
  id?: string
  permissionName: string
  permissionKey: string
  apiPath?: string
  httpMethod?: string
  permissionType?: number
  status?: number
  remark?: string
}

// 部门类型
export interface Dept {
  id?: string
  deptName: string
  parentId?: string
  orderNum?: number
  leader?: string
  phone?: string
  email?: string
  status?: number
  remark?: string
  children?: Dept[]
  createTime?: string
  updateTime?: string
}

// 字典类型
export interface Dict {
  id?: string
  appId?: string
  dictName: string
  dictType: string
  status?: number
  remark?: string
  createTime?: string
  updateTime?: string
}

// 字典数据类型
export interface DictData {
  id?: string
  appId?: string
  dictSort?: number
  dictLabel: string
  dictValue: string
  dictType: string
  cssClass?: string
  listClass?: string
  isDefault?: number
  status?: number
  remark?: string
}

// 登录日志类型
export interface LoginLog {
  id?: string
  userId?: string
  username?: string
  ipAddress?: string
  loginLocation?: string
  browser?: string
  os?: string
  status?: number
  msg?: string
  loginTime?: string
}

// 操作日志类型
export interface OperationLog {
  id?: string
  userId?: string
  username?: string
  operation?: string
  method?: string
  params?: string
  time?: number
  ipAddress?: string
  location?: string
  status?: number
  errorMsg?: string
  operationTime?: string
}

// 在线用户类型
export interface OnlineUser {
  userId?: string
  username?: string
  nickname?: string
  deptName?: string
  loginIp?: string
  loginLocation?: string
  browser?: string
  os?: string
  loginTime?: string
  lastAccessTime?: string
  token?: string
}

// 服务器信息类型
export interface ServerInfo {
  serverName?: string
  serverIp?: string
  osName?: string
  osVersion?: string
  osArch?: string
  javaVersion?: string
  javaVendor?: string
  jvmName?: string
  jvmVersion?: string
  jvmVendor?: string
  totalMemory?: string
  usedMemory?: string
  freeMemory?: string
  memoryUsage?: string
  processorCount?: number
  systemLoad?: string
  uptime?: string
}

// 应用信息类型
export interface Application {
  id?: string
  appName: string
  appCode: string
  appType: string
  appIcon?: string
  appUrl?: string
  status?: number
  orderNum?: number
  remark?: string
  createTime?: string
  updateTime?: string
}

// 应用资源类型
export interface ApplicationResource {
  id?: string
  appId: string
  resourceName: string
  parentId?: string
  resourceType: string
  path?: string
  component?: string
  perms?: string
  icon?: string
  visible?: number
  openType?: string
  orderNum?: number
  status?: number
  remark?: string
  children?: ApplicationResource[]
  appName?: string
  createTime?: string
  updateTime?: string
}
