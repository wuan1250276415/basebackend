# RocketMQ故障排查指南

## 常见问题

### 1. Broker启动失败：NullPointerException at ScheduleMessageService

**错误信息：**
```
java.lang.NullPointerException
at org.apache.rocketmq.broker.schedule.ScheduleMessageService.configFilePath(ScheduleMessageService.java:272)
at org.apache.rocketmq.common.ConfigManager.persist(ConfigManager.java:83)
```

**原因：**
- 存储路径配置缺失或不正确
- 容器内用户权限不足
- 配置文件路径问题

**解决方案：**

#### 方案1：使用修复脚本（推荐）

Windows:
```cmd
cd docker\compose\middleware
fix-rocketmq.bat
```

Linux/Mac:
```bash
cd docker/compose/middleware
./fix-rocketmq.sh
```

#### 方案2：手动修复

1. **更新broker.conf配置**

确保包含完整的存储路径配置：
```properties
# 存储路径配置
storePathRootDir = /home/rocketmq/store
storePathCommitLog = /home/rocketmq/store/commitlog
storePathConsumeQueue = /home/rocketmq/store/consumequeue
storePathIndex = /home/rocketmq/store/index
storeCheckpoint = /home/rocketmq/store/checkpoint
abortFile = /home/rocketmq/store/abort
```

2. **更新docker-compose配置**

添加用户权限和环境变量：
```yaml
rocketmq-broker:
  user: "0:0"  # 使用root用户
  environment:
    JAVA_OPT_EXT: "-Xms512m -Xmx512m -Duser.home=/home/rocketmq"
```

3. **重启服务**

```bash
docker-compose down rocketmq-broker
docker-compose up -d rocketmq-broker
```

### 2. Broker无法连接NameServer

**错误信息：**
```
connect to <namesrv-address> failed
```

**检查步骤：**

1. **检查NameServer是否运行**
```bash
docker ps | grep namesrv
docker logs basebackend-rocketmq-namesrv
```

2. **检查网络连通性**
```bash
docker exec basebackend-rocketmq-broker ping rocketmq-namesrv
```

3. **检查配置**
```bash
docker exec basebackend-rocketmq-broker cat /home/rocketmq/broker.conf | grep namesrvAddr
```

**解决方案：**

确保broker.conf中配置正确：
```properties
namesrvAddr = rocketmq-namesrv:9876
```

确保两个容器在同一网络：
```yaml
networks:
  - basebackend-network
```

### 3. 消息发送失败

**错误信息：**
```
No route info of this topic
```

**原因：**
主题不存在或未正确创建

**解决方案：**

1. **启用自动创建主题**

在broker.conf中：
```properties
autoCreateTopicEnable = true
```

2. **手动创建主题**

```bash
# 创建主题
docker exec basebackend-rocketmq-broker sh mqadmin updateTopic \
  -n localhost:9876 \
  -t MyTopic \
  -c DefaultCluster

# 查看主题列表
docker exec basebackend-rocketmq-namesrv sh mqadmin topicList -n localhost:9876
```

3. **通过Console创建**

访问 http://localhost:8180，在"主题"页面创建。

### 4. Console无法访问

**问题：**
无法访问 http://localhost:8180

**检查步骤：**

1. **检查Console是否运行**
```bash
docker ps | grep console
docker logs basebackend-rocketmq-console
```

2. **检查端口映射**
```bash
docker port basebackend-rocketmq-console
```

3. **检查NameServer连接**
```bash
docker exec basebackend-rocketmq-console env | grep namesrv
```

**解决方案：**

重启Console：
```bash
docker-compose restart rocketmq-console
```

### 5. 内存不足

**错误信息：**
```
Java heap space
OutOfMemoryError
```

**解决方案：**

调整JVM内存配置：

```yaml
rocketmq-namesrv:
  environment:
    JAVA_OPT_EXT: "-Xms1g -Xmx1g"

rocketmq-broker:
  environment:
    JAVA_OPT_EXT: "-Xms1g -Xmx1g"
```

### 6. 磁盘空间不足

**错误信息：**
```
No space left on device
```

**检查磁盘使用：**
```bash
# 查看容器磁盘使用
docker exec basebackend-rocketmq-broker df -h

# 查看volume大小
docker system df -v | grep rocketmq
```

**解决方案：**

1. **清理过期消息**

在broker.conf中配置：
```properties
fileReservedTime = 48  # 保留48小时
deleteWhen = 04        # 凌晨4点删除
```

2. **手动清理**
```bash
# 停止broker
docker-compose stop rocketmq-broker

# 清理数据
docker volume rm middleware_rocketmq-broker-data

# 重启
docker-compose up -d rocketmq-broker
```

## 测试和验证

### 基础连接测试

```bash
# 运行测试脚本
./test-rocketmq.sh  # Linux/Mac
test-rocketmq.bat   # Windows
```

### 手动测试

```bash
# 1. 检查NameServer
docker exec basebackend-rocketmq-namesrv sh -c "netstat -an | grep 9876"

# 2. 检查Broker
docker exec basebackend-rocketmq-broker sh -c "netstat -an | grep 10911"

# 3. 查看集群信息
docker exec basebackend-rocketmq-namesrv sh mqadmin clusterList -n localhost:9876

# 4. 创建测试主题
docker exec basebackend-rocketmq-broker sh mqadmin updateTopic \
  -n localhost:9876 -t TestTopic -c DefaultCluster

# 5. 发送测试消息
docker exec basebackend-rocketmq-broker sh mqadmin sendMessage \
  -n localhost:9876 -t TestTopic -p "Hello RocketMQ"
```

### Spring Boot集成测试

