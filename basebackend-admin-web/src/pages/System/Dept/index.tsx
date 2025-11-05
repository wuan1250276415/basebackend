import { useState, useEffect } from 'react'
import { Card, Tree, Button, Space, Form, Modal, message, Tag, Popconfirm, Input, InputNumber, Select, Row, Col } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, ApartmentOutlined, ReloadOutlined } from '@ant-design/icons'
import type { DataNode } from 'antd/es/tree'
import { getDeptTree, createDept, updateDept, deleteDept } from '@/api/dept'
import { Dept } from '@/types'
import DeptTreeSelect from '@/components/DeptTreeSelect'
import DeptStatistics from './components/DeptStatistics'
import DeptDetailPanel from './components/DeptDetailPanel'
import './index.css'

const DeptList = () => {
  const [form] = Form.useForm()
  const [searchForm] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [deptTree, setDeptTree] = useState<Dept[]>([])
  const [allDepts, setAllDepts] = useState<Dept[]>([])
  const [expandedKeys, setExpandedKeys] = useState<string[]>([])
  const [modalVisible, setModalVisible] = useState(false)
  const [modalTitle, setModalTitle] = useState('新增部门')
  const [editingId, setEditingId] = useState<string | null>(null)
  const [selectedDept, setSelectedDept] = useState<Dept | null>(null)

  const loadData = async () => {
    setLoading(true)
    try {
      const response = await getDeptTree()
      setDeptTree(response.data)
      setAllDepts(response.data)
      // 默认展开所有节点
      const keys = getAllKeys(response.data)
      setExpandedKeys(keys)
    } catch (error) {
      message.error('加载部门列表失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [])

  // 获取所有节点的key
  const getAllKeys = (depts: Dept[]): string[] => {
    let keys: string[] = []
    depts.forEach((dept) => {
      if (dept.id) {
        keys.push(dept.id)
      }
      if (dept.children && dept.children.length > 0) {
        keys = keys.concat(getAllKeys(dept.children))
      }
    })
    return keys
  }

  const handleOpenModal = (record?: Dept, parentDept?: Dept) => {
    if (record) {
      setModalTitle('编辑部门')
      setEditingId(record.id!)
      form.setFieldsValue(record)
    } else {
      setModalTitle('新增部门')
      setEditingId(null)
      form.resetFields()
      form.setFieldsValue({
        parentId: parentDept?.id || '0',
        status: 1
      })
    }
    setModalVisible(true)
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      if (editingId) {
        await updateDept(editingId, values)
        message.success('更新成功')
      } else {
        await createDept(values)
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

  const handleDelete = async (id: string) => {
    try {
      await deleteDept(id)
      message.success('删除成功')
      loadData()
    } catch (error) {
      message.error('删除失败')
    }
  }

  // 构建树节点
  const buildTreeNodes = (depts: Dept[]): DataNode[] => {
    return depts.map((dept) => ({
      key: dept.id!,
      title: (
        <div className="dept-tree-node">
          <div className="dept-tree-content">
            <div className="dept-tree-title">
              <Space size={4}>
                <ApartmentOutlined style={{ color: '#1890ff' }} />
                <span className="dept-name">{dept.deptName}</span>
                {dept.leader && <Tag color="blue">{dept.leader}</Tag>}
                {dept.status === 0 && <Tag color="error">禁用</Tag>}
                {dept.children && dept.children.length > 0 && (
                  <Tag color="cyan">{dept.children.length}</Tag>
                )}
              </Space>
            </div>
          </div>
          <Space className="dept-tree-actions" size={2}>
            <Button
              type="link"
              size="small"
              icon={<PlusOutlined />}
              onClick={(e) => {
                e.stopPropagation()
                handleOpenModal(undefined, dept)
              }}
            >
              新增
            </Button>
            <Button
              type="link"
              size="small"
              icon={<EditOutlined />}
              onClick={(e) => {
                e.stopPropagation()
                handleOpenModal(dept)
              }}
            >
              编辑
            </Button>
            <Popconfirm
              title="确定要删除吗?"
              onConfirm={() => handleDelete(dept.id!)}
              okText="确定"
              cancelText="取消"
            >
              <Button
                type="link"
                size="small"
                danger
                icon={<DeleteOutlined />}
                onClick={(e) => e.stopPropagation()}
              >
                删除
              </Button>
            </Popconfirm>
          </Space>
        </div>
      ),
      children: dept.children ? buildTreeNodes(dept.children) : undefined,
    }))
  }

  // 构建父部门选项
  const buildParentDeptOptions = (depts: Dept[], level = 0): any[] => {
    let options: any[] = []
    depts.forEach((dept) => {
      options.push({
        label: `${'　'.repeat(level)}${dept.deptName}`,
        value: dept.id,
      })
      if (dept.children && dept.children.length > 0) {
        options = options.concat(buildParentDeptOptions(dept.children, level + 1))
      }
    })
    return options
  }

  // 根据ID查找部门
  const findDept = (depts: Dept[], id: string): Dept | null => {
    for (const dept of depts) {
      if (dept.id === id) return dept
      if (dept.children) {
        const found = findDept(dept.children, id)
        if (found) return found
      }
    }
    return null
  }

  return (
    <div>
      {/* 统计卡片 */}
      <div style={{ marginBottom: 16 }}>
        <DeptStatistics depts={allDepts} />
      </div>

      {/* 搜索筛选栏 */}
      <Card style={{ marginBottom: 16 }}>
        <Form form={searchForm} layout="inline">
          <Form.Item label="部门名称">
            <Input placeholder="搜索部门名称" allowClear style={{ width: 200 }} />
          </Form.Item>
          <Form.Item label="负责人">
            <Input placeholder="搜索负责人" allowClear style={{ width: 150 }} />
          </Form.Item>
          <Form.Item label="状态">
            <Select
              placeholder="请选择状态"
              allowClear
              style={{ width: 150 }}
              options={[
                { label: '全部', value: undefined },
                { label: '启用', value: 1 },
                { label: '禁用', value: 0 },
              ]}
            />
          </Form.Item>
        </Form>
      </Card>

      {/* 左右分屏布局 */}
      <Row gutter={16}>
        {/* 左侧：部门树 */}
        <Col xs={24} lg={10}>
          <Card
            title="部门树"
            extra={
              <Space>
                <Button icon={<ReloadOutlined />} onClick={loadData} size="small">
                  刷新
                </Button>
                <Button
                  type="primary"
                  icon={<PlusOutlined />}
                  onClick={() => handleOpenModal()}
                  size="small"
                >
                  新增根部门
                </Button>
              </Space>
            }
            style={{ minHeight: 600 }}
          >
            <Tree
              showLine
              defaultExpandAll
              expandedKeys={expandedKeys}
              onExpand={(keys: any) => setExpandedKeys(keys)}
              onSelect={(selectedKeys) => {
                if (selectedKeys.length > 0) {
                  const dept = findDept(deptTree, selectedKeys[0] as string)
                  setSelectedDept(dept)
                }
              }}
              treeData={buildTreeNodes(deptTree)}
              loading={loading}
            />
          </Card>
        </Col>

        {/* 右侧：详情面板 */}
        <Col xs={24} lg={14}>
          <DeptDetailPanel
            dept={selectedDept}
            onEdit={handleOpenModal}
            onDelete={(id) => {
              Modal.confirm({
                title: '确定要删除吗?',
                content: '删除后将无法恢复',
                onOk: () => handleDelete(id),
              })
            }}
            onAddChild={(parentDept) => handleOpenModal(undefined, parentDept)}
          />
        </Col>
      </Row>

      <Modal
        title={modalTitle}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        width={600}
        destroyOnClose
      >
        <Form form={form} labelCol={{ span: 6 }} wrapperCol={{ span: 16 }}>
          <Form.Item name="deptName" label="部门名称" rules={[{ required: true, message: '请输入部门名称' }]}>
            <Input placeholder="请输入部门名称" />
          </Form.Item>

          <Form.Item name="parentId" label="上级部门" initialValue="0">
            <DeptTreeSelect 
              placeholder="请选择上级部门" 
              treeData={deptTree}
            />
          </Form.Item>

          <Form.Item name="orderNum" label="排序">
            <InputNumber min={0} style={{ width: '100%' }} placeholder="请输入排序" />
          </Form.Item>

          <Form.Item name="leader" label="负责人">
            <Input placeholder="请输入负责人" />
          </Form.Item>

          <Form.Item name="phone" label="联系电话" rules={[
            { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }
          ]}>
            <Input placeholder="请输入联系电话" />
          </Form.Item>

          <Form.Item name="email" label="邮箱" rules={[
            { type: 'email', message: '邮箱格式不正确' }
          ]}>
            <Input placeholder="请输入邮箱" />
          </Form.Item>

          <Form.Item name="status" label="部门状态" initialValue={1}>
            <Select>
              <Select.Option value={1}>启用</Select.Option>
              <Select.Option value={0}>禁用</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={3} placeholder="请输入备注" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default DeptList