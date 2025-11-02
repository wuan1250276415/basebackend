import { Card, Descriptions, Tag, Space, Button, Empty } from 'antd'
import { EditOutlined, DeleteOutlined, PlusOutlined, CopyOutlined } from '@ant-design/icons'
import { Menu, Application } from '@/types'

interface MenuDetailPanelProps {
  menu: Menu | null
  applications: Application[]
  onEdit: (menu: Menu) => void
  onDelete: (id: string) => void
  onAddChild: (parentMenu: Menu) => void
}

const MenuDetailPanel = ({
  menu,
  applications,
  onEdit,
  onDelete,
  onAddChild
}: MenuDetailPanelProps) => {
  if (!menu) {
    return (
      <Card
        title="菜单详情"
        style={{ height: '100%', minHeight: 500 }}
      >
        <Empty
          description="请在左侧选择一个菜单"
          style={{ marginTop: 100 }}
        />
      </Card>
    )
  }

  // 获取应用名称
  const getAppName = (appId?: string) => {
    if (!appId) return '系统菜单'
    const app = applications.find((a) => a.id === appId)
    return app ? app.appName : '未知应用'
  }

  // 获取菜单类型显示
  const getMenuTypeDisplay = (menuType: string) => {
    const typeMap: Record<string, { label: string; color: string }> = {
      M: { label: '目录', color: 'blue' },
      C: { label: '菜单', color: 'green' },
      F: { label: '按钮', color: 'orange' }
    }
    const type = typeMap[menuType] || { label: '未知', color: 'default' }
    return <Tag color={type.color}>{type.label}</Tag>
  }

  return (
    <Card
      title={
        <Space>
          <span>菜单详情</span>
          <Tag color="blue">{menu.menuName}</Tag>
        </Space>
      }
      extra={
        <Space>
          <Button
            icon={<PlusOutlined />}
            onClick={() => onAddChild(menu)}
            size="small"
          >
            新增子菜单
          </Button>
          <Button
            icon={<EditOutlined />}
            onClick={() => onEdit(menu)}
            type="primary"
            size="small"
          >
            编辑
          </Button>
          <Button
            icon={<DeleteOutlined />}
            onClick={() => onDelete(menu.id!)}
            danger
            size="small"
          >
            删除
          </Button>
        </Space>
      }
      style={{ height: '100%' }}
    >
      <Descriptions column={1} bordered size="small">
        <Descriptions.Item label="菜单ID">
          {menu.id}
        </Descriptions.Item>

        <Descriptions.Item label="菜单名称">
          {menu.menuName}
        </Descriptions.Item>

        <Descriptions.Item label="所属应用">
          {menu.appId ? (
            <Tag color="blue">{getAppName(menu.appId)}</Tag>
          ) : (
            <Tag>系统菜单</Tag>
          )}
        </Descriptions.Item>

        <Descriptions.Item label="菜单类型">
          {getMenuTypeDisplay(menu.menuType)}
        </Descriptions.Item>

        <Descriptions.Item label="排序号">
          {menu.orderNum ?? '-'}
        </Descriptions.Item>

        <Descriptions.Item label="路由地址">
          <code style={{
            background: '#f5f5f5',
            padding: '2px 6px',
            borderRadius: 4,
            fontSize: 12
          }}>
            {menu.path || '-'}
          </code>
        </Descriptions.Item>

        <Descriptions.Item label="组件路径">
          <code style={{
            background: '#f5f5f5',
            padding: '2px 6px',
            borderRadius: 4,
            fontSize: 12
          }}>
            {menu.component || '-'}
          </code>
        </Descriptions.Item>

        <Descriptions.Item label="权限标识">
          {menu.perms ? (
            <Tag color="purple">{menu.perms}</Tag>
          ) : (
            '-'
          )}
        </Descriptions.Item>

        <Descriptions.Item label="菜单图标">
          {menu.icon || '-'}
        </Descriptions.Item>

        <Descriptions.Item label="显示状态">
          {menu.visible === 1 ? (
            <Tag color="success">显示</Tag>
          ) : (
            <Tag color="default">隐藏</Tag>
          )}
        </Descriptions.Item>

        <Descriptions.Item label="菜单状态">
          {menu.status === 1 ? (
            <Tag color="success">启用</Tag>
          ) : (
            <Tag color="error">禁用</Tag>
          )}
        </Descriptions.Item>

        <Descriptions.Item label="是否外链">
          {menu.isFrame === 0 ? (
            <Tag color="orange">是</Tag>
          ) : (
            <Tag>否</Tag>
          )}
        </Descriptions.Item>

        <Descriptions.Item label="是否缓存">
          {menu.isCache === 1 ? (
            <Tag color="blue">缓存</Tag>
          ) : (
            <Tag>不缓存</Tag>
          )}
        </Descriptions.Item>

        {menu.remark && (
          <Descriptions.Item label="备注">
            {menu.remark}
          </Descriptions.Item>
        )}

        <Descriptions.Item label="创建时间">
          {menu.createTime || '-'}
        </Descriptions.Item>

        <Descriptions.Item label="更新时间">
          {menu.updateTime || '-'}
        </Descriptions.Item>
      </Descriptions>
    </Card>
  )
}

export default MenuDetailPanel
