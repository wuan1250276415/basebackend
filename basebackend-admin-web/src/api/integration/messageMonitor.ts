import request from '@/utils/request'

/**
 * 消息监控API
 */

// 获取消息统计
export const getMessageStatistics = () => {
  return request.get('/messaging/monitor/statistics')
}

// 获取队列监控信息
export const getQueueMonitor = () => {
  return request.get('/messaging/monitor/queue')
}
