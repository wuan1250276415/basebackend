import { LineChart, LayoutDashboard, TriangleAlert, CheckCircle2 } from 'lucide-react';
import { useState, useEffect } from 'react'
import { Card, Row, Col, Statistic, Spin, Alert } from 'antd'

import { getSystemOverview, type SystemOverview } from '@/api/observability/metrics'
import { getLogStats, type LogStats } from '@/api/observability/logs'
import { getTraceStats, type TraceStats } from '@/api/observability/traces'
import { getAlertStats, type AlertStats } from '@/api/observability/alerts'

/**
 * 可观测性概览页面
 */
const ObservabilityOverview = () => {
  const [loading, setLoading] = useState(true)
  const [metricsData, setMetricsData] = useState<SystemOverview | null>(null)
  const [logData, setLogData] = useState<LogStats | null>(null)
  const [traceData, setTraceData] = useState<TraceStats | null>(null)
  const [alertData, setAlertData] = useState<AlertStats | null>(null)
  const [error, setError] = useState<string>('')

  // 加载数据
  const loadData = async () => {
    try {
      setLoading(true)
      setError('')

      // 并行加载所有数据
      const [metricsRes, logRes, traceRes, alertRes] = await Promise.all([
        getSystemOverview(),
        getLogStats(),
        getTraceStats(),
        getAlertStats()
      ])

      setMetricsData(metricsRes)
      setLogData(logRes)
      setTraceData(traceRes)
      setAlertData(alertRes)
    } catch (err: any) {
      setError(err.message || '加载数据失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
    // 每30秒自动刷新
    const interval = setInterval(loadData, 30000)
    return () => clearInterval(interval)
  }, [])

  if (error) {
    return (
      <Alert
        message="加载失败"
        description={error}
        type="error"
        showIcon
        style={{ margin: '20px' }}
      />
    )
  }

  return (
    <div style={{ padding: '24px' }}>
      <Spin spinning={loading}>
        <h2 style={{ marginBottom: '24px' }}>
          <LayoutDashboard /> 可观测性概览
        </h2>

        {/* 系统指标 */}
        <Card title="系统指标" style={{ marginBottom: '24px' }}>
          <Row gutter={16}>
            <Col span={6}>
              <Statistic
                title="CPU 核心数"
                value={metricsData?.system?.processors || 0}
              />
            </Col>
            <Col span={6}>
              <Statistic
                title="指标总数"
                value={metricsData?.metricsCount || 0}
              />
            </Col>
            <Col span={6}>
              <Statistic
                title="JVM 总内存"
                value={formatBytes(metricsData?.memory?.total)}
                prefix={<LineChart />}
              />
            </Col>
            <Col span={6}>
              <Statistic
                title="JVM 已用内存"
                value={formatBytes(metricsData?.memory?.used)}
              />
            </Col>
          </Row>
          <Row gutter={16} style={{ marginTop: '16px' }}>
            <Col span={6}>
              <Statistic
                title="操作系统"
                value={metricsData?.system?.osName || '-'}
              />
            </Col>
            <Col span={6}>
              <Statistic
                title="Java 版本"
                value={metricsData?.system?.javaVersion || '-'}
              />
            </Col>
          </Row>
        </Card>

        {/* 日志统计 */}
        <Card title="日志统计（最近1小时）" style={{ marginBottom: '24px' }}>
          <Row gutter={16}>
            <Col span={6}>
              <Statistic
                title="日志总量"
                value={logData?.totalLogs || 0}
                valueStyle={{ color: '#1890ff' }}
              />
            </Col>
            <Col span={6}>
              <Statistic
                title="WARN"
                value={logData?.warnLogs || 0}
                valueStyle={{ color: '#faad14' }}
              />
            </Col>
            <Col span={6}>
              <Statistic
                title="ERROR"
                value={logData?.errorLogs || 0}
                valueStyle={{ color: '#f5222d' }}
                prefix={<TriangleAlert />}
              />
            </Col>
            <Col span={6}>
              <Statistic
                title="INFO"
                value={logData?.infoLogs || 0}
                valueStyle={{ color: '#52c41a' }}
              />
            </Col>
          </Row>
        </Card>

        {/* 追踪统计 */}
        <Card title="追踪统计（最近1小时）" style={{ marginBottom: '24px' }}>
          <Row gutter={16}>
            <Col span={8}>
              <Statistic
                title="总追踪数"
                value={traceData?.totalTraces || 0}
              />
            </Col>
            <Col span={8}>
              <Statistic
                title="平均响应时间"
                value={traceData?.avgDuration || 0}
                suffix="ms"
              />
            </Col>
            <Col span={8}>
              <Statistic
                title="异常追踪"
                value={traceData?.errorTraces || 0}
                valueStyle={{ color: '#faad14' }}
                prefix={<TriangleAlert />}
              />
            </Col>
          </Row>
        </Card>

        {/* 告警统计 */}
        <Card title="告警统计（最近24小时）">
          <Row gutter={16}>
            <Col span={6}>
              <Statistic
                title="规则总数"
                value={alertData?.totalRules || 0}
                valueStyle={{ color: '#f5222d' }}
              />
            </Col>
            <Col span={6}>
              <Statistic
                title="启用规则"
                value={alertData?.activeRules || 0}
                valueStyle={{ color: '#52c41a' }}
              />
            </Col>
            <Col span={6}>
              <Statistic
                title="告警事件总数"
                value={alertData?.totalEvents || 0}
                valueStyle={{ color: '#ff7a45' }}
              />
            </Col>
            <Col span={6}>
              <Statistic
                title="近24小时事件"
                value={alertData?.recentEvents24h || 0}
                prefix={<CheckCircle2 />}
                valueStyle={{ color: '#52c41a' }}
              />
            </Col>
          </Row>
        </Card>
      </Spin>
    </div>
  )
}

const formatBytes = (value?: number) => {
  if (!value) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let size = value
  let unitIndex = 0

  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024
    unitIndex++
  }

  return `${size.toFixed(size >= 10 ? 0 : 1)} ${units[unitIndex]}`
}

export default ObservabilityOverview
