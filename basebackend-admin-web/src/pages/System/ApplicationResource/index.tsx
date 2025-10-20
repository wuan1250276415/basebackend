import { useState, useEffect } from 'react'
import {
  Card,
  Table,
  Button,
  Space,
  Form,
  Modal,
  message,
  Tag,
  Popconfirm,
  Select,
  InputNumber,
  TreeSelect,
  Input,
  Switch,
} from 'antd'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  RollbackOutlined,
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import {
  getResourceTree,
  createResource,
  updateResource,
  deleteResource,
} from '@/api/application'
import { ApplicationResource } from '@/types'
import { useSearchParams } from 'react-router-dom'

const ApplicationResourceManagement = () => {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [dataSource, setDataSource] = useState<ApplicationResource[]>([])
  const [modalVisible, setModalVisible] = useState(false)
  const [modalTitle, setModalTitle] = useState('新增资源')
  const [editingId, setEditingId] = useState<string | null>(null)
  const [searchParams] = useSearchParams()
  const appId = searchParams.get('appId') || ''
  const appName = searchParams.get('appName') || ''

  // 加载数据
  const loadData = async () => {
    if (!appId) {
      message.error('缺少应用ID参数')
      return
    }
    setLoading(true)
    try {
      const response = await getResourceTree(appId)
      setDataSource(response.data)
    } catch (error) {
      message.error('加载资源列表失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [appId])

  // 构建树形选择数据
  const buildTreeData = (data: ApplicationResource[]): any[] => {
    return data.map((item) => ({
      title: item.resourceName,
      value: item.id,
      children: item.children ? buildTreeData(item.children) : undefined,
    }))
  }

  // 展平树形数据用于表格显示
  const flattenTree = (data: ApplicationResource[], level = 0): ApplicationResource[] => {
    const result: ApplicationResource[] = []
    data.forEach((item) => {
      result.push({ ...item, level })
      if (item.children && item.children.length > 0) {
        result.push(...flattenTree(item.children, level + 1))
      }
    })
    return result
  }

  // 打开新增/编辑弹窗
  const handleOpenModal = (record?: ApplicationResource) => {
    if (record) {
      setModalTitle('编辑资源')
      setEditingId(record.id!)
      form.setFieldsValue({
        ...record,
        parentId: record.parentId === '0' ? undefined : record.parentId,
      })
    } else {
      setModalTitle('新增资源')
      setEditingId(null)
      form.resetFields()
      form.setFieldsValue({
        appId,
        status: 1,
        visible: 1,
        openType: 'current',
        orderNum: 0,
      })
    }
    setModalVisible(true)
  }

  // 保存
  const handleSave = async () => {
    try {
      const values = await form.validateFields()
      const data = {
        ...values,
        appId,
        parentId: values.parentId || '0',
      }
      if (editingId) {
        await updateResource({ ...data, id: editingId })
        message.success('更新成功')
      } else {
        await createResource(data)
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
      await deleteResource(id)
      message.success('删除成功')
      loadData()
    } catch (error) {
      message.error('删除失败')
    }
  }

  // 返回应用列表
  const handleBack = () => {
    window.location.href = '/system/application'
  }

  // 表格列定义
  const columns: ColumnsType<ApplicationResource> = [
    {
      title: '资源名称',
      dataIndex: 'resourceName',
      key: 'resourceName',
      width: 200,
      render: (text: string, record: any) => {
        const indent = '—'.repeat(record.level || 0)
        return `${indent} ${text}`
      },
    },
    {
      title: '资源类型',
      dataIndex: 'resourceType',
      key: 'resourceType',
      width: 100,
      render: (text: string) => {
        const typeMap: Record<string, { color: string; text: string }> = {
          M: { color: 'blue', text: '目录' },
          C: { color: 'green', text: '菜单' },
          F: { color: 'orange', text: '按钮' },
        }
        const type = typeMap[text] || { color: 'default', text: text }
        return <Tag color={type.color}>{type.text}</Tag>
      },
    },
    {
      title: '路由地址',
      dataIndex: 'path',
      key: 'path',
      width: 180,
      render: (text: string) => text || '-',
    },
    {
      title: '组件路径',
      dataIndex: 'component',
      key: 'component',
      width: 200,
      render: (text: string) => text || '-',
    },
    {
      title: '权限标识',
      dataIndex: 'perms',
      key: 'perms',
      width: 150,
      render: (text: string) => text || '-',
    },
    {
      title: '图标',
      dataIndex: 'icon',
      key: 'icon',
      width: 100,
      render: (text: string) => text || '-',
    },
    {
      title: '打开方式',
      dataIndex: 'openType',
      key: 'openType',
      width: 100,
      render: (text: string) => {
        const typeMap: Record<string, string> = {
          current: '当前页',
          blank: '新窗口',
        }
        return typeMap[text] || text
      },
    },
    {
      title: '显示',
      dataIndex: 'visible',
      key: 'visible',
      width: 80,
      render: (visible: number) => (
        <Tag color={visible === 1 ? 'success' : 'default'}>
          {visible === 1 ? '是' : '否'}
        </Tag>
      ),
    },
    {
      title: '排序',
      dataIndex: 'orderNum',
      key: 'orderNum',
      width: 80,
      sorter: (a, b) => (a.orderNum || 0) - (b.orderNum || 0),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (status: number) => (
        <Tag color={status === 1 ? 'success' : 'error'}>
          {status === 1 ? '启用' : '禁用'}
        </Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      fixed: 'right',
      width: 180,
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
            title="确定要删除这个资源吗？"
            description="删除前请先删除子资源"
            onConfirm={() => handleDelete(record.id!)}
            okText="确定"
            cancelText="取消"
          >
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  const flatData = flattenTree(dataSource)

  return (
    <div style={{ padding: '24px' }}>
      <Card>
        {/* 标题和工具栏 */}
        <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
          <div>
            <h2 style={{ margin: 0 }}>
              {appName} - 资源管理
            </h2>
          </div>
          <Space>
            <Button icon={<RollbackOutlined />} onClick={handleBack}>
              返回应用列表
            </Button>
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => handleOpenModal()}
            >
              新增资源
            </Button>
          </Space>
        </div>

        {/* 表格 */}
        <Table
          loading={loading}
          columns={columns}
          dataSource={flatData}
          rowKey="id"
          pagination={false}
          scroll={{ x: 1400 }}
        />
      </Card>

      {/* 新增/编辑弹窗 */}
      <Modal
        title={modalTitle}
        open={modalVisible}
        onOk={handleSave}
        onCancel={() => setModalVisible(false)}
        width={700}
        destroyOnClose
      >
        <Form form={form} labelCol={{ span: 6 }} wrapperCol={{ span: 16 }}>
          <Form.Item label="上级资源" name="parentId">
            <TreeSelect
              placeholder="请选择上级资源（不选则为顶级）"
              treeData={buildTreeData(dataSource)}
              allowClear
              treeDefaultExpandAll
            />
          </Form.Item>

          <Form.Item
            label="资源名称"
            name="resourceName"
            rules={[{ required: true, message: '请输入资源名称' }]}
          >
            <Input placeholder="请输入资源名称" maxLength={100} />
          </Form.Item>

          <Form.Item
            label="资源类型"
            name="resourceType"
            rules={[{ required: true, message: '请选择资源类型' }]}
          >
            <Select placeholder="请选择资源类型">
              <Select.Option value="M">目录</Select.Option>
              <Select.Option value="C">菜单</Select.Option>
              <Select.Option value="F">按钮</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item label="路由地址" name="path">
            <Input placeholder="请输入路由地址（如：/user）" />
          </Form.Item>

          <Form.Item label="组件路径" name="component">
            <Input placeholder="请输入组件路径（如：System/User）" />
          </Form.Item>

          <Form.Item label="权限标识" name="perms">
            <Input placeholder="请输入权限标识（如：system:user:list）" />
          </Form.Item>

          <Form.Item label="资源图标" name="icon">
            <Input placeholder="请输入图标class" />
          </Form.Item>

          <Form.Item label="打开方式" name="openType">
            <Select placeholder="请选择打开方式">
              <Select.Option value="current">当前页</Select.Option>
              <Select.Option value="blank">新窗口</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item label="是否显示" name="visible" valuePropName="checked">
            <Switch checkedChildren="显示" unCheckedChildren="隐藏" />
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

export default ApplicationResourceManagement
