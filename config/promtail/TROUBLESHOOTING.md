# Promtail/Loki 故障排查指南

## 常见问题

### 1. Loki权限错误：mkdir /loki/chunks: permission denied

**问题描述：**
```
level=error ts=2025-11-19T01:48:19.910424023Z caller=log.go:223 msg="error running loki" 
err="mkdir /loki/chunks: permission denied"
```

**原因：**
Loki容器内的用户没有权限在挂载的volume中创建目录。

**解决方案1：使用root用户运行（推荐）**

在docker-compose.yml中添加：
```yaml
loki:
  image: grafana/loki:2.9.3
  user: "0:0"  # 使用root用户
  volumes:
    - loki-data:/loki
```

**解决方案2：预创建目录并设置权限**

```bash
# Linux/Mac
docker volume create observability_loki-data
docker run --rm -v observability_loki-data:/loki alpine sh -c "mkdir -p /loki/chunks /loki/rules && chmod -R 777 /loki"

# Windows (PowerShell)
docker volume create observability_loki-data
docker run --rm -v observability_loki-data:/loki alpine sh -c "mkdir -p /loki/chunks /loki/rules && chmod -R 777 /loki"
```

**解决方案3：删除旧volume重新创建**

```bash
# 停止服务
docker-compose down

# 删除volume
docker volume rm observability_loki-data

# 重新启动
docker-compose up -d loki
```

**快速修复脚本：**

Linux/Mac:
```bash
cd docker/compose/observability
./fix-loki-permissions.sh
```

Windows:
```cmd
cd docker\compose\observability
fix-loki-permissions.bat
```

### 2. Promtail无法读取日志文件

**问题描述：**
Promtail启动正常，但没有收集到日志。

**检查步骤：**

1. **检查日志文件路径**
```bash
# 查看Promtail容器内的日志目录
docker exec basebackend-promtail ls -la /app/logs

# 查看宿主机日志目录
ls -la logs/
```

2. **检查Promtail targets**
```bash
curl http://localhost:9080/targets
```

3. **查看Promtail日志**
```bash
docker logs basebackend-promtail
```

**解决方案：**

如果是权限问题，在docker-compose.yml中添加：
```yaml
promtail:
  image: grafana/promtail:2.9.3
  user: "0:0"  # 使用root用户
  volumes:
    - ../../../logs:/app/logs:ro
```

### 3. Loki配置错误

**问题描述：**
```
error parsing config: yaml: unmarshal errors
```

**检查配置文件：**
```bash
# 验证YAML语法
docker run --rm -v $(pwd)/config/promtail/loki-config.yml:/config.yml alpine/yamllint /config.yml

# 测试Loki配置
docker run --rm -v $(pwd)/config/promtail/loki-config.yml:/etc/loki/local-config.yaml \
  grafana/loki:2.9.3 -config.file=/etc/loki/local-config.yaml -verify-config
```

**常见配置问题：**

1. **Schema版本不匹配**
   - 使用 `schema: v13` 和 `store: tsdb` (Loki 2.8+)
   - 或使用 `schema: v11` 和 `store: boltdb-shipper` (旧版本)

2. **路径配置错误**
   - 确保所有路径都在 `/loki` 下
   - 使用绝对路径

### 4. Promtail无法连接Loki

**问题描述：**
```
level=error msg="error sending batch" error="Post http://loki:3100/loki/api/v1/push: dial tcp: lookup loki"
```

**检查步骤：**

1. **检查网络连接**
```bash
# 检查容器是否在同一网络
docker network inspect basebackend-network

# 测试网络连通性
docker exec basebackend-promtail ping loki
```

2. **检查Loki是否运行**
```bash
docker ps | grep loki
curl http://localhost:3100/ready
```

**解决方案：**

确保两个容器在同一网络：
```yaml
networks:
  basebackend-network:
    external: true
```

### 5. 日志查询慢

**问题描述：**
在Grafana中查询日志响应很慢。

**优化方法：**

1. **使用标签过滤**
```logql
# 好 - 使用标签过滤
{service="basebackend-gateway", level="ERROR"}

# 差 - 全文搜索
{job=~".+"} |= "error"
```

2. **缩短时间范围**
```logql
# 查询最近5分钟
{service="basebackend-gateway"}[5m]
```

3. **增加Loki内存**
```yaml
loki:
  environment:
    - GOMEMLIMIT=2GiB
  deploy:
    resources:
      limits:
        memory: 2G
```

4. **启用查询缓存**
```yaml
query_range:
  results_cache:
    cache:
      embedded_cache:
        enabled: true
        max_size_mb: 500
```

### 6. 日志丢失

**问题描述：**
部分日志没有被收集。

**检查步骤：**

1. **检查Promtail限流**
```bash
curl http://localhost:9080/metrics | grep promtail_dropped
```

2. **检查Loki存储空间**
```bash
docker exec basebackend-loki df -h /loki
```

3. **检查日志文件轮转**
```bash
# 确保Promtail能跟踪轮转的日志文件
ls -la logs/gateway/
```

**解决方案：**

1. **增加限流配置**
```yaml
# promtail-config.yml
limits_config:
  readline_rate: 20000
  readline_burst: 40000
```

