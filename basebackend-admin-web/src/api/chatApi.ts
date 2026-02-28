import request from '@/api/request';
import type { ChatMessage, ChatGroup, ChatGroupMember, PageResult } from '@/types';

/**
 * 聊天管理 API
 * 对接后端 /api/chat/* 接口
 */
export const chatApi = {
  /** 分页查询消息列表 */
  messageList: (params: {
    current: number;
    size: number;
    senderId?: number;
    content?: string;
    beginTime?: string;
    endTime?: string;
  }): Promise<PageResult<ChatMessage>> =>
    request.get('/api/chat/messages', { params }),

  /** 分页查询群组列表 */
  groupList: (params?: {
    current: number;
    size: number;
    groupName?: string;
    status?: number;
  }): Promise<PageResult<ChatGroup>> =>
    request.get('/api/chat/groups', { params }),

  /** 获取群组详情 */
  groupInfo: (groupId: number): Promise<ChatGroup> =>
    request.get(`/api/chat/groups/${groupId}`),

  /** 获取群组成员列表 */
  groupMembers: (groupId: number): Promise<ChatGroupMember[]> =>
    request.get(`/api/chat/groups/${groupId}/members`),

  /** 解散群组 */
  dissolveGroup: (groupId: number): Promise<void> =>
    request.delete(`/api/chat/groups/${groupId}`),

  /** 删除消息 */
  deleteMessage: (messageId: number): Promise<void> =>
    request.delete(`/api/chat/messages/${messageId}`),
};
