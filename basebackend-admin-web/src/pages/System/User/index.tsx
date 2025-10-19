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
} from 'antd'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  SearchOutlined,
  ReloadOutlined,
  KeyOutlined,
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import {
  getUserPage,
  createUser,
  updateUser,
  deleteUser,
  resetUserPassword,
  changeUserStatus,
} from '@/api/user'
import { getDeptTree } from '@/api/dept'
import { getRolePage } from '@/api/role'
import { User, Dept, Role } from '@/types'

const UserList = () => {
  const [form] = Form.useForm()
  const [searchForm] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [dataSource, setDataSource] = useState<User[]>([])
  const [total, setTotal] = useState(0)
  const [current, setCurrent] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const [modalVisible, setModalVisible] = useState(false)
  const [modalTitle, setModalTitle] = useState('新增用户')
  const [editingId, setEditingId] = useState<string | null>(null)
  const [deptList, setDeptList] = useState<Dept[]>([])
  const [roleList, setRoleList] = useState<Role[]>([])

  // 加载数据
  const loadData = async (page = current, size = pageSize) => {
    setLoading(true)
    try {
      const searchValues = searchForm.getFieldsValue()
      const response = await getUserPage({
        current: page,
        size,
        ...searchValues,
      })
      setDataSource(response.data.records)
      setTotal(response.data.total)
      setCurrent(response.data.current)
      setPageSize(response.data.size)
    } catch (error) {
      message.error('加载用户列表失败')
    } finally {
      setLoading(false)
    }
  }

  // 加载部门列表
  const loadDeptList = async () => {
    try {
      const response = await getDeptTree()
      setDeptList(response.data)
    } catch (error) {
      console.error('加载部门列表失败', error)
    }
  }

  // 加载角色列表
  const loadRoleList = async () => {
    try {
      const response = await getRolePage({ current: 1, size: 100 })
      setRoleList(response.data.records)
    } catch (error) {
      console.error('加载角色列表失败', error)
    }
  }

  useEffect(() => {
    loadData()
    loadDeptList()
    loadRoleList()
  }, [])

  // 打开新增/编辑弹窗
  const handleOpenModal = (record?: User) => {
    if (record) {
      setModalTitle('编辑用户')
      setEditingId(record.id!)
      form.setFieldsValue(record)
    } else {
      setModalTitle('新增用户')
      setEditingId(null)
      form.resetFields()
    }
    setModalVisible(true)
  }

  // 提交表单
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      if (editingId) {
        await updateUser(editingId, values)
        message.success('更新成功')
      } else {
        await createUser(values)
        message.success('创建成功')
      }
      setModalVisible(false)
      loadData()
    } catch (error: any) {
      if (error.errorFields) {
        message.error('请填写必填项')
      }
    }
  }

  // 删除用户
  const handleDelete = async (id: string) => {
    try {
      await deleteUser(id)
      message.success('删除成功')
      loadData()
    } catch (error) {
      message.error('删除失败')
    }
  }

  // 重置密码
  const handleResetPassword = async (id: string) => {
    try {
      await resetUserPassword(id, '123456')
      message.success('密码已重置为123456')
    } catch (error) {
      message.error('重置密码失败')
    }
  }

  // 修改状态
  const handleChangeStatus = async (id: string, status: number) => {
    try {
      await changeUserStatus(id, status === 1 ? 0 : 1)
      message.success('状态修改成功')
      loadData()
    } catch (error) {
      message.error('状态修改失败')
    }
  }

  const columns: ColumnsType<User> = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
    },
    {
      title: '用户名',
      dataIndex: 'username',
      key: 'username',
    },
    {
      title: '昵称',
      dataIndex: 'nickname',
      key: 'nickname',
    },
    {
      title: '邮箱',
      dataIndex: 'email',
      key: 'email',
    },
    {
      title: '手机号',
      dataIndex: 'phone',
      key: 'phone',
    },
    {
      title: '部门',
      dataIndex: 'deptName',
      key: 'deptName',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: number) =>
        status === 1 ? <Tag color="success">启用</Tag> : <Tag color="error">禁用</Tag>,
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
      width: 260,
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
          <Button
            type="link"
            size="small"
            icon={<KeyOutlined />}
            onClick={() => handleResetPassword(record.id!)}
          >
            重置密码
          </Button>
          <Button
            type="link"
            size="small"
            onClick={() => handleChangeStatus(record.id!, record.status!)}
          >
            {record.status === 1 ? '禁用' : '启用'}
          </Button>
          <Popconfirm
            title="确定要删除吗?"
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

  return (
    <div>
      <Card style={{ marginBottom: 16 }}>
        <Form form={searchForm} layout="inline">
          <Form.Item name="username" label="用户名">
            <Input placeholder="请输入用户名" allowClear />
          </Form.Item>
          <Form.Item name="nickname" label="昵称">
            <Input placeholder="请输入昵称" allowClear />
          </Form.Item>
          <Form.Item name="phone" label="手机号">
            <Input placeholder="请输入手机号" allowClear />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select placeholder="请选择状态" allowClear style={{ width: 120 }}>
              <Select.Option value={1}>启用</Select.Option>
              <Select.Option value={0}>禁用</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" icon={<SearchOutlined />} onClick={() => loadData(1)}>
                查询
              </Button>
              <Button icon={<ReloadOutlined />} onClick={() => {
                searchForm.resetFields()
                loadData(1)
              }}>
                重置
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>

      <Card
        title="用户列表"
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={() => handleOpenModal()}>
            新增用户
          </Button>
        }
      >
        <Table
          loading={loading}
          dataSource={dataSource}
          columns={columns}
          rowKey="id"
          pagination={{
            current,
            pageSize,
            total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (page, size) => {
              setCurrent(page)
              setPageSize(size)
              loadData(page, size)
            },
          }}
          scroll={{ x: 1200 }}
        />
      </Card>

      <Modal
        title={modalTitle}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        width={600}
        destroyOnClose
      >
        <Form form={form} labelCol={{ span: 6 }} wrapperCol={{ span: 16 }}>
          <Form.Item
            name="username"
            label="用户名"
            rules={[
              { required: true, message: '请输入用户名' },
              { min: 2, max: 20, message: '用户名长度必须在2-20个字符之间' },
            ]}
          >
            <Input placeholder="请输入用户名" disabled={!!editingId} />
          </Form.Item>

          {!editingId && (
            <Form.Item
              name="password"
              label="密码"
              rules={[
                { required: true, message: '请输入密码' },
                { min: 6, max: 20, message: '密码长度必须在6-20个字符之间' },
              ]}
            >
              <Input.Password placeholder="请输入密码" />
            </Form.Item>
          )}

          <Form.Item
            name="nickname"
            label="昵称"
            rules={[{ required: true, message: '请输入昵称' }]}
          >
            <Input placeholder="请输入昵称" />
          </Form.Item>

          <Form.Item
            name="email"
            label="邮箱"
            rules={[{ type: 'email', message: '邮箱格式不正确' }]}
          >
            <Input placeholder="请输入邮箱" />
          </Form.Item>

          <Form.Item
            name="phone"
            label="手机号"
            rules={[
              { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' },
            ]}
          >
            <Input placeholder="请输入手机号" />
          </Form.Item>

          <Form.Item name="gender" label="性别">
            <Select placeholder="请选择性别">
              <Select.Option value={0}>未知</Select.Option>
              <Select.Option value={1}>男</Select.Option>
              <Select.Option value={2}>女</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item name="deptId" label="部门">
            <Select placeholder="请选择部门" showSearch optionFilterProp="label">
              {deptList.map((dept) => (
                <Select.Option key={dept.id} value={dept.id} label={dept.deptName}>
                  {dept.deptName}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item name="roleIds" label="角色">
            <Select mode="multiple" placeholder="请选择角色" showSearch optionFilterProp="label">
              {roleList.map((role) => (
                <Select.Option key={role.id} value={role.id} label={role.roleName}>
                  {role.roleName}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item name="status" label="状态" initialValue={1}>
            <Select>
              <Select.Option value={1}>启用</Select.Option>
              <Select.Option value={0}>禁用</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={4} placeholder="请输入备注" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default UserList
