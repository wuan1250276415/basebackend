import React, { useState, useEffect } from 'react'
import { Tree, Button, Input, Modal, Form, InputNumber, Select, Space, message } from 'antd'
import { PlusOutlined, SearchOutlined } from '@ant-design/icons'
import type { DataNode } from 'antd/es/tree'
import { Role } from '@/types'
import { getRoleTree, createRole } from '@/api/role'

interface RoleTreeProps {
  appId?: string
  onSelect: (role: Role | null) => void
  selectedRoleId?: string
}

const RoleTree: React.FC<RoleTreeProps> = ({ appId, onSelect, selectedRoleId }) => {
  const [treeData, setTreeData] = useState<DataNode[]>([])
  const [expandedKeys, setExpandedKeys] = useState<React.Key[]>([])
  const [searchValue, setSearchValue] = useState('')
  const [autoExpandParent, setAutoExpandParent] = useState(true)
  const [loading, setLoading] = useState(false)
  const [createModalVisible, setCreateModalVisible] = useState(false)
  const [form] = Form.useForm()

  // 加载角色树
  const loadRoleTree = async () => {
    setLoading(true)
    try {
      const res = await getRoleTree(appId)
      if (res.code === 200) {
        const roles = res.data || []
        const treeNodes = convertToTreeData(roles)
        setTreeData(treeNodes)

        // 默认展开第一层
        if (roles.length > 0) {
          setExpandedKeys(roles.map((r: Role) => r.id!))
        }
      }
    } catch (error) {
      message.error('加载角色树失败')
      console.error(error)
    } finally {
      setLoading(false)
    }
  }

  // 转换为Tree组件需要的数据格式
  const convertToTreeData = (roles: Role[]): DataNode[] => {
    return roles.map(role => ({
      key: role.id!,
      title: role.roleName,
      children: role.children ? convertToTreeData(role.children) : undefined,
      data: role,
    }))
  }

  useEffect(() => {
    loadRoleTree()
  }, [appId])

  // 树节点选择
  const handleSelect = (selectedKeys: React.Key[], info: any) => {
    if (selectedKeys.length > 0) {
      const selectedNode = info.node
      onSelect(selectedNode.data)
    } else {
      onSelect(null)
    }
  }

  // 搜索功能
  const handleSearch = (value: string) => {
    setSearchValue(value)
    if (!value) {
      setExpandedKeys([])
      setAutoExpandParent(false)
      return
    }

    // 查找所有匹配的节点并展开其父节点
    const keys: React.Key[] = []
    const findMatchNodes = (nodes: DataNode[]) => {
      nodes.forEach(node => {
        const title = (node.title as string).toLowerCase()
        if (title.includes(value.toLowerCase())) {
          keys.push(node.key)
        }
        if (node.children) {
          findMatchNodes(node.children)
        }
      })
    }
    findMatchNodes(treeData)
    setExpandedKeys(keys)
    setAutoExpandParent(true)
  }

  // 新建角色
  const handleCreateRole = () => {
    form.resetFields()
    form.setFieldsValue({
      parentId: selectedRoleId || '0',
      appId: appId,
      roleSort: 0,
      status: 1,
      dataScope: 1,
    })
    setCreateModalVisible(true)
  }

  const handleCreateSubmit = async () => {
    try {
      const values = await form.validateFields()
      await createRole(values)
      message.success('角色创建成功')
      setCreateModalVisible(false)
      loadRoleTree()
    } catch (error: any) {
      if (error.response) {
        message.error(error.response.data.message || '创建失败')
      } else if (error.errorFields) {
        message.error('请检查表单输入')
      }
    }
  }

  // 高亮搜索文本
  const renderTreeTitle = (title: string) => {
    if (!searchValue) return title

    const index = title.toLowerCase().indexOf(searchValue.toLowerCase())
    if (index === -1) return title

    const beforeStr = title.substring(0, index)
    const matchStr = title.substring(index, index + searchValue.length)
    const afterStr = title.substring(index + searchValue.length)

    return (
      <span>
        {beforeStr}
        <span style={{ color: '#f50', fontWeight: 'bold' }}>{matchStr}</span>
        {afterStr}
      </span>
    )
  }

  // 渲染树节点标题
  const renderTreeData = (nodes: DataNode[]): DataNode[] => {
    return nodes.map(node => ({
      ...node,
      title: renderTreeTitle(node.title as string),
      children: node.children ? renderTreeData(node.children) : undefined,
    }))
  }

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Space direction="vertical" style={{ width: '100%', marginBottom: 16 }}>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={handleCreateRole}
          block
        >
          新建角色
        </Button>
        <Input
          placeholder="搜索角色"
          prefix={<SearchOutlined />}
          allowClear
          onChange={(e) => handleSearch(e.target.value)}
        />
      </Space>

      <div style={{ flex: 1, overflow: 'auto' }}>
        <Tree
          loading={loading}
          treeData={renderTreeData(treeData)}
          selectedKeys={selectedRoleId ? [selectedRoleId] : []}
          expandedKeys={expandedKeys}
          autoExpandParent={autoExpandParent}
          onExpand={(keys) => {
            setExpandedKeys(keys)
            setAutoExpandParent(false)
          }}
          onSelect={handleSelect}
          showLine
        />
      </div>

      {/* 新建角色Modal */}
      <Modal
        title="新建角色"
        open={createModalVisible}
        onOk={handleCreateSubmit}
        onCancel={() => setCreateModalVisible(false)}
        width={600}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="parentId" hidden>
            <Input />
          </Form.Item>
          <Form.Item name="appId" hidden>
            <Input />
          </Form.Item>
          <Form.Item
            label="角色名称"
            name="roleName"
            rules={[{ required: true, message: '请输入角色名称' }]}
          >
            <Input placeholder="请输入角色名称" />
          </Form.Item>
          <Form.Item
            label="角色标识"
            name="roleKey"
            rules={[{ required: true, message: '请输入角色标识' }]}
          >
            <Input placeholder="请输入角色标识，如：admin、user" />
          </Form.Item>
          <Form.Item label="显示顺序" name="roleSort">
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item label="状态" name="status">
            <Select>
              <Select.Option value={1}>启用</Select.Option>
              <Select.Option value={0}>禁用</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item label="数据范围" name="dataScope">
            <Select>
              <Select.Option value={1}>全部数据权限</Select.Option>
              <Select.Option value={2}>本部门数据权限</Select.Option>
              <Select.Option value={3}>本部门及以下数据权限</Select.Option>
              <Select.Option value={4}>仅本人数据权限</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item label="备注" name="remark">
            <Input.TextArea rows={3} placeholder="请输入备注" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default RoleTree
