/**
 * 通知轮询 Hook
 * 智能轮询未读通知数量，并提供 Toast 和浏览器通知
 */

import { useEffect, useRef } from 'react'
import { useQuery, useQueryClient } from 'react-query'
import { getUnreadCount } from '@/api/notification'
import { useNotificationStore } from '@/stores/notification'
import {
  showNotificationToast,
  showBrowserNotification,
  isPageVisible,
} from '@/utils/notification'

export const useNotificationPolling = () => {
  const queryClient = useQueryClient()
  const {
    unreadCount: storedCount,
    setUnreadCount,
    pollingEnabled,
    sseConnected,
  } = useNotificationStore()

  const prevCountRef = useRef(storedCount)

  // 轮询未读数量（仅在 SSE 未连接且轮询启用时）
  const { data: unreadCount = 0, isLoading } = useQuery(
    'notification-unread-count',
    getUnreadCount,
    {
      enabled: pollingEnabled && !sseConnected,
      refetchInterval: () => {
        // 页面可见时 30 秒，隐藏时 60 秒
        return isPageVisible() ? 30000 : 60000
      },
      refetchIntervalInBackground: true,
      onSuccess: (count) => {
        setUnreadCount(count)
      },
    }
  )

  // 监听页面可见性变化
  useEffect(() => {
    const handleVisibilityChange = () => {
      if (isPageVisible() && !sseConnected) {
        // 页面变为可见时，立即刷新
        queryClient.invalidateQueries('notification-unread-count')
        queryClient.invalidateQueries('notification-list')
      }
    }

    document.addEventListener('visibilitychange', handleVisibilityChange)
    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange)
    }
  }, [queryClient, sseConnected])

  // 检测新通知
  useEffect(() => {
    const currentCount = unreadCount
    const prevCount = prevCountRef.current

    if (currentCount > prevCount) {
      const newCount = currentCount - prevCount
      const message = `您有 ${newCount} 条新通知`

      // Toast 提示
      showNotificationToast(message)

      // 浏览器通知（仅在页面不可见时）
      if (!isPageVisible()) {
        showBrowserNotification('新通知', {
          body: message,
          onClick: () => {
            window.location.href = '/notification/center'
          },
        })
      }
    }

    prevCountRef.current = currentCount
  }, [unreadCount])

  return {
    unreadCount,
    isLoading,
    refresh: () => queryClient.invalidateQueries('notification-unread-count'),
  }
}
