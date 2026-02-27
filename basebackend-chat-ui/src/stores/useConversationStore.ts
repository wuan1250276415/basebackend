import { create } from 'zustand';
import type { Conversation } from '@/types';

interface ConversationState {
  /** 会话列表 */
  conversations: Conversation[];
  /** 当前选中的会话 ID */
  activeConversationId: number | null;
  /** 是否正在加载 */
  loading: boolean;

  /** 设置会话列表 */
  setConversations: (list: Conversation[]) => void;
  /** 选中会话 */
  setActive: (id: number | null) => void;
  /** 更新单个会话（收到新消息、已读清零等） */
  updateConversation: (id: number, partial: Partial<Conversation>) => void;
  /** 将会话移到列表顶部 */
  moveToTop: (id: number) => void;
  /** 添加新会话 */
  addConversation: (conv: Conversation) => void;
  /** 删除会话 */
  removeConversation: (id: number) => void;
  /** 设置加载态 */
  setLoading: (v: boolean) => void;
}

export const useConversationStore = create<ConversationState>((set) => ({
  conversations: [],
  activeConversationId: null,
  loading: false,

  setConversations: (list) => set({ conversations: list }),

  setActive: (id) => set({ activeConversationId: id }),

  updateConversation: (id, partial) =>
    set((state) => ({
      conversations: state.conversations.map((c) =>
        c.conversationId === id ? { ...c, ...partial } : c,
      ),
    })),

  moveToTop: (id) =>
    set((state) => {
      const idx = state.conversations.findIndex((c) => c.conversationId === id);
      if (idx <= 0) return state;
      const conv = state.conversations[idx]!;
      const rest = state.conversations.filter((_, i) => i !== idx);
      return { conversations: [conv, ...rest] };
    }),

  addConversation: (conv) =>
    set((state) => {
      if (state.conversations.some((c) => c.conversationId === conv.conversationId)) return state;
      return { conversations: [conv, ...state.conversations] };
    }),

  removeConversation: (id) =>
    set((state) => ({
      conversations: state.conversations.filter((c) => c.conversationId !== id),
      activeConversationId: state.activeConversationId === id ? null : state.activeConversationId,
    })),

  setLoading: (v) => set({ loading: v }),
}));
