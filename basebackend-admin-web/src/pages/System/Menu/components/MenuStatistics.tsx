import { Card, Row, Col, Statistic } from 'antd'
import { FolderOutlined, FileOutlined, ApiOutlined, AppstoreOutlined } from '@ant-design/icons'
import { Menu } from '@/types'

interface MenuStatisticsProps {
  menus: Menu[]
}

const MenuStatistics = ({ menus }: MenuStatisticsProps) => {
  // 递归计算菜单统计
  const calculateStatistics = (menuList: Menu[]) => {
    let total = 0
    let directories = 0
    let pages = 0
    let buttons = 0

    const count = (items: Menu[]) => {
      items.forEach((item) => {
        total++
        if (item.menuType === 'M') directories++
        else if (item.menuType === 'C') pages++
        else if (item.menuType === 'F') buttons++

        if (item.children && item.children.length > 0) {
          count(item.children)
        }
      })
    }

    count(menuList)
    return { total, directories, pages, buttons }
  }

  const stats = calculateStatistics(menus)

  return (
    <Row gutter={16}>
      <Col xs={24} sm={12} md={6}>
        <Card>
          <Statistic
            title="总菜单数"
            value={stats.total}
            prefix={<AppstoreOutlined style={{ color: '#1890ff' }} />}
            valueStyle={{ color: '#1890ff' }}
          />
        </Card>
      </Col>
      <Col xs={24} sm={12} md={6}>
        <Card>
          <Statistic
            title="目录数"
            value={stats.directories}
            prefix={<FolderOutlined style={{ color: '#52c41a' }} />}
            valueStyle={{ color: '#52c41a' }}
          />
        </Card>
      </Col>
      <Col xs={24} sm={12} md={6}>
        <Card>
          <Statistic
            title="菜单数"
            value={stats.pages}
            prefix={<FileOutlined style={{ color: '#fa8c16' }} />}
            valueStyle={{ color: '#fa8c16' }}
          />
        </Card>
      </Col>
      <Col xs={24} sm={12} md={6}>
        <Card>
          <Statistic
            title="按钮数"
            value={stats.buttons}
            prefix={<ApiOutlined style={{ color: '#722ed1' }} />}
            valueStyle={{ color: '#722ed1' }}
          />
        </Card>
      </Col>
    </Row>
  )
}

export default MenuStatistics
