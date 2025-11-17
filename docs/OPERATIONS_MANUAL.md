# BaseBackend 微服务运维手册

## 📋 概述

本文档为 BaseBackend 微服务架构的运维指南，涵盖了日常运维操作、监控告警、日志管理、备份恢复、性能调优等内容。

---

## 📊 系统架构

```
┌─────────────────────────────────────────────────────────┐
│                    API Gateway                           │
│              (负载均衡、路由、限流、熔断)                  │
└─────────────────────┬───────────────────────────────────┘
                      │
    ┌─────────────────┴─────────────────┐
    │                                   │
┌───▼───┐                         ┌─────▼──────┐
│用户服务│                         │  权限服务   │
│ 8081  │                         │    8082    │
└───────┘                         └────────────┘
    │
┌─────────────────────────────────────────────────────────┐
│                   业务服务集群                            │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐     │
│  │  字典服务  │ │  部门服务  │ │  日志服务  │ │  菜单服务  │     │
│  │  8083    │ │  8084    │ │  8085    │ │  8088    │     │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘     │
│                                                           │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐                  │
│  │  监控服务  │ │  通知服务  │ │  个人配置  │                  │
│  │  8089    │ │  8090    │ │  8091    │                  │
│  └──────────┘ └──────────┘ └──────────┘                  │
└─────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────────────────────────────────────┐
│                   基础服务设施                            │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐     │
│  │  MySQL   │ │  Redis   │ │  Nacos   │ │ Sentinel │     │
│  │ 数据库    │ │  缓存    │ │配置中心  │ │  限流    │     │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘     │
└─────────────────────────────────────────────────────────┘
```

---

## 📋 服务清单

| 服务名称 | 服务名 | 端口 | 状态 | 健康检查 |
|----------|--------|------|------|----------|
| API Gateway | basebackend-gateway | 8080 | 🟢 | `/actuator/health` |
| 用户服务 | basebackend-user-service | 8081 | 🟢 | `/actuator/health` |
| 权限服务 | basebackend-auth-service | 8082 | 🟢 | `/actuator/health` |
| 字典服务 | basebackend-dict-service | 8083 | 🟢 | `/actuator/health` |
| 部门服务 | basebackend-dept-service | 8084 | 🟢 | `/actuator/health` |
| 日志服务 | basebackend-log-service | 8085 | 🟢 | `/actuator/health` |
| 应用服务 | basebackend-application-service | 8086 | 🟢 | `/actuator/health` |
| 菜单服务 | basebackend-menu-service | 8088 | 🟢 | `/actuator/health` |
| 监控服务 | basebackend-monitor-service | 8089 | 🟢 | `/actuator/health` |
| 通知服务 | basebackend-notification-service | 8090 | 🟢 | `/actuator/health` |
| 个人配置服务 | basebackend-profile-service | 8091 | 🟢 | `/actuator/health` |

---

## 🔄 日常运维操作

### 1. 服务启停

#### 启动单个服务

```bash
# 进入服务目录
cd /opt/basebackend/basebackend-user-service

# 启动服务
nohup mvn spring-boot:run \
    -Dspring-boot.run.jvmArguments="-Xms512m -Xmx1024m" \
    > ../../logs/user-service.log 2>&1 &

# 获取进程 ID
echo $! > user-service.pid

# 等待 10 秒检查启动状态
sleep 10

# 验证服务状态
curl -f http://localhost:8081/actuator/health || echo "服务启动失败"
```

#### 启动所有服务

```bash
#!/bin/bash

services=(
    "basebackend-user-service:8081"
    "basebackend-auth-service:8082"
    "basebackend-dict-service:8083"
    "basebackend-dept-service:8084"
    "basebackend-log-service:8085"
    "basebackend-menu-service:8088"
    "basebackend-monitor-service:8089"
    "basebackend-notification-service:8090"
    "basebackend-profile-service:8091"
    "basebackend-application-service:8086"
)

echo "开始启动所有服务..."

for service_info in "${services[@]}"; do
    IFS=':' read -r service port <<< "$service_info"
    echo "正在启动: $service (端口: $port)"

    cd /opt/basebackend/$service
    nohup mvn spring-boot:run \
        -Dspring-boot.run.jvmArguments="-Xms512m -Xmx1024m" \
        > ../../logs/${service}.log 2>&1 &

    echo "  进程 PID: $!"
    sleep 10

    if curl -f http://localhost:${port}/actuator/health > /dev/null 2>&1; then
        echo "  ✅ 启动成功"
    else
        echo "  ❌ 启动失败，请检查日志: ../../logs/${service}.log"
    fi
done

echo "所有服务启动完成"
```

