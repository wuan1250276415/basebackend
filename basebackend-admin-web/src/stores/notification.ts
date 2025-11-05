/**
 * 通知全局状态管理 Store
 * 使用 Zustand 管理通知相关的全局状态
 */

import { create } from 'zustand'
import { persist } from 'zustand/middleware'

interface NotificationState {
  // 未读数量
  unreadCount: number
  // 最新通知 ID（用于检测新通知）
  latestNotificationId: number | null
  // 是否正在加载
  isLoading: boolean
  // 轮询是否启用
  pollingEnabled: boolean
  // SSE 连接状态
  sseConnected: boolean

  // Actions
  setUnreadCount: (count: number) => void
  setLatestNotificationId: (id: number) => void
  decrementUnreadCount: (count?: number) => void
  incrementUnreadCount: (count?: number) => void
  setLoading: (loading: boolean) => void
  setPollingEnabled: (enabled: boolean) => void
  setSseConnected: (connected: boolean) => void
  reset: () => void
}

const initialState = {
  unreadCount: 0,
  latestNotificationId: null,
  isLoading: false,
  pollingEnabled: true,
  sseConnected: false,
}

export const useNotificationStore = create<NotificationState>()(
  persist(
    (set) => ({
      ...initialState,

      setUnreadCount: (count) => set({ unreadCount: Math.max(0, count) }),

      setLatestNotificationId: (id) => set({ latestNotificationId: id }),

      decrementUnreadCount: (count = 1) =>
        set((state) => ({
          unreadCount: Math.max(0, state.unreadCount - count),
        })),

      incrementUnreadCount: (count = 1) =>
        set((state) => ({
          unreadCount: state.unreadCount + count,
        })),

      setLoading: (loading) => set({ isLoading: loading }),

      setPollingEnabled: (enabled) => set({ pollingEnabled: enabled }),

      setSseConnected: (connected) => set({ sseConnected: connected }),

      reset: () => set(initialState),
    }),
    {
      name: 'notification-storage',
      // 只持久化部分状态
      partialize: (state) => ({
        unreadCount: state.unreadCount,
        latestNotificationId: state.latestNotificationId,
      }),
    }
  )
)
