import { useState, useEffect } from 'react'
import {
  Card,
  Table,
  Button,
  Space,
  Input,
  Form,
  Modal,
  message,
  Tag,
  Popconfirm,
  Select,
  InputNumber,
  Switch,
} from 'antd'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  SearchOutlined,
  ReloadOutlined,
  AppstoreOutlined,
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import {
  getApplicationList,
  createApplication,
  updateApplication,
  deleteApplication,
  updateApplicationStatus,
} from '@/api/application'
import { Application } from '@/types'

const ApplicationManagement = () => {
  const [form] = Form.useForm()
  const [searchForm] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [dataSource, setDataSource] = useState<Application[]>([])
  const [modalVisible, setModalVisible] = useState(false)
  const [modalTitle, setModalTitle] = useState('新增应用')
  const [editingId, setEditingId] = useState<string | null>(null)

  // 加载数据
  const loadData = async () => {
    setLoading(true)
    try {
      const response = await getApplicationList()
      setDataSource(response.data)
    } catch (error) {
      message.error('加载应用列表失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [])

  // 打开新增/编辑弹窗
  const handleOpenModal = (record?: Application) => {
    if (record) {
      setModalTitle('编辑应用')
      setEditingId(record.id!)
      form.setFieldsValue(record)
    } else {
      setModalTitle('新增应用')
      setEditingId(null)
      form.resetFields()
      form.setFieldsValue({ status: 1, orderNum: 0 })
    }
    setModalVisible(true)
  }

  // 保存
  const handleSave = async () => {
    try {
      const values = await form.validateFields()
      if (editingId) {
        await updateApplication({ ...values, id: editingId })
        message.success('更新成功')
      } else {
        await createApplication(values)
        message.success('创建成功')
      }
      setModalVisible(false)
      loadData()
    } catch (error: any) {
      if (error.errorFields) {
        message.error('请检查表单填写是否正确')
      } else {
        message.error(editingId ? '更新失败' : '创建失败')
      }
    }
  }

  // 删除
  const handleDelete = async (id: string) => {
    try {
      await deleteApplication(id)
      message.success('删除成功')
      loadData()
    } catch (error) {
      message.error('删除失败')
    }
  }

  // 修改状态
  const handleStatusChange = async (id: string, status: number) => {
    try {
      await updateApplicationStatus(id, status)
      message.success('状态修改成功')
      loadData()
    } catch (error) {
      message.error('状态修改失败')
    }
  }

  // 搜索
  const handleSearch = () => {
    const values = searchForm.getFieldsValue()
    if (values.appName || values.appCode) {
      const filtered = dataSource.filter((item) => {
        return (
          (!values.appName || item.appName?.includes(values.appName)) &&
          (!values.appCode || item.appCode?.includes(values.appCode))
        )
      })
      setDataSource(filtered)
    } else {
      loadData()
    }
  }

  // 重置搜索
  const handleReset = () => {
    searchForm.resetFields()
    loadData()
  }

  // 表格列定义
  const columns: ColumnsType<Application> = [
    {
      title: '应用名称',
      dataIndex: 'appName',
      key: 'appName',
      width: 150,
    },
    {
      title: '应用编码',
      dataIndex: 'appCode',
      key: 'appCode',
      width: 120,
    },
    {
      title: '应用类型',
      dataIndex: 'appType',
      key: 'appType',
      width: 100,
      render: (text: string) => {
        const typeMap: Record<string, { color: string; text: string }> = {
          web: { color: 'blue', text: 'Web应用' },
          mobile: { color: 'green', text: '移动应用' },
          api: { color: 'orange', text: 'API服务' },
        }
        const type = typeMap[text] || { color: 'default', text: text }
        return <Tag color={type.color}>{type.text}</Tag>
      },
    },
    {
      title: '应用图标',
      dataIndex: 'appIcon',
      key: 'appIcon',
      width: 100,
      render: (text: string) => text || '-',
    },
    {
      title: '应用地址',
      dataIndex: 'appUrl',
      key: 'appUrl',
      width: 200,
      render: (text: string) => text || '-',
    },
    {
      title: '显示顺序',
      dataIndex: 'orderNum',
      key: 'orderNum',
      width: 100,
      sorter: (a, b) => (a.orderNum || 0) - (b.orderNum || 0),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: number, record) => (
        <Switch
          checked={status === 1}
          checkedChildren="启用"
          unCheckedChildren="禁用"
          onChange={(checked) => handleStatusChange(record.id!, checked ? 1 : 0)}
        />
      ),
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
      fixed: 'right',
      width: 200,
      render: (_, record) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleOpenModal(record)}
          >
            编辑
          </Button>
          <Popconfirm
            title="确定要删除这个应用吗？"
            description="删除后该应用下的所有资源也将被删除"
            onConfirm={() => handleDelete(record.id!)}
            okText="确定"
            cancelText="取消"
          >
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
          <Button
            type="link"
            size="small"
            icon={<AppstoreOutlined />}
            onClick={() => {
              window.location.href = `/system/application-resource?appId=${record.id}&appName=${record.appName}`
            }}
          >
            资源管理
          </Button>
        </Space>
      ),
    },
  ]

  return (
    <div style={{ padding: '24px' }}>
      <Card>
        {/* 搜索栏 */}
        <Form form={searchForm} layout="inline" style={{ marginBottom: 16 }}>
          <Form.Item name="appName" label="应用名称">
            <Input placeholder="请输入应用名称" allowClear />
          </Form.Item>
          <Form.Item name="appCode" label="应用编码">
            <Input placeholder="请输入应用编码" allowClear />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>
                搜索
              </Button>
              <Button icon={<ReloadOutlined />} onClick={handleReset}>
                重置
              </Button>
            </Space>
          </Form.Item>
        </Form>

        {/* 工具栏 */}
        <div style={{ marginBottom: 16 }}>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => handleOpenModal()}
          >
            新增应用
          </Button>
        </div>

        {/* 表格 */}
        <Table
          loading={loading}
          columns={columns}
          dataSource={dataSource}
          rowKey="id"
          pagination={false}
          scroll={{ x: 1200 }}
        />
      </Card>

      {/* 新增/编辑弹窗 */}
      <Modal
        title={modalTitle}
        open={modalVisible}
        onOk={handleSave}
        onCancel={() => setModalVisible(false)}
        width={600}
        destroyOnClose
      >
        <Form form={form} labelCol={{ span: 6 }} wrapperCol={{ span: 16 }}>
          <Form.Item
            label="应用名称"
            name="appName"
            rules={[{ required: true, message: '请输入应用名称' }]}
          >
            <Input placeholder="请输入应用名称" maxLength={100} />
          </Form.Item>

          <Form.Item
            label="应用编码"
            name="appCode"
            rules={[
              { required: true, message: '请输入应用编码' },
              {
                pattern: /^[a-z][a-z0-9_]*$/,
                message: '应用编码只能包含小写字母、数字和下划线，且以字母开头',
              },
            ]}
          >
            <Input
              placeholder="请输入应用编码（如：admin, portal）"
              maxLength={50}
              disabled={!!editingId}
            />
          </Form.Item>

          <Form.Item
            label="应用类型"
            name="appType"
            rules={[{ required: true, message: '请选择应用类型' }]}
          >
            <Select placeholder="请选择应用类型">
              <Select.Option value="web">Web应用</Select.Option>
              <Select.Option value="mobile">移动应用</Select.Option>
              <Select.Option value="api">API服务</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item label="应用图标" name="appIcon">
            <Input placeholder="请输入图标class（如：el-icon-setting）" />
          </Form.Item>

          <Form.Item label="应用地址" name="appUrl">
            <Input placeholder="请输入应用访问地址（如：/admin）" />
          </Form.Item>

          <Form.Item
            label="显示顺序"
            name="orderNum"
            rules={[{ required: true, message: '请输入显示顺序' }]}
          >
            <InputNumber min={0} placeholder="请输入显示顺序" style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item
            label="状态"
            name="status"
            rules={[{ required: true, message: '请选择状态' }]}
          >
            <Select placeholder="请选择状态">
              <Select.Option value={1}>启用</Select.Option>
              <Select.Option value={0}>禁用</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item label="备注" name="remark">
            <Input.TextArea
              placeholder="请输入备注"
              rows={3}
              maxLength={500}
              showCount
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default ApplicationManagement
