import http, { request } from './api';
import type { ApiResult, Group, GroupMember, GroupAnnouncement, CreateGroupRequest } from '@/types';

/** 创建群 */
export function createGroup(data: CreateGroupRequest) {
  return request<{ groupId: number; conversationId: number; name: string; memberCount: number }>(
    http.post<ApiResult<{ groupId: number; conversationId: number; name: string; memberCount: number }>>('/groups', data),
  );
}

/** 获取群信息 */
export function getGroupInfo(groupId: number) {
  return request<Group>(
    http.get<ApiResult<Group>>(`/groups/${groupId}`),
  );
}

/** 修改群信息 */
export function updateGroup(groupId: number, data: Partial<Pick<Group, 'name' | 'avatar' | 'description' | 'joinMode' | 'inviteConfirm'>>) {
  return request<null>(
    http.put<ApiResult<null>>(`/groups/${groupId}`, data),
  );
}

/** 解散群 */
export function dismissGroup(groupId: number) {
  return request<null>(
    http.delete<ApiResult<null>>(`/groups/${groupId}`),
  );
}

/** 获取群成员列表 */
export function getGroupMembers(groupId: number) {
  return request<GroupMember[]>(
    http.get<ApiResult<GroupMember[]>>(`/groups/${groupId}/members`),
  );
}

/** 邀请入群 */
export function inviteMembers(groupId: number, userIds: number[]) {
  return request<null>(
    http.post<ApiResult<null>>(`/groups/${groupId}/members`, { userIds }),
  );
}

/** 踢出成员 */
export function kickMember(groupId: number, userId: number) {
  return request<null>(
    http.delete<ApiResult<null>>(`/groups/${groupId}/members/${userId}`),
  );
}

/** 退出群聊 */
export function leaveGroup(groupId: number) {
  return request<null>(
    http.post<ApiResult<null>>(`/groups/${groupId}/leave`),
  );
}

/** 设置角色 */
export function setMemberRole(groupId: number, userId: number, role: number) {
  return request<null>(
    http.put<ApiResult<null>>(`/groups/${groupId}/members/${userId}/role`, { role }),
  );
}

/** 获取群公告列表 */
export function getAnnouncements(groupId: number) {
  return request<GroupAnnouncement[]>(
    http.get<ApiResult<GroupAnnouncement[]>>(`/groups/${groupId}/announcements`),
  );
}

/** 发布群公告 */
export function createAnnouncement(groupId: number, data: { title: string; content: string; isPinned: boolean }) {
  return request<GroupAnnouncement>(
    http.post<ApiResult<GroupAnnouncement>>(`/groups/${groupId}/announcements`, data),
  );
}

/** 转让群主 */
export function transferOwner(groupId: number, newOwnerId: number) {
  return request<null>(
    http.put<ApiResult<null>>(`/groups/${groupId}/transfer`, { newOwnerId }),
  );
}
