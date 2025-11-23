import React, { useState, useEffect } from 'react'
import { Layout, Tabs, Card, Empty, Select, Space, message } from 'antd'
import { UserOutlined, SafetyOutlined } from '@ant-design/icons'
import { Role, Application } from '@/types'
import RoleTree from './components/RoleTree'
import RoleUserTab from './components/RoleUserTab'
import RolePermissionTab from './components/RolePermissionTab'
import request from '@/utils/request'
import './index.css'

const { Content, Sider } = Layout

const RoleManagement: React.FC = () => {
  const [applications, setApplications] = useState<Application[]>([])
  const [selectedAppId, setSelectedAppId] = useState<string>()
  const [selectedRole, setSelectedRole] = useState<Role | null>(null)
  const [activeTab, setActiveTab] = useState<string>('users')

  // 加载应用列表
  const loadApplications = async () => {
    try {
      const res = await request.get('/basebackend-system-api/api/system/application/enabled')
      if (res.code === 200) {
        const apps = res.data || []
        setApplications(apps)

        // 默认选中第一个应用
        if (apps.length > 0 && !selectedAppId) {
          setSelectedAppId(apps[0].id)
        }
      }
    } catch (error) {
      console.error('加载应用列表失败:', error)
    }
  }

  useEffect(() => {
    loadApplications()
  }, [])

  // 处理角色选择
  const handleRoleSelect = (role: Role | null) => {
    setSelectedRole(role)
    setActiveTab('users') // 切换角色时默认显示用户Tab
  }

  // 处理应用切换
  const handleAppChange = (appId: string) => {
    setSelectedAppId(appId)
    setSelectedRole(null) // 切换应用时清空选中的角色
  }

  // Tab项
  const tabItems = [
    {
      key: 'users',
      label: (
        <span>
          <UserOutlined />
          角色用户
        </span>
      ),
      children: selectedRole ? (
        <RoleUserTab roleId={selectedRole.id!} />
      ) : (
        <Empty description="请先在左侧选择一个角色" />
      ),
    },
    {
      key: 'permissions',
      label: (
        <span>
          <SafetyOutlined />
          角色权限
        </span>
      ),
      children: selectedRole ? (
        <RolePermissionTab roleId={selectedRole.id!} appId={selectedAppId} />
      ) : (
        <Empty description="请先在左侧选择一个角色" />
      ),
    },
  ]

  return (
    <div className="role-management-container">
      {/* 顶部应用切换 */}
      <Card
        bordered={false}
        style={{ marginBottom: 16 }}
        bodyStyle={{ padding: '16px 24px' }}
      >
        <Space>
          <span style={{ fontWeight: 500 }}>选择应用：</span>
          <Select
            value={selectedAppId}
            onChange={handleAppChange}
            style={{ width: 250 }}
            placeholder="请选择应用"
            loading={applications.length === 0}
          >
            {applications.map((app) => (
              <Select.Option key={app.id} value={app.id!}>
                {app.appName} ({app.appCode})
              </Select.Option>
            ))}
          </Select>
          {selectedRole && (
            <span style={{ marginLeft: 24, color: '#1890ff', fontWeight: 500 }}>
              当前角色：{selectedRole.roleName}
            </span>
          )}
        </Space>
      </Card>

      {/* 主体布局 */}
      <Layout style={{ background: '#fff', minHeight: 'calc(100vh - 180px)' }}>
        {/* 左侧角色树 */}
        <Sider width={320} theme="light" style={{ borderRight: '1px solid #f0f0f0' }}>
          <div style={{ padding: 16, height: '100%' }}>
            <RoleTree
              appId={selectedAppId}
              onSelect={handleRoleSelect}
              selectedRoleId={selectedRole?.id}
            />
          </div>
        </Sider>

        {/* 右侧内容区 */}
        <Content style={{ padding: 16 }}>
          {selectedRole ? (
            <Card bordered={false}>
              <Tabs
                activeKey={activeTab}
                onChange={setActiveTab}
                items={tabItems}
                destroyInactiveTabPane
              />
            </Card>
          ) : (
            <Card bordered={false}>
              <Empty
                description={
                  <div>
                    <p>请先在左侧选择一个角色</p>
                    <p style={{ color: '#999', fontSize: 12 }}>
                      可以点击"新建角色"按钮创建新角色
                    </p>
                  </div>
                }
              />
            </Card>
          )}
        </Content>
      </Layout>
    </div>
  )
}

export default RoleManagement
