import http, { request } from './api';
import type { ApiResult } from '@/types';

/** 搜索结果命中项 */
export interface SearchHit {
  id: string;
  score: number;
  source: {
    messageId: number;
    conversationId: number;
    senderId: number;
    senderName: string;
    content: string;
    type: number;
    sendTime: string;
  };
  highlights: Record<string, string[]>;
}

/** 搜索结果 */
export interface SearchResult {
  hits: SearchHit[];
  totalHits: number;
  tookMs: number;
}

/** 搜索消息 */
export function searchMessages(params: {
  keyword: string;
  conversationId?: number;
  startTime?: string;
  endTime?: string;
  page?: number;
  size?: number;
}) {
  return request<SearchResult>(
    http.get<ApiResult<SearchResult>>('/search/messages', { params }),
  );
}
