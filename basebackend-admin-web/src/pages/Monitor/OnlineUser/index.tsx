import { useState, useEffect } from 'react'
import { Card, Table, Button, Space, message, Modal, Tag, Statistic, Row, Col } from 'antd'
import { ReloadOutlined, LogoutOutlined, UserOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { OnlineUser } from '@/types'
import { getOnlineUsers, forceLogout } from '@/api/monitor'
import dayjs from 'dayjs'

const OnlineUserPage = () => {
  const [loading, setLoading] = useState(false)
  const [dataSource, setDataSource] = useState<OnlineUser[]>([])

  useEffect(() => {
    loadData()
    // 每30秒自动刷新
    const timer = setInterval(() => {
      loadData()
    }, 30000)
    return () => clearInterval(timer)
  }, [])

  const loadData = async () => {
    setLoading(true)
    try {
      const response = await getOnlineUsers()
      setDataSource(response.data || [])
    } catch (error: any) {
      message.error(error.message || '加载在线用户失败')
    } finally {
      setLoading(false)
    }
  }

  const handleForceLogout = (record: OnlineUser) => {
    Modal.confirm({
      title: '确认下线',
      content: `确定要强制下线用户 "${record.nickname || record.username}" 吗?`,
      okType: 'danger',
      onOk: async () => {
        try {
          await forceLogout(record.token!)
          message.success('用户已强制下线')
          loadData()
        } catch (error: any) {
          message.error(error.message || '强制下线失败')
        }
      },
    })
  }

  const columns: ColumnsType<OnlineUser> = [
    {
      title: '用户名',
      dataIndex: 'username',
      key: 'username',
      width: 120,
    },
    {
      title: '昵称',
      dataIndex: 'nickname',
      key: 'nickname',
      width: 120,
    },
    {
      title: '部门',
      dataIndex: 'deptName',
      key: 'deptName',
      width: 150,
      render: (text) => text || '-',
    },
    {
      title: '登录IP',
      dataIndex: 'loginIp',
      key: 'loginIp',
      width: 140,
    },
    {
      title: '登录地点',
      dataIndex: 'loginLocation',
      key: 'loginLocation',
      width: 120,
    },
    {
      title: '浏览器',
      dataIndex: 'browser',
      key: 'browser',
      width: 120,
      ellipsis: true,
    },
    {
      title: '操作系统',
      dataIndex: 'os',
      key: 'os',
      width: 120,
      ellipsis: true,
    },
    {
      title: '登录时间',
      dataIndex: 'loginTime',
      key: 'loginTime',
      width: 180,
      render: (text: string) => text ? dayjs(text).format('YYYY-MM-DD HH:mm:ss') : '-',
    },
    {
      title: '最后访问时间',
      dataIndex: 'lastAccessTime',
      key: 'lastAccessTime',
      width: 180,
      render: (text: string) => text ? dayjs(text).format('YYYY-MM-DD HH:mm:ss') : '-',
    },
    {
      title: '操作',
      key: 'action',
      width: 120,
      fixed: 'right',
      render: (_: any, record: OnlineUser) => (
        <Button
          type="link"
          size="small"
          danger
          icon={<LogoutOutlined />}
          onClick={() => handleForceLogout(record)}
        >
          强制下线
        </Button>
      ),
    },
  ]

  return (
    <div>
      {/* 统计卡片 */}
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <Card>
            <Statistic
              title="在线用户数"
              value={dataSource.length}
              prefix={<UserOutlined />}
              valueStyle={{ color: '#3f8600' }}
            />
          </Card>
        </Col>
      </Row>

      {/* 在线用户列表 */}
      <Card
        title="在线用户列表"
        extra={
          <Button
            type="primary"
            icon={<ReloadOutlined />}
            onClick={loadData}
            loading={loading}
          >
            刷新
          </Button>
        }
      >
        <Table
          rowKey="token"
          columns={columns}
          dataSource={dataSource}
          loading={loading}
          scroll={{ x: 1400 }}
          pagination={{
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`,
          }}
        />
      </Card>
    </div>
  )
}

export default OnlineUserPage
