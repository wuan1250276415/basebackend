import { WsFrameType, MessageType } from './enums';

/** WebSocket 基础帧 */
export interface WsBaseFrame {
  type: WsFrameType;
}

/** 连接成功帧 (下行) */
export interface WsConnectedFrame extends WsBaseFrame {
  type: WsFrameType.CONNECTED;
  userId: number;
  serverTime: number;
}

/** 发送聊天消息帧 (上行) */
export interface WsSendChatFrame extends WsBaseFrame {
  type: WsFrameType.CHAT;
  clientMsgId: string;
  conversationId: number;
  msgType: MessageType;
  content: string;
  extra?: unknown;
  quoteMessageId?: number | null;
  atUserIds?: number[] | null;
}

/** 接收聊天消息帧 (下行) */
export interface WsRecvChatFrame extends WsBaseFrame {
  type: WsFrameType.CHAT;
  messageId: number;
  conversationId: number;
  senderId: number;
  senderName: string;
  senderAvatar: string;
  msgType: MessageType;
  content: string | null;
  extra?: unknown;
  quoteMessage?: unknown;
  atUserIds?: number[] | null;
  sendTime: string;
}

/** 消息确认帧 (下行) */
export interface WsChatAckFrame extends WsBaseFrame {
  type: WsFrameType.CHAT_ACK;
  clientMsgId: string;
  messageId: number;
  sendTime: string;
  status: number;
}

/** 撤回帧 */
export interface WsRevokeFrame extends WsBaseFrame {
  type: WsFrameType.REVOKE;
  messageId: number;
  conversationId: number;
  operatorId?: number;
  operatorName?: string;
  revokeTime?: string;
}

/** 已读上报帧 (上行) */
export interface WsReadFrame extends WsBaseFrame {
  type: WsFrameType.READ;
  conversationId: number;
  lastReadMessageId: number;
}

/** 已读回执帧 (下行) */
export interface WsReadReceiptFrame extends WsBaseFrame {
  type: WsFrameType.READ_RECEIPT;
  conversationId: number;
  userId: number;
  lastReadMessageId: number;
  readTime: string;
}

/** 正在输入帧 */
export interface WsTypingFrame extends WsBaseFrame {
  type: WsFrameType.TYPING;
  conversationId: number;
  userId?: number;
  nickname?: string;
}

/** 心跳帧 */
export interface WsPingFrame extends WsBaseFrame {
  type: WsFrameType.PING;
}

export interface WsPongFrame extends WsBaseFrame {
  type: WsFrameType.PONG;
  serverTime: number;
}

/** 在线状态变更帧 */
export interface WsPresenceFrame extends WsBaseFrame {
  type: WsFrameType.PRESENCE;
  userId?: number;
  status: string;
  timestamp?: number;
}

/** 增量同步帧 (上行) */
export interface WsSyncFrame extends WsBaseFrame {
  type: WsFrameType.SYNC;
  conversations: Record<string, number>;
}

/** 会话更新帧 (下行) */
export interface WsConversationUpdateFrame extends WsBaseFrame {
  type: WsFrameType.CONVERSATION_UPDATE;
  conversationId: number;
  lastMessage: {
    messageId: number;
    type: number;
    content: string;
    senderName: string;
    sendTime: string;
  };
  unreadCount: number;
}

/** 群事件帧 (下行) */
export interface WsGroupEventFrame extends WsBaseFrame {
  type: WsFrameType.GROUP_EVENT;
  event: string;
  groupId: number;
  conversationId: number;
  data: Record<string, unknown>;
  timestamp: number;
}

/** 好友事件帧 (下行) */
export interface WsFriendEventFrame extends WsBaseFrame {
  type: WsFrameType.FRIEND_EVENT;
  event: string;
  data: Record<string, unknown>;
  timestamp: number;
}

/** 错误帧 (下行) */
export interface WsErrorFrame extends WsBaseFrame {
  type: WsFrameType.ERROR;
  code: number;
  message: string;
  refClientMsgId?: string;
}

/** 所有下行帧的联合类型 */
export type WsServerFrame =
  | WsConnectedFrame
  | WsRecvChatFrame
  | WsChatAckFrame
  | WsRevokeFrame
  | WsReadReceiptFrame
  | WsTypingFrame
  | WsPongFrame
  | WsPresenceFrame
  | WsConversationUpdateFrame
  | WsGroupEventFrame
  | WsFriendEventFrame
  | WsErrorFrame;
