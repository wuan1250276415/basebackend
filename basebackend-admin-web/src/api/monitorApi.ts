import request from '@/api/request';
import type { OnlineUserDTO, ServerInfoDTO, CacheInfoDTO } from '@/types';

/**
 * 系统监控 API
 * 对接后端 /api/system/monitor/* 接口
 */
export const monitorApi = {
  /** 查询在线用户列表 */
  onlineUsers: (params?: { username?: string }): Promise<OnlineUserDTO[]> =>
    request.get('/api/system/monitor/online', { params }),

  /** 强制下线用户 */
  forceLogout: (token: string): Promise<void> =>
    request.delete('/api/system/monitor/online', { data: { token } }),

  /** 获取服务器信息 */
  serverInfo: (): Promise<ServerInfoDTO> =>
    request.get('/api/system/monitor/server'),

  /** 获取缓存信息 */
  cacheInfo: (): Promise<CacheInfoDTO[]> =>
    request.get('/api/system/monitor/cache'),

  /** 获取系统统计数据 */
  systemStats: (): Promise<any> =>
    request.get('/api/system/monitor/stats'),
};

/**
 * 兼容项目内历史上的命名导出调用方式。
 * request 已经统一解包 data，这里直接返回业务数据本身。
 */
export const getOnlineUsers = monitorApi.onlineUsers;
export const forceLogout = monitorApi.forceLogout;
export const getServerInfo = monitorApi.serverInfo;
export const getCacheInfo = monitorApi.cacheInfo;
export const getSystemStats = monitorApi.systemStats;
