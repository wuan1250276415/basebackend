import request from '@/utils/request'

/**
 * Webhook配置API
 */

// 分页查询Webhook配置
export const getWebhookConfigPage = (params: {
  page?: number
  size?: number
  name?: string
}) => {
  return request.get('/messaging/webhook/page', { params })
}

// 获取Webhook配置详情
export const getWebhookConfig = (id: number) => {
  return request.get(`/messaging/webhook/${id}`)
}

// 创建Webhook配置
export const createWebhookConfig = (data: any) => {
  return request.post('/messaging/webhook', data)
}

// 更新Webhook配置
export const updateWebhookConfig = (id: number, data: any) => {
  return request.put(`/messaging/webhook/${id}`, data)
}

// 删除Webhook配置
export const deleteWebhookConfig = (id: number) => {
  return request.delete(`/messaging/webhook/${id}`)
}

// 启用/禁用Webhook
export const toggleWebhookConfig = (id: number, enabled: boolean) => {
  return request.put(`/messaging/webhook/${id}/toggle`, null, {
    params: { enabled }
  })
}

// 获取所有启用的Webhook配置
export const getEnabledWebhookConfigs = () => {
  return request.get('/messaging/webhook/enabled')
}
