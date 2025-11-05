import { Card, Row, Col, Statistic } from 'antd'
import {
  ApartmentOutlined,
  UserOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
} from '@ant-design/icons'
import { Dept } from '@/types'

interface DeptStatisticsProps {
  depts: Dept[]
}

const DeptStatistics = ({ depts }: DeptStatisticsProps) => {
  // 递归计算统计数据
  const calculateStatistics = (deptList: Dept[]) => {
    let total = 0
    let hasLeader = 0
    let enabled = 0
    let disabled = 0
    let rootCount = deptList.length // 根部门数量

    const count = (items: Dept[]) => {
      items.forEach((item) => {
        total++
        if (item.leader) hasLeader++
        if (item.status === 1) {
          enabled++
        } else {
          disabled++
        }
        if (item.children && item.children.length > 0) {
          count(item.children)
        }
      })
    }

    count(deptList)
    return { total, hasLeader, enabled, disabled, rootCount }
  }

  const stats = calculateStatistics(depts)

  return (
    <Row gutter={16}>
      <Col xs={24} sm={12} md={6}>
        <Card>
          <Statistic
            title="总部门数"
            value={stats.total}
            prefix={<ApartmentOutlined style={{ color: '#1890ff' }} />}
            valueStyle={{ color: '#1890ff' }}
          />
        </Card>
      </Col>
      <Col xs={24} sm={12} md={6}>
        <Card>
          <Statistic
            title="已配置负责人"
            value={stats.hasLeader}
            prefix={<UserOutlined style={{ color: '#52c41a' }} />}
            valueStyle={{ color: '#52c41a' }}
            suffix={<span style={{ fontSize: 14, color: '#8c8c8c' }}>/ {stats.total}</span>}
          />
        </Card>
      </Col>
      <Col xs={24} sm={12} md={6}>
        <Card>
          <Statistic
            title="启用部门"
            value={stats.enabled}
            prefix={<CheckCircleOutlined style={{ color: '#52c41a' }} />}
            valueStyle={{ color: '#52c41a' }}
          />
        </Card>
      </Col>
      <Col xs={24} sm={12} md={6}>
        <Card>
          <Statistic
            title="禁用部门"
            value={stats.disabled}
            prefix={<CloseCircleOutlined style={{ color: '#ff4d4f' }} />}
            valueStyle={{ color: '#ff4d4f' }}
          />
        </Card>
      </Col>
    </Row>
  )
}

export default DeptStatistics
