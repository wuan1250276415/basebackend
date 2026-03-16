import { create } from 'zustand';
import type { WsServerFrame } from '@/types';
import { WsFrameType, MessageType, MessageStatus } from '@/types';
import { chatWs } from '@/services/websocket';
import { useConversationStore } from './useConversationStore';
import { useMessageStore } from './useMessageStore';

interface WebSocketState {
  /** 当前连接状态 */
  connected: boolean;
  /** 当前用户 ID（连接后由服务端返回） */
  connectedUserId: number | null;
  /** 正在输入的用户 {conversationId: {userId, nickname, timer}} */
  typingUsers: Record<number, { userId: number; nickname: string }>;

  /** 连接 WebSocket */
  connect: (token: string) => void;
  /** 断开 */
  disconnect: () => void;
  /** 处理服务端帧 */
  handleFrame: (frame: WsServerFrame) => void;
}

export const useWebSocketStore = create<WebSocketState>((set, get) => ({
  connected: false,
  connectedUserId: null,
  typingUsers: {},

  connect: (token) => {
    const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${wsProtocol}//${window.location.host}/ws/chat`;

    chatWs.connect({
      url: wsUrl,
      token,
      deviceId: 'web-' + Date.now(),
      onMessage: (frame) => get().handleFrame(frame),
      onStatusChange: (connected) => set({ connected }),
    });
  },

  disconnect: () => {
    chatWs.disconnect();
    set({ connected: false, connectedUserId: null });
  },

  handleFrame: (frame) => {
    switch (frame.type) {
      case WsFrameType.CONNECTED:
        set({ connectedUserId: frame.userId });
        break;

      case WsFrameType.CHAT: {
        // 收到新消息
        const msg = {
          messageId: frame.messageId,
          conversationId: frame.conversationId,
          senderId: frame.senderId,
          senderName: frame.senderName,
          senderAvatar: frame.senderAvatar,
          type: frame.msgType as MessageType,
          content: frame.content,
          extra: frame.extra as null,
          quoteMessage: frame.quoteMessage as null,
          atUserIds: frame.atUserIds ?? null,
          clientMsgId: '',
          sendTime: frame.sendTime,
          status: MessageStatus.SENT,
        };
        useMessageStore.getState().addMessage(frame.conversationId, msg);
        // 更新会话列表
        const convStore = useConversationStore.getState();
        convStore.updateConversation(frame.conversationId, {
          lastMessage: {
            messageId: frame.messageId,
            type: frame.msgType,
            content: frame.content ?? '[消息]',
            senderName: frame.senderName,
            sendTime: frame.sendTime,
          },
          unreadCount:
            (convStore.conversations.find((c) => c.conversationId === frame.conversationId)
              ?.unreadCount ?? 0) +
            (frame.conversationId !== convStore.activeConversationId ? 1 : 0),
        });
        convStore.moveToTop(frame.conversationId);
        break;
      }

      case WsFrameType.CHAT_ACK: {
        // 更新本地消息状态
        const convId = useConversationStore.getState().activeConversationId;
        if (convId != null) {
          useMessageStore.getState().updateMessage(convId, frame.clientMsgId, {
            messageId: frame.messageId,
            sendTime: frame.sendTime,
            status: frame.status as MessageStatus,
          });
        }
        break;
      }

      case WsFrameType.REVOKE:
        useMessageStore.getState().revokeMessage(frame.conversationId, frame.messageId);
        break;

      case WsFrameType.READ_RECEIPT:
        // 可扩展：更新已读状态
        break;

      case WsFrameType.TYPING: {
        if (frame.userId && frame.nickname) {
          set((state) => ({
            typingUsers: {
              ...state.typingUsers,
              [frame.conversationId]: { userId: frame.userId!, nickname: frame.nickname! },
            },
          }));
          // 3 秒后自动清除
          setTimeout(() => {
            set((state) => {
              const copy = { ...state.typingUsers };
              delete copy[frame.conversationId];
              return { typingUsers: copy };
            });
          }, 3000);
        }
        break;
      }

      case WsFrameType.CONVERSATION_UPDATE:
        useConversationStore.getState().updateConversation(frame.conversationId, {
          lastMessage: frame.lastMessage,
          unreadCount: frame.unreadCount,
        });
        useConversationStore.getState().moveToTop(frame.conversationId);
        break;

      case WsFrameType.PRESENCE:
        // 在线状态变更可通过 usePresenceStore 处理
        break;

      case WsFrameType.ERROR:
        console.error('[WS Error]', frame.code, frame.message);
        break;

      default:
        break;
    }
  },
}));
