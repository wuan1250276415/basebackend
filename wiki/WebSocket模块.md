# WebSocket 模块

> `basebackend-websocket` — 实时通信基础设施，频道管理、用户多连接、心跳检测、消息路由

## 概述

基于 Spring WebSocket 的实时通信模块，支持用户级连接管理、频道/房间模型、多种消息类型路由。独立于 `basebackend-messaging`（RocketMQ + Webhook）。

## 模块结构

```
basebackend-websocket/
├── SessionManager              — 用户级连接管理 (一个用户多个连接)
├── ChannelManager              — 频道/房间管理 (正向+反向索引)
├── WsMessage                   — 消息 Record (7种消息类型)
│   PRIVATE / BROADCAST / CHANNEL_MESSAGE / SUBSCRIBE / UNSUBSCRIBE / HEARTBEAT / SYSTEM
├── WsMessageHandler            — 核心消息路由 (extends TextWebSocketHandler)
├── AuthHandshakeInterceptor    — 握手认证 (Header/Query/Token)
├── WebSocketProperties         — 配置属性
└── WebSocketAutoConfiguration  — 自动配置 (basebackend.websocket.enabled=true)
```

## 快速使用

### 1. 启用模块

```yaml
basebackend:
  websocket:
    enabled: true
    endpoint: /ws
    allowed-origins: "*"
    heartbeat-interval: 30s
    sockjs-enabled: false
    broadcast-type: local    # local / redis (集群广播)
```

### 2. 客户端连接

```javascript
// WebSocket 连接 (userId 通过 Query/Header/Token 传递)
const ws = new WebSocket("ws://localhost:8080/ws?userId=user123");

// 私信
ws.send(JSON.stringify({
    type: "PRIVATE",
    to: "user456",
    content: "你好！"
}));

// 订阅频道
ws.send(JSON.stringify({ type: "SUBSCRIBE", channel: "room-1" }));

// 频道消息
ws.send(JSON.stringify({
    type: "CHANNEL_MESSAGE",
    channel: "room-1",
    content: "大家好！"
}));

// 心跳
ws.send(JSON.stringify({ type: "HEARTBEAT" }));
```

### 3. 后端推送

```java
@Autowired
private SessionManager sessionManager;

@Autowired
private ChannelManager channelManager;

// 推送给指定用户（所有连接）
sessionManager.sendToUser("user123", "系统通知: 你有新消息");

// 频道广播
channelManager.broadcast("room-1", WsMessage.channelMessage("room-1", "系统", "欢迎！"));
```

## 消息类型

| 类型 | 方向 | 说明 |
|------|------|------|
| `PRIVATE` | 客户端→服务端 | 私信，转发给目标用户 |
| `BROADCAST` | 服务端→客户端 | 全局广播 |
| `CHANNEL_MESSAGE` | 双向 | 频道消息 |
| `SUBSCRIBE` | 客户端→服务端 | 订阅频道 |
| `UNSUBSCRIBE` | 客户端→服务端 | 取消订阅 |
| `HEARTBEAT` | 双向 | 心跳保活 |
| `SYSTEM` | 服务端→客户端 | 系统消息 |

## 测试覆盖

21 个测试全部通过：
- WsMessageTest (7): 消息类型工厂方法 / JSON 序列化
- ChannelManagerTest (14): 订阅/取消/广播/频道成员/用户频道列表/并发安全