#### 停止服务

```bash
# 停止单个服务
cd /opt/basebackend/basebackend-user-service
PID=$(cat user-service.pid)
kill -9 $PID 2>/dev/null || echo "进程不存在或已停止"
rm -f user-service.pid

# 批量停止所有服务
./scripts/stop-all-services.sh
```

**停止脚本** (`scripts/stop-all-services.sh`)：
```bash
#!/bin/bash

echo "停止所有微服务..."

# 停止 Spring Boot 应用
pkill -f "spring-boot:run"

# 停止所有 Java 进程 (谨慎使用)
# pkill -9 java

# 检查进程是否还存在
if pgrep -f "spring-boot:run" > /dev/null; then
    echo "仍有进程在运行，手动清理..."
    pkill -9 -f "spring-boot:run"
fi

echo "所有服务已停止"
```

### 2. 服务重启

```bash
#!/bin/bash

SERVICE_NAME=$1

if [ -z "$SERVICE_NAME" ]; then
    echo "用法: $0 <service-name>"
    echo "示例: $0 basebackend-user-service"
    exit 1
fi

echo "重启服务: $SERVICE_NAME"

# 停止服务
PID_FILE="${SERVICE_NAME}.pid"
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    kill -9 $PID 2>/dev/null
    rm -f "$PID_FILE"
fi

# 等待进程完全停止
sleep 5

# 启动服务
cd /opt/basebackend/$SERVICE_NAME
nohup mvn spring-boot:run \
    -Dspring-boot.run.jvmArguments="-Xms512m -Xmx1024m" \
    > ../../logs/${SERVICE_NAME}.log 2>&1 &

echo "PID: $!"
echo $! > "$PID_FILE"

# 等待 10 秒检查启动状态
sleep 10

if curl -f http://localhost:${PORT:-8081}/actuator/health > /dev/null 2>&1; then
    echo "✅ $SERVICE_NAME 重启成功"
else
    echo "❌ $SERVICE_NAME 重启失败"
fi
```

### 3. 服务状态检查

```bash
#!/bin/bash

echo "======================================="
echo "服务状态检查"
echo "======================================="
echo "检查时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo ""

services=(
    "API Gateway:8080"
    "User Service:8081"
    "Auth Service:8082"
    "Dict Service:8083"
    "Dept Service:8084"
    "Log Service:8085"
    "Application Service:8086"
    "Menu Service:8088"
    "Monitor Service:8089"
    "Notification Service:8090"
    "Profile Service:8091"
)

healthy_count=0
total_count=${#services[@]}

for service_info in "${services[@]}"; do
    IFS=':' read -r service port <<< "$service_info"

    if curl -f http://localhost:${port}/actuator/health > /dev/null 2>&1; then
        echo "🟢 $service (端口: $port) - 运行正常"
        healthy_count=$((healthy_count + 1))
    else
        echo "🔴 $service (端口: $port) - 未响应"
    fi
done

echo ""
echo "======================================="
echo "健康服务数: $healthy_count / $total_count"
echo "======================================="

# 计算健康率
if [ $total_count -gt 0 ]; then
    health_rate=$(echo "scale=2; $healthy_count * 100 / $total_count" | bc)
    echo "系统健康率: ${health_rate}%"
fi
```

---

## 📊 监控与告警

### 1. 健康检查

#### 单次健康检查

```bash
# 检查所有服务健康状态
curl -s http://localhost:8081/actuator/health | jq
```

