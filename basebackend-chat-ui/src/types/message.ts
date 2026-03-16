import { MessageType, MessageStatus } from './enums';

/** 消息实体 */
export interface Message {
  messageId: number;
  conversationId: number;
  senderId: number;
  senderName: string;
  senderAvatar: string;
  type: MessageType;
  content: string | null;
  extra: MessageExtra | null;
  quoteMessage: QuoteMessage | null;
  atUserIds: number[] | null;
  clientMsgId: string;
  sendTime: string;
  status: MessageStatus;
}

/** 消息附加信息（图片/文件/语音/视频等） */
export interface MessageExtra {
  url?: string;
  thumbnailUrl?: string;
  fileName?: string;
  width?: number;
  height?: number;
  size?: number;
  format?: string;
  duration?: number;
  /** 合并转发摘要 */
  title?: string;
  messageList?: Message[];
}

/** 引用消息 */
export interface QuoteMessage {
  messageId: number;
  senderId: number;
  senderName: string;
  type: MessageType;
  content: string | null;
}

/** 发送消息请求 */
export interface SendMessageRequest {
  conversationId: number;
  type: MessageType;
  content: string;
  clientMsgId: string;
  quoteMessageId?: number | null;
  atUserIds?: number[] | null;
  extra?: MessageExtra | null;
}

/** 历史消息响应 */
export interface HistoryMessagesResponse {
  messages: Message[];
  hasMore: boolean;
}
