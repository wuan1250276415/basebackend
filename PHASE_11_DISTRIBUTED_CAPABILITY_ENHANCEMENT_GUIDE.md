# Phase 11: 分布式能力增强实施指南

## 📋 概述

本文档详细描述了如何为 BaseBackend 微服务架构增加分布式能力，包括分布式事务、分布式缓存、任务调度、链路追踪、消息队列等。

---

## 🎯 实施目标

### 核心目标
1. 引入分布式事务管理 (Seata)
2. 构建分布式缓存集群 (Redis Cluster)
3. 部署分布式任务调度 (XXL-Job)
4. 配置分布式链路追踪 (SkyWalking)
5. 搭建分布式消息队列 (RocketMQ)

### 技术栈
- **分布式事务**: Seata 1.7.0
- **分布式缓存**: Redis Cluster 7.0
- **任务调度**: XXL-Job 2.4.0
- **链路追踪**: SkyWalking 9.2.0
- **消息队列**: RocketMQ 5.1.4

---

## 🏗️ 架构设计

### 整体架构图

```
┌─────────────────────────────────────────────────────────────┐
│                 API Gateway & Load Balancer                  │
└─────────────────────┬───────────────────────────────────────┘
                      │
        ┌─────────────┴─────────────┐
        │                           │
┌───────▼────────┐         ┌────────▼────────┐
│   微服务集群                      │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐  │
│  │ 用户服务  │ │ 权限服务  │ │ 业务服务  │  │
│  └────┬─────┘ └─────┬────┘ └─────┬────┘  │
│       │            │            │        │
│       └────────────┴────────────┘        │
└───────────────────┬───────────────────────┘
                    │
┌───────────────────▼───────────────────────┐
│               分布式能力层                   │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐     │
│  │  Seata  │ │Redis集群│ │XXL-Job  │     │
│  │分布式事务│ │  缓存   │ │任务调度  │     │
│  └─────┬───┘ └────┬────┘ └────┬────┘     │
│        │          │           │          │
│  ┌─────▼───┐ ┌────▼────┐ ┌────▼────┐    │
│  │SkyWalking│ │RocketMQ │ │ Nacos   │    │
│  │链路追踪  │ │消息队列  │ │配置中心  │    │
│  └─────────┘ └─────────┘ └─────────┘    │
└───────────────────────────────────────────┘
```

---

## 📊 组件详细设计

### 1. Seata 分布式事务

#### 架构特点
- **事务模式**: AT 模式 (自动回滚)
- **协调器**: Seata Server
- **事务存储**: MySQL
- **注册中心**: Nacos

#### 配置结构
```
basebackend-seata/
├── src/main/resources/
│   ├── seata-server/
│   │   ├── seata-server.sh              # 启动脚本
│   │   ├── registry.conf                # 注册配置
│   │   └── logback.xml                  # 日志配置
│   └── application.yml                  # 应用配置
└── pom.xml                              # 依赖配置
```

#### 核心配置
```yaml
# seata-server 配置文件
registry {
  type = "nacos"
  nacos {
    serverAddr = "localhost:8848"
    namespace = "basebackend"
    group = "SEATA_GROUP"
    cluster = "default"
  }
}

config {
  type = "nacos"
  nacos {
    serverAddr = "localhost:8848"
    namespace = "basebackend"
    group = "SEATA_GROUP"
  }
}
```

### 2. Redis Cluster 分布式缓存

#### 架构特点
- **节点数**: 6 个 (3 主 3 从)
- **端口范围**: 7000-7005
- **持久化**: RDB + AOF
- **分片数**: 16384 槽位

#### 配置结构
```
basebackend-redis-cluster/
├── redis-cluster.sh                       # 集群启动脚本
├── redis.conf                             # 配置文件模板
└── 集群管理脚本/
```

#### 节点配置
```conf
# Redis Cluster 节点配置
port 7000
bind 0.0.0.0
protected-mode no

# 集群配置
cluster-enabled yes
cluster-config-file nodes-7000.conf
cluster-node-timeout 15000
cluster-require-full-coverage no

# 持久化配置
appendonly yes
save 900 1
save 300 10
save 60 10000
```

