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
// import TodoList from '@/pages/Workflow/TaskManagement/TodoList'
import TaskDetail from '@/pages/Workflow/TaskManagement/TaskDetail'
import MyInitiated from '@/pages/Workflow/TaskManagement/MyInitiated'
import ProcessTemplateIndex from '@/pages/Workflow/ProcessTemplate'
// import LeaveApproval from '@/pages/Workflow/ProcessTemplate/LeaveApproval'
import ExpenseApproval from '@/pages/Workflow/ProcessTemplate/ExpenseApproval'
import PurchaseApproval from '@/pages/Workflow/ProcessTemplate/PurchaseApproval'
import ProcessInstanceList from '@/pages/Workflow/ProcessInstance'
import ProcessInstanceDetail from '@/pages/Workflow/ProcessInstance/Detail'
import ProcessDefinitionList from '@/pages/Workflow/ProcessDefinition'
import ProcessHistory from '@/pages/Workflow/ProcessHistory'
import FileList from '@/pages/File/FileList'
import RecycleBin from '@/pages/File/RecycleBin'
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

        {/* 工作流管理 */}
        {/* <Route path="workflow/todo" element={<TodoList />} /> */}
        <Route path="workflow/todo/:taskId" element={<TaskDetail />} />
        <Route path="workflow/initiated" element={<MyInitiated />} />
        <Route path="workflow/template" element={<ProcessTemplateIndex />} />
        {/* <Route path="workflow/template/leave" element={<LeaveApproval />} /> */}
        <Route path="workflow/template/expense" element={<ExpenseApproval />} />
        <Route path="workflow/template/purchase" element={<PurchaseApproval />} />
        <Route path="workflow/instance" element={<ProcessInstanceList />} />
        <Route path="workflow/instance/:instanceId" element={<ProcessInstanceDetail />} />
        <Route path="workflow/definition" element={<ProcessDefinitionList />} />
        <Route path="workflow/history" element={<ProcessHistory />} />

        {/* 文件管理 */}
        <Route path="file/list" element={<FileList />} />
        <Route path="file/recycle-bin" element={<RecycleBin />} />
      </Route>
    </Routes>
  )
}

export default AppRouter
