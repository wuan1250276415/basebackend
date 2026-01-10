import request from '@/utils/request'
import type {
  Task,
  TaskQueryParams,
  CompleteTaskParams,
  ClaimTaskParams,
  DelegateTaskParams,
  ApiResponse,
  PageResult,
} from '@/types/workflow'

const BASE_URL = '/api/camunda/tasks'

/**
 * 分页查询任务
 * 支持租户、流程实例、任务分配人、候选用户/组过滤
 */
export const listTasks = async (
  params?: TaskQueryParams
): Promise<ApiResponse<PageResult<Task>>> => {
  return request.get(BASE_URL, { params })
}

/**
 * 查询待办任务 (Helper wrapper around listTasks)
 */
export const listPendingTasks = async (
  assignee: string,
  params?: Omit<TaskQueryParams, 'assignee'>
): Promise<ApiResponse<PageResult<Task>>> => {
  return listTasks({ ...params, assignee })
}

/**
 * 查询候选任务 (Helper wrapper around listTasks)
 */
export const listCandidateTasks = async (
  candidateUser: string,
  params?: Omit<TaskQueryParams, 'candidateUser'>
): Promise<ApiResponse<PageResult<Task>>> => {
  return listTasks({ ...params, candidateUser })
}

/**
 * 根据流程实例ID查询任务 (Helper wrapper around listTasks)
 */
export const listTasksByProcessInstanceId = async (
  processInstanceId: string
): Promise<ApiResponse<PageResult<Task>>> => {
  return listTasks({ processInstanceId })
}

/**
 * 根据任务ID查询任务
 */
export const getTaskById = async (
  taskId: string,
  withVariables: boolean = false
): Promise<ApiResponse<Task>> => {
  return request.get(`${BASE_URL}/${taskId}`, {
    params: { withVariables }
  })
}

/**
 * 完成任务
 */
export const completeTask = async (
  taskId: string,
  variables: CompleteTaskParams
): Promise<ApiResponse> => {
  return request.post(`${BASE_URL}/${taskId}/complete`, variables)
}

/**
 * 认领任务
 */
export const claimTask = async (
  taskId: string,
  data: ClaimTaskParams
): Promise<ApiResponse> => {
  return request.post(`${BASE_URL}/${taskId}/claim`, data)
}

/**
 * 释放任务 (Unclaim)
 */
export const unclaimTask = async (taskId: string): Promise<ApiResponse> => {
  return request.post(`${BASE_URL}/${taskId}/unclaim`)
}

/**
 * 委派任务
 */
export const delegateTask = async (
  taskId: string,
  data: DelegateTaskParams
): Promise<ApiResponse> => {
  return request.post(`${BASE_URL}/${taskId}/delegate`, data)
}

/**
 * 获取任务变量
 */
export const getTaskVariables = async (
  taskId: string
): Promise<ApiResponse<Record<string, any>>> => {
  return request.get(`${BASE_URL}/${taskId}/variables`)
}

/**
 * 设置/更新任务变量
 */
export const setTaskVariables = async (
  taskId: string,
  variables: { key: string; value: any; local?: boolean }
): Promise<ApiResponse> => {
  return request.put(`${BASE_URL}/${taskId}/variables`, variables)
}

/**
 * 删除任务变量
 */
export const deleteTaskVariable = async (
  taskId: string,
  key: string
): Promise<ApiResponse> => {
  return request.delete(`${BASE_URL}/${taskId}/variables/${key}`)
}

/**
 * 查询任务附件
 */
export const listTaskAttachments = async (
  taskId: string
): Promise<ApiResponse<any[]>> => {
  return request.get(`${BASE_URL}/${taskId}/attachments`)
}

/**
 * 添加任务附件
 */
export const addTaskAttachment = async (
  taskId: string,
  data: { name: string; description?: string; type: string; url: string }
): Promise<ApiResponse<any>> => {
  return request.post(`${BASE_URL}/${taskId}/attachments`, data)
}

/**
 * 查询任务评论
 */
export const listTaskComments = async (
  taskId: string
): Promise<ApiResponse<any[]>> => {
  return request.get(`${BASE_URL}/${taskId}/comments`)
}

/**
 * 添加任务评论
 */
export const addTaskComment = async (
  taskId: string,
  data: { message: string }
): Promise<ApiResponse<any>> => {
  return request.post(`${BASE_URL}/${taskId}/comments`, data)
}

// ========== 超时任务查询 ==========

/**
 * 查询已超时任务
 */
export const listOverdueTasks = async (
  params?: TaskQueryParams
): Promise<ApiResponse<PageResult<Task>>> => {
  return request.get(`${BASE_URL}/overdue`, { params })
}

/**
 * 查询即将超时任务
 */
export const listDueSoonTasks = async (
  params?: TaskQueryParams,
  hours: number = 24
): Promise<ApiResponse<PageResult<Task>>> => {
  return request.get(`${BASE_URL}/due-soon`, { params: { ...params, hours } })
}

/**
 * 统计已超时任务数量
 */
export const countOverdueTasks = async (): Promise<ApiResponse<number>> => {
  return request.get(`${BASE_URL}/overdue/count`)
}

/**
 * 统计即将超时任务数量
 */
export const countDueSoonTasks = async (hours: number = 24): Promise<ApiResponse<number>> => {
  return request.get(`${BASE_URL}/due-soon/count`, { params: { hours } })
}

// ========== 批量任务操作 ==========

export interface BatchOperationResult {
  total: number
  success: number
  failed: number
}

/**
 * 批量完成任务
 */
export const batchCompleteTasks = async (
  taskIds: string[],
  variables?: Record<string, any>
): Promise<ApiResponse<BatchOperationResult>> => {
  return request.post(`${BASE_URL}/batch-complete`, taskIds, {
    params: variables ? { variables } : undefined
  })
}

/**
 * 批量认领任务
 */
export const batchClaimTasks = async (
  taskIds: string[],
  userId: string
): Promise<ApiResponse<BatchOperationResult>> => {
  return request.post(`${BASE_URL}/batch-claim`, taskIds, {
    params: { userId }
  })
}

/**
 * 批量委派任务
 */
export const batchDelegateTasks = async (
  taskIds: string[],
  userId: string
): Promise<ApiResponse<BatchOperationResult>> => {
  return request.post(`${BASE_URL}/batch-delegate`, taskIds, {
    params: { userId }
  })
}
