# 指标采集问题修复文档

## 问题描述

在 `observability` 模块中引用了 Prometheus 并在 `ApiMetricsAspect` 中采集指标，但在 `admin-api` 模块的 `MetricsQueryService.getSystemOverview()` 方法中查询不到指标数据。

## 问题分析

通过排查发现了以下几个关键问题：

### 0. **Prometheus 无法连接到应用服务（最关键）**

**症状：**
- Prometheus 目标状态显示 `"health":"down"`
- 错误信息：`dial tcp: lookup host.docker.internal on 127.0.0.11:53: no such host`
- 即使指标被正确采集，Prometheus 也无法抓取

**根本原因：**
- Prometheus 在 Docker 容器内运行
- 配置文件使用了 `host.docker.internal:8080`，但这个主机名在 Linux 上不可用
- Docker 容器无法解析宿主机的地址

**解决方案：**
- 将 `host.docker.internal` 替换为宿主机的实际 IP 地址（如 `192.168.66.13`）
- 或者使用 Docker 的 `host` 网络模式

### 1. AOP 切面未生效

**症状：**
- `ApiMetricsAspect` 虽然被 Spring 扫描到，但切面方法从未执行
- `/actuator/prometheus` 端点中没有自定义指标（`api_calls_total` 等）
- 只能看到 Spring Boot 默认的 JVM 指标

**根本原因：**
- 虽然 `observability` 模块引入了 `spring-boot-starter-aop` 依赖
- 但 `AdminApiApplication` 启动类没有显式启用 AOP 自动代理
- Spring Boot 的 AOP 自动配置在某些情况下不会自动生效

### 2. 指标命名不一致

**问题：**
- `CustomMetrics` 中定义的指标名使用了点号分隔符：`api.calls.total`
- Prometheus 规范建议使用下划线：`api_calls_total`
- `MetricsQueryService` 中查询的指标名与实际定义不匹配

**影响：**
- 即使指标被采集，Prometheus 也无法正确识别
- 查询语句无法返回正确结果

### 3. 指标查询语句错误

**问题：**
- `MetricsQueryService.getSystemOverview()` 中使用的 PromQL 查询语句引用了不存在的指标
- 查询 `http_server_requests_seconds_count`（Spring Boot Actuator 默认指标）
- 但实际应该查询 `api_calls_total`（自定义指标）

## 解决方案

### 0. 修复 Prometheus 网络连接（最重要）

#### 获取宿主机 IP

```bash
ip addr show | grep "inet " | grep -v "127.0.0.1" | awk '{print $2}' | cut -d'/' -f1 | head -1
```

输出示例：`192.168.66.13`

#### 修改 Prometheus 配置

编辑 `docker/observability/prometheus.yml`：

```yaml
scrape_configs:
  - job_name: 'basebackend-admin-api'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['192.168.66.13:8080']  # ✅ 使用实际 IP
        labels:
          application: 'basebackend-admin-api'
          service: 'admin'
```

**修改前（错误）：**
```yaml
- targets: ['host.docker.internal:8080']  # ❌ Linux 不支持
```

#### 重启 Prometheus

```bash
cd docker/observability

# 使用 docker-compose
sudo docker-compose restart prometheus

# 或使用 docker compose (v2)
sudo docker compose restart prometheus

# 或者使用提供的脚本
./restart-prometheus.sh
```

#### 验证连接状态

```bash
# 检查 Prometheus 目标状态
curl -s "http://141.98.196.113:9190/api/v1/targets" | grep '"health"'

# 预期看到：
# "health":"up"  ✅
```

### 1. 启用 AOP 自动代理

#### 方案一：在启动类添加注解（已实施）

```java
@SpringBootApplication
@EnableAspectJAutoProxy  // 👈 添加此注解
public class AdminApiApplication {
    // ...
}
```

#### 方案二：在配置文件中启用（已实施）

在 `application-observability.yml` 中添加：

```yaml
spring:
  aop:
    auto: true
    proxy-target-class: true
```

### 2. 修复指标命名

将所有指标名从点号分隔改为下划线分隔：

**修改前：**
```java
Counter.builder("api.calls.total")  // ❌ 不符合 Prometheus 规范
Timer.builder("api.response.time")  // ❌
```

**修改后：**
```java
Counter.builder("api_calls_total")  // ✅ 符合 Prometheus 规范
Timer.builder("api_response_time_seconds")  // ✅
```

### 3. 修复查询语句

更新 `MetricsQueryService.getSystemOverview()` 中的 PromQL 语句：

