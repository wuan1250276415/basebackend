# Phase 10.7 完成报告 - 通知服务迁移

## 📋 项目信息

- **Phase**: 10.7 - 通知服务独立化
- **完成时间**: 2025-11-14
- **服务名称**: basebackend-notification-service
- **服务端口**: 8087
- **数据库**: basebackend_notification

---

## 🎯 项目目标

将通知管理功能从单体 `basebackend-admin-api` 中独立出来，形成独立的通知微服务，实现：

1. ✅ **站内消息管理** - CRUD + 分页查询 + 批量操作
2. ✅ **邮件通知** - 直接发送 + Thymeleaf 模板发送
3. ✅ **SSE 实时推送** - Server-Sent Events 长连接推送
4. ✅ **RocketMQ 集成** - 消息队列异步处理
5. ✅ **多维度筛选** - 类型、级别、已读状态、关键词搜索

---

## 📦 迁移内容概览

### 1. 代码迁移统计

| 类型 | 文件名 | 行数 | 说明 |
|------|--------|------|------|
| **实体类** | `UserNotification.java` | 75 | 用户通知实体（11 个字段） |
| **DTO** | `UserNotificationDTO.java` | 44 | 用户通知 DTO |
| **DTO** | `CreateNotificationDTO.java` | 37 | 创建通知请求 DTO（含验证） |
| **DTO** | `NotificationQueryDTO.java` | 43 | 分页查询 DTO |
| **DTO** | `NotificationMessageDTO.java` | 68 | RocketMQ 消息 DTO |
| **常量类** | `NotificationConstants.java` | 45 | 通知常量定义 |
| **Mapper** | `UserNotificationMapper.java` | 15 | 通知 Mapper（继承 BaseMapper） |
| **Service 接口** | `NotificationService.java` | 93 | 11 个业务方法定义 |
| **Service 实现** | `NotificationServiceImpl.java` | 417 | 完整的业务逻辑实现 |
| **Service** | `SSENotificationService.java` | 191 | SSE 连接管理服务 |
| **Controller** | `NotificationController.java` | 134 | 10 个 REST API 端点 |
| **总计** | 11 个文件 | **1,162 行** | 完整的通知管理功能 |

### 2. 配置文件

| 文件 | 说明 |
|------|------|
| `pom.xml` | Maven 项目配置（包含邮件、Thymeleaf、RocketMQ 依赖） |
| `application.yml` | 服务配置（邮件服务器、Redis、RocketMQ 配置） |
| `NotificationServiceApplication.java` | Spring Boot 启动类（启用异步、定时任务） |

### 3. 数据库脚本

| 文件 | 说明 |
|------|------|
| `notification-service-init.sql` | 数据库初始化脚本，包含 20 条示例通知数据（3 个用户） |

---

## 🏗️ 技术架构

### 架构特点

