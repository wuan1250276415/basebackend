import { GroupRole, OnlineStatus } from './enums';

/** 群组信息 */
export interface Group {
  groupId: number;
  name: string;
  avatar: string;
  description: string;
  ownerId: number;
  ownerName: string;
  conversationId: number;
  maxMembers: number;
  memberCount: number;
  isMuted: boolean;
  joinMode: number;
  inviteConfirm: boolean;
  myRole: GroupRole;
  createTime: string;
}

/** 群成员 */
export interface GroupMember {
  userId: number;
  nickname: string;
  groupNickname: string | null;
  avatar: string;
  role: GroupRole;
  status: OnlineStatus;
  isMuted: boolean;
  joinTime: string;
}

/** 群公告 */
export interface GroupAnnouncement {
  id: number;
  title: string;
  content: string;
  isPinned: boolean;
  publisherId: number;
  publisherName: string;
  createTime: string;
}

/** 创建群请求 */
export interface CreateGroupRequest {
  name: string;
  avatar?: string | null;
  description?: string;
  memberIds: number[];
}