2. **增加Loki存储**
```yaml
# loki-config.yml
limits_config:
  ingestion_rate_mb: 20
  ingestion_burst_size_mb: 40
```

### 7. Grafana无法连接Loki

**问题描述：**
在Grafana中添加Loki数据源失败。

**检查步骤：**

1. **检查Loki URL**
```bash
# 从Grafana容器测试
docker exec basebackend-grafana curl http://loki:3100/ready
```

2. **检查网络**
```bash
docker network inspect basebackend-network | grep -A 5 grafana
docker network inspect basebackend-network | grep -A 5 loki
```

**解决方案：**

在Grafana中配置Loki数据源：
- URL: `http://loki:3100` (容器内部访问)
- 或 `http://localhost:3100` (如果Grafana在宿主机)

### 8. 日志格式解析失败

**问题描述：**
日志被收集但字段没有正确解析。

**调试方法：**

1. **测试正则表达式**
使用在线工具测试regex：https://regex101.com/

2. **查看原始日志**
```bash
# 查看实际的日志格式
tail -f logs/gateway/application.log
```

3. **简化pipeline测试**
```yaml
pipeline_stages:
  - regex:
      expression: '^(?P<timestamp>\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3})\s+(?P<level>\w+).*'
  - labels:
      level:
  - output:
      source: message
```

**常见日志格式：**

Spring Boot默认格式：
```
2024-01-15 10:30:45.123 INFO [http-nio-8080-exec-1] c.b.gateway.filter.AuthFilter : User authenticated
```

对应的regex：
```regex
^(?P<timestamp>\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3})\s+(?P<level>\w+)\s+\[(?P<thread>[^\]]+)\]\s+(?P<logger>[^\s]+)\s+:\s+(?P<message>.*)
```

## 监控和诊断

### 查看Promtail指标

```bash
# 所有指标
curl http://localhost:9080/metrics

# 关键指标
curl http://localhost:9080/metrics | grep -E "promtail_(read|sent|dropped)"
```

### 查看Loki指标

```bash
# 所有指标
curl http://localhost:3100/metrics

# 关键指标
curl http://localhost:3100/metrics | grep -E "loki_(ingester|distributor)"
```

### 查看容器日志

```bash
# Loki日志
docker logs -f basebackend-loki

# Promtail日志
docker logs -f basebackend-promtail

# 最近100行
docker logs --tail 100 basebackend-loki
```

### 查看容器资源使用

```bash
# 实时监控
docker stats basebackend-loki basebackend-promtail

# 一次性查看
docker stats --no-stream basebackend-loki basebackend-promtail
```

## 性能调优

### Loki性能优化

```yaml
# loki-config.yml
limits_config:
  # 增加并发查询数
  max_query_parallelism: 64
  
  # 增加查询结果限制
  max_entries_limit_per_query: 50000
  
  # 增加流数量限制
  max_streams_per_user: 50000

# 增加缓存
query_range:
  results_cache:
    cache:
      embedded_cache:
        enabled: true
        max_size_mb: 1000
```

### Promtail性能优化

```yaml
# promtail-config.yml
limits_config:
  # 增加读取速率
  readline_rate: 50000
  readline_burst: 100000

# 增加批量大小
clients:
  - url: http://loki:3100/loki/api/v1/push
    batchwait: 1s
    batchsize: 1048576  # 1MB
```

## 日志级别调整

### 临时调整Loki日志级别

```bash
# 设置为debug
curl -X POST http://localhost:3100/log_level -d "level=debug"

# 恢复为info
curl -X POST http://localhost:3100/log_level -d "level=info"
```

### 临时调整Promtail日志级别

修改docker-compose.yml：
```yaml
promtail:
  command: 
    - -config.file=/etc/promtail/config.yml
    - -log.level=debug
```

## 完全重置

如果问题无法解决，可以完全重置：

```bash
# 停止所有服务
docker-compose down

# 删除所有volume
docker volume rm observability_loki-data
docker volume rm observability_prometheus-data
docker volume rm observability_grafana-data

# 清理容器
docker rm -f basebackend-loki basebackend-promtail

# 重新启动
docker-compose up -d
```

## 获取帮助

如果以上方法都无法解决问题：

1. 收集诊断信息：
```bash
# 保存日志
docker logs basebackend-loki > loki.log 2>&1
docker logs basebackend-promtail > promtail.log 2>&1

# 保存配置
docker exec basebackend-loki cat /etc/loki/local-config.yaml > loki-config-actual.yml
docker exec basebackend-promtail cat /etc/promtail/config.yml > promtail-config-actual.yml

# 保存指标
curl http://localhost:3100/metrics > loki-metrics.txt
curl http://localhost:9080/metrics > promtail-metrics.txt
```

2. 查看官方文档：
   - [Loki文档](https://grafana.com/docs/loki/latest/)
   - [Promtail文档](https://grafana.com/docs/loki/latest/clients/promtail/)

3. 社区支持：
   - [Grafana Community](https://community.grafana.com/)
   - [GitHub Issues](https://github.com/grafana/loki/issues)
