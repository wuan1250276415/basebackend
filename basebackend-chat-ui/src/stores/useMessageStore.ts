import { create } from 'zustand';
import type { Message } from '@/types';
import { MessageStatus } from '@/types';

interface MessageState {
  /** 按 conversationId 分桶存储消息列表 */
  messageMap: Record<number, Message[]>;
  /** 每个会话是否还有更多历史消息 */
  hasMoreMap: Record<number, boolean>;
  /** 正在加载的会话 */
  loadingSet: Set<number>;

  /** 设置某个会话的消息 */
  setMessages: (conversationId: number, messages: Message[], hasMore: boolean) => void;
  /** 追加历史消息（插入到头部） */
  prependMessages: (conversationId: number, messages: Message[], hasMore: boolean) => void;
  /** 添加新到达的消息（追加到尾部） */
  addMessage: (conversationId: number, message: Message) => void;
  /** 更新消息（ACK 回填 ID、状态变更等） */
  updateMessage: (conversationId: number, clientMsgId: string, partial: Partial<Message>) => void;
  /** 撤回消息 */
  revokeMessage: (conversationId: number, messageId: number) => void;
  /** 设置加载状态 */
  setLoading: (conversationId: number, loading: boolean) => void;
  /** 获取某会话最后一条消息 ID，用于重连同步 */
  getLastMessageId: (conversationId: number) => number | undefined;
}

export const useMessageStore = create<MessageState>((set, get) => ({
  messageMap: {},
  hasMoreMap: {},
  loadingSet: new Set(),

  setMessages: (conversationId, messages, hasMore) =>
    set((state) => ({
      messageMap: { ...state.messageMap, [conversationId]: messages },
      hasMoreMap: { ...state.hasMoreMap, [conversationId]: hasMore },
    })),

  prependMessages: (conversationId, messages, hasMore) =>
    set((state) => {
      const existing = state.messageMap[conversationId] ?? [];
      return {
        messageMap: { ...state.messageMap, [conversationId]: [...messages, ...existing] },
        hasMoreMap: { ...state.hasMoreMap, [conversationId]: hasMore },
      };
    }),

  addMessage: (conversationId, message) =>
    set((state) => {
      const existing = state.messageMap[conversationId] ?? [];
      // 客户端去重
      if (existing.some((m) => m.messageId === message.messageId || m.clientMsgId === message.clientMsgId)) {
        return state;
      }
      return {
        messageMap: { ...state.messageMap, [conversationId]: [...existing, message] },
      };
    }),

  updateMessage: (conversationId, clientMsgId, partial) =>
    set((state) => {
      const messages = state.messageMap[conversationId];
      if (!messages) return state;
      return {
        messageMap: {
          ...state.messageMap,
          [conversationId]: messages.map((m) =>
            m.clientMsgId === clientMsgId ? { ...m, ...partial } : m,
          ),
        },
      };
    }),

  revokeMessage: (conversationId, messageId) =>
    set((state) => {
      const messages = state.messageMap[conversationId];
      if (!messages) return state;
      return {
        messageMap: {
          ...state.messageMap,
          [conversationId]: messages.map((m) =>
            m.messageId === messageId ? { ...m, status: MessageStatus.REVOKED, content: null } : m,
          ),
        },
      };
    }),

  setLoading: (conversationId, loading) =>
    set((state) => {
      const next = new Set(state.loadingSet);
      if (loading) next.add(conversationId);
      else next.delete(conversationId);
      return { loadingSet: next };
    }),

  getLastMessageId: (conversationId) => {
    const messages = get().messageMap[conversationId];
    if (!messages || messages.length === 0) return undefined;
    return messages[messages.length - 1]!.messageId;
  },
}));
