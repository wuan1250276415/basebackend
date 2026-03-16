import http, { request } from './api';
import type { ApiResult, PageResult, Conversation, CreateConversationRequest, CreateConversationResponse } from '@/types';

/** 获取会话列表 */
export function getConversations(pageNum = 1, pageSize = 20) {
  return request<PageResult<Conversation>>(
    http.get<ApiResult<PageResult<Conversation>>>('/conversations', { params: { pageNum, pageSize } }),
  );
}

/** 创建/打开会话 */
export function createConversation(data: CreateConversationRequest) {
  return request<CreateConversationResponse>(
    http.post<ApiResult<CreateConversationResponse>>('/conversations', data),
  );
}

/** 置顶/取消置顶 */
export function pinConversation(conversationId: number, isPinned: boolean) {
  return request<null>(
    http.put<ApiResult<null>>(`/conversations/${conversationId}/pin`, { isPinned }),
  );
}

/** 免打扰 */
export function muteConversation(conversationId: number, isMuted: boolean) {
  return request<null>(
    http.put<ApiResult<null>>(`/conversations/${conversationId}/mute`, { isMuted }),
  );
}

/** 保存草稿 */
export function saveDraft(conversationId: number, draft: string) {
  return request<null>(
    http.put<ApiResult<null>>(`/conversations/${conversationId}/draft`, { draft }),
  );
}

/** 删除会话 */
export function deleteConversation(conversationId: number) {
  return request<null>(
    http.delete<ApiResult<null>>(`/conversations/${conversationId}`),
  );
}

/** 清空未读 */
export function clearUnread(conversationId: number, lastReadMessageId: number) {
  return request<{ clearedCount: number }>(
    http.put<ApiResult<{ clearedCount: number }>>(`/conversations/${conversationId}/read`, { lastReadMessageId }),
  );
}
