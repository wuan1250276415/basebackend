import { useEffect, useState } from 'react'
import {
  Card,
  Table,
  Button,
  Space,
  Modal,
  Form,
  Input,
  Select,
  InputNumber,
  Switch,
  message,
  Tag,
  Popconfirm
} from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, PoweroffOutlined } from '@ant-design/icons'
import {
  getWebhookConfigPage,
  createWebhookConfig,
  updateWebhookConfig,
  deleteWebhookConfig,
  toggleWebhookConfig
} from '@/api/integration/webhook'

const { TextArea } = Input
const { Option } = Select

const WebhookConfig = () => {
  const [loading, setLoading] = useState(false)
  const [dataSource, setDataSource] = useState([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [size, setSize] = useState(20)
  const [searchName, setSearchName] = useState('')
  const [modalVisible, setModalVisible] = useState(false)
  const [editingRecord, setEditingRecord] = useState<any>(null)
  const [form] = Form.useForm()

  // 获取列表数据
  const fetchData = async () => {
    try {
      setLoading(true)
      const res = await getWebhookConfigPage({
        page,
        size,
        name: searchName
      })

      if (res.code === 200) {
        setDataSource(res.data.records)
        setTotal(res.data.total)
      }
    } catch (error) {
      message.error('获取Webhook配置失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchData()
  }, [page, size, searchName])

  // 打开新增/编辑弹窗
  const handleOpenModal = (record?: any) => {
    setEditingRecord(record)
    if (record) {
      form.setFieldsValue(record)
    } else {
      form.resetFields()
    }
    setModalVisible(true)
  }

  // 提交表单
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      if (editingRecord) {
        await updateWebhookConfig(editingRecord.id, values)
        message.success('更新成功')
      } else {
        await createWebhookConfig(values)
        message.success('创建成功')
      }
      setModalVisible(false)
      fetchData()
    } catch (error) {
      message.error('操作失败')
    }
  }

  // 删除
  const handleDelete = async (id: number) => {
    try {
      await deleteWebhookConfig(id)
      message.success('删除成功')
      fetchData()
    } catch (error) {
      message.error('删除失败')
    }
  }

  // 启用/禁用
  const handleToggle = async (id: number, enabled: boolean) => {
    try {
      await toggleWebhookConfig(id, !enabled)
      message.success(enabled ? '已禁用' : '已启用')
      fetchData()
    } catch (error) {
      message.error('操作失败')
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
      title: '名称',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: 'URL',
      dataIndex: 'url',
      key: 'url',
      ellipsis: true,
    },
    {
      title: '事件类型',
      dataIndex: 'eventTypes',
      key: 'eventTypes',
      render: (text: string) => {
        const types = text.split(',')
        return (
          <>
            {types.slice(0, 3).map((type) => (
              <Tag key={type}>{type}</Tag>
            ))}
            {types.length > 3 && <Tag>+{types.length - 3}</Tag>}
          </>
        )
      },
    },
    {
      title: '方法',
      dataIndex: 'method',
      key: 'method',
      width: 80,
    },
    {
      title: '签名',
      dataIndex: 'signatureEnabled',
      key: 'signatureEnabled',
      width: 80,
      render: (enabled: boolean) => (
        <Tag color={enabled ? 'green' : 'default'}>{enabled ? '启用' : '禁用'}</Tag>
      ),
    },
    {
      title: '状态',
      dataIndex: 'enabled',
      key: 'enabled',
      width: 80,
      render: (enabled: boolean) => (
        <Tag color={enabled ? 'success' : 'default'}>{enabled ? '启用' : '禁用'}</Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      render: (_: any, record: any) => (
        <Space>
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleOpenModal(record)}
          >
            编辑
          </Button>
          <Button
            type="link"
            size="small"
            icon={<PoweroffOutlined />}
            onClick={() => handleToggle(record.id, record.enabled)}
          >
            {record.enabled ? '禁用' : '启用'}
          </Button>
          <Popconfirm
            title="确定删除吗？"
            onConfirm={() => handleDelete(record.id)}
          >
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <Card
        title="Webhook配置"
        extra={
          <Space>
            <Input.Search
              placeholder="搜索名称"
              onSearch={setSearchName}
              style={{ width: 200 }}
            />
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => handleOpenModal()}
            >
              新增
            </Button>
          </Space>
        }
      >
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

      {/* 新增/编辑弹窗 */}
      <Modal
        title={editingRecord ? '编辑Webhook配置' : '新增Webhook配置'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        width={600}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="name"
            label="名称"
            rules={[{ required: true, message: '请输入名称' }]}
          >
            <Input placeholder="请输入Webhook名称" />
          </Form.Item>

          <Form.Item
            name="url"
            label="URL"
            rules={[
              { required: true, message: '请输入URL' },
              { type: 'url', message: '请输入有效的URL' }
            ]}
          >
            <Input placeholder="https://example.com/webhook" />
          </Form.Item>

          <Form.Item
            name="eventTypes"
            label="事件类型"
            rules={[{ required: true, message: '请输入事件类型' }]}
            extra="多个事件类型用逗号分隔，输入 * 表示订阅所有事件"
          >
            <Input placeholder="user.created,user.updated 或 *" />
          </Form.Item>

          <Form.Item
            name="method"
            label="HTTP方法"
            initialValue="POST"
            rules={[{ required: true }]}
          >
            <Select>
              <Option value="POST">POST</Option>
              <Option value="PUT">PUT</Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="signatureEnabled"
            label="启用签名验证"
            valuePropName="checked"
            initialValue={true}
          >
            <Switch />
          </Form.Item>

          <Form.Item
            name="secret"
            label="签名密钥"
            extra="用于HMAC-SHA256签名验证"
          >
            <Input.Password placeholder="请输入签名密钥" />
          </Form.Item>

          <Form.Item name="headers" label="自定义请求头" extra="JSON格式">
            <TextArea
              rows={3}
              placeholder='{"Authorization": "Bearer token"}'
            />
          </Form.Item>

          <Form.Item
            name="timeout"
            label="超时时间(秒)"
            initialValue={30}
          >
            <InputNumber min={1} max={300} style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item
            name="maxRetries"
            label="最大重试次数"
            initialValue={3}
          >
            <InputNumber min={0} max={10} style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item
            name="retryInterval"
            label="重试间隔(秒)"
            initialValue={60}
          >
            <InputNumber min={1} max={3600} style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item
            name="enabled"
            label="启用"
            valuePropName="checked"
            initialValue={true}
          >
            <Switch />
          </Form.Item>

          <Form.Item name="remark" label="备注">
            <TextArea rows={2} placeholder="请输入备注" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default WebhookConfig
