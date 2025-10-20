import { useEffect, useState } from 'react'
import { Card, Row, Col, Statistic, Table, Tag, Button, Space, message } from 'antd'
import {
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  SendOutlined,
  ReloadOutlined
} from '@ant-design/icons'
import { getMessageStatistics, getQueueMonitor } from '@/api/integration/messageMonitor'

const MessageMonitor = () => {
  const [loading, setLoading] = useState(false)
  const [statistics, setStatistics] = useState<any>({})
  const [queueData, setQueueData] = useState<any>({})
  const [autoRefresh, setAutoRefresh] = useState(true)

  // 获取统计数据
  const fetchStatistics = async () => {
    try {
      setLoading(true)
      const [statsRes, queueRes] = await Promise.all([
        getMessageStatistics(),
        getQueueMonitor()
      ])

      if (statsRes.code === 200) {
        setStatistics(statsRes.data)
      }
      if (queueRes.code === 200) {
        setQueueData(queueRes.data)
      }
    } catch (error) {
      message.error('获取监控数据失败')
    } finally {
      setLoading(false)
    }
  }

  // 自动刷新
  useEffect(() => {
    fetchStatistics()

    let timer: NodeJS.Timeout
    if (autoRefresh) {
      timer = setInterval(fetchStatistics, 30000) // 30秒刷新一次
    }

    return () => {
      if (timer) clearInterval(timer)
    }
  }, [autoRefresh])

  // 队列表格列
  const queueColumns = [
    {
      title: '指标',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '数值',
      dataIndex: 'value',
      key: 'value',
      render: (value: any) => <strong>{value}</strong>
    },
  ]

  const queueTableData = [
    { key: '1', name: '队列总数', value: queueData.queueCount || 0 },
    { key: '2', name: '消息总数', value: queueData.totalMessages || 0 },
    { key: '3', name: '待处理消息', value: queueData.readyMessages || 0 },
    { key: '4', name: '未确认消息', value: queueData.unackedMessages || 0 },
    { key: '5', name: '消费者数量', value: queueData.consumerCount || 0 },
    { key: '6', name: '消息速率(条/秒)', value: queueData.messageRate || 0 },
    { key: '7', name: '确认速率(条/秒)', value: queueData.ackRate || 0 },
  ]

  return (
    <div>
      <Card
        title="消息监控"
        extra={
          <Space>
            <Tag color={autoRefresh ? 'green' : 'default'}>
              {autoRefresh ? '自动刷新' : '手动刷新'}
            </Tag>
            <Button
              icon={<ReloadOutlined />}
              onClick={() => setAutoRefresh(!autoRefresh)}
            >
              {autoRefresh ? '停止刷新' : '启动刷新'}
            </Button>
            <Button
              type="primary"
              icon={<ReloadOutlined />}
              onClick={fetchStatistics}
              loading={loading}
            >
              立即刷新
            </Button>
          </Space>
        }
      >
        {/* 消息统计 */}
        <Row gutter={16} style={{ marginBottom: 24 }}>
          <Col span={6}>
            <Card>
              <Statistic
                title="消息总数"
                value={statistics.total || 0}
                valueStyle={{ color: '#1890ff' }}
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic
                title="待发送"
                value={statistics.pending || 0}
                prefix={<ClockCircleOutlined />}
                valueStyle={{ color: '#faad14' }}
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic
                title="已发送"
                value={statistics.sent || 0}
                prefix={<SendOutlined />}
                valueStyle={{ color: '#52c41a' }}
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic
                title="已消费"
                value={statistics.consumed || 0}
                prefix={<CheckCircleOutlined />}
                valueStyle={{ color: '#52c41a' }}
              />
            </Card>
          </Col>
        </Row>

        <Row gutter={16} style={{ marginBottom: 24 }}>
          <Col span={6}>
            <Card>
              <Statistic
                title="失败"
                value={statistics.failed || 0}
                prefix={<CloseCircleOutlined />}
                valueStyle={{ color: '#ff4d4f' }}
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic
                title="死信"
                value={statistics.deadLetter || 0}
                prefix={<CloseCircleOutlined />}
                valueStyle={{ color: '#ff4d4f' }}
              />
            </Card>
          </Col>
          <Col span={12}>
            <Card>
              <Statistic
                title="成功率"
                value={statistics.successRate || '0.00%'}
                valueStyle={{
                  color: parseFloat(statistics.successRate) > 95 ? '#52c41a' :
                         parseFloat(statistics.successRate) > 80 ? '#faad14' : '#ff4d4f'
                }}
              />
            </Card>
          </Col>
        </Row>

        {/* 队列监控 */}
        <Card title="队列监控" style={{ marginTop: 16 }}>
          <Table
            columns={queueColumns}
            dataSource={queueTableData}
            pagination={false}
            size="small"
            loading={loading}
          />
        </Card>
      </Card>
    </div>
  )
}

export default MessageMonitor
