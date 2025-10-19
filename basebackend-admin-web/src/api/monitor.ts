import request from '@/utils/request'
import { OnlineUser, ServerInfo, Result } from '@/types'

// ============ 在线用户接口 ============

// 获取在线用户列表
export const getOnlineUsers = () => {
  return request.get<Result<OnlineUser[]>>('/admin/monitor/online')
}

// 强制下线用户
export const forceLogout = (token: string) => {
  return request.delete<Result<string>>(`/admin/monitor/online/${token}`)
}

// ============ 服务器监控接口 ============

// 获取服务器信息
export const getServerInfo = () => {
  return request.get<Result<ServerInfo>>('/admin/monitor/server')
}
