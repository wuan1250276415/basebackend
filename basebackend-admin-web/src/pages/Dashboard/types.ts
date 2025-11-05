/**
 * Dashboard 类型定义
 */

// 核心指标数据
export interface CoreMetricsData {
  userCount: number
  roleCount: number
  onlineUsers: number
  pendingTasks: number
  fileCount: number
  todayLogs: number
  activeAlerts: number
  runningProcesses: number
}

// 系统监控数据
export interface SystemMonitorData {
  cpuUsage: number
  memoryUsage: number
  jvmMemoryUsed: number
  jvmMemoryTotal: number
  systemLoad: {
    load1: number
    load5: number
    load15: number
  }
  apiCallsPerMin: number
  apiErrorRate: number
  avgResponseTime: number
  activeRequests: number
}

// 工作流统计数据
export interface WorkflowStatisticsData {
  runningInstances: number
  completedInstances: number
  suspendedInstances: number
  terminatedInstances: number
  totalInstances: number
  todayStarted: number
  todayCompleted: number
  weekStarted: number
  weekCompleted: number
}

// 可观测性日志统计
export interface LogStatisticsData {
  infoCount: number
  warnCount: number
  errorCount: number
  debugCount: number
  totalCount: number
}

// 可观测性追踪统计
export interface TraceStatisticsData {
  totalTraces: number
  avgDuration: number
  slowTraces: number
}

// 可观测性告警统计
export interface AlertStatisticsData {
  totalAlerts: number
  bySeverity: {
    CRITICAL: number
    ERROR: number
    WARNING: number
    INFO: number
  }
  notifySuccessRate: number
}

// 文件管理统计
export interface FileStatisticsData {
  totalFiles: number
  totalSize: number
  storageUsed: number
  storageTotal: number
  storagePercentage: number
  typeDistribution: {
    type: string
    count: number
    size: number
  }[]
}

// 最近登录记录
export interface RecentLoginRecord {
  id: string
  username: string
  ip: string
  location?: string
  loginTime: string
  status: string
  message?: string
}

// 最近操作日志
export interface RecentOperationLog {
  id: string
  operator: string
  module: string
  operationType: string
  description?: string
  operationTime: string
  status: string
}

// 系统通知
export interface SystemNotification {
  id: string
  title: string
  content: string
  type: 'info' | 'success' | 'warning' | 'error'
  createTime: string
  read: boolean
}

// Dashboard 总体数据
export interface DashboardData {
  coreMetrics: CoreMetricsData
  systemMonitor: SystemMonitorData
  workflowStatistics: WorkflowStatisticsData
  logStatistics: LogStatisticsData
  traceStatistics: TraceStatisticsData
  alertStatistics: AlertStatisticsData
  fileStatistics: FileStatisticsData
  recentLogins: RecentLoginRecord[]
  recentOperations: RecentOperationLog[]
  notifications: SystemNotification[]
  unreadNotificationCount: number
}

// 快捷操作配置
export interface QuickAction {
  key: string
  icon: React.ReactNode
  title: string
  description: string
  path: string
  color?: string
}
