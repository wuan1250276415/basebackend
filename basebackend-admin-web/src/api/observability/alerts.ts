import request from '@/utils/request'

/**
 * 可观测性 - 告警相关 API
 */

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

// 注册告警规则
export const registerAlertRule = (data: AlertRule) => {
  return request.post('/observability/alerts/rules', data)
}

// 删除告警规则
export const deleteAlertRule = (ruleId: number) => {
  return request.delete(`/observability/alerts/rules/${ruleId}`)
}

// 获取所有告警规则
export const getAllAlertRules = () => {
  return request.get('/observability/alerts/rules')
}

// 获取最近的告警事件
export const getRecentAlerts = () => {
  return request.get('/observability/alerts/events')
}

// 测试告警规则
export const testAlertRule = (data: AlertRule) => {
  return request.post('/observability/alerts/rules/test', data)
}

// 获取告警统计
export const getAlertStats = () => {
  return request.get('/observability/alerts/stats')
}
