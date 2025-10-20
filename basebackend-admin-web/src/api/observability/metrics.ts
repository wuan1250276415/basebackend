import request from '@/utils/request'

/**
 * 可观测性 - 指标相关 API
 */

// 指标查询请求类型
export interface MetricsQueryRequest {
  metricName: string
  tags?: string
  startTime?: string
  endTime?: string
  aggregation?: string
  step?: number
}

// 查询指标数据
export const queryMetrics = (data: MetricsQueryRequest) => {
  return request.post('/observability/metrics/query', data)
}

// 获取所有可用指标
export const getAvailableMetrics = () => {
  return request.get('/observability/metrics/available')
}

// 获取系统概览
export const getSystemOverview = () => {
  return request.get('/observability/metrics/overview')
}
