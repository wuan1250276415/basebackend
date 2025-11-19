# Redis连接故障排查指南

## 问题：无法通过redis-cli连接Redis

### 症状
使用 `redis-cli` 或其他Redis客户端无法连接到Docker中的Redis。

### 常见原因和解决方案

#### 1. 密码配置错误

**问题：** Redis密码配置有语法错误或不匹配

**检查：**
```bash
# 查看Redis容器日志
docker logs basebackend-redis

# 查看Redis配置
docker exec basebackend-redis redis-cli CONFIG GET requirepass
```

**解决方案：**

确保docker-compose.base.yml中的密码配置正确：
```yaml
redis:
  command: redis-server --appendonly yes --requirepass ${REDIS_PASSWORD:-redis2025}
  healthcheck:
    test: ["CMD", "redis-cli", "-a", "${REDIS_PASSWORD:-redis2025}", "ping"]
```

**正确的连接方式：**
```bash
# 使用密码连接
redis-cli -h localhost -p 6379 -a redis2025

# 或者先连接再认证
redis-cli -h localhost -p 6379
127.0.0.1:6379> AUTH redis2025
127.0.0.1:6379> PING
```

#### 2. 端口未正确映射

**检查：**
```bash
# 查看端口映射
docker ps | grep redis

# 测试端口是否开放
telnet localhost 6379
# 或
nc -zv localhost 6379
```

**解决方案：**

确保端口正确映射：
```yaml
redis:
  ports:
    - "6379:6379"  # 宿主机端口:容器端口
```

#### 3. 防火墙阻止连接

**Windows防火墙：**
```powershell
# 检查防火墙规则
netsh advfirewall firewall show rule name=all | findstr 6379

# 添加防火墙规则（如需要）
netsh advfirewall firewall add rule name="Redis" dir=in action=allow protocol=TCP localport=6379
```

**Linux防火墙：**
```bash
# 检查防火墙状态
sudo ufw status

# 允许6379端口
sudo ufw allow 6379/tcp
```

#### 4. Docker网络问题

**检查：**
```bash
# 查看容器网络
docker network inspect basebackend-network

# 查看容器IP
docker inspect basebackend-redis | grep IPAddress
```

**解决方案：**

从其他容器连接时使用容器名：
```bash
# 在同一网络的容器中
redis-cli -h basebackend-redis -p 6379 -a redis2025
```

从宿主机连接时使用localhost：
```bash
redis-cli -h localhost -p 6379 -a redis2025
```

#### 5. Redis未正常启动

**检查：**
```bash
# 查看容器状态
docker ps -a | grep redis

# 查看容器日志
docker logs basebackend-redis

# 查看健康检查状态
docker inspect basebackend-redis | grep -A 10 Health
```

**解决方案：**

重启Redis容器：
```bash
docker-compose -f docker-compose.base.yml restart redis

# 或完全重建
docker-compose -f docker-compose.base.yml down
docker-compose -f docker-compose.base.yml up -d redis
```

## 连接测试步骤

### 1. 基础连接测试

```bash
# 使用测试脚本
./test-redis-connection.sh  # Linux/Mac
test-redis-connection.bat   # Windows

# 手动测试
docker exec basebackend-redis redis-cli -a redis2025 ping
# 应该返回: PONG
```

### 2. 从宿主机连接

```bash
# 安装redis-cli (如果没有)
# Ubuntu/Debian
sudo apt-get install redis-tools

# macOS
brew install redis

# Windows
# 下载Redis for Windows或使用WSL

# 连接测试
redis-cli -h localhost -p 6379 -a redis2025 ping
```

### 3. 从应用连接

**Spring Boot配置：**

```yaml
spring:
  redis:
    host: localhost  # 或 basebackend-redis (容器内)
    port: 6379
    password: redis2025
    timeout: 3000
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
        max-wait: -1ms
```

**Java测试代码：**

```java
@Autowired
private StringRedisTemplate redisTemplate;

public void testRedis() {
    try {
        redisTemplate.opsForValue().set("test", "value");
        String value = redisTemplate.opsForValue().get("test");
        System.out.println("Redis连接成功: " + value);
    } catch (Exception e) {
        System.err.println("Redis连接失败: " + e.getMessage());
    }
}
```

## 常用Redis命令

### 连接和认证
```bash
# 连接
redis-cli -h host -p port -a password

# 认证
AUTH password

# 测试连接
PING

# 查看服务器信息
INFO
INFO server
INFO stats
```

### 基本操作
```bash
# 设置值
SET key value

# 获取值
GET key

# 删除键
DEL key

# 查看所有键
KEYS *

# 查看键是否存在
EXISTS key

# 设置过期时间
EXPIRE key seconds

# 查看剩余时间
TTL key
```

