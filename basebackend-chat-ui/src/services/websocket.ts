import type { WsServerFrame } from '@/types';
import { WsFrameType } from '@/types';

/** WebSocket 连接配置 */
export interface WebSocketConfig {
  /** WebSocket 端点路径 */
  url: string;
  /** JWT Token */
  token: string;
  /** 设备标识 */
  deviceId?: string;
  /** 收到服务端帧的回调 */
  onMessage: (frame: WsServerFrame) => void;
  /** 连接状态变更回调 */
  onStatusChange: (connected: boolean) => void;
}

/** 心跳间隔 25 秒 (Spec 5.3) */
const HEARTBEAT_INTERVAL = 25_000;
/** 重连最大延迟 30 秒 */
const MAX_RECONNECT_DELAY = 30_000;

/**
 * WebSocket 客户端管理器
 *
 * 自动连接、25s 心跳、指数退避重连、帧分发
 */
export class ChatWebSocket {
  private ws: WebSocket | null = null;
  private config: WebSocketConfig | null = null;
  private heartbeatTimer: ReturnType<typeof setInterval> | null = null;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private reconnectAttempt = 0;
  private manualClose = false;

  /** 建立连接 */
  connect(config: WebSocketConfig) {
    this.config = config;
    this.manualClose = false;
    this.doConnect();
  }

  /** 主动断开 */
  disconnect() {
    this.manualClose = true;
    this.cleanup();
    if (this.ws) {
      this.ws.close(1000, '用户主动断开');
      this.ws = null;
    }
  }

  /** 发送帧 */
  send(frame: object) {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(frame));
    }
  }

  /** 发送聊天消息 */
  sendChat(data: {
    clientMsgId: string;
    conversationId: number;
    msgType: number;
    content: string;
    extra?: unknown;
    quoteMessageId?: number | null;
    atUserIds?: number[] | null;
  }) {
    this.send({ type: WsFrameType.CHAT, ...data });
  }

  /** 发送已读上报 */
  sendRead(conversationId: number, lastReadMessageId: number) {
    this.send({ type: WsFrameType.READ, conversationId, lastReadMessageId });
  }

  /** 发送正在输入 */
  sendTyping(conversationId: number) {
    this.send({ type: WsFrameType.TYPING, conversationId });
  }

  /** 发送撤回 */
  sendRevoke(conversationId: number, messageId: number) {
    this.send({ type: WsFrameType.REVOKE, conversationId, messageId });
  }

  /** 发送重连同步 */
  sendSync(conversations: Record<string, number>) {
    this.send({ type: WsFrameType.SYNC, conversations });
  }

  /** 设置在线状态 */
  sendPresence(status: string) {
    this.send({ type: WsFrameType.PRESENCE, status });
  }

  /** 是否已连接 */
  get connected() {
    return this.ws?.readyState === WebSocket.OPEN;
  }

  // ---- 内部方法 ----

  private doConnect() {
    if (!this.config) return;
    const { url, token, deviceId } = this.config;

    let wsUrl = `${url}?token=${encodeURIComponent(token)}`;
    if (deviceId) wsUrl += `&deviceId=${encodeURIComponent(deviceId)}`;

    const ws = new WebSocket(wsUrl);

    ws.onopen = () => {
      this.reconnectAttempt = 0;
      this.config?.onStatusChange(true);
      this.startHeartbeat();
    };

    ws.onmessage = (event: MessageEvent) => {
      try {
        const frame = JSON.parse(event.data as string) as WsServerFrame;
        // 心跳回复不上报给业务层
        if (frame.type === WsFrameType.PONG) return;
        this.config?.onMessage(frame);
      } catch {
        // 忽略非 JSON 帧
      }
    };

    ws.onclose = () => {
      this.config?.onStatusChange(false);
      this.stopHeartbeat();
      if (!this.manualClose) {
        this.scheduleReconnect();
      }
    };

    ws.onerror = () => {
      // onclose 会随后触发，这里不做额外处理
    };

    this.ws = ws;
  }

  /** 心跳：每 25 秒发送 ping */
  private startHeartbeat() {
    this.stopHeartbeat();
    this.heartbeatTimer = setInterval(() => {
      this.send({ type: WsFrameType.PING });
    }, HEARTBEAT_INTERVAL);
  }

  private stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer);
      this.heartbeatTimer = null;
    }
  }

  /** 指数退避重连：1s → 2s → 4s → 8s → 16s → 30s (上限) */
  private scheduleReconnect() {
    const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempt), MAX_RECONNECT_DELAY);
    this.reconnectAttempt++;
    this.reconnectTimer = setTimeout(() => {
      this.doConnect();
    }, delay);
  }

  private cleanup() {
    this.stopHeartbeat();
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
  }
}

/** 全局单例 */
export const chatWs = new ChatWebSocket();
