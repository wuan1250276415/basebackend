import type { ProcessInstance, Task } from '@/types/workflow'

/**
 * 根据优先级获取颜色
 */
export const getPriorityColor = (priority: number): string => {
  if (priority >= 80) return '#f5222d' // 紧急 - 红色
  if (priority >= 50) return '#faad14' // 重要 - 橙色
  return '#1890ff' // 普通 - 蓝色
}

/**
 * 根据优先级获取文本
 */
export const getPriorityText = (priority: number): string => {
  if (priority >= 80) return '紧急'
  if (priority >= 50) return '重要'
  return '普通'
}

/**
 * 根据流程状态获取颜色
 */
export const getProcessStatusColor = (instance: ProcessInstance): string => {
  if (instance.deleteReason) return '#f5222d' // 已终止 - 红色
  if (instance.ended) return '#52c41a' // 已完成 - 绿色
  if (instance.suspended) return '#faad14' // 已挂起 - 橙色
  return '#1890ff' // 进行中 - 蓝色
}

/**
 * 根据流程状态获取文本
 */
export const getProcessStatusText = (instance: ProcessInstance): string => {
  if (instance.deleteReason) return '已终止'
  if (instance.ended) return '已完成'
  if (instance.suspended) return '已挂起'
  return '进行中'
}

/**
 * 根据任务状态获取颜色
 */
export const getTaskStatusColor = (task: Task): string => {
  if (task.endTime) return '#52c41a' // 已完成 - 绿色
  if (task.dueDate) {
    const now = new Date()
    const due = new Date(task.dueDate)
    if (due < now) return '#f5222d' // 已超时 - 红色
    const hoursDiff = (due.getTime() - now.getTime()) / (1000 * 60 * 60)
    if (hoursDiff < 24) return '#faad14' // 即将超时 - 橙色
  }
  return '#1890ff' // 正常 - 蓝色
}

/**
 * 根据任务状态获取文本
 */
export const getTaskStatusText = (task: Task): string => {
  if (task.endTime) return '已完成'
  if (task.dueDate) {
    const now = new Date()
    const due = new Date(task.dueDate)
    if (due < now) return '已超时'
    const hoursDiff = (due.getTime() - now.getTime()) / (1000 * 60 * 60)
    if (hoursDiff < 24) return '即将超时'
  }
  return '正常'
}

/**
 * 根据流程类型获取颜色
 */
export const getProcessTypeColor = (processName: string): string => {
  const name = processName.toLowerCase()
  if (name.includes('leave') || name.includes('请假')) return '#1890ff'
  if (name.includes('expense') || name.includes('报销')) return '#52c41a'
  if (name.includes('purchase') || name.includes('采购')) return '#faad14'
  return '#d9d9d9'
}

/**
 * 根据流程类型获取文本
 */
export const getProcessTypeText = (processName: string): string => {
  const name = processName.toLowerCase()
  if (name.includes('leave') || name.includes('请假')) return '请假审批'
  if (name.includes('expense') || name.includes('报销')) return '报销审批'
  if (name.includes('purchase') || name.includes('采购')) return '采购审批'
  return '其他流程'
}
