# 通知中心 RocketMQ + SSE 集成方案

## 📊 架构概览

本方案将通知系统与 RocketMQ 消息队列和 SSE (Server-Sent Events) 实时推送技术集成，实现高性能、可扩展的实时通知推送。

```
业务系统创建通知
      ↓
保存到数据库 + 发送到RocketMQ
      ↓
  RocketMQ Topic
  (notification-topic)
      ↓
NotificationConsumer 消费
      ↓
SSENotificationService 推送
      ↓
  前端 SSE 连接池
      ↓
  实时推送到浏览器
```

## 🚀 已实现功能

### 后端实现

1. **SSE 推送服务** (`SSENotificationService.java`)
   - 管理用户 SSE 连接池
   - 实时推送通知到在线用户
   - 心跳机制保持连接
   - 自动重连机制

2. **RocketMQ 消息消费者** (`NotificationConsumer.java`)
   - 消费 `notification-topic` 主题消息
   - 支持三种 Tag: SYSTEM, ANNOUNCEMENT, REMINDER
   - 消费后通过 SSE 推送

3. **通知服务增强** (`NotificationServiceImpl.java`)
   - 创建通知时自动发送到 RocketMQ
   - 分页查询支持（筛选类型、级别、已读状态）
   - 批量删除通知
   - 关键词搜索

4. **API 接口增强** (`NotificationController.java`)
   - `GET /api/admin/notifications/list` - 分页查询通知
   - `DELETE /api/admin/notifications/batch-delete` - 批量删除
   - `GET /api/admin/notifications/stream` - SSE 连接

### 前端实现

1. **SSE Hook** (`hooks/useSSE.ts`)
   - 自动建立 SSE 连接
   - 指数退避重连策略
   - 页面可见性API集成
   - 事件监听与处理

2. **通知中心页面** (`pages/Notification/index.tsx`)
   - 统计卡片展示
   - 高级筛选（类型/级别/已读状态）
   - 关键词搜索
   - 分页与批量操作

## 📦 关键文件

### 后端文件
```
basebackend-admin-api/
├── src/main/java/com/basebackend/admin/
│   ├── constants/
│   │   └── NotificationConstants.java        # 通知常量定义
│   ├── consumer/
│   │   └── NotificationConsumer.java          # RocketMQ 消费者
│   ├── dto/notification/
│   │   ├── NotificationMessageDTO.java        # 消息传输对象
│   │   └── NotificationQueryDTO.java          # 查询参数对象
│   ├── service/
│   │   ├── NotificationService.java           # 通知服务接口
│   │   ├── SSENotificationService.java        # SSE 推送服务
│   │   └── impl/
│   │       └── NotificationServiceImpl.java   # 通知服务实现
│   └── controller/
│       └── NotificationController.java        # 通知控制器
└── src/main/resources/
    └── application-messaging.yml              # RocketMQ 配置
```

### 前端文件
```
basebackend-admin-web/src/
├── hooks/
│   ├── useSSE.ts                             # SSE 连接 Hook
│   └── useNotificationPolling.ts             # 轮询备份 Hook
├── stores/
│   └── notification.ts                       # 通知全局状态
├── utils/
│   └── notification.ts                       # 通知工具函数
├── components/NotificationCenter/
│   ├── NotificationBell.tsx                  # 通知铃铛组件
│   └── NotificationBell.module.scss
├── pages/Notification/
│   ├── index.tsx                             # 通知中心页面
│   └── index.module.scss
└── layouts/BasicLayout/
    └── index.tsx                             # 布局（集成SSE）
```

## ⚙️ 配置说明

### RocketMQ 配置 (`application-messaging.yml`)

```yaml
rocketmq:
  name-server: 192.168.66.126:9876           # RocketMQ 地址
  producer:
    group: notification-producer-group        # 生产者组
    send-message-timeout: 3000
  consumer:
    group: notification-consumer-group        # 消费者组
    consume-timeout: 15

messaging:
  rocketmq:
    enabled: true
    default-topic: notification-topic         # 通知主题
```

### SSE 配置 (`NotificationConstants.java`)

```java
public static final long SSE_TIMEOUT = 5 * 60 * 1000;      // 5分钟超时
public static final long SSE_HEARTBEAT_INTERVAL = 30 * 1000; // 30秒心跳
```

## 🔌 API 使用示例

### 1. 创建通知并自动推送

```bash
POST /api/admin/notifications
Content-Type: application/json

{
  "userId": 1,
  "title": "系统通知",
  "content": "您有新的任务待处理",
  "type": "system",
  "level": "info",
  "linkUrl": "/tasks/123"
}
```

**流程：**
1. 保存到数据库
2. 发送消息到 RocketMQ (`notification-topic:SYSTEM`)
3. NotificationConsumer 消费消息
4. SSENotificationService 推送到用户的 SSE 连接
5. 前端实时收到通知