### 监控和调试
```bash
# 监控所有命令
MONITOR

# 查看慢查询
SLOWLOG GET 10

# 查看客户端连接
CLIENT LIST

# 查看内存使用
MEMORY STATS

# 查看配置
CONFIG GET *
```

## 环境变量配置

创建 `.env` 文件：

```env
# Redis配置
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=redis2025

# 或在docker-compose中使用
REDIS_PASSWORD=your_secure_password
```

在docker-compose.base.yml中引用：

```yaml
redis:
  command: redis-server --appendonly yes --requirepass ${REDIS_PASSWORD:-redis2025}
  environment:
    - REDIS_PASSWORD=${REDIS_PASSWORD:-redis2025}
```

## 安全建议

### 1. 使用强密码

```yaml
# 生成强密码
openssl rand -base64 32

# 在docker-compose中使用
redis:
  command: redis-server --appendonly yes --requirepass your_strong_password
```

### 2. 限制访问

```yaml
# 只绑定到localhost（不对外暴露）
redis:
  ports:
    - "127.0.0.1:6379:6379"
```

### 3. 使用Redis ACL (Redis 6+)

```bash
# 创建用户
ACL SETUSER myuser on >mypassword ~* +@all

# 查看用户
ACL LIST

# 使用用户连接
redis-cli -h localhost -p 6379 --user myuser --pass mypassword
```

### 4. 启用TLS/SSL

```yaml
redis:
  command: >
    redis-server
    --appendonly yes
    --requirepass redis2025
    --tls-port 6380
    --port 0
    --tls-cert-file /path/to/redis.crt
    --tls-key-file /path/to/redis.key
    --tls-ca-cert-file /path/to/ca.crt
```

## 性能优化

### 1. 持久化配置

```yaml
redis:
  command: >
    redis-server
    --appendonly yes
    --appendfsync everysec
    --save 900 1
    --save 300 10
    --save 60 10000
```

### 2. 内存限制

```yaml
redis:
  command: >
    redis-server
    --maxmemory 2gb
    --maxmemory-policy allkeys-lru
```

### 3. 连接池配置

```yaml
spring:
  redis:
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
        max-wait: 3000ms
      shutdown-timeout: 100ms
```

## 数据备份和恢复

### 备份

```bash
# RDB备份
docker exec basebackend-redis redis-cli -a redis2025 BGSAVE

# 复制备份文件
docker cp basebackend-redis:/data/dump.rdb ./backup/

# AOF备份
docker exec basebackend-redis redis-cli -a redis2025 BGREWRITEAOF
docker cp basebackend-redis:/data/appendonly.aof ./backup/
```

### 恢复

```bash
# 停止Redis
docker-compose -f docker-compose.base.yml stop redis

# 复制备份文件
docker cp ./backup/dump.rdb basebackend-redis:/data/

# 启动Redis
docker-compose -f docker-compose.base.yml start redis
```

## 监控指标

### 关键指标

```bash
# 内存使用
INFO memory | grep used_memory_human

# 连接数
INFO clients | grep connected_clients

# 命令统计
INFO stats | grep instantaneous_ops_per_sec

# 键空间
INFO keyspace

# 持久化状态
INFO persistence
```

### 使用Redis Exporter (Prometheus)

```yaml
redis-exporter:
  image: oliver006/redis_exporter:latest
  container_name: redis-exporter
  ports:
    - "9121:9121"
  environment:
    - REDIS_ADDR=redis://basebackend-redis:6379
    - REDIS_PASSWORD=redis2025
  networks:
    - basebackend-network
```

## 故障恢复

### 完全重置Redis

```bash
# 停止容器
docker-compose -f docker-compose.base.yml stop redis

# 删除容器和数据
docker rm basebackend-redis
docker volume rm base_redis-data

# 重新创建
docker-compose -f docker-compose.base.yml up -d redis
```

### 清空所有数据

```bash
# 清空当前数据库
docker exec basebackend-redis redis-cli -a redis2025 FLUSHDB

# 清空所有数据库
docker exec basebackend-redis redis-cli -a redis2025 FLUSHALL
```

## 获取帮助

如果问题仍未解决：

1. **查看日志：**
```bash
docker logs -f basebackend-redis
```

2. **检查配置：**
```bash
docker exec basebackend-redis redis-cli -a redis2025 CONFIG GET "*"
```

3. **测试网络：**
```bash
docker exec basebackend-redis ping -c 3 google.com
```

4. **参考文档：**
   - [Redis官方文档](https://redis.io/documentation)
   - [Redis命令参考](https://redis.io/commands)
   - [Docker Redis镜像](https://hub.docker.com/_/redis)
