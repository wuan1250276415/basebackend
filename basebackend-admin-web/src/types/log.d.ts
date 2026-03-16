/**
 * 日志类型定义
 * 对应后端 OperationLogDTO 和 LoginLogDTO
 */

/** 操作日志数据传输对象，对应后端 OperationLogDTO */
export interface OperationLogDTO {
  /** 日志ID */
  id: string;
  /** 用户ID */
  userId: number;
  /** 用户名 */
  username: string;
  /** 操作描述 */
  operation: string;
  /** 请求方法 */
  method: string;
  /** 请求参数 */
  params: string;
  /** 耗时（毫秒） */
  time: number;
  /** IP地址 */
  ipAddress: string;
  /** 操作地点 */
  location: string;
  /** 状态（0-失败, 1-成功） */
  status: number;
  /** 错误信息 */
  errorMsg: string;
  /** 操作时间 */
  operationTime: string;
}

/** 登录日志数据传输对象，对应后端 LoginLogDTO */
export interface LoginLogDTO {
  /** 日志ID */
  id: string;
  /** 用户ID */
  userId: number;
  /** 用户名 */
  username: string;
  /** IP地址 */
  ipAddress: string;
  /** 登录地点 */
  loginLocation: string;
  /** 浏览器 */
  browser: string;
  /** 操作系统 */
  os: string;
  /** 状态（0-失败, 1-成功） */
  status: number;
  /** 提示消息 */
  msg: string;
  /** 登录时间 */
  loginTime: string;
}
