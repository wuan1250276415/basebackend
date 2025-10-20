import { Navigate, Route, Routes } from 'react-router-dom'
import Login from '@/pages/Login'
import Layout from '@/layouts/BasicLayout'
import Dashboard from '@/pages/Dashboard'
import UserList from '@/pages/System/User'
import RoleList from '@/pages/System/Role'
import MenuList from '@/pages/System/Menu'
import DeptList from '@/pages/System/Dept'
import DictList from '@/pages/System/Dict'
import ApplicationManagement from '@/pages/System/Application'
import ApplicationResourceManagement from '@/pages/System/ApplicationResource'
import LoginLog from '@/pages/Monitor/LoginLog'
import OperationLog from '@/pages/Monitor/OperationLog'
import OnlineUser from '@/pages/Monitor/OnlineUser'
import ServerMonitor from '@/pages/Monitor/ServerMonitor'
import ObservabilityOverview from '@/pages/Monitor/Observability/Overview'
import LogQuery from '@/pages/Monitor/Observability/LogQuery'
import TraceQuery from '@/pages/Monitor/Observability/TraceQuery'
import AlertManagement from '@/pages/Monitor/Observability/AlertManagement'
import MessageMonitor from '@/pages/Integration/MessageMonitor'
import WebhookConfig from '@/pages/Integration/WebhookConfig'
import EventLog from '@/pages/Integration/EventLog'
import DeadLetter from '@/pages/Integration/DeadLetter'
import Profile from '@/pages/User/Profile'
import ApiDocs from '@/pages/Developer/ApiDocs'
import { useAuthStore } from '@/stores/auth'

const ProtectedRoute = ({ children }: { children: React.ReactNode }) => {
  const { token } = useAuthStore()
  return token ? <>{children}</> : <Navigate to="/login" replace />
}

const AppRouter = () => {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<Dashboard />} />
        
        {/* 系统管理 */}
        <Route path="system/user" element={<UserList />} />
        <Route path="system/role" element={<RoleList />} />
        <Route path="system/menu" element={<MenuList />} />
        <Route path="system/dept" element={<DeptList />} />
        <Route path="system/dict" element={<DictList />} />
        <Route path="system/application" element={<ApplicationManagement />} />
        <Route path="system/application-resource" element={<ApplicationResourceManagement />} />
        
        {/* 系统监控 */}
        <Route path="monitor/online" element={<OnlineUser />} />
        <Route path="monitor/server" element={<ServerMonitor />} />
        <Route path="monitor/loginlog" element={<LoginLog />} />
        <Route path="monitor/operlog" element={<OperationLog />} />

        {/* 可观测性监控 */}
        <Route path="monitor/observability/overview" element={<ObservabilityOverview />} />
        <Route path="monitor/observability/logs" element={<LogQuery />} />
        <Route path="monitor/observability/traces" element={<TraceQuery />} />
        <Route path="monitor/observability/alerts" element={<AlertManagement />} />

        {/* 消息与集成 */}
        <Route path="integration/message-monitor" element={<MessageMonitor />} />
        <Route path="integration/webhook-config" element={<WebhookConfig />} />
        <Route path="integration/event-log" element={<EventLog />} />
        <Route path="integration/dead-letter" element={<DeadLetter />} />

        {/* 个人中心 */}
        <Route path="user/profile" element={<Profile />} />

        {/* 开发者工具 */}
        <Route path="developer/api-docs" element={<ApiDocs />} />
      </Route>
    </Routes>
  )
}

export default AppRouter
