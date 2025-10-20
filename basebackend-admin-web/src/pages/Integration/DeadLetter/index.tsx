import { useEffect, useState } from 'react'
import {
  Card,
  Table,
  Space,
  Tag,
  Button,
  Select,
  Drawer,
  Descriptions,
  message,
  Popconfirm,
  Modal
} from 'antd'
import {
  SearchOutlined,
  EyeOutlined,
  RedoOutlined,
  DeleteOutlined,
  ExclamationCircleOutlined
} from '@ant-design/icons'
import {
  getDeadLetterPage,
  getDeadLetter,
  redeliverDeadLetter,
  discardDeadLetter,
  batchRedeliverDeadLetters
} from '@/api/integration/deadLetter'

const DeadLetter = () => {
  const [loading, setLoading] = useState(false)
  const [dataSource, setDataSource] = useState([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [size, setSize] = useState(20)
  const [status, setStatus] = useState('')
  const [detailVisible, setDetailVisible] = useState(false)
  const [detailData, setDetailData] = useState<any>(null)
  const [selectedRowKeys, setSelectedRowKeys] = useState<number[]>([])

  // 获取死信列表
  const fetchData = async () => {
    try {
      setLoading(true)
      const res = await getDeadLetterPage({
        page,
        size,
        status: status || undefined,
      })

      if (res.code === 200) {
        setDataSource(res.data.records)
        setTotal(res.data.total)
      }
    } catch (error) {
      message.error('获取死信列表失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchData()
  }, [page, size, status])

  // 查看详情
  const handleViewDetail = async (id: number) => {
    try {
      const res = await getDeadLetter(id)
      if (res.code === 200) {
        setDetailData(res.data)
        setDetailVisible(true)
      }
    } catch (error) {
      message.error('获取详情失败')
    }
  }

  // 重新投递
  const handleRedeliver = async (id: number) => {
    try {
      await redeliverDeadLetter(id)
      message.success('重新投递成功')
      fetchData()
    } catch (error) {
      message.error('重新投递失败')
    }
  }

  // 丢弃
  const handleDiscard = async (id: number) => {
    try {
      await discardDeadLetter(id)
      message.success('已丢弃')
      fetchData()
    } catch (error) {
      message.error('丢弃失败')
    }
  }

  // 批量重新投递
  const handleBatchRedeliver = () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请选择要重新投递的死信')
      return
    }

    Modal.confirm({
      title: '确认批量重新投递',
      icon: <ExclamationCircleOutlined />,
      content: `确定要重新投递选中的 ${selectedRowKeys.length} 条死信吗？`,
      onOk: async () => {
        try {
          await batchRedeliverDeadLetters(selectedRowKeys)
          message.success('批量重新投递成功')
          setSelectedRowKeys([])
          fetchData()
        } catch (error) {
          message.error('批量重新投递失败')
        }
      },
    })
  }

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
    },
    {
      title: '消息ID',
      dataIndex: 'messageId',
      key: 'messageId',
      width: 200,
      ellipsis: true,
    },
    {
      title: '主题',
      dataIndex: 'topic',
      key: 'topic',
    },
    {
      title: '路由键',
      dataIndex: 'routingKey',
      key: 'routingKey',
      ellipsis: true,
    },
    {
      title: '原始队列',
      dataIndex: 'originalQueue',
      key: 'originalQueue',
      ellipsis: true,
    },
    {
      title: '重试次数',
      dataIndex: 'retryCount',
      key: 'retryCount',
      width: 100,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string) => {
        const colorMap: Record<string, string> = {
          PENDING: 'warning',
          REDELIVERED: 'success',
          DISCARDED: 'default',
        }
        const textMap: Record<string, string> = {
          PENDING: '待处理',
          REDELIVERED: '已重投',
          DISCARDED: '已丢弃',
        }
        return <Tag color={colorMap[status]}>{textMap[status] || status}</Tag>
      },
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180,
    },
    {
      title: '操作',
      key: 'action',
      width: 220,
      render: (_: any, record: any) => (
        <Space>
          <Button
            type="link"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => handleViewDetail(record.id)}
          >
            详情
          </Button>
          {record.status === 'PENDING' && (
            <>
              <Popconfirm
                title="确定重新投递吗？"
                onConfirm={() => handleRedeliver(record.id)}
              >
                <Button
                  type="link"
                  size="small"
                  icon={<RedoOutlined />}
                >
                  重投
                </Button>
              </Popconfirm>
              <Popconfirm
                title="确定丢弃吗？"
                onConfirm={() => handleDiscard(record.id)}
              >
                <Button
                  type="link"
                  size="small"
                  danger
                  icon={<DeleteOutlined />}
                >
                  丢弃
                </Button>
              </Popconfirm>
            </>
          )}
        </Space>
      ),
    },
  ]

  const rowSelection = {
    selectedRowKeys,
    onChange: (selectedRowKeys: any) => setSelectedRowKeys(selectedRowKeys),
    getCheckboxProps: (record: any) => ({
      disabled: record.status !== 'PENDING',
    }),
  }

  return (
    <div>
      <Card
        title="死信处理"
        extra={
          <Space>
            <Button
              type="primary"
              icon={<RedoOutlined />}
              onClick={handleBatchRedeliver}
              disabled={selectedRowKeys.length === 0}
            >
              批量重投 ({selectedRowKeys.length})
            </Button>
          </Space>
        }
      >
        {/* 搜索条件 */}
        <Space style={{ marginBottom: 16 }}>
          <Select
            placeholder="状态"
            style={{ width: 150 }}
            allowClear
            onChange={setStatus}
          >
            <Select.Option value="PENDING">待处理</Select.Option>
            <Select.Option value="REDELIVERED">已重投</Select.Option>
            <Select.Option value="DISCARDED">已丢弃</Select.Option>
          </Select>

          <Button
            type="primary"
            icon={<SearchOutlined />}
            onClick={fetchData}
          >
            查询
          </Button>
        </Space>

        <Table
          columns={columns}
          dataSource={dataSource}
          loading={loading}
          rowKey="id"
          rowSelection={rowSelection}
          pagination={{
            current: page,
            pageSize: size,
            total: total,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (page, pageSize) => {
              setPage(page)
              setSize(pageSize)
            },
          }}
        />
      </Card>

      {/* 详情抽屉 */}
      <Drawer
        title="死信详情"
        width={720}
        open={detailVisible}
        onClose={() => setDetailVisible(false)}
      >
        {detailData && (
          <>
            <Descriptions column={2} bordered>
              <Descriptions.Item label="消息ID" span={2}>
                {detailData.messageId}
              </Descriptions.Item>
              <Descriptions.Item label="主题">
                {detailData.topic}
              </Descriptions.Item>
              <Descriptions.Item label="路由键">
                {detailData.routingKey}
              </Descriptions.Item>
              <Descriptions.Item label="原始队列" span={2}>
                {detailData.originalQueue}
              </Descriptions.Item>
              <Descriptions.Item label="重试次数">
                {detailData.retryCount}
              </Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag
                  color={
                    detailData.status === 'PENDING' ? 'warning' :
                    detailData.status === 'REDELIVERED' ? 'success' : 'default'
                  }
                >
                  {detailData.status === 'PENDING' ? '待处理' :
                   detailData.status === 'REDELIVERED' ? '已重投' : '已丢弃'}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="创建时间" span={2}>
                {detailData.createTime}
              </Descriptions.Item>
              {detailData.handledTime && (
                <Descriptions.Item label="处理时间" span={2}>
                  {detailData.handledTime}
                </Descriptions.Item>
              )}
            </Descriptions>

            {detailData.headers && (
              <Card title="消息头" style={{ marginTop: 16 }} size="small">
                <pre>{detailData.headers}</pre>
              </Card>
            )}

            <Card title="消息体" style={{ marginTop: 16 }} size="small">
              <pre>{detailData.payload}</pre>
            </Card>

            {detailData.errorMessage && (
              <Card title="错误信息" style={{ marginTop: 16 }} size="small">
                <pre style={{ color: 'red' }}>{detailData.errorMessage}</pre>
              </Card>
            )}
          </>
        )}
      </Drawer>
    </div>
  )
}

export default DeadLetter
