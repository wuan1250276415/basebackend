/**
 * 通知工具函数
 * 包含浏览器通知、Toast 提示等功能
 */

import { message } from 'antd'

/**
 * 请求浏览器通知权限
 */
export const requestNotificationPermission = async (): Promise<boolean> => {
  if (!('Notification' in window)) {
    console.warn('此浏览器不支持桌面通知')
    return false
  }

  if (Notification.permission === 'granted') {
    return true
  }

  if (Notification.permission !== 'denied') {
    try {
      const permission = await Notification.requestPermission()
      return permission === 'granted'
    } catch (error) {
      console.error('请求通知权限失败:', error)
      return false
    }
  }

  return false
}

/**
 * 显示浏览器原生通知
 */
export const showBrowserNotification = (
  title: string,
  options?: NotificationOptions & { onClick?: () => void }
): Notification | null => {
  if (Notification.permission !== 'granted') {
    return null
  }

  try {
    const { onClick, ...notificationOptions } = options || {}

    const notification = new Notification(title, {
      icon: '/logo.png',
      badge: '/logo.png',
      requireInteraction: false,
      ...notificationOptions,
    })

    notification.onclick = () => {
      window.focus()
      onClick?.()
      notification.close()
    }

    // 3 秒后自动关闭
    setTimeout(() => notification.close(), 3000)

    return notification
  } catch (error) {
    console.error('显示浏览器通知失败:', error)
    return null
  }
}

/**
 * 显示 Toast 消息提示
 */
export const showNotificationToast = (
  title: string,
  content?: string,
  type: 'info' | 'success' | 'warning' | 'error' = 'info'
) => {
  message[type]({
    content: content ? `${title}: ${content}` : title,
    duration: 3,
  })
}

/**
 * 检查页面是否可见
 */
export const isPageVisible = (): boolean => {
  return !document.hidden
}

/**
 * 格式化通知时间（相对时间）
 */
export const formatNotificationTime = (time: string): string => {
  const now = new Date()
  const notificationTime = new Date(time)
  const diffMs = now.getTime() - notificationTime.getTime()
  const diffSeconds = Math.floor(diffMs / 1000)
  const diffMinutes = Math.floor(diffSeconds / 60)
  const diffHours = Math.floor(diffMinutes / 60)
  const diffDays = Math.floor(diffHours / 24)

  if (diffSeconds < 60) {
    return '刚刚'
  } else if (diffMinutes < 60) {
    return `${diffMinutes}分钟前`
  } else if (diffHours < 24) {
    return `${diffHours}小时前`
  } else if (diffDays < 7) {
    return `${diffDays}天前`
  } else {
    return notificationTime.toLocaleDateString()
  }
}

/**
 * 获取通知类型图标颜色
 */
export const getNotificationTypeColor = (type: string): string => {
  switch (type) {
    case 'system':
      return '#1890ff'
    case 'announcement':
      return '#52c41a'
    case 'reminder':
      return '#faad14'
    default:
      return '#8c8c8c'
  }
}

/**
 * 获取通知级别颜色
 */
export const getNotificationLevelColor = (level: string): string => {
  switch (level) {
    case 'success':
      return '#52c41a'
    case 'info':
      return '#1890ff'
    case 'warning':
      return '#faad14'
    case 'error':
      return '#ff4d4f'
    default:
      return '#8c8c8c'
  }
}

/**
 * 批量操作防抖
 */
export const debounce = <T extends (...args: any[]) => any>(
  func: T,
  wait: number
): ((...args: Parameters<T>) => void) => {
  let timeout: NodeJS.Timeout | null = null

  return (...args: Parameters<T>) => {
    if (timeout) clearTimeout(timeout)
    timeout = setTimeout(() => func(...args), wait)
  }
}
