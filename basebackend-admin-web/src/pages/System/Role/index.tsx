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
  Tree,
} from 'antd'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  SearchOutlined,
  ReloadOutlined,
  SafetyOutlined,
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import {
  getRolePage,
  createRole,
  updateRole,
  deleteRole,
  assignRoleMenus,
  getRoleMenus,
} from '@/api/role'
import { getMenuTree } from '@/api/menu'
import { Role, Menu } from '@/types'

const RoleList = () => {
  const [form] = Form.useForm()
  const [searchForm] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [dataSource, setDataSource] = useState<Role[]>([])
  const [total, setTotal] = useState(0)
  const [current, setCurrent] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const [modalVisible, setModalVisible] = useState(false)
  const [menuModalVisible, setMenuModalVisible] = useState(false)
  const [modalTitle, setModalTitle] = useState('新增角色')
  const [editingId, setEditingId] = useState<string | null>(null)
  const [menuTree, setMenuTree] = useState<Menu[]>([])
  const [selectedMenuKeys, setSelectedMenuKeys] = useState<string[]>([])
  const [currentRoleId, setCurrentRoleId] = useState<string | null>(null)

  // 加载数据
  const loadData = async (page = current, size = pageSize) => {
    setLoading(true)
    try {
      const searchValues = searchForm.getFieldsValue()
      const response = await getRolePage({
        current: page,
        size,
        ...searchValues,
      })
      setDataSource(response.data.records)
      setTotal(response.data.total)
      setCurrent(response.data.current)
      setPageSize(response.data.size)
    } catch (error) {
      message.error('加载角色列表失败')
    } finally {
      setLoading(false)
    }
  }

  // 加载菜单树
  const loadMenuTree = async () => {
    try {
      const response = await getMenuTree()
      setMenuTree(response.data)
    } catch (error) {
      console.error('加载菜单树失败', error)
    }
  }

  useEffect(() => {
    loadData()
    loadMenuTree()
  }, [])

  // 打开新增/编辑弹窗
  const handleOpenModal = (record?: Role) => {
    if (record) {
      setModalTitle('编辑角色')
      setEditingId(record.id!)
      form.setFieldsValue(record)
    } else {
      setModalTitle('新增角色')
      setEditingId(null)
      form.resetFields()
      form.setFieldsValue({ status: 1, dataScope: 1 })
    }
    setModalVisible(true)
  }

  // 打开菜单分配弹窗
  const handleOpenMenuModal = async (record: Role) => {
    setCurrentRoleId(record.id!)
    try {
      const response = await getRoleMenus(record.id!)
      setSelectedMenuKeys(response.data)
      setMenuModalVisible(true)
    } catch (error) {
      message.error('加载角色菜单失败')
    }
  }

  // 提交表单
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      if (editingId) {
        await updateRole(editingId, values)
        message.success('更新成功')
      } else {
        await createRole(values)
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

  // 提交菜单分配
  const handleSubmitMenus = async () => {
    try {
      await assignRoleMenus(currentRoleId!, selectedMenuKeys)
      message.success('菜单分配成功')
      setMenuModalVisible(false)
    } catch (error) {
      message.error('菜单分配失败')
    }
  }

  // 删除角色
  const handleDelete = async (id: string) => {
    try {
      await deleteRole(id)
      message.success('删除成功')
      loadData()
    } catch (error) {
      message.error('删除失败')
    }
  }

  // 构建树节点
  const buildTreeData = (menus: Menu[]): any[] => {
    return menus.map((menu) => ({
      key: menu.id,
      title: menu.menuName,
      children: menu.children ? buildTreeData(menu.children) : undefined,
    }))
  }

  const columns: ColumnsType<Role> = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
    },
    {
      title: '角色名称',
      dataIndex: 'roleName',
      key: 'roleName',
    },
    {
      title: '角色标识',
      dataIndex: 'roleKey',
      key: 'roleKey',
    },
    {
      title: '排序',
      dataIndex: 'roleSort',
      key: 'roleSort',
      width: 80,
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
      width: 240,
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
            icon={<SafetyOutlined />}
            onClick={() => handleOpenMenuModal(record)}
          >
            分配菜单
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
          <Form.Item name="roleName" label="角色名称">
            <Input placeholder="请输入角色名称" allowClear />
          </Form.Item>
          <Form.Item name="roleKey" label="角色标识">
            <Input placeholder="请输入角色标识" allowClear />
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
        title="角色列表"
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={() => handleOpenModal()}>
            新增角色
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
            name="roleName"
            label="角色名称"
            rules={[{ required: true, message: '请输入角色名称' }]}
          >
            <Input placeholder="请输入角色名称" />
          </Form.Item>

          <Form.Item
            name="roleKey"
            label="角色标识"
            rules={[{ required: true, message: '请输入角色标识' }]}
          >
            <Input placeholder="请输入角色标识" />
          </Form.Item>

          <Form.Item name="roleSort" label="排序">
            <InputNumber min={0} placeholder="请输入排序" style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item name="dataScope" label="数据范围">
            <Select placeholder="请选择数据范围">
              <Select.Option value={1}>全部数据权限</Select.Option>
              <Select.Option value={2}>本部门数据权限</Select.Option>
              <Select.Option value={3}>本部门及以下数据权限</Select.Option>
              <Select.Option value={4}>仅本人数据权限</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item name="status" label="状态">
            <Select placeholder="请选择状态">
              <Select.Option value={1}>启用</Select.Option>
              <Select.Option value={0}>禁用</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={4} placeholder="请输入备注" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="分配菜单"
        open={menuModalVisible}
        onOk={handleSubmitMenus}
        onCancel={() => setMenuModalVisible(false)}
        width={400}
      >
        <Tree
          checkable
          defaultExpandAll
          checkedKeys={selectedMenuKeys}
          onCheck={(checkedKeys: any) => setSelectedMenuKeys(checkedKeys)}
          treeData={buildTreeData(menuTree)}
        />
      </Modal>
    </div>
  )
}

export default RoleList
