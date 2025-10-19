import { useState, useEffect } from 'react'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { Layout, Menu, Avatar, Dropdown, theme, Spin } from 'antd'
import {
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  DashboardOutlined,
  UserOutlined,
  TeamOutlined,
  ApartmentOutlined,
  SafetyOutlined,
  BookOutlined,
  MonitorOutlined,
  LogoutOutlined,
  SettingOutlined,
} from '@ant-design/icons'
import type { MenuProps } from 'antd'
import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'
import { getCurrentUserMenuTree } from '@/api/menu'
import { Menu as MenuType } from '@/types'
import './index.css'

const { Header, Sider, Content } = Layout

// 图标映射
const iconMap: Record<string, any> = {
  dashboard: DashboardOutlined,
  system: SettingOutlined,
  monitor: MonitorOutlined,
  user: UserOutlined,
  peoples: TeamOutlined,
  'tree-table': BookOutlined,
  tree: ApartmentOutlined,
  dict: BookOutlined,
  form: BookOutlined,
  logininfor: MonitorOutlined,
  online: MonitorOutlined,
  server: MonitorOutlined,
}

const BasicLayout = () => {
  const navigate = useNavigate()
  const location = useLocation()
  const { userInfo, logout } = useAuthStore()
  const { menuList, collapsed, setMenuList, setCollapsed, clearMenu } = useMenuStore()
  const [loading, setLoading] = useState(false)
  const [selectedKeys, setSelectedKeys] = useState<string[]>([])
  const {
    token: { colorBgContainer, borderRadiusLG },
  } = theme.useToken()

  // 加载用户菜单
  useEffect(() => {
    loadUserMenu()
  }, [])

  // 根据路由设置选中的菜单
  useEffect(() => {
    const path = location.pathname
    setSelectedKeys([path])
  }, [location.pathname])

  const loadUserMenu = async () => {
    // 如果已有菜单数据，不重复加载
    if (menuList && menuList.length > 0) {
      return
    }

    setLoading(true)
    try {
      const response = await getCurrentUserMenuTree()
      const menus = response.data || []
      setMenuList(menus)
    } catch (error) {
      console.error('加载菜单失败:', error)
    } finally {
      setLoading(false)
    }
  }

  // 构建菜单项
  const buildMenuItems = (menus: MenuType[]): MenuProps['items'] => {
    return menus
      .filter((menu) => menu.visible === 1 && menu.status === 1) // 只显示启用且可见的菜单
      .sort((a, b) => (a.orderNum || 0) - (b.orderNum || 0)) // 按排序号排序
      .map((menu) => {
        const IconComponent = menu.icon ? iconMap[menu.icon] : null

        // 如果是按钮类型，不显示在菜单中
        if (menu.menuType === 'F') {
          return null
        }

        const menuItem: any = {
          key: menu.path || menu.id,
          icon: IconComponent ? <IconComponent /> : null,
          label: menu.menuName,
        }

        // 如果有子菜单，先递归构建
        if (menu.children && menu.children.length > 0) {
          const childrenItems = buildMenuItems(menu.children)
          // 只有在过滤后还有可见子菜单时才设置 children
          if (childrenItems && childrenItems.length > 0) {
            menuItem.children = childrenItems
          } else if (menu.path) {
            // 子菜单都是按钮(F)被过滤掉了，当前菜单有路径则可点击
            menuItem.onClick = () => {
              const path = menu.path!.startsWith('/') ? menu.path! : `/${menu.path!}`
              navigate(path)
            }
          }
        } else if (menu.path) {
          // 没有子菜单但有路径的菜单/目录都添加点击事件
          menuItem.onClick = () => {
            // 确保路径以 / 开头
            const path = menu.path!.startsWith('/') ? menu.path! : `/${menu.path!}`
            navigate(path)
          }
        }

        return menuItem
      })
      .filter(Boolean) // 过滤掉null项
  }

  const userMenuItems: MenuProps['items'] = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: '个人中心',
      onClick: () => navigate('/user/profile'),
    },
    {
      type: 'divider',
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: () => {
        logout()
        clearMenu() // 清除菜单缓存
        navigate('/login')
      },
    },
  ]

  return (
    <Layout className="layout-container">
      <Sider trigger={null} collapsible collapsed={collapsed} theme="dark">
        <div className="logo">
          <h1>{collapsed ? 'BB' : 'BaseBackend'}</h1>
        </div>
        {loading ? (
          <div style={{ textAlign: 'center', padding: '20px' }}>
            <Spin />
          </div>
        ) : (
          <Menu
            theme="dark"
            mode="inline"
            selectedKeys={selectedKeys}
            items={buildMenuItems(menuList)}
          />
        )}
      </Sider>
      <Layout>
        <Header style={{ padding: 0, background: colorBgContainer }}>
          <div className="header-content">
            <div className="header-left">
              {collapsed ? (
                <MenuUnfoldOutlined
                  className="trigger"
                  onClick={() => setCollapsed(!collapsed)}
                />
              ) : (
                <MenuFoldOutlined
                  className="trigger"
                  onClick={() => setCollapsed(!collapsed)}
                />
              )}
            </div>
            <div className="header-right">
              <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
                <div className="user-info">
                  <Avatar icon={<UserOutlined />} src={userInfo?.avatar} />
                  <span className="user-name">{userInfo?.nickname || userInfo?.username}</span>
                </div>
              </Dropdown>
            </div>
          </div>
        </Header>
        <Content
          style={{
            margin: '24px 16px',
            padding: 24,
            minHeight: 280,
            background: colorBgContainer,
            borderRadius: borderRadiusLG,
            overflow: 'auto',
          }}
        >
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}

export default BasicLayout