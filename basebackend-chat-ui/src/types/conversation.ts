import { ConversationType } from './enums';

/** 会话最后一条消息摘要 */
export interface LastMessage {
  messageId: number;
  type: number;
  content: string;
  senderName: string;
  sendTime: string;
}

/** 会话实体 */
export interface Conversation {
  conversationId: number;
  type: ConversationType;
  targetId: number;
  targetName: string;
  targetAvatar: string;
  lastMessage: LastMessage | null;
  unreadCount: number;
  isPinned: boolean;
  isMuted: boolean;
  draft: string | null;
  memberCount?: number;
  updateTime: string;
}

/** 创建/打开会话请求 */
export interface CreateConversationRequest {
  type: ConversationType;
  targetId: number;
}

/** 创建会话响应 */
export interface CreateConversationResponse {
  conversationId: number;
  type: ConversationType;
  targetId: number;
  targetName: string;
  targetAvatar: string;
  created: boolean;
}
