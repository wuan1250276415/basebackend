import React from 'react'
import { Card, Row, Col, Statistic } from 'antd'
import {
  ClockCircleOutlined,
  CheckCircleOutlined,
  SyncOutlined,
  PauseCircleOutlined,
} from '@ant-design/icons'

/**
 * 工作流统计卡片组件
 */
interface WorkflowStatisticsProps {
  total: number
  active?: number
  completed?: number
  suspended?: number
  loading?: boolean
  style?: React.CSSProperties
}

export const WorkflowStatistics: React.FC<WorkflowStatisticsProps> = ({
  total,
  active = 0,
  completed = 0,
  suspended = 0,
  loading = false,
  style,
}) => {
  return (
    <Row gutter={16} style={style}>
      <Col xs={24} sm={12} md={6}>
        <Card size="small" loading={loading}>
          <Statistic
            title="总数"
            value={total}
            valueStyle={{ color: '#1890ff' }}
            prefix={<ClockCircleOutlined />}
          />
        </Card>
      </Col>
      <Col xs={24} sm={12} md={6}>
        <Card size="small" loading={loading}>
          <Statistic
            title="进行中"
            value={active}
            valueStyle={{ color: '#52c41a' }}
            prefix={<SyncOutlined spin />}
          />
        </Card>
      </Col>
      <Col xs={24} sm={12} md={6}>
        <Card size="small" loading={loading}>
          <Statistic
            title="已完成"
            value={completed}
            valueStyle={{ color: '#13c2c2' }}
            prefix={<CheckCircleOutlined />}
          />
        </Card>
      </Col>
      <Col xs={24} sm={12} md={6}>
        <Card size="small" loading={loading}>
          <Statistic
            title="已挂起"
            value={suspended}
            valueStyle={{ color: '#faad14' }}
            prefix={<PauseCircleOutlined />}
          />
        </Card>
      </Col>
    </Row>
  )
}

/**
 * 简单统计卡片（两列布局）
 */
interface SimpleStatisticsProps {
  items: Array<{
    title: string
    value: number
    color?: string
    icon?: React.ReactNode
  }>
  loading?: boolean
  style?: React.CSSProperties
}

export const SimpleStatistics: React.FC<SimpleStatisticsProps> = ({
  items,
  loading = false,
  style,
}) => {
  const span = 24 / items.length

  return (
    <Row gutter={16} style={style}>
      {items.map((item, index) => (
        <Col xs={24} sm={12} md={span} key={index}>
          <Card size="small" loading={loading}>
            <Statistic
              title={item.title}
              value={item.value}
              valueStyle={{ color: item.color || '#1890ff' }}
              prefix={item.icon}
            />
          </Card>
        </Col>
      ))}
    </Row>
  )
}
