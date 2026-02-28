/**
 * 聊天管理类型定义
 * 对应后端 ChatMessage、GroupVO、GroupMemberVO
 */

/** 聊天消息，对应后端 ChatMessage 实体 */
export interface ChatMessage {
  /** 消息ID */
  id: number;
  /** 会话ID */
  conversationId: number;
  /** 发送者ID */
  senderId: number;
  /** 发送者名称 */
  senderName?: string;
  /** 消息类型（TEXT, IMAGE, FILE 等） */
  messageType: string;
  /** 消息内容 */
  content: string;
  /** 状态 */
  status: number;
  /** 发送时间 */
  sendTime: string;
}

/** 聊天群组，对应后端 GroupVO */
export interface ChatGroup {
  /** 群组ID */
  id: number;
  /** 群组名称 */
  groupName: string;
  /** 群主ID */
  ownerId: number;
  /** 群主名称 */
  ownerName?: string;
  /** 成员数量 */
  memberCount: number;
  /** 最大成员数 */
  maxMembers: number;
  /** 状态 */
  status: number;
  /** 创建时间 */
  createTime: string;
}

/** 群组成员，对应后端 GroupMemberVO */
export interface ChatGroupMember {
  /** 用户ID */
  userId: number;
  /** 用户名 */
  username: string;
  /** 昵称 */
  nickname: string;
  /** 头像 */
  avatar: string;
  /** 角色（GroupRole 枚举值） */
  role: number;
  /** 加入时间 */
  joinTime: string;
}
