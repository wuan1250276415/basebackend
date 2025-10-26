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

const BASE_URL = '/api/workflow/tasks'

/**
 * 查询待办任务
 */
export const listPendingTasks = async (
  assignee: string
): Promise<ApiResponse<PageResult<Task>>> => {
  return request.get(`${BASE_URL}/pending/${assignee}`)
}

/**
 * 查询候选任务
 */
export const listCandidateTasks = async (
  candidateUser: string
): Promise<ApiResponse<PageResult<Task>>> => {
  return request.get(`${BASE_URL}/candidate/${candidateUser}`)
}

/**
 * 根据流程实例ID查询任务
 */
export const listTasksByProcessInstanceId = async (
  processInstanceId: string
): Promise<ApiResponse<PageResult<Task>>> => {
  return request.get(`${BASE_URL}/process-instance/${processInstanceId}`)
}

/**
 * 根据任务ID查询任务
 */
export const getTaskById = async (
  taskId: string
): Promise<ApiResponse<Task>> => {
  return request.get(`${BASE_URL}/${taskId}`)
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
  userId: ClaimTaskParams
): Promise<ApiResponse> => {
  return request.post(`${BASE_URL}/${taskId}/claim`, userId)
}

/**
 * 取消认领任务
 */
export const unclaimTask = async (taskId: string): Promise<ApiResponse> => {
  return request.post(`${BASE_URL}/${taskId}/unclaim`)
}

/**
 * 委派任务
 */
export const delegateTask = async (
  taskId: string,
  userId: DelegateTaskParams
): Promise<ApiResponse> => {
  return request.post(`${BASE_URL}/${taskId}/delegate`, userId)
}

/**
 * 转办任务
 */
export const assignTask = async (
  taskId: string,
  userId: { userId: string }
): Promise<ApiResponse> => {
  return request.post(`${BASE_URL}/${taskId}/assign`, userId)
}

/**
 * 设置任务变量
 */
export const setTaskVariables = async (
  taskId: string,
  variables: Record<string, any>
): Promise<ApiResponse> => {
  return request.put(`${BASE_URL}/${taskId}/variables`, variables)
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
 * 查询历史任务
 */
export const listHistoricTasksByProcessInstanceId = async (
  processInstanceId: string
): Promise<ApiResponse<PageResult<Task>>> => {
  return request.get(`${BASE_URL}/history/process-instance/${processInstanceId}`)
}