### 3. XXL-Job 分布式任务调度

#### 架构特点
- **调度中心**: XXL-Job Admin
- **执行器**: 嵌入各微服务
- **调度模式**: 贝蒂式调度
- **数据库**: MySQL

#### 配置结构
```
basebackend-xxl-job/
├── admin/
│   └── src/main/resources/
│       └── application.yml               # 调度中心配置
├── executor/
│   └── src/main/resources/
│       └── application.yml               # 执行器配置
└── pom.xml
```

#### 核心配置
```yaml
# 调度中心配置
xxl:
  job:
    admin:
      addresses: http://localhost:8080/xxl-job-admin
    executor:
      appname: basebackend-executor
      port: 9999
```

### 4. SkyWalking 分布式链路追踪

#### 架构特点
- **OAP Server**: SkyWalking 后端
- **UI**: SkyWalking Web UI
- **存储**: ElasticSearch
- **告警**: Nacos

#### 配置结构
```
basebackend-skywalking/
├── oap/
│   └── config/
│       └── application.yml               # OAP Server 配置
├── agent/
│   └── skywalking-agent.jar             # Java Agent
└── webapp/
    └── webapp.yml                       # Web UI 配置
```

#### 核心配置
```yaml
# OAP Server 配置
cluster:
  selector: ${SW_CLUSTER:nacos}
  nacos:
    serviceName: ${SW_SERVICE_NAME:"SkyWalking"}
    hostPort: ${SW_CLUSTER_NACOS_HOST_PORT:localhost:8848}
    namespace: ${SW_CLUSTER_NACOS_NAMESPACE:"basebackend"}
```

### 5. RocketMQ 分布式消息队列

#### 架构特点
- **NameServer**: 集群模式
- **Broker**: 异步部署
- **消息模型**: 发布/订阅
- **事务消息**: 支持

#### 配置结构
```
basebackend-rocketmq/
├── nameserver/
│   ├── start-nameserver.sh              # NameServer 启动脚本
│   └── conf/
│       └── nameserver.properties        # NameServer 配置
├── broker/
│   ├── start-broker.sh                  # Broker 启动脚本
│   └── conf/
│       └── broker.properties            # Broker 配置
└── tools.sh                              # 管理工具
```

#### 核心配置
```properties
# NameServer 配置
listenPort=9876
serverSocketCores=2000
serverSocketQueues=8000
serverChannelMaxIdleTimeSeconds=100
```

---

## 🔧 详细实施步骤

### Step 1: 部署 Seata 分布式事务

#### 1.1 下载 Seata
```bash
cd /opt/basebackend
wget https://github.com/seata/seata/releases/download/v1.7.0/seata-server-1.7.0.tar.gz
tar -xzf seata-server-1.7.0.tar.gz
mv seata-server seata
```

#### 1.2 配置 Seata
```bash
# 创建 registry.conf
cat > seata/conf/registry.conf << 'EOF'
registry {
  type = "nacos"
  nacos {
    serverAddr = "localhost:8848"
    namespace = "basebackend"
    group = "SEATA_GROUP"
  }
}

config {
  type = "nacos"
  nacos {
    serverAddr = "localhost:8848"
    namespace = "basebackend"
    group = "SEATA_GROUP"
  }
}
EOF
```

#### 1.3 启动 Seata
```bash
cd seata/bin
nohup sh seata-server.sh -p 7091 -m db > /opt/basebackend/logs/seata/seata-server.log 2>&1 &
```

#### 1.4 验证 Seata
```bash
curl http://localhost:7091
```

### Step 2: 部署 Redis Cluster

#### 2.1 安装 Redis
```bash
# 安装 Redis 7.0
cd /tmp
wget http://download.redis.io/releases/redis-7.0.0.tar.gz
tar -xzf redis-7.0.0.tar.gz
cd redis-7.0.0
make && make install
```

#### 2.2 创建集群目录
```bash
mkdir -p /opt/basebackend/redis-cluster/{7000,7001,7002,7003,7004,7005}
```

