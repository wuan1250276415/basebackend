import request from '@/utils/request'

/**
 * 死信处理API
 */

// 分页查询死信
export const getDeadLetterPage = (params: {
  page?: number
  size?: number
  status?: string
}) => {
  return request.get('/messaging/dead-letter/page', { params })
}

// 获取死信详情
export const getDeadLetter = (id: number) => {
  return request.get(`/messaging/dead-letter/${id}`)
}

// 重新投递死信
export const redeliverDeadLetter = (id: number) => {
  return request.post(`/messaging/dead-letter/${id}/redeliver`)
}

// 丢弃死信
export const discardDeadLetter = (id: number) => {
  return request.post(`/messaging/dead-letter/${id}/discard`)
}

// 批量重新投递
export const batchRedeliverDeadLetters = (ids: number[]) => {
  return request.post('/messaging/dead-letter/batch-redeliver', ids)
}
