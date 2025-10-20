import { useEffect, useState } from 'react'
import {
  Card,
  Table,
  Space,
  Tag,
  Button,
  Select,
  DatePicker,
  Drawer,
  Descriptions,
  message,
  Modal,
  Form,
  Input
} from 'antd'
import { SearchOutlined, EyeOutlined, SendOutlined } from '@ant-design/icons'
import { getWebhookLogPage, getWebhookLog } from '@/api/integration/event'
import { getEnabledWebhookConfigs } from '@/api/integration/webhook'
import { publishEvent } from '@/api/integration/event'
import dayjs from 'dayjs'

const { RangePicker } = DatePicker
const { TextArea } = Input

const EventLog = () => {
  const [loading, setLoading] = useState(false)
  const [dataSource, setDataSource] = useState([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [size, setSize] = useState(20)
  const [webhookId, setWebhookId] = useState<number>()
  const [eventType, setEventType] = useState('')
  const [success, setSuccess] = useState<boolean>()
  const [timeRange, setTimeRange] = useState<any>([])
  const [webhookList, setWebhookList] = useState([])
  const [detailVisible, setDetailVisible] = useState(false)
  const [detailData, setDetailData] = useState<any>(null)
  const [publishVisible, setPublishVisible] = useState(false)
  const [form] = Form.useForm()

  // 获取Webhook列表
  useEffect(() => {
    const fetchWebhooks = async () => {
      try {
        const res = await getEnabledWebhookConfigs()
        if (res.code === 200) {
          setWebhookList(res.data)
        }
      } catch (error) {
        console.error('获取Webhook列表失败')
      }
    }
    fetchWebhooks()
  }, [])

  // 获取事件日志
  const fetchData = async () => {
    try {
      setLoading(true)
      const res = await getWebhookLogPage({
        page,
        size,
        webhookId,
        eventType,
        success,
        startTime: timeRange[0] ? dayjs(timeRange[0]).format('YYYY-MM-DD HH:mm:ss') : undefined,
        endTime: timeRange[1] ? dayjs(timeRange[1]).format('YYYY-MM-DD HH:mm:ss') : undefined,
      })

      if (res.code === 200) {
        setDataSource(res.data.records)
        setTotal(res.data.total)
      }
    } catch (error) {
      message.error('获取事件日志失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchData()
  }, [page, size])

  // 查看详情
  const handleViewDetail = async (id: number) => {
    try {
      const res = await getWebhookLog(id)
      if (res.code === 200) {
        setDetailData(res.data)
        setDetailVisible(true)
      }
    } catch (error) {
      message.error('获取详情失败')
    }
  }

  // 发布事件
  const handlePublishEvent = async () => {
    try {
      const values = await form.validateFields()
      let data = values.data
      try {
        data = JSON.parse(data)
      } catch (e) {
        message.error('数据格式错误，请输入有效的JSON')
        return
      }

      await publishEvent({
        eventType: values.eventType,
        data,
        source: 'admin-web'
      })
      message.success('事件发布成功')
      setPublishVisible(false)
      form.resetFields()
      fetchData()
    } catch (error) {
      message.error('事件发布失败')
    }
  }

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
    },
    {
      title: '事件ID',
      dataIndex: 'eventId',
      key: 'eventId',
      width: 200,
      ellipsis: true,
    },
    {
      title: '事件类型',
      dataIndex: 'eventType',
      key: 'eventType',
    },
    {
      title: 'Webhook',
      dataIndex: 'webhookId',
      key: 'webhookId',
      render: (id: number) => {
        const webhook = webhookList.find((w: any) => w.id === id)
        return webhook ? webhook.name : id
      },
    },
    {
      title: '请求URL',
      dataIndex: 'requestUrl',
      key: 'requestUrl',
      ellipsis: true,
    },
    {
      title: '响应状态',
      dataIndex: 'responseStatus',
      key: 'responseStatus',
      width: 100,
      render: (status: number) => {
        const color = status >= 200 && status < 300 ? 'success' : 'error'
        return <Tag color={color}>{status}</Tag>
      },
    },
    {
      title: '响应时间',
      dataIndex: 'responseTime',
      key: 'responseTime',
      width: 120,
      render: (time: number) => `${time}ms`,
    },
    {
      title: '状态',
      dataIndex: 'success',
      key: 'success',
      width: 80,
      render: (success: boolean) => (
        <Tag color={success ? 'success' : 'error'}>{success ? '成功' : '失败'}</Tag>
      ),
    },
    {
      title: '重试次数',
      dataIndex: 'retryCount',
      key: 'retryCount',
      width: 100,
    },
    {
      title: '调用时间',
      dataIndex: 'callTime',
      key: 'callTime',
      width: 180,
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      render: (_: any, record: any) => (
        <Button
          type="link"
          size="small"
          icon={<EyeOutlined />}
          onClick={() => handleViewDetail(record.id)}
        >
          详情
        </Button>
      ),
    },
  ]

  return (
    <div>
      <Card
        title="事件日志"
        extra={
          <Button
            type="primary"
            icon={<SendOutlined />}
            onClick={() => setPublishVisible(true)}
          >
            发布事件
          </Button>
        }
      >
        {/* 搜索条件 */}
        <Space style={{ marginBottom: 16 }} wrap>
          <Select
            placeholder="选择Webhook"
            style={{ width: 200 }}
            allowClear
            onChange={setWebhookId}
          >
            {webhookList.map((w: any) => (
              <Select.Option key={w.id} value={w.id}>
                {w.name}
              </Select.Option>
            ))}
          </Select>

          <Input
            placeholder="事件类型"
            style={{ width: 200 }}
            onChange={(e) => setEventType(e.target.value)}
          />

          <Select
            placeholder="状态"
            style={{ width: 120 }}
            allowClear
            onChange={setSuccess}
          >
            <Select.Option value={true}>成功</Select.Option>
            <Select.Option value={false}>失败</Select.Option>
          </Select>

          <RangePicker
            showTime
            onChange={setTimeRange}
            style={{ width: 400 }}
          />

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
        title="事件日志详情"
        width={720}
        open={detailVisible}
        onClose={() => setDetailVisible(false)}
      >
        {detailData && (
          <>
            <Descriptions column={2} bordered>
              <Descriptions.Item label="事件ID" span={2}>
                {detailData.eventId}
              </Descriptions.Item>
              <Descriptions.Item label="事件类型" span={2}>
                {detailData.eventType}
              </Descriptions.Item>
              <Descriptions.Item label="请求URL" span={2}>
                {detailData.requestUrl}
              </Descriptions.Item>
              <Descriptions.Item label="请求方法">
                {detailData.requestMethod}
              </Descriptions.Item>
              <Descriptions.Item label="响应状态">
                <Tag color={detailData.success ? 'success' : 'error'}>
                  {detailData.responseStatus}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="响应时间">
                {detailData.responseTime}ms
              </Descriptions.Item>
              <Descriptions.Item label="重试次数">
                {detailData.retryCount}
              </Descriptions.Item>
              <Descriptions.Item label="调用时间" span={2}>
                {detailData.callTime}
              </Descriptions.Item>
            </Descriptions>

            <Card title="请求头" style={{ marginTop: 16 }} size="small">
              <pre>{detailData.requestHeaders}</pre>
            </Card>

            <Card title="请求体" style={{ marginTop: 16 }} size="small">
              <pre>{detailData.requestBody}</pre>
            </Card>

            <Card title="响应体" style={{ marginTop: 16 }} size="small">
              <pre>{detailData.responseBody}</pre>
            </Card>

            {detailData.errorMessage && (
              <Card title="错误信息" style={{ marginTop: 16 }} size="small">
                <pre style={{ color: 'red' }}>{detailData.errorMessage}</pre>
              </Card>
            )}
          </>
        )}
      </Drawer>

      {/* 发布事件弹窗 */}
      <Modal
        title="发布事件"
        open={publishVisible}
        onOk={handlePublishEvent}
        onCancel={() => setPublishVisible(false)}
        width={600}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="eventType"
            label="事件类型"
            rules={[{ required: true, message: '请输入事件类型' }]}
          >
            <Input placeholder="例如: user.created" />
          </Form.Item>

          <Form.Item
            name="data"
            label="事件数据"
            rules={[{ required: true, message: '请输入事件数据' }]}
            extra="请输入有效的JSON格式数据"
          >
            <TextArea
              rows={10}
              placeholder='{"userId": 1, "username": "admin"}'
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default EventLog