**修改前：**
```java
// API 调用总数
queryInstantMetric("sum(rate(http_server_requests_seconds_count[5m]))")

// API 错误率
queryInstantMetric("sum(rate(http_server_requests_seconds_count{status=\"error\"}[5m])) / ...")
```

**修改后：**
```java
// API 调用总数 - 使用自定义指标
queryInstantMetric("sum(rate(api_calls_total[5m]))")

// API 错误率 - 使用自定义指标
queryInstantMetric("sum(rate(api_errors_total[5m])) / sum(rate(api_calls_total[5m])) * 100")
```

## 修改文件清单

### 关键修改（必须）

1. **docker/observability/prometheus.yml** ⚠️ 最重要
   - 将 `host.docker.internal:8080` 替换为实际的宿主机 IP
   - 修正 Prometheus 自监控端口：`9090` 而不是 `9190`

2. **AdminApiApplication.java**
   - 添加 `@EnableAspectJAutoProxy` 注解

3. **application-observability.yml**
   - 添加 `spring.aop` 配置

### 次要修改（优化）

4. **CustomMetrics.java**
   - 修改所有指标名：点号 → 下划线
   - 时间单位统一为秒（seconds）

5. **MetricsQueryService.java**
   - 更新所有 PromQL 查询语句
   - 使用自定义指标名替代默认指标名

## 验证步骤

### 1. 重启服务

```bash
# 停止并重启 admin-api 服务
./start-admin-api.sh
```

### 2. 运行测试脚本

```bash
./test-metrics-collection.sh
```

### 3. 手动验证

#### 检查 Actuator 端点

```bash
# 发起一个测试请求触发指标采集
curl http://localhost:8080/actuator/health

# 查看自定义指标
curl http://localhost:8080/actuator/prometheus | grep "^api_"
```

**预期输出：**
```
api_calls_total{method="GET",status="success",uri="/actuator/health",} 1.0
api_response_time_seconds_count{method="GET",uri="/actuator/health",} 1.0
api_response_time_seconds_sum{method="GET",uri="/actuator/health",} 0.015
api_active_requests 0.0
```

#### 检查 Prometheus

```bash
# 查询 Prometheus 中的指标
curl "http://141.98.196.113:9190/api/v1/query?query=api_calls_total"
```

#### 测试 MetricsQueryService

调用 `getSystemOverview()` 方法，应该能正常返回指标数据。

## 常见问题

### Q1: 重启后仍然看不到自定义指标

**A:** 检查以下几点：
1. 确认 AOP 依赖已正确引入（`spring-boot-starter-aop`）
2. 确认 `observability` 包在启动类的 `scanBasePackages` 中
3. 发起至少一次 API 请求以触发指标采集
4. 检查日志中是否有 AOP 相关的错误信息

### Q2: Prometheus 中查询不到指标

**A:** 可能的原因：
1. Prometheus 还未抓取到新指标（等待下一个抓取周期，通常 15-60 秒）
2. Prometheus 配置中未添加 admin-api 的 scrape target
3. 服务的 `/actuator/prometheus` 端点未对 Prometheus 开放

### Q3: 指标值始终为 0 或 null

**A:** 检查：
1. PromQL 查询语句是否正确
2. 时间范围是否合理（`[5m]` 表示最近 5 分钟）
3. 是否有足够的数据点（刚启动的服务可能没有足够的历史数据）

## 最佳实践

### 指标命名规范

遵循 Prometheus 命名规范：
- 使用下划线分隔单词：`api_calls_total`
- Counter 类型指标使用 `_total` 后缀：`api_errors_total`
- Timer/Histogram 会自动添加 `_seconds_count` 和 `_seconds_sum` 后缀
- Gauge 不需要特殊后缀：`api_active_requests`

### AOP 配置

推荐在启动类上显式添加 `@EnableAspectJAutoProxy`，确保 AOP 在所有环境下都能正常工作。

### 指标设计

- 使用合理的标签（tags/labels）进行维度划分
- 避免高基数标签（如用户 ID、完整 URL 等）
- 对 URI 进行清洗，将路径参数替换为占位符（如 `/users/123` → `/users/{id}`）

## 参考资料

- [Prometheus 指标命名规范](https://prometheus.io/docs/practices/naming/)
- [Spring Boot Actuator 文档](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Micrometer 文档](https://micrometer.io/docs)
- [Spring AOP 文档](https://docs.spring.io/spring-framework/reference/core/aop.html)
