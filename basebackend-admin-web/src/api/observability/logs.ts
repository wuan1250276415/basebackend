import request from '@/utils/request'

/**
 * 可观测性 - 日志相关 API
 */

// 日志查询请求类型
export interface LogQueryRequest {
  keyword?: string
  level?: string
  traceId?: string
  application?: string
  startTime?: string
  endTime?: string
  limit?: number
}

// 查询日志
export const queryLogs = (data: LogQueryRequest) => {
  return request.post('/observability/logs/query', data)
}

// 获取日志统计
export const getLogStats = (startTime?: string, endTime?: string) => {
  return request.get('/observability/logs/stats', {
    params: { startTime, endTime }
  })
}

// 根据 TraceId 查询日志
export const getLogsByTraceId = (traceId: string) => {
  return request.get(`/observability/logs/trace/${traceId}`)
}
