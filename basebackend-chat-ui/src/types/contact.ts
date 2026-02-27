import { OnlineStatus, FriendRequestStatus } from './enums';

/** 好友信息 */
export interface Friend {
  userId: number;
  nickname: string;
  remark: string | null;
  avatar: string;
  status: OnlineStatus;
  groupId: number;
  groupName: string;
}

/** 好友分组 */
export interface FriendGroup {
  groupId: number;
  name: string;
  sortOrder: number;
  count?: number;
}

/** 好友申请 */
export interface FriendRequest {
  requestId: number;
  fromUser: {
    userId: number;
    nickname: string;
    avatar: string;
  };
  message: string;
  source: number;
  status: FriendRequestStatus;
  createTime: string;
  expireTime: string;
}

/** 黑名单用户 */
export interface BlacklistUser {
  userId: number;
  nickname: string;
  avatar: string;
  reason: string;
  blockedTime: string;
}

/** 用户简要信息 */
export interface UserInfo {
  userId: number;
  nickname: string;
  avatar: string;
  status?: OnlineStatus;
}
