/**
 * Dashboard 数据获取 Hook
 * 使用 React Query 并行获取所有数据，并设置不同的刷新频率
 */

import { useQueries } from 'react-query'
import * as userApi from '@/api/user'
import * as roleApi from '@/api/role'
import * as monitorApi from '@/api/monitor'
import * as logApi from '@/api/log'
import * as notificationApi from '@/api/notification'
import type { DashboardData, CoreMetricsData, SystemMonitorData } from '../types'

export const useDashboardData = () => {
  const today = new Date().toISOString().split('T')[0]

  const queries = useQueries([
    // 用户总数 - 30秒刷新
    {
      queryKey: ['dashboardUserCount'],
      queryFn: async () => {
        const res = await userApi.getUserPage({ current: 1, size: 1 })
        return res.data?.total || 0
      },
      refetchInterval: 30000,
    },
    // 角色总数 - 30秒刷新
    {
      queryKey: ['dashboardRoleCount'],
      queryFn: async () => {
        const res = await roleApi.getRolePage({ current: 1, size: 1 })
        return res.data?.total || 0
      },
      refetchInterval: 30000,
    },
    // 在线用户 - 10秒刷新
    {
      queryKey: ['dashboardOnlineUsers'],
      queryFn: async () => {
        const res = await monitorApi.getOnlineUsers()
        return res.data?.length || 0
      },
      refetchInterval: 10000,
    },
    // 服务器信息 - 5秒刷新
    {
      queryKey: ['dashboardServerInfo'],
      queryFn: async () => {
        const res = await monitorApi.getServerInfo()
        return res.data
      },
      refetchInterval: 5000,
    },
    // 今日操作日志 - 30秒刷新
    {
      queryKey: ['dashboardTodayLogs'],
      queryFn: async () => {
        const res = await logApi.getOperationLogPage({
          current: 1,
          size: 1,
          startTime: `${today} 00:00:00`,
          endTime: `${today} 23:59:59`,
        })
        return res.data?.total || 0
      },
      refetchInterval: 30000,
    },
    // 最近登录记录 - 60秒刷新
    {
      queryKey: ['dashboardRecentLogins'],
      queryFn: async () => {
        const res = await logApi.getLoginLogPage({ current: 1, size: 5 })
        return res.data?.records || []
      },
      refetchInterval: 60000,
    },
    // 最近操作日志 - 60秒刷新
    {
      queryKey: ['dashboardRecentOperations'],
      queryFn: async () => {
        const res = await logApi.getOperationLogPage({ current: 1, size: 5 })
        return res.data?.records || []
      },
      refetchInterval: 60000,
    },
    // 系统通知 - 60秒刷新
    {
      queryKey: ['dashboardNotifications'],
      queryFn: async () => {
        const res = await notificationApi.getNotifications(5)
        return res.data || []
      },
      refetchInterval: 60000,
    },
    // 未读通知数量 - 60秒刷新
    {
      queryKey: ['dashboardUnreadCount'],
      queryFn: async () => {
        const res = await notificationApi.getUnreadCount()
        return res.data || 0
      },
      refetchInterval: 60000,
    },
  ])

  // 提取数据
  const [
    userCountQuery,
    roleCountQuery,
    onlineUsersQuery,
    serverInfoQuery,
    todayLogsQuery,
    recentLoginsQuery,
    recentOperationsQuery,
    notificationsQuery,
    unreadCountQuery,
  ] = queries

  // 构建核心指标数据
  const coreMetrics: CoreMetricsData = {
    userCount: userCountQuery.data || 0,
    roleCount: roleCountQuery.data || 0,
    onlineUsers: onlineUsersQuery.data || 0,
    pendingTasks: 0, // 需要工作流 API
    fileCount: 0, // 需要文件 API
    todayLogs: todayLogsQuery.data || 0,
    activeAlerts: 0, // 需要告警 API
    runningProcesses: 0, // 需要工作流 API
  }

  // 构建系统监控数据
  const serverInfo = serverInfoQuery.data
  const systemMonitor: SystemMonitorData | null = serverInfo
    ? {
        cpuUsage: serverInfo.cpu?.usage || 0,
        memoryUsage: serverInfo.memory?.usage || 0,
        jvmMemoryUsed: serverInfo.jvm?.memoryUsed || 0,
        jvmMemoryTotal: serverInfo.jvm?.memoryTotal || 0,
        systemLoad: {
          load1: serverInfo.sys?.load1 || 0,
          load5: serverInfo.sys?.load5 || 0,
          load15: serverInfo.sys?.load15 || 0,
        },
        apiCallsPerMin: 0, // 需要可观测性 API
        apiErrorRate: 0, // 需要可观测性 API
        avgResponseTime: 0, // 需要可观测性 API
        activeRequests: 0, // 需要可观测性 API
      }
    : null

  // 判断是否加载中
  const isLoading = queries.some((q) => q.isLoading)
  const isError = queries.some((q) => q.isError)

  // 刷新所有数据
  const refetchAll = () => {
    queries.forEach((q) => q.refetch())
  }

  return {
    coreMetrics,
    systemMonitor,
    recentLogins: recentLoginsQuery.data || [],
    recentOperations: recentOperationsQuery.data || [],
    notifications: notificationsQuery.data || [],
    unreadNotificationCount: unreadCountQuery.data || 0,
    isLoading,
    isError,
    refetchAll,
  }
}
