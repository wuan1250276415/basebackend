import request from '@/api/request'

/**
 * 可观测性 - 追踪相关 API
 */

const BASE_URL = '/api/traces'

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

export interface TraceItem {
  traceId: string
  serviceName: string
  operationName: string
  startTime: number
  duration: number
  spanCount: number
  status?: string
}

export interface TraceDetail extends TraceItem {
  spans: Record<string, any>[]
}

export interface TraceSearchResult {
  traces: TraceItem[]
  total: number
}

export interface TraceStats {
  serviceName?: string
  hours: number
  totalTraces: number
  errorTraces: number
  errorRate: number
  avgDuration: number
  maxDuration: number
  minDuration: number
  startTime: number
  endTime: number
}

const toMillis = (value?: string) => {
  if (!value) return undefined
  const parsed = Date.parse(value)
  return Number.isNaN(parsed) ? undefined : parsed
}

const normalizeTrace = (trace: Record<string, any>): TraceDetail => ({
  traceId: String(trace.traceId ?? ''),
  serviceName: String(trace.serviceName ?? trace.rootServiceName ?? 'unknown'),
  operationName: String(trace.operationName ?? trace.rootTraceName ?? 'unknown'),
  startTime: Number(trace.startTime ?? trace.startTimeUnixNano ?? Date.now()),
  duration: Number(trace.duration ?? trace.durationMs ?? 0),
  spanCount: Number(trace.spanCount ?? 0),
  status: trace.status ? String(trace.status) : undefined,
  spans: Array.isArray(trace.spans) ? trace.spans : [],
})

// 根据 TraceId 查询追踪详情
export const getTraceById = async (traceId: string): Promise<TraceDetail> => {
  const trace = await request.get<Record<string, any>>(`${BASE_URL}/${traceId}`) as unknown as Record<string, any>
  return normalizeTrace(trace)
}

// 搜索追踪
export const searchTraces = async (data: TraceQueryRequest): Promise<TraceSearchResult> => {
  const result = await request.post<{
    traces?: Record<string, any>[]
    total?: number
  }>(`${BASE_URL}/search`, {
    serviceName: data.serviceName,
    operationName: data.operationName,
    startTime: toMillis(data.startTime),
    endTime: toMillis(data.endTime),
    minDuration: data.minDuration,
    maxDuration: data.maxDuration,
    limit: data.limit,
  }) as unknown as {
    traces?: Record<string, any>[]
    total?: number
  }

  const traces = Array.isArray(result.traces) ? result.traces.map(normalizeTrace) : []

  return {
    traces,
    total: typeof result.total === 'number' ? result.total : traces.length,
  }
}

// 获取服务列表
export const getServices = () => {
  return request.get<string[]>(`${BASE_URL}/services`)
}

// 获取追踪统计
export const getTraceStats = (serviceName?: string, hours?: number) => {
  return request.get<TraceStats>(`${BASE_URL}/stats`, {
    params: { serviceName, hours }
  })
}
