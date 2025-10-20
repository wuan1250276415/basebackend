# RabbitMQ 消息队列部署指南

## 1. 环境要求

- Docker 20.10+
- Docker Compose 2.0+

## 2. 快速启动

### 2.1 启动 RabbitMQ

```bash
cd docker/messaging
./start.sh
```

### 2.2 验证服务

访问 RabbitMQ 管理界面：http://localhost:15672

默认登录信息：
- 用户名：admin
- 密码：admin123
- 虚拟主机：basebackend

### 2.3 停止服务

```bash
cd docker/messaging
./stop.sh
```

## 3. 服务配置

### 3.1 端口说明

| 端口 | 说明 |
|------|------|
| 5672 | AMQP 协议端口 |
| 15672 | 管理界面端口 |

### 3.2 插件说明

系统已自动启用以下插件：
- `rabbitmq_management`：管理界面
- `rabbitmq_delayed_message_exchange`：延迟消息插件

### 3.3 虚拟主机

默认创建虚拟主机：`basebackend`

## 4. 应用配置

在 `application.yml` 或 `application-messaging.yml` 中配置：

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: admin
    password: admin123
    virtual-host: basebackend
```

在 `application.yml` 的 `spring.profiles.active` 中添加 `messaging` 配置：

```yaml
spring:
  profiles:
    active: dev,messaging
```

## 5. 数据库初始化

执行 SQL 脚本创建必要的表：

```bash
# 进入项目根目录
cd /path/to/basebackend

# 执行 SQL 脚本
mysql -u root -p your_database < basebackend-admin-api/src/main/resources/db/migration/V1.3__create_messaging_tables.sql
```

或者在应用启动时自动创建（如果使用 Flyway）。

## 6. 功能验证

### 6.1 消息监控

访问前端页面：`http://localhost:3000/integration/message-monitor`

查看消息发送、消费、失败统计。

### 6.2 Webhook 配置

访问前端页面：`http://localhost:3000/integration/webhook-config`

配置 Webhook 订阅。

### 6.3 事件日志

访问前端页面：`http://localhost:3000/integration/event-log`

查看事件发布和 Webhook 调用日志。

### 6.4 死信处理

访问前端页面：`http://localhost:3000/integration/dead-letter`

处理失败的消息。

## 7. 常见问题

### 7.1 延迟消息插件未启用

如果延迟消息功能不可用，请确认插件已启用：

```bash
# 进入 RabbitMQ 容器
docker exec -it basebackend-rabbitmq bash

# 列出已启用的插件
rabbitmq-plugins list

# 启用延迟消息插件（如未启用）
rabbitmq-plugins enable rabbitmq_delayed_message_exchange
```

### 7.2 连接被拒绝

确保：
1. RabbitMQ 服务已启动
2. 端口 5672 未被占用
3. 虚拟主机 `basebackend` 已创建

### 7.3 消息积压

1. 检查消费者是否正常运行
2. 增加消费者并发数量
3. 查看死信队列是否有消息

## 8. 性能优化

### 8.1 生产环境配置

修改 `docker-compose.yml` 中的资源限制：

```yaml
services:
  rabbitmq:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
        reservations:
          cpus: '1'
          memory: 1G
```

### 8.2 消费者并发配置

在 `application-messaging.yml` 中调整：

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        concurrency: 5
        max-concurrency: 20
```

## 9. 监控和告警

### 9.1 查看 RabbitMQ 日志

```bash
docker logs -f basebackend-rabbitmq
```

### 9.2 查看队列状态

访问管理界面：http://localhost:15672/#/queues

### 9.3 监控指标

通过应用的 `/api/messaging/monitor/statistics` 接口获取统计数据。

## 10. 备份和恢复

### 10.1 数据备份

```bash
# 备份 RabbitMQ 数据卷
docker run --rm -v messaging_rabbitmq-data:/data -v $(pwd):/backup alpine tar czf /backup/rabbitmq-backup.tar.gz -C /data .
```

### 10.2 数据恢复

```bash
# 恢复 RabbitMQ 数据
docker run --rm -v messaging_rabbitmq-data:/data -v $(pwd):/backup alpine tar xzf /backup/rabbitmq-backup.tar.gz -C /data
```
