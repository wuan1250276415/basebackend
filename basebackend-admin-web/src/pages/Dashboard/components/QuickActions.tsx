import React from 'react'
import { Card, Row, Col } from 'antd'
import {
  UserAddOutlined,
  TeamOutlined,
  FormOutlined,
  FileAddOutlined,
  MonitorOutlined,
  LineChartOutlined,
  ApiOutlined,
  SettingOutlined,
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import type { QuickAction } from '../types'

export const QuickActions: React.FC = React.memo(() => {
  const navigate = useNavigate()

  const actions: QuickAction[] = [
    {
      key: 'addUser',
      icon: <UserAddOutlined style={{ fontSize: 24 }} />,
      title: '新增用户',
      description: '快速添加系统用户',
      path: '/system/user',
      color: '#1890ff',
    },
    {
      key: 'roleManagement',
      icon: <TeamOutlined style={{ fontSize: 24 }} />,
      title: '角色管理',
      description: '管理用户角色权限',
      path: '/system/role',
      color: '#52c41a',
    },
    {
      key: 'startProcess',
      icon: <FormOutlined style={{ fontSize: 24 }} />,
      title: '发起流程',
      description: '创建新的工作流',
      path: '/workflow/template',
      color: '#faad14',
    },
    {
      key: 'uploadFile',
      icon: <FileAddOutlined style={{ fontSize: 24 }} />,
      title: '上传文件',
      description: '上传和管理文件',
      path: '/file/list',
      color: '#722ed1',
    },
    {
      key: 'serverMonitor',
      icon: <MonitorOutlined style={{ fontSize: 24 }} />,
      title: '服务器监控',
      description: '查看服务器状态',
      path: '/monitor/server',
      color: '#fa541c',
    },
    {
      key: 'observability',
      icon: <LineChartOutlined style={{ fontSize: 24 }} />,
      title: '可观测性',
      description: '日志追踪和监控',
      path: '/monitor/observability/overview',
      color: '#13c2c2',
    },
    {
      key: 'apiDocs',
      icon: <ApiOutlined style={{ fontSize: 24 }} />,
      title: 'API文档',
      description: '查看接口文档',
      path: '/developer/api-docs',
      color: '#2f54eb',
    },
    {
      key: 'systemSettings',
      icon: <SettingOutlined style={{ fontSize: 24 }} />,
      title: '系统设置',
      description: '菜单和配置管理',
      path: '/system/menu',
      color: '#595959',
    },
  ]

  return (
    <Card title="快捷入口" style={{ marginTop: 16 }}>
      <Row gutter={[16, 16]}>
        {actions.map((action) => (
          <Col xs={24} sm={12} md={6} key={action.key}>
            <Card
              hoverable
              style={{
                textAlign: 'center',
                cursor: 'pointer',
                borderLeft: `4px solid ${action.color}`,
              }}
              onClick={() => navigate(action.path)}
            >
              <div style={{ color: action.color, marginBottom: 8 }}>{action.icon}</div>
              <div style={{ fontWeight: 500, marginBottom: 4 }}>{action.title}</div>
              <div style={{ fontSize: 12, color: '#8c8c8c' }}>{action.description}</div>
            </Card>
          </Col>
        ))}
      </Row>
    </Card>
  )
})

QuickActions.displayName = 'QuickActions'
