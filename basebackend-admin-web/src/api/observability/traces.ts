import request from '@/utils/request'

/**
 * 可观测性 - 追踪相关 API
 */

// 追踪查询请求类型
export interface TraceQueryRequest {
  traceId?: string
  serviceName?: string
  operationName?: string
  minDuration?: number
  maxDuration?: number
  startTime?: string
  endTime?: string
  limit?: number
}

// 根据 TraceId 查询追踪详情
export const getTraceById = (traceId: string) => {
  return request.get(`/observability/traces/${traceId}`)
}

// 搜索追踪
export const searchTraces = (data: TraceQueryRequest) => {
  return request.post('/observability/traces/search', data)
}

// 获取服务列表
export const getServices = () => {
  return request.get('/observability/traces/services')
}

// 获取追踪统计
export const getTraceStats = (serviceName?: string, hours?: number) => {
  return request.get('/observability/traces/stats', {
    params: { serviceName, hours }
  })
}
