import React from 'react'
import { Card, Row, Col, Statistic, Progress, Tag } from 'antd'
import { DatabaseOutlined, ApiOutlined, ThunderboltOutlined } from '@ant-design/icons'
import type { SystemMonitorData } from '../types'

interface SystemMonitorProps {
  data: SystemMonitorData | null
  loading?: boolean
}

export const SystemMonitor: React.FC<SystemMonitorProps> = React.memo(({ data, loading = false }) => {
  if (!data) {
    return (
      <Card title="系统监控" loading={loading}>
        <div style={{ textAlign: 'center', padding: '40px 0', color: '#999' }}>
          暂无监控数据
        </div>
      </Card>
    )
  }

  const getProgressStatus = (value: number, thresholds: [number, number]) => {
    if (value > thresholds[1]) return 'exception'
    if (value > thresholds[0]) return 'normal'
    return 'success'
  }

  const getTagColor = (value: number, thresholds: [number, number]) => {
    if (value > thresholds[1]) return 'red'
    if (value > thresholds[0]) return 'orange'
    return 'green'
  }

  const formatMemory = (bytes: number) => {
    return `${(bytes / 1024 / 1024 / 1024).toFixed(2)} GB`
  }

  return (
    <Card title="系统监控" loading={loading}>
      <Row gutter={[16, 16]}>
        <Col span={12}>
          <div style={{ marginBottom: 8 }}>
            <span>CPU 使用率</span>
            <Tag
              color={getTagColor(data.cpuUsage, [60, 80])}
              style={{ float: 'right' }}
            >
              {data.cpuUsage.toFixed(2)}%
            </Tag>
          </div>
          <Progress percent={data.cpuUsage} status={getProgressStatus(data.cpuUsage, [60, 80])} />
        </Col>
        <Col span={12}>
          <div style={{ marginBottom: 8 }}>
            <span>内存使用率</span>
            <Tag
              color={getTagColor(data.memoryUsage, [70, 85])}
              style={{ float: 'right' }}
            >
              {data.memoryUsage.toFixed(2)}%
            </Tag>
          </div>
          <Progress
            percent={data.memoryUsage}
            status={getProgressStatus(data.memoryUsage, [70, 85])}
          />
        </Col>
      </Row>

      {data.jvmMemoryTotal > 0 && (
        <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
          <Col span={12}>
            <Statistic
              title="JVM 堆内存"
              value={`${formatMemory(data.jvmMemoryUsed)} / ${formatMemory(data.jvmMemoryTotal)}`}
              prefix={<DatabaseOutlined />}
            />
          </Col>
          <Col span={12}>
            <Statistic
              title="系统负载"
              value={`${data.systemLoad.load1.toFixed(2)} / ${data.systemLoad.load5.toFixed(2)} / ${data.systemLoad.load15.toFixed(2)}`}
              valueStyle={{ fontSize: 16 }}
            />
          </Col>
        </Row>
      )}

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col span={8}>
          <Statistic
            title="API 调用/分钟"
            value={data.apiCallsPerMin}
            prefix={<ApiOutlined />}
          />
        </Col>
        <Col span={8}>
          <Statistic
            title="API 错误率"
            value={data.apiErrorRate}
            precision={2}
            suffix="%"
            valueStyle={{ color: data.apiErrorRate > 5 ? '#f5222d' : '#52c41a' }}
          />
        </Col>
        <Col span={8}>
          <Statistic
            title="平均响应时间"
            value={data.avgResponseTime}
            suffix="ms"
            prefix={<ThunderboltOutlined />}
          />
        </Col>
      </Row>
    </Card>
  )
})

SystemMonitor.displayName = 'SystemMonitor'