#### 2.3 创建配置文件
```bash
# 为每个节点创建配置文件
for i in {0..5}; do
    port=$((7000 + i))
    cat > /opt/basebackend/redis-cluster/$port/redis.conf << EOF
port $port
bind 0.0.0.0
cluster-enabled yes
cluster-config-file nodes-$port.conf
cluster-node-timeout 15000
appendonly yes
EOF
done
```

#### 2.4 启动集群
```bash
# 启动所有实例
for i in {0..5}; do
    port=$((7000 + i))
    redis-server /opt/basebackend/redis-cluster/$port/redis.conf &
done

# 创建集群
sleep 5
redis-cli --cluster create \
    127.0.0.1:7000 \
    127.0.0.1:7001 \
    127.0.0.1:7002 \
    127.0.0.1:7003 \
    127.0.0.1:7004 \
    127.0.0.1:7005 \
    --cluster-replicas 1
```

### Step 3: 部署 XXL-Job

#### 3.1 下载 XXL-Job
```bash
cd /opt/basebackend
wget https://github.com/xuxueli/xxl-job/releases/download/2.4.0/xxl-job-2.4.0.zip
unzip xxl-job-2.4.0.zip
mv xxl-job-2.4.0 xxl-job
```

#### 3.2 配置数据库
```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS xxl_job DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 导入 SQL 脚本
mysql -u root -p xxl_job < xxl-job/doc/db/tables_xxl_job.sql
```

#### 3.3 启动调度中心
```bash
cd xxl-job/xxl-job-admin
mvn clean package -DskipTests
java -jar xxl-job-admin-2.4.0.jar --spring.profiles.active=dev
```

#### 3.4 验证调度中心
```bash
curl http://localhost:8080/xxl-job-admin
```

### Step 4: 部署 SkyWalking

#### 4.1 下载 SkyWalking
```bash
cd /opt/basebackend
wget https://archive.apache.org/dist/skywalking/9.2.0/apache-skywalking-apm-9.2.0.tar.gz
tar -xzf apache-skywalking-apm-9.2.0.tar.gz
mv apache-skywalking-apm-bin skywalking
```

#### 4.2 配置 OAP Server
```bash
# 修改配置文件
cat > skywalking/oap/config/application.yml << 'EOF'
cluster:
  selector: ${SW_CLUSTER:nacos}
  nacos:
    serviceName: ${SW_SERVICE_NAME:"SkyWalking"}
    hostPort: ${SW_CLUSTER_NACOS_HOST_PORT:localhost:8848}
    namespace: ${SW_CLUSTER_NACOS_NAMESPACE:"basebackend"}

core:
  selector: ${SW_CORE:slow}
  slow:
    defaultSamplingRate: ${SW_CORE_DEFAULT_SAMPLING_RATE:100}
    storage:
      selector: ${SW_STORAGE:elasticsearch}
      elasticsearch:
        servers: ${SW_STORAGE_ES_SERVERS:127.0.0.1:9200}
        indexPrefix: ${SW_STORAGE_ES_INDEX_PREFIX:sw}
EOF
```

#### 4.3 启动 OAP Server
```bash
cd skywalking
oapService.sh
```

#### 4.4 启动 Web UI
```bash
webappService.sh
```

### Step 5: 部署 RocketMQ

#### 5.1 下载 RocketMQ
```bash
cd /opt/basebackend
wget https://archive.apache.org/dist/rocketmq/rocketmq-all/5.1.4/rocketmq-all-5.1.4-bin-release.zip
unzip rocketmq-all-5.1.4-bin-release.zip
mv rocketmq-all-5.1.4-bin-release rocketmq
```

#### 5.2 配置 NameServer
```bash
cat > rocketmq/conf/nameserver.properties << 'EOF'
listenPort=9876
serverSocketCores=2000
serverSocketQueues=8000
serverChannelMaxIdleTimeSeconds=100
EOF
```

#### 5.3 启动 NameServer
```bash
cd rocketmq
nohup sh tools.sh org.apache.rocketmq.namesrv.NamesrvStartup -c conf/nameserver.properties > /opt/basebackend/logs/rocketmq/nameserver.log 2>&1 &
```

#### 5.4 验证 NameServer
```bash
sh tools.sh mqadmin clusterList -t 'cluster-demo'
```

---

## 📝 微服务集成

### 1. 集成 Seata

