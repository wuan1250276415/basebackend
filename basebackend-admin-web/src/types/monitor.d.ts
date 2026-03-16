/**
 * 监控类型定义
 * 对应后端 OnlineUserDTO、ServerInfoDTO、CacheInfoDTO
 */

/** 在线用户数据传输对象，对应后端 OnlineUserDTO */
export interface OnlineUserDTO {
  /** 用户ID */
  userId: number;
  /** 用户名 */
  username: string;
  /** 昵称 */
  nickname: string;
  /** 部门名称 */
  deptName: string;
  /** 登录IP */
  loginIp: string;
  /** 登录地点 */
  loginLocation: string;
  /** 浏览器 */
  browser: string;
  /** 操作系统 */
  os: string;
  /** 登录时间 */
  loginTime: string;
  /** 最后访问时间 */
  lastAccessTime: string;
  /** 会话令牌 */
  token: string;
}

/** 服务器信息数据传输对象，对应后端 ServerInfoDTO */
export interface ServerInfoDTO {
  /** 服务器名称 */
  serverName: string;
  /** 服务器IP */
  serverIp: string;
  /** 操作系统名称 */
  osName: string;
  /** 操作系统版本 */
  osVersion: string;
  /** 系统架构 */
  osArch: string;
  /** Java版本 */
  javaVersion: string;
  /** Java厂商 */
  javaVendor: string;
  /** JVM名称 */
  jvmName: string;
  /** JVM版本 */
  jvmVersion: string;
  /** JVM厂商 */
  jvmVendor: string;
  /** 总内存 */
  totalMemory: string;
  /** 已用内存 */
  usedMemory: string;
  /** 空闲内存 */
  freeMemory: string;
  /** 内存使用率 */
  memoryUsage: string;
  /** 处理器数量 */
  processorCount: number;
  /** 系统负载 */
  systemLoad: string;
  /** 运行时间 */
  uptime: string;
}

/** 缓存信息数据传输对象，对应后端 CacheInfoDTO */
export interface CacheInfoDTO {
  /** 缓存名称 */
  cacheName: string;
  /** 缓存大小 */
  cacheSize: number;
  /** 命中次数 */
  hitCount: number;
  /** 未命中次数 */
  missCount: number;
  /** 命中率 */
  hitRate: string;
}
