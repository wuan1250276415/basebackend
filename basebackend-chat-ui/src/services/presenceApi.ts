import http, { request } from './api';
import type { ApiResult } from '@/types';

/** 在线状态信息 */
export interface PresenceInfo {
  status: string;
  lastActive: string | null;
}

/** 批量查询在线状态 */
export function batchGetPresence(userIds: number[]) {
  return request<Record<string, PresenceInfo>>(
    http.get<ApiResult<Record<string, PresenceInfo>>>('/presence/batch', {
      params: { userIds: userIds.join(',') },
    }),
  );
}

/** 设置当前用户在线状态 */
export function setPresence(status: string) {
  return request<null>(
    http.put<ApiResult<null>>('/presence/status', { status }),
  );
}
