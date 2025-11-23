package com.basebackend.logging.monitoring.config;

/**
 * Prometheus 告警规则配置
 *
 * 定义日志系统相关的告警规则，包括：
 * - 高错误率告警
 * - 高延迟告警
 * - 队列满载告警
 * - 资源使用率告警
 *
 * @author basebackend team
 * @since 2025-11-22
 */
public final class PrometheusAlertRules {

    private PrometheusAlertRules() {
        // 工具类，私有构造器
    }

    /**
     * 获取完整的告警规则 YAML 配置
     */
    public static String getAlertRulesYaml() {
        return """
groups:
  - name: basebackend.logging.alerts
    interval: 30s
    rules:
      # 高错误率告警
      - alert: LoggingHighErrorRate
        expr: |
          (
            sum(increase(logging_ingest_count{type="error"}[5m])) by (job, instance) /
            sum(increase(logging_ingest_count[5m])) by (job, instance)
          ) > 0.05
        for: 2m
        labels:
          severity: critical
          team: platform
          component: logging
        annotations:
          summary: "日志系统错误率过高"
          description: |
            实例 {{ $labels.instance }} 的日志处理错误率在5分钟内超过5%，
            当前值：{{ $value | humanizePercentage }}
          runbook_url: "https://wiki.basebackend.com/runbooks/logging-high-error-rate"

      # 高延迟告警 (P95)
      - alert: LoggingHighLatencyP95
        expr: |
          histogram_quantile(0.95, rate(logging_latency_seconds_bucket[5m])) > 0.5
        for: 3m
        labels:
          severity: warning
          team: platform
          component: logging
        annotations:
          summary: "日志处理P95延迟过高"
          description: |
            实例 {{ $labels.instance }} 的P95延迟超过500ms，
            当前值：{{ $value }}s
          runbook_url: "https://wiki.basebackend.com/runbooks/logging-high-latency"

      # 高延迟告警 (P99)
      - alert: LoggingHighLatencyP99
        expr: |
          histogram_quantile(0.99, rate(logging_latency_seconds_bucket[5m])) > 1.0
        for: 2m
        labels:
          severity: critical
          team: platform
          component: logging
        annotations:
          summary: "日志处理P99延迟过高"
          description: |
            实例 {{ $labels.instance }} 的P99延迟超过1秒，
            当前值：{{ $value }}s
          runbook_url: "https://wiki.basebackend.com/runbooks/logging-high-latency"

      # 队列深度告警
      - alert: LoggingQueueDepthHigh
        expr: logging_queue_depth > 1000
        for: 2m
        labels:
          severity: critical
          team: platform
          component: logging
        annotations:
          summary: "日志队列深度过高"
          description: |
            实例 {{ $labels.instance }} 的队列深度超过1000，
            当前值：{{ $value }}
          runbook_url: "https://wiki.basebackend.com/runbooks/logging-queue-depth"

      # 缓存命中率过低
      - alert: LoggingCacheHitRatioLow
        expr: logging_cache_hit_ratio < 80
        for: 5m
        labels:
          severity: warning
          team: platform
          component: logging
        annotations:
          summary: "日志缓存命中率过低"
          description: |
            实例 {{ $labels.instance }} 的缓存命中率低于80%，
            当前值：{{ $value }}%
          runbook_url: "https://wiki.basebackend.com/runbooks/logging-cache-hit-ratio"

      # 磁盘使用率告警
      - alert: LoggingDiskUsageHigh
        expr: |
          (
            node_filesystem_avail_bytes{mountpoint="/var/lib/prometheus"} /
            node_filesystem_size_bytes{mountpoint="/var/lib/prometheus"}
          ) < 0.15
        for: 5m
        labels:
          severity: warning
          team: sre
          component: storage
        annotations:
          summary: "Prometheus磁盘空间不足"
          description: |
            Prometheus数据目录磁盘使用率超过85%，
            可用空间仅剩余 {{ $value | humanizePercentage }}
          runbook_url: "https://wiki.basebackend.com/runbooks/prometheus-disk-usage"

      # 活跃线程数过高
      - alert: LoggingActiveThreadsHigh
        expr: logging_active_threads > 50
        for: 3m
        labels:
          severity: warning
          team: platform
          component: logging
        annotations:
          summary: "日志系统活跃线程数过高"
          description: |
            实例 {{ $labels.instance }} 的活跃线程数超过50，
            当前值：{{ $value }}
          runbook_url: "https://wiki.basebackend.com/runbooks/logging-active-threads"

      # 内存使用率过高
      - alert: LoggingMemoryUsageHigh
        expr: logging_memory_usage > 85
        for: 5m
        labels:
          severity: critical
          team: platform
          component: logging
        annotations:
          summary: "日志系统内存使用率过高"
          description: |
            实例 {{ $labels.instance }} 的内存使用率超过85%，
            当前值：{{ $value }}%
          runbook_url: "https://wiki.basebackend.com/runbooks/logging-memory-usage"

      # 压缩比异常
      - alert: LoggingCompressionRatioAbnormal
        expr: |
          (
            logging_compression_ratio < 20 or logging_compression_ratio > 90
          )
        for: 10m
        labels:
          severity: warning
          team: platform
          component: logging
        annotations:
          summary: "日志压缩比异常"
          description: |
            实例 {{ $labels.instance }} 的压缩比异常，
            当前值：{{ $value }}%
          runbook_url: "https://wiki.basebackend.com/runbooks/logging-compression-ratio"

      # 批量操作失败
      - alert: LoggingBatchOperationFailures
        expr: |
          increase(logging_error_count[5m]) by (job, instance) > 100
        for: 1m
        labels:
          severity: critical
          team: platform
          component: logging
        annotations:
          summary: "批量操作失败频繁"
          description: |
            实例 {{ $labels.instance }} 在5分钟内失败超过100次，
            当前值：{{ $value }}
          runbook_url: "https://wiki.basebackend.com/runbooks/logging-batch-failures"

      # 实例不可用
      - alert: LoggingInstanceDown
        expr: up{job="basebackend-logging"} == 0
        for: 1m
        labels:
          severity: critical
          team: platform
          component: logging
        annotations:
          summary: "日志实例不可用"
          description: |
            实例 {{ $labels.instance }} 已离线超过1分钟
          runbook_url: "https://wiki.basebackend.com/runbooks/logging-instance-down"

  - name: basebackend.logging.business
    interval: 60s
    rules:
      # 审计事件缺失
      - alert: LoggingAuditEventsLow
        expr: increase(logging_audit_events[10m]) < 1
        for: 15m
        labels:
          severity: warning
          team: security
          component: audit
        annotations:
          summary: "审计事件数量异常"
          description: |
            实例 {{ $labels.instance }} 在10分钟内审计事件数量低于1，
            可能存在审计缺失
          runbook_url: "https://wiki.basebackend.com/runbooks/logging-audit-events"

      # 脱敏操作异常
      - alert: LoggingMaskingOperationsHigh
        expr: increase(logging_masking_operations[5m]) > 10000
        for: 5m
        labels:
          severity: warning
          team: security
          component: masking
        annotations:
          summary: "脱敏操作数量过高"
          description: |
            实例 {{ $labels.instance }} 在5分钟内脱敏操作超过10000次，
            当前值：{{ $value }}
          runbook_url: "https://wiki.basebackend.com/runbooks/logging-masking-operations"
""";
    }

    /**
     * 获取告警规则（适合作为文件内容）
     */
    public static String getAlertRulesForFile() {
        return getAlertRulesYaml();
    }
}
