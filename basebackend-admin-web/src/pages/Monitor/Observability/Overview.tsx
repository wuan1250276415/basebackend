import { useState, useEffect } from 'react'
import { Card, Row, Col, Statistic, Spin, Alert } from 'antd'
import {
  LineChartOutlined,
  DashboardOutlined,
  WarningOutlined,
  CheckCircleOutlined
} from '@ant-design/icons'
import { getSystemOverview } from '@/api/observability/metrics'
import { getLogStats } from '@/api/observability/logs'
import { getTraceStats } from '@/api/observability/traces'
import { getAlertStats } from '@/api/observability/alerts'

/**
 * 可观测性概览页面
 */
const ObservabilityOverview = () => {
  const [loading, setLoading] = useState(true)
  const [metricsData, setMetricsData] = useState<any>({})
  const [logData, setLogData] = useState<any>({})
  const [traceData, setTraceData] = useState<any>({})
  const [alertData, setAlertData] = useState<any>({})
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

      setMetricsData(metricsRes.data || {})
      setLogData(logRes.data || {})
      setTraceData(traceRes.data || {})
      setAlertData(alertRes.data || {})
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
          <DashboardOutlined /> 可观测性概览
        </h2>

        {/* 系统指标 */}
        <Card title="系统指标" style={{ marginBottom: '24px' }}>
          <Row gutter={16}>
            <Col span={6}>
              <Statistic
                title="CPU 使用率"
                value={metricsData.cpuUsage || 0}
                precision={2}
                suffix="%"
                valueStyle={{ color: getCpuColor(metricsData.cpuUsage) }}
              />
            </Col>
            <Col span={6}>
              <Statistic
                title="内存使用率"
                value={metricsData.memoryUsage || 0}
                precision={2}
                suffix="%"
                valueStyle={{ color: getMemoryColor(metricsData.memoryUsage) }}
              />
            </Col>
            <Col span={6}>
              <Statistic
                title="API 调用数/分钟"
                value={metricsData.apiCallsTotal || 0}
                precision={0}
                prefix={<LineChartOutlined />}
              />
            </Col>
            <Col span={6}>
              <Statistic
                title="API 错误率"
                value={metricsData.apiErrorRate || 0}
                precision={2}
                suffix="%"
                valueStyle={{ color: getErrorRateColor(metricsData.apiErrorRate) }}
              />
            </Col>
          </Row>
          <Row gutter={16} style={{ marginTop: '16px' }}>
            <Col span={6}>
              <Statistic
                title="平均响应时间"
                value={metricsData.avgResponseTime || 0}
                precision={0}
                suffix="ms"
              />
            </Col>
            <Col span={6}>
              <Statistic
                title="活跃请求数"
                value={metricsData.activeRequests || 0}
                precision={0}
              />
            </Col>
          </Row>
        </Card>

        {/* 日志统计 */}
        <Card title="日志统计（最近1小时）" style={{ marginBottom: '24px' }}>
          <Row gutter={16}>
            <Col span={6}>
              <Statistic
                title="INFO"
                value={logData.infoCount || 0}
                valueStyle={{ color: '#1890ff' }}
              />
            </Col>
            <Col span={6}>
              <Statistic
                title="WARN"
                value={logData.warnCount || 0}
                valueStyle={{ color: '#faad14' }}
              />
            </Col>
            <Col span={6}>
              <Statistic
                title="ERROR"
                value={logData.errorCount || 0}
                valueStyle={{ color: '#f5222d' }}
                prefix={<WarningOutlined />}
              />
            </Col>
            <Col span={6}>
              <Statistic
                title="DEBUG"
                value={logData.debugCount || 0}
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
                value={traceData.totalTraces || 0}
              />
            </Col>
            <Col span={8}>
              <Statistic
                title="平均响应时间"
                value={traceData.avgDuration || 0}
                suffix="ms"
              />
            </Col>
            <Col span={8}>
              <Statistic
                title="慢追踪 (>1s)"
                value={traceData.slowTraces || 0}
                valueStyle={{ color: '#faad14' }}
                prefix={<WarningOutlined />}
              />
            </Col>
          </Row>
        </Card>

        {/* 告警统计 */}
        <Card title="告警统计（最近24小时）">
          <Row gutter={16}>
            <Col span={6}>
              <Statistic
                title="总告警数"
                value={alertData.totalAlerts || 0}
                valueStyle={{ color: '#f5222d' }}
              />
            </Col>
            <Col span={6}>
              <Statistic
                title="CRITICAL"
                value={alertData.bySeverity?.CRITICAL || 0}
                valueStyle={{ color: '#ff4d4f' }}
              />
            </Col>
            <Col span={6}>
              <Statistic
                title="ERROR"
                value={alertData.bySeverity?.ERROR || 0}
                valueStyle={{ color: '#ff7a45' }}
              />
            </Col>
            <Col span={6}>
              <Statistic
                title="通知成功率"
                value={alertData.notifySuccessRate || '0%'}
                prefix={<CheckCircleOutlined />}
                valueStyle={{ color: '#52c41a' }}
              />
            </Col>
          </Row>
        </Card>
      </Spin>
    </div>
  )
}

// 辅助函数：根据值返回颜色
const getCpuColor = (value: number) => {
  if (value > 80) return '#f5222d'
  if (value > 60) return '#faad14'
  return '#52c41a'
}

const getMemoryColor = (value: number) => {
  if (value > 85) return '#f5222d'
  if (value > 70) return '#faad14'
  return '#52c41a'
}

const getErrorRateColor = (value: number) => {
  if (value > 5) return '#f5222d'
  if (value > 1) return '#faad14'
  return '#52c41a'
}

export default ObservabilityOverview
