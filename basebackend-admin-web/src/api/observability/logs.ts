import request from '@/api/request'

/**
 * 可观测性 - 日志相关 API
 */

const BASE_URL = '/api/logs'

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

export interface LogEntry {
  timestamp: string
  service: string
  level: string
  message: string
  thread?: string
  logger?: string
}

export interface LogSearchResult {
  logs: LogEntry[]
  total: number
}

export interface LogStats {
  serviceName?: string
  hours: number
  totalLogs: number
  errorLogs: number
  warnLogs: number
  infoLogs: number
}

const toMillis = (value?: string) => {
  if (!value) return undefined
  const parsed = Date.parse(value)
  return Number.isNaN(parsed) ? undefined : parsed
}

const normalizeLog = (log: Record<string, any>): LogEntry => ({
  timestamp: String(log.timestamp ?? ''),
  service: String(log.service ?? log.serviceName ?? 'unknown'),
  level: String(log.level ?? 'INFO').toUpperCase(),
  message: String(log.message ?? log.line ?? ''),
  thread: log.thread ? String(log.thread) : undefined,
  logger: log.logger ? String(log.logger) : undefined,
})

// 查询日志
export const queryLogs = async (data: LogQueryRequest): Promise<LogSearchResult> => {
  const keyword = [data.keyword, data.traceId].filter(Boolean).join(' ').trim() || undefined
  const result = await request.post<{
    logs?: Record<string, any>[]
    total?: number
  }>(`${BASE_URL}/search`, {
    serviceName: data.application,
    level: data.level,
    keyword,
    startTime: toMillis(data.startTime),
    endTime: toMillis(data.endTime),
    limit: data.limit,
    sort: 'desc',
  }) as unknown as {
    logs?: Record<string, any>[]
    total?: number
  }

  const logs = Array.isArray(result.logs) ? result.logs.map(normalizeLog) : []
  return {
    logs,
    total: typeof result.total === 'number' ? result.total : logs.length,
  }
}

// 获取日志统计
export const getLogStats = (serviceName?: string, hours?: number) => {
  return request.get<LogStats>(`${BASE_URL}/stats`, {
    params: { serviceName, hours }
  })
}

// 根据 TraceId 查询日志
export const getLogsByTraceId = (traceId: string) => {
  return queryLogs({ traceId, limit: 100 })
}