**响应示例**:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "MySQL",
        "validationQuery": "isValid()"
      }
    },
    "redis": {
      "status": "UP",
      "details": {
        "version": "7.0.0"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 107374182400,
        "free": 85899345920,
        "threshold": 10485760,
        "exists": true
      }
    }
  }
}
```

#### 定期健康检查

创建 cron 任务：

```bash
# 编辑 crontab
crontab -e

# 添加以下内容
# 每分钟检查一次健康状态
*/1 * * * * /opt/basebackend/scripts/health-check.sh >> /var/log/basebackend-health.log 2>&1

# 每5分钟发送告警
*/5 * * * * /opt/basebackend/scripts/send-alerts.sh >> /var/log/basebackend-alerts.log 2>&1
```

### 2. 性能监控

#### Prometheus 指标

```bash
# 查看 JVM 内存使用
curl http://localhost:8081/actuator/prometheus | grep jvm_memory_used_bytes

# 查看 HTTP 请求量
curl http://localhost:8081/actuator/prometheus | grep http_server_requests_seconds_count

# 查看 GC 次数
curl http://localhost:8081/actuator/prometheus | grep jvm_gc_collection_seconds_count
```

#### 自定义监控脚本

```bash
#!/bin/bash

LOG_FILE="/var/log/basebackend-monitor.log"
TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')