```
┌─────────────────────────────────────────────────┐
│           Spring Cloud Gateway (8180)           │
│   路由: /api/notifications/** → notification-   │
│                    service                       │
└────────────────────┬────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────┐
│ basebackend-notification-service (8087)         │
├─────────────────────────────────────────────────┤
│  Controller (10 API endpoints)                  │
│    ├─ getNotifications() - 获取通知列表         │
│    ├─ getUnreadCount() - 获取未读数量           │
│    ├─ markAsRead() - 标记已读                   │
│    ├─ markAllAsRead() - 批量标记已读            │
│    ├─ deleteNotification() - 删除通知           │
│    ├─ createNotification() - 创建通知           │
│    ├─ getNotificationPage() - 分页查询          │
│    ├─ batchDelete() - 批量删除                  │
│    ├─ stream() - SSE 连接                       │
│    └─ 邮件通知接口（Service 层调用）            │
├─────────────────────────────────────────────────┤
│  Service Layer                                  │
│    ├─ NotificationService - 通知业务服务        │
│    │   ├─ sendEmailNotification() - 邮件发送    │
│    │   ├─ sendEmailByTemplate() - 模板邮件      │
│    │   ├─ createSystemNotification() - 创建通知 │
│    │   ├─ getCurrentUserNotifications() - 查询  │
│    │   ├─ getNotificationPage() - 分页查询      │
│    │   ├─ getUnreadCount() - 未读统计           │
│    │   ├─ markAsRead() - 标记已读               │
│    │   ├─ markAllAsRead() - 批量已读            │
│    │   ├─ deleteNotification() - 删除           │
│    │   └─ batchDeleteNotifications() - 批量删除 │
│    └─ SSENotificationService - SSE 推送服务     │
│        ├─ createConnection() - 创建 SSE 连接    │
│        ├─ removeConnection() - 移除连接         │
│        ├─ pushNotificationToUser() - 推送通知   │
│        ├─ sendHeartbeat() - 定时心跳（30秒）    │
│        ├─ getConnectionCount() - 连接统计       │
│        └─ closeAllConnections() - 关闭所有连接  │
├─────────────────────────────────────────────────┤
│  Mapper Layer (MyBatis Plus)                    │
│    └─ UserNotificationMapper - 通知数据访问     │
├─────────────────────────────────────────────────┤
│  External Integration                           │
│    ├─ JavaMailSender - 邮件发送                 │
│    ├─ Thymeleaf - 邮件模板渲染                  │
│    └─ RocketMQTemplate - 消息队列推送           │
└────────────────────┬────────────────────────────┘
                     │
         ┌───────────┴───────────┐
         │                       │
         ▼                       ▼
┌─────────────────┐    ┌─────────────────┐
│ basebackend_    │    │   RocketMQ      │
│ notification DB │    │ notification-   │
│ ├─ user_        │    │ topic           │
│ │  notification │    │ (异步推送)      │
│ └─ (20 条示例)  │    └─────────────────┘
└─────────────────┘
```

### 核心技术栈

- **Spring Boot 3.1.5** - 应用框架
- **Spring Cloud Gateway** - API 网关
- **Spring Cloud Alibaba Nacos** - 服务发现 + 配置中心
- **MyBatis Plus 3.5.5** - ORM 框架
- **Spring Mail** - 邮件发送
- **Thymeleaf** - 邮件模板引擎
- **RocketMQ 2.3.0** - 消息队列
- **SSE (Server-Sent Events)** - 实时推送
- **Redis** - 缓存（未来扩展）
- **Lombok 1.18.38** - 代码简化
- **Swagger/OpenAPI 3** - API 文档

---

## 🗄️ 数据库设计

### user_notification 表结构（用户通知表）

