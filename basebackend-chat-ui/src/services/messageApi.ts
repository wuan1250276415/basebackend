import http, { request } from './api';
import type { ApiResult, Message, HistoryMessagesResponse, SendMessageRequest } from '@/types';

/** 获取历史消息 */
export function getMessages(
  conversationId: number,
  params: { beforeId?: number; afterId?: number; limit?: number } = {},
) {
  return request<HistoryMessagesResponse>(
    http.get<ApiResult<HistoryMessagesResponse>>('/messages', {
      params: { conversationId, ...params },
    }),
  );
}

/** 发送消息（REST 备用通道） */
export function sendMessage(data: SendMessageRequest) {
  return request<{
    messageId: number;
    conversationId: number;
    sendTime: string;
    status: number;
  }>(http.post<ApiResult<{ messageId: number; conversationId: number; sendTime: string; status: number }>>('/messages', data));
}

/** 撤回消息 */
export function revokeMessage(messageId: number) {
  return request<{ messageId: number; revokeTime: string }>(
    http.post<ApiResult<{ messageId: number; revokeTime: string }>>(`/messages/${messageId}/revoke`),
  );
}

/** 转发消息 */
export function forwardMessages(data: {
  messageIds: number[];
  targetConversationIds: number[];
  forwardType: 'single' | 'merge';
  title?: string;
}) {
  return request<{ forwardedCount: number; targetCount: number }>(
    http.post<ApiResult<{ forwardedCount: number; targetCount: number }>>('/messages/forward', data),
  );
}

/** 获取消息已读详情 */
export function getReadStatus(messageId: number) {
  return request<{
    messageId: number;
    totalMembers: number;
    readCount: number;
    unreadCount: number;
    readUsers: { userId: number; nickname: string; readTime: string }[];
  }>(http.get<ApiResult<{ messageId: number; totalMembers: number; readCount: number; unreadCount: number; readUsers: { userId: number; nickname: string; readTime: string }[] }>>(`/messages/${messageId}/read-status`));
}

export type { Message };