# 监控服务响应时间
check_response_time() {
    service=$1
    port=$2

    response_time=$(curl -o /dev/null -s -w "%{time_total}" http://localhost:${port}/actuator/health)
    status_code=$(curl -o /dev/null -s -w "%{http_code}" http://localhost:${port}/actuator/health)

    echo "$TIMESTAMP,$service,$response_time,$status_code" >> $LOG_FILE

    # 检查响应时间是否超阈值 (1秒)
    if (( $(echo "$response_time > 1.0" | bc -l) )); then
        echo "⚠️  告警: $service 响应时间过长: ${response_time}s" >> /var/log/basebackend-alerts.log
        # 这里可以添加邮件或短信告警
    fi
}

# 监控所有服务
check_response_time "user-service" 8081
check_response_time "auth-service" 8082
check_response_time "dict-service" 8083
check_response_time "dept-service" 8084
check_response_time "log-service" 8085
check_response_time "menu-service" 8088
check_response_time "monitor-service" 8089
check_response_time "notification-service" 8090
check_response_time "profile-service" 8091
```

### 3. 告警配置

#### 邮件告警

```bash
#!/bin/bash

# 安装邮件工具
# apt-get install mailutils

# 配置邮件服务器
# 修改 /etc/postfix/main.cf

# 发送告警邮件
send_alert_email() {
    subject="$1"
    message="$2"

    echo "$message" | mail -s "$subject" admin@example.com
}

# 告警示例
send_alert_email "BaseBackend 服务告警" "用户服务响应时间过长: 5.0s"
```

#### 短信告警 (阿里云)

```bash
#!/bin/bash

# 安装阿里云 CLI
# curl -fsSL https://raw.githubusercontent.com/aliyun/aliyun-cli/master/install.sh | bash

# 配置 AK/SK
# aliyun configure set --mode AK --region cn-hangzhou --access-key-id <your-access-key-id> --access-key-secret <your-access-key-secret>

# 发送短信
aliyun dyvmsapi SendSms \
    --PhoneNumbers "13800138000" \
    --SignName "BaseBackend系统" \
    --TemplateCode "SMS_123456789" \
    --TemplateParam "{\"service\":\"用户服务\",\"status\":\"异常\"}"
```

---

## 📝 日志管理

### 1. 日志目录

```
/opt/basebackend/logs/
├── gateway.log                    # API Gateway 日志
├── user-service.log               # 用户服务日志
├── auth-service.log               # 权限服务日志
├── dict-service.log               # 字典服务日志
├── dept-service.log               # 部门服务日志
├── log-service.log                # 日志服务日志
├── menu-service.log               # 菜单服务日志
├── monitor-service.log            # 监控服务日志
├── notification-service.log       # 通知服务日志
└── profile-service.log            # 个人配置服务日志
```

### 2. 日志查看

#### 实时查看日志

```bash
# 查看用户服务实时日志
tail -f logs/user-service.log

# 查看所有服务实时日志
for service in user-service auth-service dict-service dept-service; do
    echo "=== $service 日志 ==="
    tail -f logs/${service}.log
    echo ""
done
```

#### 查看历史日志

```bash
# 查看昨天错误日志
grep ERROR logs/user-service.log | grep "$(date -d '1 day ago' '+%Y-%m-%d')"

# 查看最近100行日志
tail -n 100 logs/user-service.log

# 搜索关键词
grep "Exception" logs/user-service.log
grep "SQLException" logs/user-service.log
```

#### 日志统计

```bash
# 统计错误日志数量
grep -c "ERROR" logs/user-service.log

# 统计各类日志数量
grep -c "WARN" logs/user-service.log
grep -c "INFO" logs/user-service.log
grep -c "DEBUG" logs/user-service.log

# 统计最近1小时日志数量
grep "$(date -d '1 hour ago' '+%Y-%m-%d %H')" logs/user-service.log | wc -l
```

### 3. 日志轮转

#### 配置 logrotate

创建 `/etc/logrotate.d/basebackend` 文件：

```
/opt/basebackend/logs/*.log {
    daily
    rotate 30
    compress
    delaycompress
    missingok
    notifempty
    copytruncate
    dateext
}
```

**说明**:
- `daily`: 每天轮转
- `rotate 30`: 保留30个轮转文件
- `compress`: 压缩旧日志文件
- `delaycompress`: 延迟压缩，保留最新的日志文件不压缩
- `missingok`: 丢失日志文件不报错
- `notifempty`: 空文件不轮转
- `copytruncate`: 复制后截断，避免重启服务

手动测试轮转：

```bash
sudo logrotate -d /etc/logrotate.d/basebackend
sudo logrotate -f /etc/logrotate.d/basebackend
```

### 4. 集中式日志

#### 使用 ELK Stack

**Elasticsearch**: 存储日志
**Logstash**: 收集和解析日志
**Kibana**: 可视化日志

**配置 Logstash**:

```ruby
# /etc/logstash/conf.d/basebackend.conf
input {
  file {
    path => "/opt/basebackend/logs/*.log"
    start_position => "beginning"
  }
}

filter {
  grok {
    match => { "message" => "%{TIMESTAMP_ISO8601:timestamp} \[%{DATA:thread}\] %{LOGLEVEL:level} %{DATA:logger} - %{GREEDYDATA:log_message}" }
  }
  date {
    match => [ "timestamp", "yyyy-MM-dd HH:mm:ss.SSS" ]
  }
}

output {
  elasticsearch {
    hosts => ["localhost:9200"]
    index => "basebackend-%{+YYYY.MM.dd}"
  }
}
```

---

## 💾 备份与恢复

### 1. 数据库备份

#### 全量备份

```bash
#!/bin/bash

BACKUP_DIR="/opt/basebackend/backups/mysql"
DATE=$(date '+%Y%m%d_%H%M%S')

mkdir -p $BACKUP_DIR

# 备份所有数据库
mysqldump -u root -p --all-databases \
    --routines \
    --triggers \
    --events \
    --single-transaction \
    --flush-logs \
    --hex-blob \
    > $BACKUP_DIR/full_backup_$DATE.sql

# 压缩备份文件
gzip $BACKUP_DIR/full_backup_$DATE.sql

echo "数据库备份完成: $BACKUP_DIR/full_backup_$DATE.sql.gz"
```

#### 增量备份 (二进制日志)

```bash
# 刷新日志，生成新的二进制日志文件
mysql -u root -p -e "FLUSH LOGS;"

# 复制二进制日志文件
cp /var/log/mysql/mysql-bin.* /opt/basebackend/backups/mysql/binlog/
```

#### 自动备份 (cron)

```bash
# 编辑 crontab
crontab -e

# 添加以下内容
# 每天凌晨2点执行全量备份
0 2 * * * /opt/basebackend/scripts/backup-database.sh >> /var/log/basebackend-backup.log 2>&1

# 每小时执行增量备份
0 * * * * /opt/basebackend/scripts/increment-backup.sh >> /var/log/basebackend-backup.log 2>&1
```

### 2. 恢复数据库

#### 全量恢复

```bash
# 解压备份文件
gunzip full_backup_20251115_020000.sql.gz

# 恢复数据库
mysql -u root -p < full_backup_20251115_020000.sql

# 恢复用户权限
mysql -u root -p -e "FLUSH PRIVILEGES;"
```

#### 时间点恢复

```bash
# 1. 恢复全量备份
mysql -u root -p < full_backup_20251114_020000.sql

# 2. 恢复增量日志到指定时间点
mysqlbinlog --stop-datetime="2025-11-15 10:00:00" /opt/basebackend/backups/mysql/binlog/mysql-bin.000001 | mysql -u root -p
```

### 3. 配置文件备份

```bash
#!/bin/bash

BACKUP_DIR="/opt/basebackend/backups/config"
DATE=$(date '+%Y%m%d_%H%M%S')

mkdir -p $BACKUP_DIR

# 备份 Nacos 配置
cp -r /path/to/nacos/config/* $BACKUP_DIR/nacos_$DATE/

# 备份应用配置
tar -czf $BACKUP_DIR/app-config_$DATE.tar.gz \
    /opt/basebackend/basebackend-*/src/main/resources/application.yml \
    /opt/basebackend/basebackend-*/src/main/resources/config/*

# 备份系统配置
cp /etc/redis/redis.conf $BACKUP_DIR/redis_$DATE.conf
cp /etc/mysql/mysql.conf.d/mysqld.cnf $BACKUP_DIR/mysql_$DATE.cnf

echo "配置备份完成: $BACKUP_DIR"
```

### 4. 自动清理过期备份

```bash
#!/bin/bash

# 删除30天前的备份文件
find /opt/basebackend/backups -type f -mtime +30 -delete

# 删除30天前的日志文件
find /opt/basebackend/logs -type f -mtime +30 -delete

echo "过期文件清理完成"
```

---

## 🔧 性能调优

### 1. JVM 调优

#### 查看 JVM 参数

```bash
# 查看启动参数
jinfo -flags <pid>

# 查看 GC 信息
jstat -gc <pid> 5s

# 查看堆内存使用
jmap -heap <pid>
```

#### 调整 JVM 参数

编辑启动脚本：

```bash
nohup mvn spring-boot:run \
    -Dspring-boot.run.jvmArguments="
        -Xms2g -Xmx2g                    # 堆内存
        -XX:NewRatio=8                   # 新生代比例
        -XX:+UseG1GC                     # 使用 G1GC
        -XX:MaxGCPauseMillis=200         # 最大 GC 暂停时间
        -XX:+PrintGCDetails              # 打印 GC 详情
        -Xloggc:/opt/basebackend/logs/gc.log  # GC 日志
    " \
    > /opt/basebackend/logs/user-service.log 2>&1 &
```

### 2. 数据库调优

#### 查看慢查询

```sql
-- 开启慢查询日志
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 1;
SET GLOBAL log_queries_not_using_indexes = 'ON';

-- 查看慢查询日志
SELECT * FROM mysql.slow_log ORDER BY start_time DESC LIMIT 10;
```

#### 优化索引

```sql
-- 查看未使用索引
SELECT * FROM sys.schema_unused_indexes;

-- 查看表统计信息
SELECT * FROM sys.schema_table_statistics WHERE table_schema = 'basebackend';

-- 分析表
ANALYZE TABLE sys_user;
```

#### 调整连接池

```yaml
# application.yml
spring:
  datasource:
    druid:
      initial-size: 5
      min-idle: 5
      max-active: 20
      max-wait: 60000
      time-between-eviction-runs-millis: 60000
      min-evictable-idle-time-millis: 300000
```

### 3. Redis 调优

#### 查看 Redis 状态

```bash
# 查看 Redis 信息
redis-cli info

# 查看内存使用
redis-cli info memory

# 查看慢查询
redis-cli slowlog get 10
```

#### 优化 Redis 配置

```conf
# /etc/redis/redis.conf
maxmemory 2gb
maxmemory-policy allkeys-lru
save 900 1
save 300 10
save 60 10000
```

---

## 🚨 应急响应

### 1. 服务宕机

#### 快速恢复

```bash
#!/bin/bash

SERVICE_NAME=$1

if [ -z "$SERVICE_NAME" ]; then
    echo "用法: $0 <service-name>"
    exit 1
fi

echo "服务 $SERVICE_NAME 宕机，正在重启..."

# 启动服务
cd /opt/basebackend/$SERVICE_NAME
nohup mvn spring-boot:run \
    -Dspring-boot.run.jvmArguments="-Xms512m -Xmx1024m" \
    > ../../logs/${SERVICE_NAME}.log 2>&1 &

echo "服务已重启，PID: $!"
sleep 10

# 验证服务
if curl -f http://localhost:8081/actuator/health > /dev/null 2>&1; then
    echo "✅ 服务恢复成功"
else
    echo "❌ 服务恢复失败，请检查日志"
fi
```

#### 自动故障恢复

创建 systemd 服务：

```bash
# 创建服务文件
sudo tee /etc/systemd/system/basebackend-user.service > /dev/null <<EOF
[Unit]
Description=BaseBackend User Service
After=network.target

[Service]
Type=simple
User=basebackend
WorkingDirectory=/opt/basebackend/basebackend-user-service
ExecStart=/usr/bin/mvn spring-boot:run
Restart=always
RestartSec=10
StandardOutput=syslog
StandardError=syslog
SyslogIdentifier=basebackend-user

[Install]
WantedBy=multi-user.target
EOF

# 启用服务
sudo systemctl daemon-reload
sudo systemctl enable basebackend-user.service
sudo systemctl start basebackend-user.service

# 查看服务状态
sudo systemctl status basebackend-user.service
```

### 2. 数据库宕机

#### 检查数据库状态

```bash
# 检查 MySQL 进程
ps aux | grep mysqld

# 检查 MySQL 端口
netstat -tlnp | grep 3306

# 尝试连接 MySQL
mysql -u root -p -e "SELECT 1;"
```

#### 重启 MySQL

```bash
# 停止 MySQL
sudo systemctl stop mysql

# 查看错误日志
sudo tail -100 /var/log/mysql/error.log

# 启动 MySQL
sudo systemctl start mysql

# 查看状态
sudo systemctl status mysql
```

### 3. 磁盘空间不足

#### 清理日志文件

```bash
# 查看磁盘使用情况
df -h

# 查找大文件
find /opt/basebackend -type f -size +100M -exec ls -lh {} \;

# 清理日志文件
find /opt/basebackend/logs -type f -mtime +7 -delete

# 清理备份文件
find /opt/basebackend/backups -type f -mtime +30 -delete
```

#### 清理系统缓存

```bash
# 清理页面缓存
sudo sync && sudo sysctl vm.drop_caches=3

# 清理目录缓存
sudo sync && echo 3 > /proc/sys/vm/drop_caches

# 清理交换空间
sudo swapoff -a && sudo swapon -a
```

---

## 📞 联系方式

### 紧急联系人

| 角色 | 姓名 | 电话 | 邮箱 |
|------|------|------|------|
| 系统管理员 | 张三 | 13800138000 | admin@example.com |
| 开发负责人 | 李四 | 13800138001 | dev@example.com |
| DBA | 王五 | 13800138002 | dba@example.com |

### 外部支持

- **技术支持邮箱**: support@basebackend.com
- **官方网站**: https://basebackend.com
- **文档中心**: https://docs.basebackend.com
- **GitHub**: https://github.com/basebackend/basebackend

---

## 📚 参考资料

- [Spring Boot 运维指南](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [MySQL 运维手册](https://dev.mysql.com/doc/)
- [Redis 运维指南](https://redis.io/documentation/)
- [Linux 系统管理](https://www.linux.org/)

---

**编制**: 浮浮酱 🐱（猫娘工程师）
**日期**: 2025-11-15
**版本**: v1.0.0
