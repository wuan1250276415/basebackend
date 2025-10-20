import request from '@/utils/request'

/**
 * Webhook调用日志API
 */

// 分页查询Webhook调用日志
export const getWebhookLogPage = (params: {
  page?: number
  size?: number
  webhookId?: number
  eventType?: string
  success?: boolean
  startTime?: string
  endTime?: string
}) => {
  return request.get('/messaging/webhook-log/page', { params })
}

// 获取Webhook调用日志详情
export const getWebhookLog = (id: number) => {
  return request.get(`/messaging/webhook-log/${id}`)
}

/**
 * 事件发布API
 */

// 发布事件
export const publishEvent = (data: {
  eventType: string
  data: any
  source?: string
  metadata?: Record<string, any>
}) => {
  return request.post('/messaging/event/publish', data)
}