#### 1.1 添加依赖
```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-seata</artifactId>
</dependency>
```

#### 1.2 配置 Seata
```yaml
seata:
  application-id: ${spring.application.name}
  tx-service-group: basebackend_tx_group
  service:
    vgroup-mapping:
      basebackend_tx_group: default
  registry:
    type: nacos
    nacos:
      server-addr: ${NACOS_SERVER_ADDR:localhost:8848}
      namespace: ${NACOS_NAMESPACE:basebackend}
      group: SEATA_GROUP
```

#### 1.3 使用分布式事务
```java
@Service
public class UserService {
    @GlobalTransactional
    public void transfer(Long fromUserId, Long toUserId, BigDecimal amount) {
        // 扣减账户余额
        debit(fromUserId, amount);

        // 增加账户余额
        credit(toUserId, amount);
    }
}
```

### 2. 集成 Redis Cluster

#### 2.1 添加依赖
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
</dependency>
```

#### 2.2 配置 Redis Cluster
```yaml
spring:
  redis:
    cluster:
      nodes:
        - localhost:7000
        - localhost:7001
        - localhost:7002
        - localhost:7003
        - localhost:7004
        - localhost:7005
      max-redirects: 3
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
```

#### 2.3 使用 Redis
```java
@RestController
public class CacheController {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/cache/{key}")
    public Object getCache(@PathVariable String key) {
        return redisTemplate.opsForValue().get(key);
    }
}
```

### 3. 集成 XXL-Job

#### 3.1 添加依赖
```xml
<dependency>
    <groupId>com.xuxueli</groupId>
    <artifactId>xxl-job-core</artifactId>
    <version>2.4.0</version>
</dependency>
```

#### 3.2 配置 XXL-Job
```yaml
xxl:
  job:
    admin:
      addresses: http://localhost:8080/xxl-job-admin
    executor:
      appname: basebackend-executor
      port: 9999
```

#### 3.3 使用 XXL-Job
```java
@Component
public class ScheduledJob {
    @XxlJob("demoJob")
    public void demoJob() {
        System.out.println("执行定时任务");
    }
}
```

### 4. 集成 SkyWalking

#### 4.1 添加 Agent
```bash
# 下载 SkyWalking Agent
cp skywalking/agent/skywalking-agent.jar /opt/basebackend/agent/
```

#### 4.2 配置启动参数
```bash
-javaagent:/opt/basebackend/agent/skywalking-agent.jar
-DSW_AGENT_NAME=basebackend-user-service
-DSW_AGENT_COLLECTOR_BACKEND_SERVICES=localhost:12800
```

#### 4.3 自动追踪
```yaml
# SkyWalking 会自动追踪以下组件
- Spring MVC
- Spring Boot
- Redis
- MySQL
- HTTP Client
```

### 5. 集成 RocketMQ

#### 5.1 添加依赖
```xml
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-spring-boot-starter</artifactId>
    <version>2.2.3</version>
</dependency>
```

#### 5.2 配置 RocketMQ
```yaml
rocketmq:
  name-server: localhost:9876
  producer:
    group: basebackend_producer_group
```

#### 5.3 使用 RocketMQ
```java
@Component
public class MessageProducer {
    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    public void sendMessage(String message) {
        rocketMQTemplate.convertAndSend("basebackend-topic", message);
    }
}
```

---

## 🚀 性能调优

### 1. Seata 调优

```properties
# 事务存储优化
store.db.max-conn=20
store.db.min-conn=5
store.db.max-wait=5000
```

### 2. Redis Cluster 调优

```conf
# 内存优化
maxmemory 4gb
maxmemory-policy allkeys-lru

# 网络优化
tcp-backlog 511
tcp-keepalive 300

# AOF 优化
appendfsync everysec
auto-aof-rewrite-percentage 100
```

### 3. XXL-Job 调优

```yaml
# 执行器优化
xxl:
  job:
    executor:
      # 并发线程数
      thread-num: 20
      # 最大日志保留天数
      log-retention-days: 30
```

### 4. SkyWalking 调优

```yaml
# 存储优化
storage:
  elasticsearch:
    indexReplicasNumber: 1
    indexShardsNumber: 2
    ttl: 7