**配置：**
```yaml
rocketmq:
  name-server: localhost:9876
  producer:
    group: test-producer-group
    send-message-timeout: 3000
  consumer:
    group: test-consumer-group
```

**测试代码：**
```java
@Autowired
private RocketMQTemplate rocketMQTemplate;

public void testSendMessage() {
    rocketMQTemplate.convertAndSend("TestTopic", "Hello RocketMQ");
    System.out.println("消息发送成功");
}
```

## 监控和诊断

### 查看日志

```bash
# NameServer日志
docker logs -f basebackend-rocketmq-namesrv

# Broker日志
docker logs -f basebackend-rocketmq-broker

# Console日志
docker logs -f basebackend-rocketmq-console

# 查看最近100行
docker logs --tail 100 basebackend-rocketmq-broker
```

### 查看运行状态

```bash
# 查看Broker状态
docker exec basebackend-rocketmq-namesrv sh mqadmin brokerStatus \
  -n localhost:9876 -b rocketmq-broker:10911

# 查看主题状态
docker exec basebackend-rocketmq-namesrv sh mqadmin topicStatus \
  -n localhost:9876 -t TestTopic

# 查看消费者组
docker exec basebackend-rocketmq-namesrv sh mqadmin consumerProgress \
  -n localhost:9876
```

### 查看性能指标

```bash
# 查看Broker统计
docker exec basebackend-rocketmq-namesrv sh mqadmin statsAll \
  -n localhost:9876

# 查看消息堆积
docker exec basebackend-rocketmq-namesrv sh mqadmin consumerProgress \
  -n localhost:9876 -g your-consumer-group
```

## 性能优化

### Broker配置优化

```properties
# broker.conf

# 增加发送线程数
sendMessageThreadPoolNums = 8
pullMessageThreadPoolNums = 8

# 刷盘策略
flushDiskType = ASYNC_FLUSH  # 异步刷盘，性能更好
# flushDiskType = SYNC_FLUSH  # 同步刷盘，可靠性更高

# 主从复制
brokerRole = ASYNC_MASTER  # 异步复制
# brokerRole = SYNC_MASTER  # 同步复制

# 消息存储
mapedFileSizeCommitLog = 1073741824  # 1GB
mapedFileSizeConsumeQueue = 6000000

# 清理策略
fileReservedTime = 72  # 保留72小时
deleteWhen = 04        # 凌晨4点清理
```

### JVM优化

```yaml
rocketmq-broker:
  environment:
    JAVA_OPT_EXT: >
      -Xms2g -Xmx2g
      -XX:+UseG1GC
      -XX:MaxGCPauseMillis=200
      -XX:+UnlockExperimentalVMOptions
      -XX:G1NewSizePercent=30
      -XX:G1MaxNewSizePercent=40
```

### 网络优化

```properties
# broker.conf

# 启用VIP通道
sendMessageWithVIPChannel = true

# 网络缓冲区
socketSndbufSize = 131072
socketRcvbufSize = 131072
```

## 数据备份和恢复

### 备份

```bash
# 停止Broker
docker-compose stop rocketmq-broker

# 备份数据
docker run --rm \
  -v middleware_rocketmq-broker-data:/data \
  -v $(pwd):/backup \
  alpine tar czf /backup/rocketmq-backup-$(date +%Y%m%d).tar.gz /data

# 启动Broker
docker-compose start rocketmq-broker
```

### 恢复

```bash
# 停止Broker
docker-compose stop rocketmq-broker

# 删除旧数据
docker volume rm middleware_rocketmq-broker-data

# 恢复数据
docker volume create middleware_rocketmq-broker-data
docker run --rm \
  -v middleware_rocketmq-broker-data:/data \
  -v $(pwd):/backup \
  alpine tar xzf /backup/rocketmq-backup-20241119.tar.gz -C /

# 启动Broker
docker-compose start rocketmq-broker
```

## 完全重置

如果所有方法都无法解决问题：

```bash
# 停止所有服务
docker-compose down

# 删除所有数据
docker volume rm middleware_rocketmq-namesrv-data
docker volume rm middleware_rocketmq-broker-data

# 删除容器
docker rm -f basebackend-rocketmq-namesrv
docker rm -f basebackend-rocketmq-broker
docker rm -f basebackend-rocketmq-console

# 重新启动
docker-compose up -d
```

## 生产环境建议

### 1. 主从部署

```yaml
# Master Broker
rocketmq-broker-master:
  environment:
    BROKER_ROLE: SYNC_MASTER

# Slave Broker
rocketmq-broker-slave:
  environment:
    BROKER_ROLE: SLAVE
    BROKER_ID: 1
```

### 2. 多NameServer

```yaml
rocketmq-namesrv-1:
  container_name: rocketmq-namesrv-1
  ports:
    - "9876:9876"

rocketmq-namesrv-2:
  container_name: rocketmq-namesrv-2
  ports:
    - "9877:9876"
```

### 3. 持久化配置

```properties
# 同步刷盘
flushDiskType = SYNC_FLUSH

# 同步复制
brokerRole = SYNC_MASTER

# 增加保留时间
fileReservedTime = 168  # 7天
```

### 4. 监控告警

使用Prometheus + Grafana监控RocketMQ：

```yaml
rocketmq-exporter:
  image: apache/rocketmq-exporter:latest
  ports:
    - "5557:5557"
  environment:
    - rocketmq.config.namesrvAddr=rocketmq-namesrv:9876
```

## 参考资料

- [RocketMQ官方文档](https://rocketmq.apache.org/docs/quick-start/)
- [RocketMQ最佳实践](https://rocketmq.apache.org/docs/bestPractice/01bestpractice)
- [Docker镜像文档](https://hub.docker.com/r/apache/rocketmq)
- [RocketMQ Console](https://github.com/apache/rocketmq-dashboard)