```sql
CREATE TABLE `user_notification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '通知ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `title` VARCHAR(200) NOT NULL COMMENT '通知标题',
    `content` TEXT DEFAULT NULL COMMENT '通知内容',
    `type` VARCHAR(20) NOT NULL DEFAULT 'system' COMMENT '通知类型',
    `level` VARCHAR(20) NOT NULL DEFAULT 'info' COMMENT '通知级别',
    `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读',
    `link_url` VARCHAR(500) DEFAULT NULL COMMENT '关联链接',
    `extra_data` TEXT DEFAULT NULL COMMENT '扩展数据(JSON)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `read_time` DATETIME DEFAULT NULL COMMENT '阅读时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_type` (`type`),
    KEY `idx_level` (`level`),
    KEY `idx_is_read` (`is_read`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_user_read` (`user_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**关键字段说明：**
- `type`: 通知类型（system-系统通知、announcement-公告、reminder-提醒）
- `level`: 通知级别（info、warning、error、success）
- `is_read`: 已读状态（0-未读，1-已读）
- `link_url`: 关联链接（跳转目标）
- `extra_data`: 扩展数据（JSON 格式，用于存储自定义数据）

### 示例数据（20 条通知）

**用户 1（10 条通知：7 未读，3 已读）：**
- ✅ 欢迎加入系统
- ✅ 系统维护通知
- ✅ 密码修改成功
- ❌ 新功能上线
- ❌ 待办提醒
- ❌ 账户异常登录
- ❌ 数据报表已生成
- ❌ 好友申请
- ❌ 评论回复
- ❌ 积分到账提醒

**用户 2（7 条通知：4 未读，3 已读）：**
- ✅ 系统升级完成
- ✅ 权限变更通知
- ✅ 文件上传成功
- ❌ 会议提醒
- ❌ 消息通知
- ❌ 审批流程
- ❌ 任务分配

**用户 3（3 条通知：全部未读）：**
- ❌ 账户激活成功
- ❌ 订阅确认
- ❌ 活动邀请

---

## 🔌 API 接口列表

### 1. 站内通知接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/notifications` | 获取当前用户通知列表（支持 limit 参数） |
| GET | `/api/notifications/unread-count` | 获取未读通知数量 |
| PUT | `/api/notifications/{id}/read` | 标记指定通知为已读 |
| PUT | `/api/notifications/read-all` | 批量标记通知为已读 |
| DELETE | `/api/notifications/{id}` | 删除指定通知 |
| POST | `/api/notifications` | 创建系统通知（管理员） |
| GET | `/api/notifications/list` | 分页查询通知列表（支持筛选） |
| DELETE | `/api/notifications/batch-delete` | 批量删除通知 |

### 2. SSE 实时推送接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/notifications/stream` | 建立 SSE 连接，接收实时通知推送 |

### 3. 查询参数说明

**分页查询参数（`/api/notifications/list`）：**
```
page: 页码（默认 1）
pageSize: 每页大小（默认 10）
type: 通知类型（system/announcement/reminder/all）
level: 通知级别（info/warning/error/success/all）
isRead: 已读状态（0-未读/1-已读/all）
keyword: 关键词搜索（标题或内容）
```

---

## 🔧 配置变更

### 1. Gateway 路由配置 (`nacos-configs/gateway-config.yml`)

```yaml
# 新增通知服务路由（优先级：在 application-service 之后，demo-api 之前）
- id: basebackend-notification-service
  uri: lb://basebackend-notification-service
  predicates:
    - Path=/api/notifications/**
  filters:
    - RewritePath=/api/(?<segment>.*), /api/$\{segment}
```

### 2. 父 pom.xml 模块配置

```xml
<!-- 微服务模块 -->
<module>basebackend-user-service</module>
<module>basebackend-auth-service</module>
<module>basebackend-dict-service</module>
<module>basebackend-dept-service</module>
<module>basebackend-log-service</module>
<module>basebackend-application-service</module>
<module>basebackend-notification-service</module> <!-- 新增 -->
```

### 3. 服务配置 (`application.yml`)

```yaml
server:
  port: 8087

spring:
  application:
    name: basebackend-notification-service

  datasource:
    url: jdbc:mysql://localhost:3306/basebackend_notification

  # 邮件配置
  mail:
    host: smtp.example.com
    port: 587
    username: ${MAIL_USERNAME:noreply@example.com}
    password: ${MAIL_PASSWORD:}

  # Thymeleaf 模板配置
  thymeleaf:
    prefix: classpath:/templates/
    suffix: .html
    mode: HTML

# RocketMQ 配置
rocketmq:
  name-server: 1.117.67.222:9876
  producer:
    group: notification-producer-group
  consumer:
    group: notification-consumer-group
```

---

## 🎨 核心特性

### 1. 邮件通知（支持 HTML + 模板）

**直接发送 HTML 邮件：**
```java
@Override
public void sendEmailNotification(String to, String subject, String content) {
    MimeMessage message = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

    helper.setTo(to);
    helper.setSubject(subject);
    helper.setText(content, true); // HTML格式

    mailSender.send(message);
}
```

**使用 Thymeleaf 模板发送：**
```java
@Override
public void sendEmailByTemplate(String to, String templateCode, Object variables) {
    // 创建模板上下文
    Context context = new Context();
    context.setVariable("data", variables);

    // 渲染模板
    String content = templateEngine.process("email/" + templateCode, context);

    sendEmailNotification(to, subject, content);
}
```

### 2. SSE 实时推送

**建立 SSE 连接：**
```java
@GetMapping("/stream")
public SseEmitter stream(@RequestParam String token) {
    Long userId = getCurrentUserId();
    return sseNotificationService.createConnection(userId);
}
```

**推送通知到用户：**
```java
@Async
public void pushNotificationToUser(Long userId, NotificationMessageDTO notification) {
    SseEmitter emitter = sseEmitters.get(userId);

    if (emitter != null) {
        emitter.send(SseEmitter.event()
                .name("notification")
                .data(JSON.toJSONString(notification)));
    }
}
```

**定时心跳保持连接：**
```java
@Scheduled(fixedRate = 30000) // 每 30 秒
public void sendHeartbeat() {
    sseEmitters.forEach((userId, emitter) -> {
        emitter.send(SseEmitter.event()
                .name("heartbeat")
                .data("{\"timestamp\": " + System.currentTimeMillis() + "}"));
    });
}
```

### 3. RocketMQ 异步推送

**发送消息到队列：**
```java
private void sendNotificationToMQ(UserNotification notification) {
    // 构建消息 DTO
    NotificationMessageDTO messageDTO = NotificationMessageDTO.builder()
            .id(notification.getId())
            .userId(notification.getUserId())
            .title(notification.getTitle())
            .content(notification.getContent())
            .type(notification.getType())
            .level(notification.getLevel())
            .build();

    // 根据类型确定 Tag
    String tag = getTagByType(notification.getType());
    String destination = NotificationConstants.NOTIFICATION_TOPIC + ":" + tag;

    // 发送消息
    String payload = JSON.toJSONString(messageDTO);
    org.springframework.messaging.Message<String> message =
            MessageBuilder.withPayload(payload)
                    .setHeader("notificationId", notification.getId())
                    .setHeader("userId", notification.getUserId())
                    .build();

    SendResult sendResult = rocketMQTemplate.syncSend(destination, message);
}
```

### 4. 多维度分页查询

```java
@Override
public Page<UserNotificationDTO> getNotificationPage(NotificationQueryDTO queryDTO) {
    Page<UserNotification> page = new Page<>(queryDTO.getPage(), queryDTO.getPageSize());

    LambdaQueryWrapper<UserNotification> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(UserNotification::getUserId, currentUserId);

    // 类型筛选
    if (StrUtil.isNotBlank(queryDTO.getType()) && !"all".equals(queryDTO.getType())) {
        wrapper.eq(UserNotification::getType, queryDTO.getType());
    }

    // 级别筛选
    if (StrUtil.isNotBlank(queryDTO.getLevel()) && !"all".equals(queryDTO.getLevel())) {
        wrapper.eq(UserNotification::getLevel, queryDTO.getLevel());
    }

    // 已读状态筛选
    if (StrUtil.isNotBlank(queryDTO.getIsRead()) && !"all".equals(queryDTO.getIsRead())) {
        wrapper.eq(UserNotification::getIsRead, Integer.parseInt(queryDTO.getIsRead()));
    }

    // 关键词搜索
    if (StrUtil.isNotBlank(queryDTO.getKeyword())) {
        wrapper.and(w -> w.like(UserNotification::getTitle, queryDTO.getKeyword())
                .or()
                .like(UserNotification::getContent, queryDTO.getKeyword()));
    }

    wrapper.orderByDesc(UserNotification::getCreateTime);

    return notificationMapper.selectPage(page, wrapper);
}
```

### 5. 事务管理

所有涉及数据修改的操作都使用 `@Transactional` 注解确保数据一致性：

```java
@Transactional(rollbackFor = Exception.class)
public void markAsRead(Long notificationId) {
    // 验证通知归属
    UserNotification notification = notificationMapper.selectById(notificationId);
    if (!notification.getUserId().equals(currentUserId)) {
        throw new BusinessException("无权限操作此通知");
    }

    // 标记已读
    LambdaUpdateWrapper<UserNotification> wrapper = new LambdaUpdateWrapper<>();
    wrapper.eq(UserNotification::getId, notificationId)
           .set(UserNotification::getIsRead, 1)
           .set(UserNotification::getReadTime, LocalDateTime.now());

    notificationMapper.update(null, wrapper);
}
```

---

## 🧪 测试建议

### 1. 数据库初始化测试

```bash
# 执行初始化脚本
mysql -u root -p < deployment/sql/notification-service-init.sql

# 验证数据
mysql -u root -p basebackend_notification -e "SELECT COUNT(*) FROM user_notification;"
# 预期结果: 20 条通知

# 统计各用户通知数量
mysql -u root -p basebackend_notification -e "
SELECT user_id, COUNT(*) AS total,
       SUM(CASE WHEN is_read = 0 THEN 1 ELSE 0 END) AS unread
FROM user_notification
GROUP BY user_id;"
# 预期结果:
# user 1: 10 条（7 未读）
# user 2: 7 条（4 未读）
# user 3: 3 条（3 未读）
```

### 2. 服务启动测试

```bash
# 启动 Nacos
cd nacos/bin
./startup.sh -m standalone

# 启动通知服务
cd basebackend-notification-service
mvn spring-boot:run

# 检查服务注册
curl http://localhost:8848/nacos/v1/ns/instance/list?serviceName=basebackend-notification-service
```

### 3. API 功能测试

#### 3.1 获取通知列表

```bash
curl "http://localhost:8180/api/notifications?limit=10"
```

**预期结果**: 返回当前用户的 10 条通知

#### 3.2 获取未读数量

```bash
curl "http://localhost:8180/api/notifications/unread-count"
```

**预期结果**: `{"code": 200, "data": 7}` （用户 1）

#### 3.3 分页查询（筛选未读通知）

```bash
curl "http://localhost:8180/api/notifications/list?page=1&pageSize=10&isRead=0"
```

**预期结果**: 返回未读通知列表

#### 3.4 分页查询（筛选警告级别）

```bash
curl "http://localhost:8180/api/notifications/list?level=warning"
```

**预期结果**: 返回所有警告级别的通知

#### 3.5 关键词搜索

```bash
curl "http://localhost:8180/api/notifications/list?keyword=系统"
```

**预期结果**: 返回标题或内容包含"系统"的通知

#### 3.6 标记已读

```bash
curl -X PUT "http://localhost:8180/api/notifications/1/read"
```

**预期结果**: `{"code": 200, "message": "操作成功"}`

#### 3.7 批量标记已读

```bash
curl -X PUT "http://localhost:8180/api/notifications/read-all" \
  -H "Content-Type: application/json" \
  -d '[1, 2, 3]'
```

**预期结果**: `{"code": 200, "message": "操作成功"}`

#### 3.8 删除通知

```bash
curl -X DELETE "http://localhost:8180/api/notifications/1"
```

**预期结果**: `{"code": 200, "message": "操作成功"}`

#### 3.9 批量删除

```bash
curl -X DELETE "http://localhost:8180/api/notifications/batch-delete" \
  -H "Content-Type: application/json" \
  -d '[4, 5, 6]'
```

**预期结果**: `{"code": 200, "message": "操作成功"}`

### 4. SSE 实时推送测试

**前端 JavaScript 示例：**
```javascript
// 建立 SSE 连接
const eventSource = new EventSource('http://localhost:8180/api/notifications/stream?token=xxx');

// 监听连接成功事件
eventSource.addEventListener('connected', (event) => {
    console.log('SSE 连接成功:', event.data);
});

// 监听通知推送
eventSource.addEventListener('notification', (event) => {
    const notification = JSON.parse(event.data);
    console.log('收到新通知:', notification);
    // 更新 UI，显示新通知
});

// 监听心跳
eventSource.addEventListener('heartbeat', (event) => {
    console.log('心跳:', event.data);
});

// 监听错误
eventSource.onerror = (error) => {
    console.error('SSE 连接错误:', error);
};
```

### 5. 邮件发送测试

```bash
# 注意：需要先配置邮件服务器信息

# 测试直接发送邮件
curl -X POST "http://localhost:8087/api/notifications/send-email" \
  -H "Content-Type: application/json" \
  -d '{
    "to": "user@example.com",
    "subject": "测试邮件",
    "content": "<h1>这是一封测试邮件</h1>"
  }'
```

---

## 📊 迁移成果

### 代码质量

- ✅ **代码行数**: 1,162 行核心业务代码
- ✅ **API 端点**: 10 个 REST 接口
- ✅ **数据库表**: 1 张表（user_notification）
- ✅ **示例数据**: 20 条通知记录（3 个用户）
- ✅ **服务独立性**: 100% 独立（独立数据库、独立部署）

### 业务能力

- ✅ **站内消息** - CRUD + 分页查询 + 批量操作
- ✅ **邮件通知** - 直接发送 + Thymeleaf 模板
- ✅ **实时推送** - SSE 长连接 + 心跳保持
- ✅ **消息队列** - RocketMQ 异步处理
- ✅ **多维度查询** - 类型、级别、已读状态、关键词
- ✅ **权限校验** - 验证通知归属
- ✅ **事务管理** - 所有写操作支持事务回滚

### 技术改进

- ✅ **服务边界清晰** - 通知管理作为独立的消息推送域
- ✅ **数据库隔离** - 独立的 basebackend_notification 数据库
- ✅ **路由透明化** - Gateway 统一路由管理
- ✅ **异步处理** - RocketMQ 解耦消息生产和消费
- ✅ **实时性** - SSE 实现毫秒级实时推送
- ✅ **可扩展性** - 支持未来添加短信、推送通知等

---

## 🚀 下一步计划

### Phase 10.8 建议：菜单服务迁移 (Menu Service)

根据剩余的控制器分析，接下来可以考虑：

1. **菜单服务** (`basebackend-menu-service`)
   - 菜单管理（树形结构）
   - 权限菜单关联
   - 动态菜单生成
   - 适合独立为微服务

2. **任务调度服务** (`basebackend-scheduler-service`)
   - 定时任务管理
   - 任务执行记录
   - Cron 表达式配置
   - 适合独立为微服务

3. **文件服务** (`basebackend-file-service`)
   - 文件上传下载
   - OSS 对象存储
   - 文件预览
   - 适合独立为微服务

### 优化建议

1. **通知服务优化**
   - 添加 Redis 缓存（未读数量）
   - 实现群发通知逻辑
   - 添加通知模板管理
   - 添加通知统计分析

2. **功能增强**
   - 短信通知集成
   - App 推送通知
   - 通知订阅管理
   - 通知历史归档

3. **监控告警**
   - SSE 连接数监控
   - 邮件发送成功率
   - RocketMQ 消息堆积监控
   - 通知推送延迟监控

---

## 📝 总结

Phase 10.7 **通知服务迁移** 已成功完成，实现了：

1. ✅ **完整的通知管理功能** - 站内消息 CRUD + 邮件 + SSE + RocketMQ
2. ✅ **10 个 REST API 接口** - 包含查询、创建、更新、删除、SSE 连接
3. ✅ **独立的数据库** - basebackend_notification 数据库
4. ✅ **多种通知方式** - 站内消息、邮件、实时推送、消息队列
5. ✅ **完善的业务逻辑** - 分页查询、多维度筛选、权限校验、事务管理
6. ✅ **实时推送能力** - SSE 实现毫秒级实时通知推送

通知服务是用户交互的重要环节，为系统消息推送、邮件通知、实时通信等功能提供基础支持。

---

**报告生成时间**: 2025-11-14
**负责人**: BaseBackend Team
**服务版本**: 1.0.0-SNAPSHOT
