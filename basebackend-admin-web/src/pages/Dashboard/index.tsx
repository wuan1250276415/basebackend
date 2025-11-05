import { Row, Col, Spin } from 'antd'
import { WelcomeHeader } from './components/WelcomeHeader'
import { CoreMetrics } from './components/CoreMetrics'
import { SystemMonitor } from './components/SystemMonitor'
import { QuickActions } from './components/QuickActions'
import { RecentActivities } from './components/RecentActivities'
import { useDashboardData } from './hooks/useDashboardData'

const Dashboard = () => {
  const {
    coreMetrics,
    systemMonitor,
    recentLogins,
    recentOperations,
    notifications,
    unreadNotificationCount,
    isLoading,
    refetchAll,
  } = useDashboardData()

  return (
    <div>
      {/* 欢迎头部 */}
      <WelcomeHeader onRefresh={refetchAll} loading={isLoading} />

      {/* 核心指标卡片 */}
      <CoreMetrics data={coreMetrics} loading={isLoading} />

      {/* 系统监控 */}
      <Row gutter={16} style={{ marginTop: 16 }}>
        <Col xs={24} lg={24}>
          <SystemMonitor data={systemMonitor} loading={isLoading} />
        </Col>
      </Row>

      {/* 最近动态 */}
      <RecentActivities
        recentLogins={recentLogins}
        recentOperations={recentOperations}
        notifications={notifications}
        unreadCount={unreadNotificationCount}
        loading={isLoading}
      />

      {/* 快捷入口 */}
      <QuickActions />
    </div>
  )
}

export default Dashboard
