import request from '@/api/request'

/**
 * 可观测性 - 指标相关 API
 */

const BASE_URL = '/api/metrics'

// 指标查询请求类型
export interface MetricsQueryRequest {
  metricName: string
  tags?: string
  startTime?: string
  endTime?: string
  aggregation?: string
  step?: number
}

export interface SystemOverview {
  memory?: {
    total?: number
    free?: number
    used?: number
    max?: number
  }
  system?: {
    processors?: number
    osName?: string
    osVersion?: string
    javaVersion?: string
  }
  metricsCount?: number
}

// 查询指标数据
export const queryMetrics = (data: MetricsQueryRequest) => {
  return request.post<Record<string, any>>(`${BASE_URL}/query`, data)
}

// 获取所有可用指标
export const getAvailableMetrics = () => {
  return request.get<string[]>(`${BASE_URL}/available`)
}

// 获取系统概览
export const getSystemOverview = () => {
  return request.get<SystemOverview>(`${BASE_URL}/overview`)
}
