import request from '@/api/request'

/**
 * 可观测性 - 告警相关 API
 */

const BASE_URL = '/api/alerts'
const OPERATOR_TO_BACKEND: Record<string, string> = {
  '>': 'gt',
  '>=': 'gte',
  '<': 'lt',
  '<=': 'lte',
  '==': 'eq',
}

const OPERATOR_TO_FRONTEND: Record<string, string> = {
  gt: '>',
  gte: '>=',
  lt: '<',
  lte: '<=',
  eq: '==',
}

const normalizeSeverity = (severity?: string) =>
  String(severity ?? 'INFO').toUpperCase() as AlertRule['severity']

// 告警规则类型
export interface AlertRule {
  id?: number
  ruleName: string
  ruleType: 'THRESHOLD' | 'LOG' | 'CUSTOM'
  metricName?: string
  thresholdValue?: number
  comparisonOperator?: string
  durationSeconds?: number
  severity: 'INFO' | 'WARNING' | 'ERROR' | 'CRITICAL'
  enabled: boolean
  notifyChannels: string
  description?: string
}

// 告警事件类型
export interface AlertEvent {
  id?: number
  ruleId: number
  ruleName: string
  severity: string
  message: string
  triggerValue: string
  thresholdValue: string
  alertTime: string
  notifyStatus: string
  status: string
}

export interface AlertStats {
  totalRules: number
  activeRules: number
  totalEvents: number
  recentEvents24h: number
}

const toRulePayload = (data: AlertRule) => ({
  ruleName: data.ruleName,
  description: data.description,
  metricName: data.metricName,
  threshold: data.thresholdValue,
  operator: OPERATOR_TO_BACKEND[data.comparisonOperator ?? '>'] ?? 'gt',
  duration: data.durationSeconds,
  severity: String(data.severity).toLowerCase(),
  enabled: data.enabled,
  notificationChannels: data.notifyChannels,
})

const fromRulePayload = (rule: Record<string, any>): AlertRule => ({
  id: rule.id ? Number(rule.id) : undefined,
  ruleName: String(rule.ruleName ?? ''),
  ruleType: 'THRESHOLD',
  metricName: rule.metricName ? String(rule.metricName) : undefined,
  thresholdValue: rule.threshold != null ? Number(rule.threshold) : undefined,
  comparisonOperator: OPERATOR_TO_FRONTEND[String(rule.operator ?? 'gt')] ?? '>',
  durationSeconds: rule.duration != null ? Number(rule.duration) : undefined,
  severity: normalizeSeverity(rule.severity),
  enabled: Boolean(rule.enabled),
  notifyChannels: String(rule.notificationChannels ?? ''),
  description: rule.description ? String(rule.description) : undefined,
})

const fromEventPayload = (event: Record<string, any>): AlertEvent => ({
  id: event.id ? Number(event.id) : undefined,
  ruleId: event.ruleId ? Number(event.ruleId) : 0,
  ruleName: String(event.ruleName ?? ''),
  severity: normalizeSeverity(event.severity),
  message: String(event.message ?? ''),
  triggerValue: event.currentValue != null ? String(event.currentValue) : '-',
  thresholdValue: event.threshold != null ? String(event.threshold) : '-',
  alertTime: String(event.firedAt ?? event.createTime ?? ''),
  notifyStatus: 'UNKNOWN',
  status: String(event.status ?? '').toUpperCase(),
})

// 注册告警规则
export const registerAlertRule = (data: AlertRule) => {
  return request.post<void>(`${BASE_URL}/rules`, toRulePayload(data))
}

// 删除告警规则
export const deleteAlertRule = (ruleId: number) => {
  return request.delete<void>(`${BASE_URL}/rules/${ruleId}`)
}

// 获取所有告警规则
export const getAllAlertRules = async () => {
  const rules = await request.get<Record<string, any>[]>(`${BASE_URL}/rules`)
  return Array.isArray(rules) ? rules.map(fromRulePayload) : []
}

// 获取最近的告警事件
export const getRecentAlerts = async () => {
  const events = await request.get<Record<string, any>[]>(`${BASE_URL}/events`)
  return Array.isArray(events) ? events.map(fromEventPayload) : []
}

// 测试告警规则
export const testAlertRule = (data: AlertRule) => {
  return request.post<Record<string, any>>(`${BASE_URL}/rules/test`, toRulePayload(data))
}

// 获取告警统计
export const getAlertStats = () => {
  return request.get<AlertStats>(`${BASE_URL}/stats`)
}
