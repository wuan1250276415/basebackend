import { useState, useEffect } from 'react'
import { Card, Tree, Button, Space, Form, Modal, message, Tag, Popconfirm, Input, Select, InputNumber, Row, Col } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, FolderOutlined, FileOutlined, ApiOutlined, ReloadOutlined } from '@ant-design/icons'
import type { DataNode } from 'antd/es/tree'
import { getMenuTree, createMenu, updateMenu, deleteMenu } from '@/api/menu'
import { getEnabledApplications } from '@/api/application'
import { Menu, Application } from '@/types'
import MenuStatistics from './components/MenuStatistics'
import MenuDetailPanel from './components/MenuDetailPanel'
import './index.css'

const MenuList = () => {
  const [form] = Form.useForm()
  const [searchForm] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [menuTree, setMenuTree] = useState<Menu[]>([])
  const [allMenus, setAllMenus] = useState<Menu[]>([])
  const [applications, setApplications] = useState<Application[]>([])
  const [expandedKeys, setExpandedKeys] = useState<string[]>([])
  const [modalVisible, setModalVisible] = useState(false)
  const [modalTitle, setModalTitle] = useState('新增菜单')
  const [editingId, setEditingId] = useState<string | null>(null)
  const [selectedMenu, setSelectedMenu] = useState<Menu | null>(null)
  const [selectedAppId, setSelectedAppId] = useState<string | undefined>(undefined)

  // 加载应用列表
  const loadApplications = async () => {
    try {
      const response = await getEnabledApplications()
      setApplications(response.data)
    } catch (error) {
      console.error('加载应用列表失败', error)
    }
  }

  const loadData = async () => {
    setLoading(true)
    try {
      const response = await getMenuTree()
      setAllMenus(response.data)
      filterMenusByApp(response.data, selectedAppId)
      // 默认展开所有节点
      const keys = getAllKeys(response.data)
      setExpandedKeys(keys)
    } catch (error) {
      message.error('加载菜单列表失败')
    } finally {
      setLoading(false)
    }
  }

  // 根据应用过滤菜单
  const filterMenusByApp = (menus: Menu[], appId?: string) => {
    if (appId === undefined) {
      // 显示所有菜单
      setMenuTree(menus)
      return
    }

    if (appId === '') {
      // 只显示系统菜单（appId为空）
      const filtered = menus.filter(menu => !menu.appId)
      setMenuTree(filterChildrenRecursive(filtered))
      return
    }

    // 显示指定应用的菜单
    const filtered = menus.filter(menu => menu.appId === appId)
    setMenuTree(filterChildrenRecursive(filtered))
  }

  // 递归过滤子菜单
  const filterChildrenRecursive = (menus: Menu[]): Menu[] => {
    return menus.map(menu => ({
      ...menu,
      children: menu.children ? filterChildrenRecursive(menu.children) : undefined
    }))
  }

  // 处理应用筛选变化
  const handleAppFilterChange = (appId: string | undefined) => {
    setSelectedAppId(appId)
    filterMenusByApp(allMenus, appId)
  }

  useEffect(() => {
    loadData()
    loadApplications()
  }, [])

  // 获取所有节点的key
  const getAllKeys = (menus: Menu[]): string[] => {
    let keys: string[] = []
    menus.forEach((menu) => {
      if (menu.id) {
        keys.push(menu.id)
      }
      if (menu.children && menu.children.length > 0) {
        keys = keys.concat(getAllKeys(menu.children))
      }
    })
    return keys
  }

  // 获取应用名称
  const getAppName = (appId?: string) => {
    if (!appId) return <Tag>系统菜单</Tag>
    const app = applications.find((a) => a.id === appId)
    return app ? <Tag color="blue">{app.appName}</Tag> : <Tag color="blue">应用菜单</Tag>
  }

  const handleOpenModal = (record?: Menu, parentMenu?: Menu) => {
    if (record) {
      setModalTitle('编辑菜单')
      setEditingId(record.id!)
      form.setFieldsValue(record)
    } else {
      setModalTitle('新增菜单')
      setEditingId(null)
      form.resetFields()
      form.setFieldsValue({
        parentId: parentMenu?.id || '0',
        appId: parentMenu?.appId || selectedAppId,
        status: 1,
        visible: 1,
        isFrame: 1,
        isCache: 0,
        menuType: 'C'
      })
    }
    setModalVisible(true)
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      if (editingId) {
        await updateMenu(editingId, values)
        message.success('更新成功')
      } else {
        await createMenu(values)
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
      await deleteMenu(id)
      message.success('删除成功')
      loadData()
    } catch (error) {
      message.error('删除失败')
    }
  }

  // 获取菜单图标
  const getMenuIcon = (menuType: string) => {
    switch (menuType) {
      case 'M':
        return <FolderOutlined style={{ color: '#1890ff' }} />
      case 'C':
        return <FileOutlined style={{ color: '#52c41a' }} />
      case 'F':
        return <ApiOutlined style={{ color: '#faad14' }} />
      default:
        return <FileOutlined />
    }
  }

  // 构建树节点
  const buildTreeNodes = (menus: Menu[]): DataNode[] => {
    return menus.map((menu) => ({
      key: menu.id!,
      title: (
        <div className="menu-tree-node">
          <div className="menu-tree-content">
            <div className="menu-tree-title">
              <Space size={4}>
                {getMenuIcon(menu.menuType)}
                <span className="menu-name">{menu.menuName}</span>
                {menu.menuType === 'M' && <Tag color="blue">目录</Tag>}
                {menu.menuType === 'C' && <Tag color="green">菜单</Tag>}
                {menu.menuType === 'F' && <Tag color="orange">按钮</Tag>}
                {menu.status === 0 && <Tag color="error">禁用</Tag>}
                {menu.children && menu.children.length > 0 && (
                  <Tag color="cyan">{menu.children.length}</Tag>
                )}
              </Space>
            </div>
            <div className="menu-tree-meta">
              {menu.path && <span className="meta-item">路由: {menu.path}</span>}
              {menu.perms && <span className="meta-item">权限: {menu.perms}</span>}
            </div>
          </div>
          <Space className="menu-tree-actions" size={2}>
            <Button
              type="link"
              size="small"
              icon={<PlusOutlined />}
              onClick={(e) => {
                e.stopPropagation()
                handleOpenModal(undefined, menu)
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
                handleOpenModal(menu)
              }}
            >
              编辑
            </Button>
            <Popconfirm
              title="确定要删除吗?"
              onConfirm={() => handleDelete(menu.id!)}
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
      children: menu.children ? buildTreeNodes(menu.children) : undefined,
    }))
  }

  // 构建父菜单选项（只包含目录和菜单）
  const buildParentMenuOptions = (menus: Menu[], level = 0): any[] => {
    let options: any[] = []
    menus.forEach((menu) => {
      if (menu.menuType !== 'F') {
        options.push({
          label: `${'　'.repeat(level)}${menu.menuName}`,
          value: menu.id,
        })
        if (menu.children && menu.children.length > 0) {
          options = options.concat(buildParentMenuOptions(menu.children, level + 1))
        }
      }
    })
    return options
  }

  return (
    <div>
      {/* 统计卡片 */}
      <div style={{ marginBottom: 16 }}>
        <MenuStatistics menus={allMenus} />
      </div>

      {/* 搜索筛选栏 */}
      <Card style={{ marginBottom: 16 }}>
        <Form form={searchForm} layout="inline">
          <Form.Item label="菜单名称">
            <Input
              placeholder="搜索菜单名称"
              allowClear
              style={{ width: 200 }}
            />
          </Form.Item>
          <Form.Item label="所属应用">
            <Select
              placeholder="请选择应用"
              allowClear
              style={{ width: 200 }}
              value={selectedAppId}
              onChange={handleAppFilterChange}
              options={[
                { label: '全部', value: undefined },
                { label: '系统菜单', value: '' },
                ...applications.map((app) => ({
                  label: app.appName,
                  value: app.id,
                })),
              ]}
            />
          </Form.Item>
          <Form.Item label="菜单类型">
            <Select
              placeholder="请选择类型"
              allowClear
              style={{ width: 150 }}
              options={[
                { label: '全部', value: undefined },
                { label: '目录', value: 'M' },
                { label: '菜单', value: 'C' },
                { label: '按钮', value: 'F' },
              ]}
            />
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
        {/* 左侧：菜单树 */}
        <Col xs={24} lg={10}>
          <Card
            title="菜单树"
            extra={
              <Space>
                <Button icon={<ReloadOutlined />} onClick={loadData} size="small">
                  刷新
                </Button>
                <Button type="primary" icon={<PlusOutlined />} onClick={() => handleOpenModal()} size="small">
                  新增根菜单
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
                  const findMenu = (menus: Menu[], id: string): Menu | null => {
                    for (const menu of menus) {
                      if (menu.id === id) return menu
                      if (menu.children) {
                        const found = findMenu(menu.children, id)
                        if (found) return found
                      }
                    }
                    return null
                  }
                  const menu = findMenu(allMenus, selectedKeys[0] as string)
                  setSelectedMenu(menu)
                }
              }}
              treeData={buildTreeNodes(menuTree)}
              loading={loading}
            />
          </Card>
        </Col>

        {/* 右侧：详情面板 */}
        <Col xs={24} lg={14}>
          <MenuDetailPanel
            menu={selectedMenu}
            applications={applications}
            onEdit={handleOpenModal}
            onDelete={(id) => {
              Modal.confirm({
                title: '确定要删除吗?',
                content: '删除后将无法恢复',
                onOk: () => handleDelete(id)
              })
            }}
            onAddChild={(parentMenu) => handleOpenModal(undefined, parentMenu)}
          />
        </Col>
      </Row>

      <Modal
        title={modalTitle}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        width={700}
        destroyOnClose
      >
        <Form form={form} labelCol={{ span: 6 }} wrapperCol={{ span: 16 }}>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="appId" label="所属应用">
                <Select placeholder="请选择所属应用（系统菜单请不选）" allowClear>
                  {applications.map((app) => (
                    <Select.Option key={app.id} value={app.id}>
                      {app.appName}
                    </Select.Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="menuType" label="菜单类型" rules={[{ required: true }]} initialValue="C">
                <Select>
                  <Select.Option value="M">目录</Select.Option>
                  <Select.Option value="C">菜单</Select.Option>
                  <Select.Option value="F">按钮</Select.Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="menuName" label="菜单名称" rules={[{ required: true }]}>
                <Input placeholder="请输入菜单名称" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="parentId" label="父菜单" initialValue="0">
                <Select placeholder="请选择父菜单">
                  <Select.Option value="0">根菜单</Select.Option>
                  {buildParentMenuOptions(allMenus).map((option) => (
                    <Select.Option key={option.value} value={option.value}>
                      {option.label}
                    </Select.Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="orderNum" label="排序">
                <InputNumber min={0} style={{ width: '100%' }} placeholder="请输入排序" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="icon" label="菜单图标">
                <Input placeholder="请输入图标名称" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="path" label="路由地址">
                <Input placeholder="请输入路由地址" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="component" label="组件路径">
                <Input placeholder="请输入组件路径" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="perms" label="权限标识">
                <Input placeholder="请输入权限标识" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="visible" label="显示状态" initialValue={1}>
                <Select>
                  <Select.Option value={1}>显示</Select.Option>
                  <Select.Option value={0}>隐藏</Select.Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="status" label="菜单状态" initialValue={1}>
                <Select>
                  <Select.Option value={1}>启用</Select.Option>
                  <Select.Option value={0}>禁用</Select.Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="isFrame" label="是否外链" initialValue={1}>
                <Select>
                  <Select.Option value={1}>否</Select.Option>
                  <Select.Option value={0}>是</Select.Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="isCache" label="是否缓存" initialValue={0}>
                <Select>
                  <Select.Option value={1}>缓存</Select.Option>
                  <Select.Option value={0}>不缓存</Select.Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>

          <Form.Item name="remark" label="备注" labelCol={{ span: 3 }} wrapperCol={{ span: 20 }}>
            <Input.TextArea rows={3} placeholder="请输入备注" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default MenuList
