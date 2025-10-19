import { Card, Row, Col, Statistic } from 'antd'
import { UserOutlined, TeamOutlined, FileTextOutlined, AuditOutlined } from '@ant-design/icons'
import { useAuthStore } from '@/stores/auth'

const Dashboard = () => {
  const { userInfo } = useAuthStore()

  return (
    <div>
      <h2 style={{ marginBottom: 24 }}>欢迎回来，{userInfo?.nickname || userInfo?.username}！</h2>
      
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="用户总数"
              value={128}
              prefix={<UserOutlined />}
              valueStyle={{ color: '#3f8600' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="角色总数"
              value={12}
              prefix={<TeamOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="菜单总数"
              value={45}
              prefix={<FileTextOutlined />}
              valueStyle={{ color: '#faad14' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="权限总数"
              value={86}
              prefix={<AuditOutlined />}
              valueStyle={{ color: '#cf1322' }}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col span={24}>
          <Card title="系统信息">
            <p><strong>系统名称：</strong>BaseBackend 后台管理系统</p>
            <p><strong>系统版本：</strong>1.0.0</p>
            <p><strong>后端技术：</strong>Java 17 + Spring Boot 3.1.5 + MyBatis Plus</p>
            <p><strong>前端技术：</strong>React 18 + Ant Design 5 + TypeScript</p>
            <p><strong>数据库：</strong>MySQL 8.0</p>
            <p><strong>缓存：</strong>Redis 6.0</p>
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default Dashboard
