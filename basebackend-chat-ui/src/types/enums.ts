/** 消息类型枚举 */
export enum MessageType {
  TEXT = 1,
  IMAGE = 2,
  FILE = 3,
  AUDIO = 4,
  VIDEO = 5,
  LOCATION = 6,
  CONTACT_CARD = 7,
  EMOJI = 8,
  REVOKED = 9,
  SYSTEM = 10,
  MERGED_FORWARD = 11,
}

/** 消息状态枚举 */
export enum MessageStatus {
  SENDING = 0,
  SENT = 1,
  REVOKED = 2,
  FAILED = 3,
}

/** 会话类型枚举 */
export enum ConversationType {
  PRIVATE = 1,
  GROUP = 2,
}

/** 好友申请状态 */
export enum FriendRequestStatus {
  PENDING = 0,
  ACCEPTED = 1,
  REJECTED = 2,
  EXPIRED = 3,
}

/** 群角色 */
export enum GroupRole {
  MEMBER = 0,
  ADMIN = 1,
  OWNER = 2,
}

/** 在线状态 */
export enum OnlineStatus {
  ONLINE = 'online',
  OFFLINE = 'offline',
  BUSY = 'busy',
  AWAY = 'away',
}

/** WebSocket 帧类型 */
export enum WsFrameType {
  CHAT = 'chat',
  CHAT_ACK = 'chat_ack',
  REVOKE = 'revoke',
  READ = 'read',
  READ_RECEIPT = 'read_receipt',
  TYPING = 'typing',
  PING = 'ping',
  PONG = 'pong',
  PRESENCE = 'presence',
  SYNC = 'sync',
  SYNC_RESP = 'sync_resp',
  CONNECTED = 'connected',
  CONVERSATION_UPDATE = 'conversation_update',
  GROUP_EVENT = 'group_event',
  FRIEND_EVENT = 'friend_event',
  ERROR = 'error',
}
