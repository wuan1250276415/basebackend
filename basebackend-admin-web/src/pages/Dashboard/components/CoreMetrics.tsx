import React from 'react'
import { Card, Row, Col, Statistic } from 'antd'
import {
  UserOutlined,
  TeamOutlined,
  GlobalOutlined,
  CheckSquareOutlined,
  FileOutlined,
  FileTextOutlined,
  WarningOutlined,
  SyncOutlined,
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import type { CoreMetricsData } from '../types'

interface CoreMetricsProps {
  data: CoreMetricsData
  loading?: boolean
}

export const CoreMetrics: React.FC<CoreMetricsProps> = React.memo(({ data, loading = false }) => {
  const navigate = useNavigate()

  const metrics = [
    {
      title: '用户总数',
      value: data.userCount,
      icon: <UserOutlined />,
      color: '#1890ff',
      path: '/system/user',
    },
    {
      title: '角色总数',
      value: data.roleCount,
      icon: <TeamOutlined />,
      color: '#52c41a',
      path: '/system/role',
    },
    {
      title: '在线用户',
      value: data.onlineUsers,
      icon: <GlobalOutlined />,
      color: '#13c2c2',
      path: '/monitor/online-user',
    },
    {
      title: '待办任务',
      value: data.pendingTasks,
      icon: <CheckSquareOutlined />,
      color: '#faad14',
      path: '/workflow/task',
    },
    {
      title: '文件总数',
      value: data.fileCount,
      icon: <FileOutlined />,
      color: '#722ed1',
      path: '/file/list',
    },
    {
      title: '今日日志',
      value: data.todayLogs,
      icon: <FileTextOutlined />,
      color: '#595959',
      path: '/monitor/operation-log',
    },
    {
      title: '活跃告警',
      value: data.activeAlerts,
      icon: <WarningOutlined />,
      color: '#f5222d',
      path: '/monitor/observability/alert',
    },
    {
      title: '运行中流程',
      value: data.runningProcesses,
      icon: <SyncOutlined spin={data.runningProcesses > 0} />,
      color: '#1890ff',
      path: '/workflow/instance',
    },
  ]

  return (
    <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
      {metrics.map((metric, index) => (
        <Col xs={24} sm={12} md={6} key={index}>
          <Card
            loading={loading}
            hoverable
            onClick={() => navigate(metric.path)}
            style={{ cursor: 'pointer' }}
          >
            <Statistic
              title={metric.title}
              value={metric.value}
              prefix={metric.icon}
              valueStyle={{ color: metric.color }}
            />
          </Card>
        </Col>
      ))}
    </Row>
  )
})

CoreMetrics.displayName = 'CoreMetrics'
