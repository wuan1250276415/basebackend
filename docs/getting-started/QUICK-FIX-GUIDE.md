# 指标采集问题快速修复指南

## 问题症状

在 `MetricsQueryService.getSystemOverview()` 方法中查询不到 Prometheus 指标数据。

## 根本原因

**Prometheus 无法连接到 admin-api 服务**，因为 Docker 容器内的 Prometheus 无法解析 `host.docker.internal` 主机名（这在 Linux 上不可用）。

## 快速修复步骤（3 步）

### 第 1 步：获取宿主机 IP

```bash
ip addr show | grep "inet " | grep -v "127.0.0.1" | awk '{print $2}' | cut -d'/' -f1 | head -1
```

输出示例：`192.168.66.13`

### 第 2 步：修改 Prometheus 配置

编辑 `docker/observability/prometheus.yml`，将 `host.docker.internal` 替换为实际 IP：

```yaml
scrape_configs:
  - job_name: 'basebackend-admin-api'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['192.168.66.13:8080']  # 👈 修改这里
```

### 第 3 步：重启 Prometheus

```bash
# 使用提供的脚本
./restart-prometheus.sh

# 或手动重启
cd docker/observability
sudo docker-compose restart prometheus
# 或
sudo docker compose restart prometheus
```

## 验证修复

运行诊断脚本：

```bash
./diagnose-metrics.sh
```

**预期输出：**
```
✅ 所有检查通过！指标采集应该正常工作。
```

## 手动验证

### 1. 检查 Prometheus 连接状态

```bash
curl -s "http://141.98.196.113:9190/api/v1/targets" | grep '"health"'
```

预期看到：`"health":"up"` ✅

### 2. 检查自定义指标

```bash
# 触发一次 API 调用
curl http://localhost:8080/actuator/health

# 查看指标
curl http://localhost:8080/actuator/prometheus | grep "^api_calls_total"
```

预期看到类似：
```
api_calls_total{method="GET",status="success",uri="/actuator/health",} 1.0
```

### 3. 查询 Prometheus

```bash
curl -s "http://141.98.196.113:9190/api/v1/query?query=api_calls_total" | jq
```

## 如果仍然不工作

运行完整的诊断脚本查看具体问题：

```bash
./diagnose-metrics.sh
```

该脚本会检查：
1. admin-api 服务状态
2. Actuator 端点可访问性
3. Spring AOP 启用状态
4. 自定义指标采集情况
5. Prometheus 服务状态
6. Prometheus 配置正确性
7. Prometheus 抓取状态
8. Prometheus 中的指标数据

## 其他已修复的问题

除了 Prometheus 连接问题，我们还修复了：

1. ✅ 启用了 Spring AOP（添加 `@EnableAspectJAutoProxy`）
2. ✅ 修正了指标命名（点号 → 下划线）
3. ✅ 更新了 PromQL 查询语句

详细信息请参考：[METRICS-COLLECTION-FIX.md](./METRICS-COLLECTION-FIX.md)

## 文件清单

- `diagnose-metrics.sh` - 自动诊断脚本
- `restart-prometheus.sh` - Prometheus 重启脚本
- `test-metrics-collection.sh` - 指标采集测试脚本
- `METRICS-COLLECTION-FIX.md` - 详细的问题分析和解决方案文档

## 常见问题

### Q: 修改配置后仍然连接失败？

A: 确保重启了 Prometheus 容器，配置热重载可能不生效。

### Q: 看到 "health":"down" 错误？

A: 检查：
1. IP 地址是否正确
2. admin-api 是否在 8080 端口运行
3. 防火墙是否阻止了连接

### Q: Prometheus 中没有指标数据？

A: 
1. 等待 15-60 秒让 Prometheus 完成抓取
2. 确保至少触发过一次 API 调用
3. 检查 Prometheus 目标状态是否为 UP

## 需要帮助？

查看详细文档：`METRICS-COLLECTION-FIX.md`
