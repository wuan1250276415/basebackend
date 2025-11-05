/**
 * Server-Sent Events (SSE) Hook
 * 用于实时接收服务器推送的通知
 */

import { useEffect, useRef, useState, useCallback } from 'react';
import { useQueryClient } from 'react-query';
import { useAuthStore } from '@/stores/auth';
import { useNotificationStore } from '@/stores/notification';
import {
  showNotificationToast,
  showBrowserNotification,
  isPageVisible,
} from '@/utils/notification';

interface SSEConfig {
  endpoint: string;
  autoReconnect?: boolean;
  maxReconnectAttempts?: number;
  baseReconnectDelay?: number;
}

interface NotificationEvent {
  id: number;
  title: string;
  content: string;
  type: string;
  level: string;
  linkUrl?: string;
}

/**
 * SSE 连接状态
 */
export type SSEStatus = 'connecting' | 'connected' | 'disconnected' | 'error';

/**
 * SSE Hook
 */
export const useSSE = (config: SSEConfig) => {
  const {
    endpoint,
    autoReconnect = true,
    maxReconnectAttempts = 5,
    baseReconnectDelay = 1000,
  } = config;

  const queryClient = useQueryClient();
  const { token } = useAuthStore();
  const { setUnreadCount, setSseConnected, incrementUnreadCount } = useNotificationStore();

  const [status, setStatus] = useState<SSEStatus>('disconnected');
  const [error, setError] = useState<Error | null>(null);

  const eventSourceRef = useRef<EventSource | null>(null);
  const reconnectAttemptsRef = useRef(0);
  const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  /**
   * 计算重连延迟（指数退避）
   */
  const getReconnectDelay = useCallback(() => {
    const attempt = reconnectAttemptsRef.current;
    const delay = Math.min(baseReconnectDelay * Math.pow(2, attempt), 30000);
    return delay + Math.random() * 1000; // 添加随机抖动
  }, [baseReconnectDelay]);

  /**
   * 处理连接打开
   */
  const handleOpen = useCallback(() => {
    console.log('[SSE] 连接已建立');
    setStatus('connected');
    setSseConnected(true);
    setError(null);
    reconnectAttemptsRef.current = 0;
  }, [setSseConnected]);

  /**
   * 处理新通知事件
   */
  const handleNotification = useCallback(
    (event: MessageEvent) => {
      try {
        const notification: NotificationEvent = JSON.parse(event.data);
        console.log('[SSE] 收到新通知:', notification);

        // 更新未读计数
        incrementUnreadCount();

        // 刷新通知列表
        queryClient.invalidateQueries('notification-list');
        queryClient.invalidateQueries('notification-center-list');
        queryClient.invalidateQueries('notification-unread-count');

        // Toast 提示
        showNotificationToast(notification.title, notification.content, notification.level as any);

        // 浏览器通知（仅在页面不可见时）
        if (!isPageVisible()) {
          showBrowserNotification(notification.title, {
            body: notification.content,
            tag: `notification-${notification.id}`,
            onClick: () => {
              if (notification.linkUrl) {
                window.location.href = notification.linkUrl;
              } else {
                window.location.href = '/notification/center';
              }
            },
          });
        }
      } catch (err) {
        console.error('[SSE] 解析通知数据失败:', err);
      }
    },
    [queryClient, incrementUnreadCount]
  );

  /**
   * 处理心跳事件
   */
  const handleHeartbeat = useCallback((event: MessageEvent) => {
    console.log('[SSE] 心跳:', event.data);
  }, []);

  /**
   * 处理连接错误
   */
  const handleError = useCallback(
    (event: Event) => {
      console.error('[SSE] 连接错误:', event);
      const err = new Error('SSE 连接失败');
      setError(err);
      setStatus('error');
      setSseConnected(false);

      // 关闭当前连接
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
        eventSourceRef.current = null;
      }

      // 尝试重连
      if (autoReconnect && reconnectAttemptsRef.current < maxReconnectAttempts) {
        const delay = getReconnectDelay();
        console.log(
          `[SSE] 将在 ${delay}ms 后尝试第 ${reconnectAttemptsRef.current + 1} 次重连...`
        );

        reconnectTimeoutRef.current = setTimeout(() => {
          reconnectAttemptsRef.current++;
          connect();
        }, delay);
      } else if (reconnectAttemptsRef.current >= maxReconnectAttempts) {
        console.error('[SSE] 已达到最大重连次数，停止重连');
        setStatus('disconnected');
      }
    },
    [autoReconnect, maxReconnectAttempts, getReconnectDelay, setSseConnected]
  );

  /**
   * 建立 SSE 连接
   */
  const connect = useCallback(() => {
    if (!token) {
      console.warn('[SSE] 未登录，跳过连接');
      return;
    }

    // 如果已有连接，先关闭
    if (eventSourceRef.current) {
      eventSourceRef.current.close();
    }

    setStatus('connecting');
    console.log('[SSE] 正在连接到:', endpoint);

    try {
      // 创建 EventSource 连接
      // 注意: EventSource 不支持自定义 header，需要通过 URL 参数传递 token
      const url = `${endpoint}?token=${encodeURIComponent(token)}`;
      const eventSource = new EventSource(url);

      eventSource.onopen = handleOpen;
      eventSource.onerror = handleError;

      // 监听自定义事件
      eventSource.addEventListener('notification', handleNotification);
      eventSource.addEventListener('heartbeat', handleHeartbeat);

      eventSourceRef.current = eventSource;
    } catch (err) {
      console.error('[SSE] 创建连接失败:', err);
      setError(err as Error);
      setStatus('error');
    }
  }, [token, endpoint, handleOpen, handleError, handleNotification, handleHeartbeat]);

  /**
   * 断开 SSE 连接
   */
  const disconnect = useCallback(() => {
    console.log('[SSE] 主动断开连接');

    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
      reconnectTimeoutRef.current = null;
    }

    if (eventSourceRef.current) {
      eventSourceRef.current.close();
      eventSourceRef.current = null;
    }

    setStatus('disconnected');
    setSseConnected(false);
    reconnectAttemptsRef.current = 0;
  }, [setSseConnected]);

  /**
   * 手动重连
   */
  const reconnect = useCallback(() => {
    console.log('[SSE] 手动重连');
    reconnectAttemptsRef.current = 0;
    disconnect();
    setTimeout(() => connect(), 100);
  }, [connect, disconnect]);

  /**
   * 页面可见性变化时的处理
   */
  useEffect(() => {
    const handleVisibilityChange = () => {
      if (isPageVisible() && status === 'disconnected' && token) {
        console.log('[SSE] 页面变为可见，尝试重连');
        reconnect();
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);
    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, [status, token, reconnect]);

  /**
   * 初始化连接
   */
  useEffect(() => {
    if (token) {
      connect();
    }

    // 清理函数
    return () => {
      disconnect();
    };
  }, [token]); // 仅在 token 变化时重新连接

  return {
    status,
    error,
    isConnected: status === 'connected',
    connect,
    disconnect,
    reconnect,
  };
};

/**
 * 通知 SSE Hook（预配置）
 */
export const useNotificationSSE = () => {
  const baseURL = import.meta.env.VITE_API_BASE_URL || '';

  return useSSE({
    endpoint: `${baseURL}/admin/notifications/stream`,
    autoReconnect: true,
    maxReconnectAttempts: 5,
    baseReconnectDelay: 1000,
  });
};
