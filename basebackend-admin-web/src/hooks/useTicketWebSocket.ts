/**
 * Ticket WebSocket Hook
 * 用于实时接收工单更新推送（基于 WebSocket 频道订阅）
 */

import { useEffect, useRef, useCallback, useState } from 'react';
import { useAuthStore } from '@/stores/authStore';

export interface TicketWsEvent {
  eventType: string;
  ticketId: number;
  payload: Record<string, unknown>;
  timestamp: string;
}

interface UseTicketWebSocketOptions {
  ticketId?: number;
  onEvent?: (event: TicketWsEvent) => void;
  autoConnect?: boolean;
}

export type WsStatus = 'connecting' | 'connected' | 'disconnected' | 'error';

export const useTicketWebSocket = (options: UseTicketWebSocketOptions = {}) => {
  const { ticketId, onEvent, autoConnect = true } = options;
  const { token } = useAuthStore();
  const wsRef = useRef<WebSocket | null>(null);
  const [status, setStatus] = useState<WsStatus>('disconnected');
  const reconnectTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const reconnectCountRef = useRef(0);
  const maxReconnects = 5;

  const connect = useCallback(() => {
    if (!token) return;

    const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const baseUrl = import.meta.env.VITE_WS_BASE_URL || `${wsProtocol}//${window.location.host}`;
    const url = `${baseUrl}/ws?token=${encodeURIComponent(token)}`;

    setStatus('connecting');

    try {
      const ws = new WebSocket(url);

      ws.onopen = () => {
        setStatus('connected');
        reconnectCountRef.current = 0;

        // 订阅指定工单频道
        if (ticketId) {
          ws.send(JSON.stringify({ action: 'subscribe', channel: `ticket:${ticketId}` }));
        }
      };

      ws.onmessage = (evt) => {
        try {
          const data = JSON.parse(evt.data);
          if (data.type === 'heartbeat') return;
          if (onEvent && data.eventType) {
            onEvent(data as TicketWsEvent);
          }
        } catch {
          // 忽略非 JSON 消息
        }
      };

      ws.onerror = () => {
        setStatus('error');
      };

      ws.onclose = () => {
        setStatus('disconnected');
        wsRef.current = null;

        // 自动重连（指数退避）
        if (autoConnect && reconnectCountRef.current < maxReconnects) {
          const delay = Math.min(1000 * Math.pow(2, reconnectCountRef.current), 30000);
          reconnectCountRef.current++;
          reconnectTimerRef.current = setTimeout(connect, delay);
        }
      };

      wsRef.current = ws;
    } catch {
      setStatus('error');
    }
  }, [token, ticketId, onEvent, autoConnect]);

  const disconnect = useCallback(() => {
    if (reconnectTimerRef.current) {
      clearTimeout(reconnectTimerRef.current);
      reconnectTimerRef.current = null;
    }
    if (wsRef.current) {
      // 取消订阅
      if (ticketId && wsRef.current.readyState === WebSocket.OPEN) {
        wsRef.current.send(JSON.stringify({ action: 'unsubscribe', channel: `ticket:${ticketId}` }));
      }
      wsRef.current.close();
      wsRef.current = null;
    }
    setStatus('disconnected');
    reconnectCountRef.current = 0;
  }, [ticketId]);

  useEffect(() => {
    if (autoConnect && token) {
      connect();
    }
    return () => {
      disconnect();
    };
  }, [token, ticketId]);

  return {
    status,
    isConnected: status === 'connected',
    connect,
    disconnect,
  };
};
