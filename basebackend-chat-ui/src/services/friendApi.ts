import http, { request } from './api';
import type {
  ApiResult,
  PageResult,
  Friend,
  FriendGroup,
  FriendRequest,
  BlacklistUser,
} from '@/types';

/** 获取好友列表 */
export function getFriends(groupId?: number) {
  return request<Friend[]>(
    http.get<ApiResult<Friend[]>>('/friends', { params: groupId ? { groupId } : {} }),
  );
}

/** 发送好友申请（后端字段为 toUserId） */
export function sendFriendRequest(data: { toUserId: number; message?: string; source?: number }) {
  return request<{ requestId: number }>(
    http.post<ApiResult<{ requestId: number }>>('/friends/request', data),
  );
}

/** 处理好友申请 */
export function handleFriendRequest(requestId: number, data: { action: 'accept' | 'reject'; remark?: string; groupId?: number }) {
  return request<null>(
    http.put<ApiResult<null>>(`/friends/request/${requestId}`, data),
  );
}

/** 获取好友申请列表 */
export function getFriendRequests(pageNum = 1, pageSize = 20) {
  return request<PageResult<FriendRequest>>(
    http.get<ApiResult<PageResult<FriendRequest>>>('/friends/request/list', { params: { pageNum, pageSize } }),
  );
}

/** 修改好友备注 */
export function updateFriendRemark(friendUserId: number, remark: string) {
  return request<null>(
    http.put<ApiResult<null>>(`/friends/${friendUserId}/remark`, { remark }),
  );
}

/** 删除好友 */
export function deleteFriend(friendUserId: number) {
  return request<null>(
    http.delete<ApiResult<null>>(`/friends/${friendUserId}`),
  );
}

/** 获取好友分组列表 */
export function getFriendGroups() {
  return request<FriendGroup[]>(
    http.get<ApiResult<FriendGroup[]>>('/friends/groups'),
  );
}

/** 创建好友分组 */
export function createFriendGroup(data: { name: string; sortOrder: number }) {
  return request<FriendGroup>(
    http.post<ApiResult<FriendGroup>>('/friends/groups', data),
  );
}

/** 拉黑用户 */
export function blockUser(data: { blockedId: number; reason?: string }) {
  return request<null>(
    http.post<ApiResult<null>>('/blacklist', data),
  );
}

/** 取消拉黑 */
export function unblockUser(userId: number) {
  return request<null>(
    http.delete<ApiResult<null>>(`/blacklist/${userId}`),
  );
}

/** 获取黑名单 */
export function getBlacklist() {
  return request<BlacklistUser[]>(
    http.get<ApiResult<BlacklistUser[]>>('/blacklist'),
  );
}
