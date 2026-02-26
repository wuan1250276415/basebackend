[根目录](../../CLAUDE.md) > **basebackend-observability**

# basebackend-observability

## 模块职责

可观测性基础设施库。提供 Metrics(Micrometer)、Tracing(OpenTelemetry)、Logging(结构化+脱敏)、SLO监控、告警评估等能力，供各微服务引用。

## 对外接口

- `ApiMetricsAspect`: API指标采集切面
- `TracingMdcFilter`: Trace ID注入MDC
- `LogSamplingTurboFilter`: 日志采样过滤
- `MaskingConverter`: 日志脱敏转换器
- `SloMonitoringAspect`: SLO监控切面
- 告警评估器

## 关键依赖

- Micrometer 1.12.0
- OpenTelemetry 1.37.0
- Logstash Logback Encoder 7.4
- Loki Logback Appender 1.5.1

## 测试与质量

6个测试: LogSamplingTurboFilterTest, ApiMetricsAspectTest, SloMonitoringAspectTest, TracingMdcFilterTest, MaskingConverterTest, EvaluatorTest

## 变更记录

| 时间 | 操作 | 说明 |
|------|------|------|
| 2026-02-20 13:17:55 | 初始创建 | 全量扫描生成 |
