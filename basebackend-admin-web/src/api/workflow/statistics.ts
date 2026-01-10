import request from '@/utils/request'
import type {
  ApiResponse,
  ProcessDefinitionsStatistics,
  InstanceStatistics,
  TaskStatistics,
  WorkflowOverview
} from '@/types/workflow'

const BASE_URL = '/api/camunda/statistics'

/**
 * 获取流程定义统计
 */
export const getProcessDefinitionStatistics = async (
  params?: { tenantId?: string; startTime?: string; endTime?: string }
): Promise<ApiResponse<ProcessDefinitionsStatistics>> => {
  return request.get(`${BASE_URL}/process-definitions`, { params })
}

/**
 * 获取流程实例统计
 */
export const getInstanceStatistics = async (): Promise<
  ApiResponse<InstanceStatistics>
> => {
  return request.get(`${BASE_URL}/instances`)
}

/**
 * 获取任务统计
 */
export const getTaskStatistics = async (): Promise<
  ApiResponse<TaskStatistics>
> => {
  return request.get(`${BASE_URL}/tasks`)
}

/**
 * 获取工作流运行状态概览
 */
export const getWorkflowOverview = async (): Promise<
  ApiResponse<WorkflowOverview>
> => {
  return request.get(`${BASE_URL}/overview`)
}
