import React from 'react'
import { Card, Tabs, List, Tag, Badge, Empty } from 'antd'
import {
  UserOutlined,
  EnvironmentOutlined,
  ClockCircleOutlined,
  BellOutlined,
} from '@ant-design/icons'
import type { RecentLoginRecord, RecentOperationLog, SystemNotification } from '../types'

interface RecentActivitiesProps {
  recentLogins: RecentLoginRecord[]
  recentOperations: RecentOperationLog[]
  notifications: SystemNotification[]
  unreadCount: number
  loading?: boolean
}

export const RecentActivities: React.FC<RecentActivitiesProps> = React.memo(
  ({ recentLogins, recentOperations, notifications, unreadCount, loading = false }) => {
    const items = [
      {
        key: 'logins',
        label: '登录记录',
        children: (
          <List
            dataSource={recentLogins}
            locale={{ emptyText: <Empty description="暂无登录记录" /> }}
            renderItem={(item) => (
              <List.Item>
                <List.Item.Meta
                  avatar={<UserOutlined style={{ fontSize: 20, color: '#1890ff' }} />}
                  title={
                    <span>
                      {item.username}
                      {item.status === 'success' ? (
                        <Tag color="success" style={{ marginLeft: 8 }}>
                          成功
                        </Tag>
                      ) : (
                        <Tag color="error" style={{ marginLeft: 8 }}>
                          失败
                        </Tag>
                      )}
                    </span>
                  }
                  description={
                    <div>
                      <div>
                        <EnvironmentOutlined /> {item.ip} {item.location && `(${item.location})`}
                      </div>
                      <div>
                        <ClockCircleOutlined /> {item.loginTime}
                      </div>
                    </div>
                  }
                />
              </List.Item>
            )}
          />
        ),
      },
      {
        key: 'operations',
        label: '操作日志',
        children: (
          <List
            dataSource={recentOperations}
            locale={{ emptyText: <Empty description="暂无操作记录" /> }}
            renderItem={(item) => (
              <List.Item>
                <List.Item.Meta
                  title={
                    <span>
                      {item.operator} · {item.module}
                      <Tag color="blue" style={{ marginLeft: 8 }}>
                        {item.operationType}
                      </Tag>
                    </span>
                  }
                  description={
                    <div>
                      <div>{item.description || '无描述'}</div>
                      <div style={{ marginTop: 4, color: '#8c8c8c' }}>
                        <ClockCircleOutlined /> {item.operationTime}
                      </div>
                    </div>
                  }
                />
              </List.Item>
            )}
          />
        ),
      },
      {
        key: 'notifications',
        label: (
          <span>
            系统通知
            {unreadCount > 0 && (
              <Badge count={unreadCount} style={{ marginLeft: 8 }} />
            )}
          </span>
        ),
        children: (
          <List
            dataSource={notifications}
            locale={{ emptyText: <Empty description="暂无通知" /> }}
            renderItem={(item) => (
              <List.Item>
                <List.Item.Meta
                  avatar={<BellOutlined style={{ fontSize: 20, color: '#faad14' }} />}
                  title={
                    <span>
                      {item.title}
                      {!item.read && (
                        <Badge status="processing" text="未读" style={{ marginLeft: 8 }} />
                      )}
                    </span>
                  }
                  description={
                    <div>
                      <div>{item.content}</div>
                      <div style={{ marginTop: 4, color: '#8c8c8c' }}>
                        <ClockCircleOutlined /> {item.createTime}
                      </div>
                    </div>
                  }
                />
              </List.Item>
            )}
          />
        ),
      },
    ]

    return (
      <Card title="最近动态" loading={loading} style={{ marginTop: 16 }}>
        <Tabs items={items} />
      </Card>
    )
  }
)

RecentActivities.displayName = 'RecentActivities'
