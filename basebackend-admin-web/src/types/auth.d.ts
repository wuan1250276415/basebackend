/**
 * 认证相关类型定义
 * 对应后端 LoginRequest、LoginResponse、UserContext 等 DTO
 */

/** 登录请求参数，对应后端 LoginRequest */
export interface LoginParams {
  /** 用户名 */
  username: string;
  /** 密码 */
  password: string;
  /** 验证码 */
  captcha?: string;
  /** 验证码ID */
  captchaId?: string;
  /** 记住我 */
  rememberMe?: boolean;
}

/** 登录响应结果，对应后端 LoginResponse */
export interface LoginResult {
  /** 访问令牌 */
  accessToken: string;
  /** 令牌类型 */
  tokenType: string;
  /** 过期时间（秒） */
  expiresIn: number;
  /** 用户信息 */
  userInfo: UserInfo;
  /** 权限标识列表 */
  permissions: string[];
  /** 角色标识列表 */
  roles: string[];
}

/** 用户基本信息，对应后端 LoginResponse.UserInfo */
export interface UserInfo {
  /** 用户ID */
  userId: number;
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
  /** 部门ID */
  deptId: number;
  /** 部门名称 */
  deptName: string;
  /** 用户类型 */
  userType: number;
  /** 状态（0-禁用, 1-启用） */
  status: number;
}

/** 用户上下文，对应后端 GET /api/user/auth/info 返回的 UserContext */
export interface UserContext extends UserInfo {
  /** 角色ID列表 */
  roleIds: number[];
  /** 角色标识列表 */
  roles: string[];
  /** 权限标识列表 */
  permissions: string[];
}

/** 个人资料详情，对应后端 ProfileDetailDTO */
export interface ProfileDetail extends UserInfo {
  /** 生日 */
  birthday: string;
  /** 最后登录IP */
  loginIp: string;
  /** 最后登录时间 */
  loginTime: string;
  /** 创建时间 */
  createTime: string;
}

/** 更新个人资料参数，对应后端 UpdateProfileDTO */
export interface UpdateProfileParams {
  /** 昵称 */
  nickname?: string;
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
}

/** 修改密码参数，对应后端 ChangePasswordDTO */
export interface ChangePasswordParams {
  /** 旧密码 */
  oldPassword: string;
  /** 新密码 */
  newPassword: string;
  /** 确认密码 */
  confirmPassword: string;
}