```

### 5. RocketMQ 调优

```properties
# NameServer 优化
listenPort=9876
serverSocketCores=2000
serverSocketQueues=8000

# Broker 优化
flushDiskType=ASYNC_FLUSH
flushCommitLogLeastPages=4
flushConsumeQueueLeastPages=2
```

---

## 📊 监控告警

### 1. 监控指标

| 组件 | 监控指标 | 告警阈值 |
|------|----------|----------|
| Seata | 事务数、成功率 | 成功率 < 99% |
| Redis Cluster | 内存使用率、节点状态 | 使用率 > 80% |
| XXL-Job | 任务失败率、运行时间 | 失败率 > 5% |
| SkyWalking | 追踪数、响应时间 | P99 > 1s |
| RocketMQ | 消息堆积量、延迟 | 堆积量 > 1000 |

### 2. 告警配置

#### SkyWalking 告警
```yaml
# 在 Nacos 中配置告警规则
alerts:
  - name: "High Response Time"
    expression: "service_response_time > 1000"
    message: "服务响应时间过长"
```

#### XXL-Job 告警
```yaml
xxl:
  job:
    alarm:
      # 邮件配置
      email:
        host: smtp.example.com
        port: 587
        username: admin@example.com
        password: yourpassword
```

---

## 🧪 测试验证

### 1. 功能测试

#### Seata 测试
```bash
# 测试分布式事务
curl -X POST http://localhost:8081/api/users/transfer \
    -H "Content-Type: application/json" \
    -d '{"fromUserId":1,"toUserId":2,"amount":100}'
```

#### Redis Cluster 测试
```bash
# 集群状态检查
redis-cli -p 7000 cluster info

# 读写测试
redis-cli -c -p 7000 set test_key test_value
redis-cli -c -p 7000 get test_key
```

#### XXL-Job 测试
```bash
# 在调度中心手动触发任务
```

### 2. 性能测试

```bash
# Redis Cluster 性能测试
redis-benchmark -h localhost -p 7000 -t set,get -n 100000 -c 50

# RocketMQ 性能测试
tools.sh org.apache.rocketmq.tools.command.tools.TestConsumer

# SkyWalking 性能测试
curl http://localhost:8080/api/users
```

---

## 📚 最佳实践

### 1. 分布式事务最佳实践
- **事务范围**: 保持事务小而精
- **隔离级别**: 使用读已提交 (Read Committed)
- **超时时间**: 设置合理的超时时间
- **重试机制**: 实现指数退避重试

### 2. 缓存最佳实践
- **缓存更新**: 使用 Cache Aside 模式
- **缓存穿透**: 使用布隆过滤器
- **缓存雪崩**: 设置随机过期时间
- **缓存一致性**: 使用双删策略

### 3. 任务调度最佳实践
- **任务拆分**: 避免任务过大
- **执行时间**: 避开业务高峰期
- **并发控制**: 限制并发任务数
- **故障处理**: 实现重试机制

### 4. 链路追踪最佳实践
- **采样率**: 设置合理的采样率 (100% 或 50%)
- **标签**: 使用有意义的标签
- **过滤**: 过滤无关的 HTTP 请求
- **告警**: 设置合理的告警阈值

### 5. 消息队列最佳实践
- **消息顺序**: 保持消息有序性
- **消息持久化**: 开启消息持久化
- **消费幂等**: 实现消费幂等性
- **消息堆积**: 及时处理消息堆积

---

## 📞 技术支持

### 联系方式
- **技术支持邮箱**: support@basebackend.com
- **技术文档**: https://docs.basebackend.com/distributed
- **GitHub**: https://github.com/basebackend/distributed-capabilities

### 参考资料
- [Seata 官方文档](https://seata.io/)
- [Redis Cluster 文档](https://redis.io/docs/manual/cluster/)
- [XXL-Job 官方文档](https://www.xuxueli.com/xxl-job/)
- [SkyWalking 官方文档](https://skywalking.apache.org/)
- [RocketMQ 官方文档](https://rocketmq.apache.org/)

---

**编制**: 浮浮酱 🐱（猫娘工程师）
**日期**: 2025-11-15
**版本**: v1.0.0