### 2. 建立 SSE 连接

```bash
GET /api/admin/notifications/stream?token=xxx
Accept: text/event-stream
```

**事件类型：**
- `connected` - 连接成功
- `notification` - 新通知
- `heartbeat` - 心跳

### 3. 分页查询通知

```bash
GET /api/admin/notifications/list?page=1&pageSize=10&type=system&isRead=0
```

### 4. 批量删除通知

```bash
DELETE /api/admin/notifications/batch-delete
Content-Type: application/json

[1, 2, 3, 4, 5]
```

## 🎯 消息流转过程

### 完整流程示例

```
1. 业务系统调用 NotificationService.createSystemNotification()
   ├─ 保存通知到数据库 (user_notification 表)
   └─ 发送消息到 RocketMQ
      ├─ Topic: notification-topic
      ├─ Tag: SYSTEM (根据 type 自动选择)
      └─ Payload: NotificationMessageDTO (JSON)

2. RocketMQ 存储消息并分发

3. NotificationConsumer 消费消息
   ├─ 解析 JSON 为 NotificationMessageDTO
   └─ 调用 SSENotificationService.pushNotificationToUser()

4. SSENotificationService 推送
   ├─ 从连接池获取用户的 SseEmitter
   ├─ 发送 'notification' 事件
   └─ 如果用户未连接，跳过（用户下次刷新时通过轮询获取）

5. 前端 SSE Hook 接收事件
   ├─ 更新全局状态 (unreadCount++)
   ├─ 显示 Toast 提示
   ├─ 铃铛摇晃动画
   └─ 如果页面不可见，显示浏览器通知
```

## 🔍 关键设计决策

### 1. **混合推送策略**
- **SSE 实时推送**：用户在线时立即收到通知
- **轮询备份**：SSE 断开或未连接时自动降级到轮询

### 2. **消息可靠性**
- 数据库优先：先保存数据库，后发送 MQ
- MQ 失败不影响主流程：即使 MQ 失败，用户仍可通过轮询获取
- RocketMQ 重试机制：最多重试 2 次

### 3. **连接管理**
- 用户级连接池：`ConcurrentHashMap<UserId, SseEmitter>`
- 自动清理：超时/错误时自动移除连接
- 心跳保活：每 30 秒发送心跳

### 4. **扩展性**
- Topic/Tag 设计：支持不同类型通知分类
- 消费者组：支持多实例负载均衡
- 分页查询：避免大数据量性能问题

## 🛠 部署要求

### 环境依赖

1. **RocketMQ 服务器**
   ```bash
   # 确保 RocketMQ 已启动并可访问
   # 默认地址：192.168.66.126:9876
   ```

2. **数据库表**
   ```sql
   -- user_notification 表（已存在）
   -- sys_message_log 表（事务消息日志，messaging 模块已包含）
   ```

3. **Spring Boot 配置**
   ```yaml
   # 确保 application.yml 引入了 messaging 配置
   spring:
     profiles:
       include: messaging
   ```

### 启动步骤

1. 启动 RocketMQ
2. 启动 admin-api 服务
3. 启动前端应用
4. 访问系统并登录
5. SSE 自动建立连接

## 📈 性能优化

1. **连接池管理**
   - 使用 ConcurrentHashMap 线程安全
   - 定时清理过期连接
   - 限制单用户连接数

2. **消息批量处理**
   - RocketMQ 批量消费（可配置）
   - 批量推送（未来优化）

3. **缓存策略**
   - 未读数量缓存（Redis）
   - 用户在线状态缓存

## 🔧 故障排查

### 问题 1：SSE 连接失败
```
检查点：
1. 浏览器是否支持 SSE (EventSource API)
2. 网络是否稳定
3. Token 是否有效
4. 用户ID 是否正确获取
```

### 问题 2：通知未实时推送
```
检查点：
1. RocketMQ 是否正常运行
2. NotificationConsumer 是否启动成功
3. SSE 连接是否建立
4. 日志中是否有异常
```

### 问题 3：消息重复消费
```
解决方案：
1. 启用幂等性配置（messaging.idempotency.enabled=true）
2. 检查消费者组配置
3. 检查 RocketMQ 消费位点
```

## 📚 扩展方向

1. **群发通知**
   - 实现批量插入
   - 广播模式推送

2. **通知模板**
   - 支持模板化通知
   - 参数化内容

3. **通知优先级**
   - 高优先级通知优先推送
   - 限流策略

4. **离线通知**
   - 用户离线时累积通知
   - 上线后批量推送

5. **推送渠道扩展**
   - 邮件通知集成
   - 短信通知集成
   - 移动端推送（APNs、FCM）

---

**创建时间**: 2025-11-07
**创建者**: Claude Code (浮浮酱) ฅ'ω'ฅ
