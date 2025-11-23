package com.basebackend.logging.monitoring.config;

/**
 * Grafana 仪表板 JSON 定义
 *
 * 包含完整的日志系统监控仪表板配置：
 * - 总览面板
 * - 性能监控面板
 * - 错误分析面板
 * - 实时日志流面板
 *
 * @author basebackend team
 * @since 2025-11-22
 */
public final class DashboardJson {

    private DashboardJson() {
        // 工具类，私有构造器
    }

    /**
     * 完整的 Grafana 仪表板 JSON 配置
     */
    public static final String JSON = """
{
  "id": null,
  "uid": "logging-observability-dashboard",
  "title": "BaseBackend Logging - 观测平台",
  "description": "BaseBackend 日志系统完整监控面板",
  "tags": ["basebackend", "logging", "observability"],
  "timezone": "browser",
  "schemaVersion": 38,
  "version": 1,
  "refresh": "10s",
  "gnetId": null,
  "graphTooltip": 1,
  "time": {
    "from": "now-6h",
    "to": "now"
  },
  "timepicker": {
    "refresh_intervals": [
      "5s",
      "10s",
      "30s",
      "1m",
      "5m",
      "15m",
      "30m",
      "1h",
      "2h",
      "1d"
    ]
  },
  "templating": {
    "list": [
      {
        "name": "instance",
        "type": "query",
        "label": "实例",
        "datasource": {
          "type": "prometheus",
          "uid": "$${DS_PROMETHEUS}"
        },
        "query": {
          "query": "label_values(up, instance)",
          "refId": "StandardVariableQuery"
        },
        "multi": true,
        "includeAll": true,
        "current": {},
        "hide": 0,
        "skipUrlSync": false,
        "sort": 1
      },
      {
        "name": "environment",
        "type": "query",
        "label": "环境",
        "datasource": {
          "type": "prometheus",
          "uid": "$${DS_PROMETHEUS}"
        },
        "query": {
          "query": "label_values(logging_ingest_count, env)",
          "refId": "StandardVariableQuery"
        },
        "multi": true,
        "includeAll": true,
        "current": {},
        "hide": 0,
        "skipUrlSync": false,
        "sort": 1
      }
    ]
  },
  "annotations": {
    "list": [
      {
        "name": "Annotations & Alerts",
        "datasource": {
          "type": "prometheus",
          "uid": "$${DS_PROMETHEUS}"
        },
        "enable": true,
        "iconColor": "red",
        "titleFormat": "告警",
        "query": "ALERTS{alertstate=\\\"firing\\\"}",
        "tags": [],
        "type": "dashboard"
      }
    ]
  },
  "panels": [
    {
      "type": "row",
      "title": "概览",
      "gridPos": {
        "h": 1,
        "w": 24,
        "x": 0,
        "y": 0
      }
    },
    {
      "type": "stat",
      "title": "总日志数",
      "description": "选定时间范围内的总日志采集量",
      "gridPos": {
        "h": 8,
        "w": 6,
        "x": 0,
        "y": 1
      },
      "targets": [
        {
          "datasource": {
            "type": "prometheus",
            "uid": "$${DS_PROMETHEUS}"
          },
          "expr": "sum(increase(logging_ingest_count[$__range]))",
          "refId": "A",
          "legendFormat": "总日志数"
        }
      ],
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "thresholds"
          },
          "thresholds": {
            "steps": [
              {
                "color": "green",
                "value": 0
              },
              {
                "color": "yellow",
                "value": 1000
              },
              {
                "color": "red",
                "value": 10000
              }
            ]
          },
          "mappings": [],
          "unit": "short"
        },
        "overrides": []
      },
      "options": {
        "orientation": "auto",
        "reduceOptions": {
          "values": false,
          "calcs": [
            "lastNotNull"
          ],
          "fields": ""
        },
        "colorMode": "value",
        "graphMode": "area",
        "justifyMode": "auto",
        "textMode": "auto"
      }
    },
    {
      "type": "stat",
      "title": "错误率",
      "description": "日志处理错误率（百分比）",
      "gridPos": {
        "h": 8,
        "w": 6,
        "x": 6,
        "y": 1
      },
      "targets": [
        {
          "datasource": {
            "type": "prometheus",
            "uid": "$${DS_PROMETHEUS}"
          },
          "expr": "sum(increase(logging_ingest_count{type=\\\"error\\\"}[5m])) / sum(increase(logging_ingest_count[5m])) * 100",
          "refId": "A",
          "legendFormat": "错误率"
        }
      ],
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "thresholds"
          },
          "thresholds": {
            "steps": [
              {
                "color": "green",
                "value": 0
              },
              {
                "color": "yellow",
                "value": 5
              },
              {
                "color": "red",
                "value": 10
              }
            ]
          },
          "mappings": [],
          "unit": "percent"
        },
        "overrides": []
      },
      "options": {
        "orientation": "auto",
        "reduceOptions": {
          "values": false,
          "calcs": [
            "lastNotNull"
          ],
          "fields": ""
        },
        "colorMode": "value",
        "graphMode": "area",
        "justifyMode": "auto",
        "textMode": "auto"
      }
    },
    {
      "type": "stat",
      "title": "平均延迟",
      "description": "日志处理平均延迟（毫秒）",
      "gridPos": {
        "h": 8,
        "w": 6,
        "x": 12,
        "y": 1
      },
      "targets": [
        {
          "datasource": {
            "type": "prometheus",
            "uid": "$${DS_PROMETHEUS}"
          },
          "expr": "rate(logging_latency_seconds_sum[5m]) / rate(logging_latency_seconds_count[5m]) * 1000",
          "refId": "A",
          "legendFormat": "平均延迟"
        }
      ],
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "thresholds"
          },
          "thresholds": {
            "steps": [
              {
                "color": "green",
                "value": 0
              },
              {
                "color": "yellow",
                "value": 100
              },
              {
                "color": "red",
                "value": 500
              }
            ]
          },
          "mappings": [],
          "unit": "ms"
        },
        "overrides": []
      },
      "options": {
        "orientation": "auto",
        "reduceOptions": {
          "values": false,
          "calcs": [
            "lastNotNull"
          ],
          "fields": ""
        },
        "colorMode": "value",
        "graphMode": "area",
        "justifyMode": "auto",
        "textMode": "auto"
      }
    },
    {
      "type": "stat",
      "title": "队列深度",
      "description": "当前队列深度",
      "gridPos": {
        "h": 8,
        "w": 6,
        "x": 18,
        "y": 1
      },
      "targets": [
        {
          "datasource": {
            "type": "prometheus",
            "uid": "$${DS_PROMETHEUS}"
          },
          "expr": "logging_queue_depth",
          "refId": "A",
          "legendFormat": "队列深度"
        }
      ],
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "thresholds"
          },
          "thresholds": {
            "steps": [
              {
                "color": "green",
                "value": 0
              },
              {
                "color": "yellow",
                "value": 500
              },
              {
                "color": "red",
                "value": 1000
              }
            ]
          },
          "mappings": [],
          "unit": "short"
        },
        "overrides": []
      },
      "options": {
        "orientation": "auto",
        "reduceOptions": {
          "values": false,
          "calcs": [
            "lastNotNull"
          ],
          "fields": ""
        },
        "colorMode": "value",
        "graphMode": "area",
        "justifyMode": "auto",
        "textMode": "auto"
      }
    },
    {
      "type": "row",
      "title": "性能监控",
      "gridPos": {
        "h": 1,
        "w": 24,
        "x": 0,
        "y": 9
      }
    },
    {
      "type": "timeseries",
      "title": "吞吐量趋势",
      "description": "日志处理吞吐量（字节/秒）",
      "gridPos": {
        "h": 9,
        "w": 12,
        "x": 0,
        "y": 10
      },
      "targets": [
        {
          "datasource": {
            "type": "prometheus",
            "uid": "$${DS_PROMETHEUS}"
          },
          "expr": "rate(logging_throughput_bytes_sum[5m])",
          "refId": "A",
          "legendFormat": "吞吐量"
        }
      ],
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "palette-classic"
          },
          "custom": {
            "axisPlacement": "auto",
            "barAlignment": 0,
            "drawStyle": "line",
            "fillOpacity": 10,
            "gradientMode": "none",
            "hideFrom": {
              "legend": false,
              "tooltip": false,
              "vis": false
            },
            "lineInterpolation": "linear",
            "lineWidth": 2,
            "pointSize": 5,
            "scaleDistribution": {
              "type": "linear"
            },
            "showPoints": "never",
            "spanNulls": false,
            "stacking": {
              "group": "A",
              "mode": "none"
            },
            "thresholdsStyle": {
              "mode": "off"
            }
          },
          "mappings": [],
          "unit": "Bps"
        },
        "overrides": []
      },
      "options": {
        "legend": {
          "calcs": [
            "mean",
            "max"
          ],
          "displayMode": "table",
          "placement": "bottom",
          "showLegend": true
        },
        "tooltip": {
          "mode": "multi",
          "sort": "none"
        }
      }
    },
    {
      "type": "timeseries",
      "title": "延迟分布",
      "description": "P50, P95, P99 延迟趋势",
      "gridPos": {
        "h": 9,
        "w": 12,
        "x": 12,
        "y": 10
      },
      "targets": [
        {
          "datasource": {
            "type": "prometheus",
            "uid": "$${DS_PROMETHEUS}"
          },
          "expr": "histogram_quantile(0.50, rate(logging_latency_seconds_bucket[5m])) * 1000",
          "refId": "A",
          "legendFormat": "P50"
        },
        {
          "datasource": {
            "type": "prometheus",
            "uid": "$${DS_PROMETHEUS}"
          },
          "expr": "histogram_quantile(0.95, rate(logging_latency_seconds_bucket[5m])) * 1000",
          "refId": "B",
          "legendFormat": "P95"
        },
        {
          "datasource": {
            "type": "prometheus",
            "uid": "$${DS_PROMETHEUS}"
          },
          "expr": "histogram_quantile(0.99, rate(logging_latency_seconds_bucket[5m])) * 1000",
          "refId": "C",
          "legendFormat": "P99"
        }
      ],
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "palette-classic"
          },
          "custom": {
            "axisPlacement": "auto",
            "barAlignment": 0,
            "drawStyle": "line",
            "fillOpacity": 10,
            "gradientMode": "none",
            "hideFrom": {
              "legend": false,
              "tooltip": false,
              "vis": false
            },
            "lineInterpolation": "linear",
            "lineWidth": 2,
            "pointSize": 5,
            "scaleDistribution": {
              "type": "linear"
            },
            "showPoints": "never",
            "spanNulls": false,
            "stacking": {
              "group": "A",
              "mode": "none"
            },
            "thresholdsStyle": {
              "mode": "off"
            }
          },
          "mappings": [],
          "unit": "ms"
        },
        "overrides": []
      },
      "options": {
        "legend": {
          "calcs": [
            "mean",
            "max"
          ],
          "displayMode": "table",
          "placement": "bottom",
          "showLegend": true
        },
        "tooltip": {
          "mode": "multi",
          "sort": "none"
        }
      }
    },
    {
      "type": "row",
      "title": "系统资源",
      "gridPos": {
        "h": 1,
        "w": 24,
        "x": 0,
        "y": 19
      }
    },
    {
      "type": "timeseries",
      "title": "缓存命中率",
      "description": "缓存命中率趋势",
      "gridPos": {
        "h": 8,
        "w": 8,
        "x": 0,
        "y": 20
      },
      "targets": [
        {
          "datasource": {
            "type": "prometheus",
            "uid": "$${DS_PROMETHEUS}"
          },
          "expr": "logging_cache_hit_ratio",
          "refId": "A",
          "legendFormat": "命中率"
        }
      ],
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "thresholds"
          },
          "custom": {
            "axisPlacement": "auto",
            "barAlignment": 0,
            "drawStyle": "line",
            "fillOpacity": 10,
            "gradientMode": "none",
            "hideFrom": {
              "legend": false,
              "tooltip": false,
              "vis": false
            },
            "lineInterpolation": "linear",
            "lineWidth": 2,
            "pointSize": 5,
            "scaleDistribution": {
              "type": "linear"
            },
            "showPoints": "never",
            "spanNulls": false,
            "stacking": {
              "group": "A",
              "mode": "none"
            },
            "thresholdsStyle": {
              "mode": "line"
            }
          },
          "mappings": [],
          "max": 100,
          "min": 0,
          "thresholds": {
            "steps": [
              {
                "color": "red",
                "value": 0
              },
              {
                "color": "yellow",
                "value": 70
              },
              {
                "color": "green",
                "value": 80
              }
            ]
          },
          "unit": "percent"
        },
        "overrides": []
      },
      "options": {
        "legend": {
          "calcs": [
            "mean",
            "max"
          ],
          "displayMode": "table",
          "placement": "bottom",
          "showLegend": true
        },
        "tooltip": {
          "mode": "multi",
          "sort": "none"
        }
      }
    },
    {
      "type": "timeseries",
      "title": "压缩比",
      "description": "GZIP 压缩比率",
      "gridPos": {
        "h": 8,
        "w": 8,
        "x": 8,
        "y": 20
      },
      "targets": [
        {
          "datasource": {
            "type": "prometheus",
            "uid": "$${DS_PROMETHEUS}"
          },
          "expr": "logging_compression_ratio",
          "refId": "A",
          "legendFormat": "压缩比"
        }
      ],
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "palette-classic"
          },
          "custom": {
            "axisPlacement": "auto",
            "barAlignment": 0,
            "drawStyle": "line",
            "fillOpacity": 10,
            "gradientMode": "none",
            "hideFrom": {
              "legend": false,
              "tooltip": false,
              "vis": false
            },
            "lineInterpolation": "linear",
            "lineWidth": 2,
            "pointSize": 5,
            "scaleDistribution": {
              "type": "linear"
            },
            "showPoints": "never",
            "spanNulls": false,
            "stacking": {
              "group": "A",
              "mode": "none"
            },
            "thresholdsStyle": {
              "mode": "off"
            }
          },
          "mappings": [],
          "unit": "percent"
        },
        "overrides": []
      },
      "options": {
        "legend": {
          "calcs": [
            "mean",
            "max"
          ],
          "displayMode": "table",
          "placement": "bottom",
          "showLegend": true
        },
        "tooltip": {
          "mode": "multi",
          "sort": "none"
        }
      }
    },
    {
      "type": "timeseries",
      "title": "活跃线程数",
      "description": "活跃线程数趋势",
      "gridPos": {
        "h": 8,
        "w": 8,
        "x": 16,
        "y": 20
      },
      "targets": [
        {
          "datasource": {
            "type": "prometheus",
            "uid": "$${DS_PROMETHEUS}"
          },
          "expr": "logging_active_threads",
          "refId": "A",
          "legendFormat": "活跃线程"
        }
      ],
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "palette-classic"
          },
          "custom": {
            "axisPlacement": "auto",
            "barAlignment": 0,
            "drawStyle": "line",
            "fillOpacity": 10,
            "gradientMode": "none",
            "hideFrom": {
              "legend": false,
              "tooltip": false,
              "vis": false
            },
            "lineInterpolation": "linear",
            "lineWidth": 2,
            "pointSize": 5,
            "scaleDistribution": {
              "type": "linear"
            },
            "showPoints": "never",
            "spanNulls": false,
            "stacking": {
              "group": "A",
              "mode": "none"
            },
            "thresholdsStyle": {
              "mode": "off"
            }
          },
          "mappings": [],
          "unit": "short"
        },
        "overrides": []
      },
      "options": {
        "legend": {
          "calcs": [
            "mean",
            "max"
          ],
          "displayMode": "table",
          "placement": "bottom",
          "showLegend": true
        },
        "tooltip": {
          "mode": "multi",
          "sort": "none"
        }
      }
    },
    {
      "type": "row",
      "title": "错误分析",
      "gridPos": {
        "h": 1,
        "w": 24,
        "x": 0,
        "y": 28
      }
    },
    {
      "type": "bargauge",
      "title": "错误类型分布",
      "description": "Top 10 错误类型分布",
      "gridPos": {
        "h": 8,
        "w": 12,
        "x": 0,
        "y": 29
      },
      "targets": [
        {
          "datasource": {
            "type": "prometheus",
            "uid": "$${DS_PROMETHEUS}"
          },
          "expr": "topk(10, sum by (type) (increase(logging_ingest_count{type=\\\"error\\\"}[15m])))",
          "refId": "A",
          "legendFormat": "{{type}}"
        }
      ],
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "thresholds"
          },
          "mappings": [],
          "thresholds": {
            "steps": [
              {
                "color": "green",
                "value": 0
              },
              {
                "color": "yellow",
                "value": 10
              },
              {
                "color": "red",
                "value": 100
              }
            ]
          },
          "unit": "short"
        },
        "overrides": []
      },
      "options": {
        "displayMode": "gradient",
        "orientation": "horizontal",
        "reduceOptions": {
          "values": false,
          "calcs": [
            "lastNotNull"
          ],
          "fields": ""
        },
        "showThresholdLabels": false,
        "showThresholdMarkers": true
      }
    },
    {
      "type": "table",
      "title": "错误 TOP 10",
      "description": "错误次数最多的模块",
      "gridPos": {
        "h": 8,
        "w": 12,
        "x": 12,
        "y": 29
      },
      "targets": [
        {
          "datasource": {
            "type": "prometheus",
            "uid": "$${DS_PROMETHEUS}"
          },
          "expr": "topk(10, sum by (job) (increase(logging_error_count[1h])))",
          "refId": "A",
          "legendFormat": "{{job}}"
        }
      ],
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "thresholds"
          },
          "mappings": [],
          "thresholds": {
            "steps": [
              {
                "color": "green",
                "value": 0
              },
              {
                "color": "yellow",
                "value": 100
              },
              {
                "color": "red",
                "value": 1000
              }
            ]
          },
          "unit": "short"
        },
        "overrides": []
      },
      "options": {
        "showHeader": true,
        "sortBy": [
          {
            "desc": true,
            "disableRowClick": false,
            "property": "Value"
          }
        ]
      }
    },
    {
      "type": "row",
      "title": "业务指标",
      "gridPos": {
        "h": 1,
        "w": 24,
        "x": 0,
        "y": 37
      }
    },
    {
      "type": "stat",
      "title": "异步批量操作",
      "description": "异步批量处理次数",
      "gridPos": {
        "h": 6,
        "w": 6,
        "x": 0,
        "y": 38
      },
      "targets": [
        {
          "datasource": {
            "type": "prometheus",
            "uid": "$${DS_PROMETHEUS}"
          },
          "expr": "logging_async_batch_count",
          "refId": "A",
          "legendFormat": "批量操作"
        }
      ],
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "thresholds"
          },
          "mappings": [],
          "thresholds": {
            "steps": [
              {
                "color": "green",
                "value": 0
              },
              {
                "color": "yellow",
                "value": 1000
              },
              {
                "color": "red",
                "value": 10000
              }
            ]
          },
          "unit": "short"
        },
        "overrides": []
      },
      "options": {
        "orientation": "auto",
        "reduceOptions": {
          "values": false,
          "calcs": [
            "lastNotNull"
          ],
          "fields": ""
        },
        "colorMode": "value",
        "graphMode": "area",
        "justifyMode": "auto",
        "textMode": "auto"
      }
    },
    {
      "type": "stat",
      "title": "GZIP 压缩次数",
      "description": "GZIP 压缩操作次数",
      "gridPos": {
        "h": 6,
        "w": 6,
        "x": 6,
        "y": 38
      },
      "targets": [
        {
          "datasource": {
            "type": "prometheus",
            "uid": "$${DS_PROMETHEUS}"
          },
          "expr": "logging_gzip_compression_count",
          "refId": "A",
          "legendFormat": "压缩次数"
        }
      ],
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "thresholds"
          },
          "mappings": [],
          "thresholds": {
            "steps": [
              {
                "color": "green",
                "value": 0
              },
              {
                "color": "yellow",
                "value": 1000
              },
              {
                "color": "red",
                "value": 10000
              }
            ]
          },
          "unit": "short"
        },
        "overrides": []
      },
      "options": {
        "orientation": "auto",
        "reduceOptions": {
          "values": false,
          "calcs": [
            "lastNotNull"
          ],
          "fields": ""
        },
        "colorMode": "value",
        "graphMode": "area",
        "justifyMode": "auto",
        "textMode": "auto"
      }
    },
    {
      "type": "stat",
      "title": "Redis 操作",
      "description": "Redis 缓存操作次数",
      "gridPos": {
        "h": 6,
        "w": 6,
        "x": 12,
        "y": 38
      },
      "targets": [
        {
          "datasource": {
            "type": "prometheus",
            "uid": "$${DS_PROMETHEUS}"
          },
          "expr": "logging_redis_cache_operations",
          "refId": "A",
          "legendFormat": "Redis 操作"
        }
      ],
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "thresholds"
          },
          "mappings": [],
          "thresholds": {
            "steps": [
              {
                "color": "green",
                "value": 0
              },
              {
                "color": "yellow",
                "value": 10000
              },
              {
                "color": "red",
                "value": 100000
              }
            ]
          },
          "unit": "short"
        },
        "overrides": []
      },
      "options": {
        "orientation": "auto",
        "reduceOptions": {
          "values": false,
          "calcs": [
            "lastNotNull"
          ],
          "fields": ""
        },
        "colorMode": "value",
        "graphMode": "area",
        "justifyMode": "auto",
        "textMode": "auto"
      }
    },
    {
      "type": "stat",
      "title": "审计事件",
      "description": "审计事件总数",
      "gridPos": {
        "h": 6,
        "w": 6,
        "x": 18,
        "y": 38
      },
      "targets": [
        {
          "datasource": {
            "type": "prometheus",
            "uid": "$${DS_PROMETHEUS}"
          },
          "expr": "logging_audit_events",
          "refId": "A",
          "legendFormat": "审计事件"
        }
      ],
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "thresholds"
          },
          "mappings": [],
          "thresholds": {
            "steps": [
              {
                "color": "green",
                "value": 0
              },
              {
                "color": "yellow",
                "value": 100
              },
              {
                "color": "red",
                "value": 1000
              }
            ]
          },
          "unit": "short"
        },
        "overrides": []
      },
      "options": {
        "orientation": "auto",
        "reduceOptions": {
          "values": false,
          "calcs": [
            "lastNotNull"
          ],
          "fields": ""
        },
        "colorMode": "value",
        "graphMode": "area",
        "justifyMode": "auto",
        "textMode": "auto"
      }
    }
  ],
  "links": [
    {
      "asDropdown": false,
      "icon": "external link",
      "includeVars": false,
      "keepTime": false,
      "tags": [],
      "targetBlank": true,
      "title": "告警管理",
      "tooltip": "",
      "type": "link",
      "url": "/alerting/list"
    },
    {
      "asDropdown": false,
      "icon": "external link",
      "includeVars": false,
      "keepTime": false,
      "tags": [],
      "targetBlank": true,
      "title": "Grafana 文档",
      "tooltip": "",
      "type": "link",
      "url": "http://docs.grafana.org"
    }
  ]
}
""";
}
