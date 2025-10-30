# RocketMQ 快速启动指南

## 一、启动 RocketMQ

```bash
# 进入项目根目录
cd /home/wuan/IdeaProjects/basebackend

# 启动 RocketMQ（NameServer + Broker + Dashboard）
docker-compose -f docker-compose-rocketmq.yml up -d

# 查看启动日志
docker-compose -f docker-compose-rocketmq.yml logs -f

# 等待所有服务启动（约30秒）
```

## 二、验证部署

### 2.1 访问 Dashboard

浏览器打开: `http://localhost:8080`

检查项：
- ✅ 集群状态显示 "ONLINE"
- ✅ Broker 列表显示 "broker-a"
- ✅ NameServer 地址正确

### 2.2 检查服务端口

```bash
# 检查 NameServer（9876）
netstat -tuln | grep 9876

# 检查 Broker（10911）
netstat -tuln | grep 10911

# 检查 Dashboard（8080）
curl http://localhost:8080
```

## 三、启动应用

```bash
# 启动后端服务
cd basebackend-admin-api
mvn spring-boot:run

# 或使用 IDE 启动 AdminApiApplication
```

## 四、测试消息发送

### 4.1 使用 Swagger UI

访问: `http://localhost:8082/doc.html`

找到 "事件管理" → "发布事件" 接口：
```json
{
  "eventType": "TEST_EVENT",
  "data": {
    "message": "Hello RocketMQ!"
  },
  "source": "test"
}
```

### 4.2 查看消息

在 RocketMQ Dashboard:
1. 进入 "Message" 页面
2. 选择 Topic: `basebackend-event-topic`
3. 点击 "Query" 查看消息列表

## 五、停止服务

```bash
# 停止 RocketMQ
docker-compose -f docker-compose-rocketmq.yml down

# 如需清理数据
docker-compose -f docker-compose-rocketmq.yml down -v
```

## 六、常用命令

```bash
# 查看运行状态
docker-compose -f docker-compose-rocketmq.yml ps

# 重启服务
docker-compose -f docker-compose-rocketmq.yml restart

# 查看 Broker 日志
docker logs -f rocketmq-broker

# 查看 NameServer 日志
docker logs -f rocketmq-nameserver
```

## 七、配置说明

### NameServer 地址

配置在 `application-messaging.yml`:
```yaml
rocketmq:
  name-server: 192.168.66.31:9876
```

**注意**: 请根据实际部署环境修改 IP 地址。

### Broker 配置

配置文件: `rocketmq/broker/broker.conf`

关键配置项：
- `brokerIP1`: Broker 对外服务地址（需修改为实际 IP）
- `autoCreateTopicEnable`: 自动创建 Topic（开发环境建议 true）
- `messageDelayLevel`: 延迟消息级别

## 八、故障排查

### 问题1: 连接失败

```bash
# 检查 NameServer 是否启动
docker ps | grep nameserver

# 检查端口是否开放
telnet 192.168.66.31 9876
```

### 问题2: Dashboard 无法访问

```bash
# 检查 Dashboard 容器
docker logs rocketmq-dashboard

# 重启 Dashboard
docker restart rocketmq-dashboard
```

### 问题3: 消息发送失败

检查：
1. NameServer 地址配置是否正确
2. Broker 是否在线（Dashboard 查看）
3. 应用日志中的错误信息

---

**更多信息**: 查看 `ROCKETMQ-MIGRATION.md`
