import request from '@/utils/request'
import type { ApiResponse } from '@/types/workflow'

const BASE_URL = '/api/workflow/statistics'

/**
 * 流程统计数据
 */
export interface ProcessStatistics {
  totalInstances: number
  runningInstances: number
  completedInstances: number
  suspendedInstances: number
  terminatedInstances: number
  totalTasks: number
  totalDefinitions: number
  activeDefinitions: number
  todayStarted: number
  todayCompleted: number
  weekStarted: number
  weekCompleted: number
  definitionStatistics?: DefinitionStatistics[]
}

/**
 * 流程定义统计
 */
export interface DefinitionStatistics {
  processDefinitionKey: string
  processDefinitionName: string
  version: number
  runningInstances: number
  completedInstances: number
  pendingTasks: number
  avgDurationInMillis: number | null
}

/**
 * 获取流程总体统计
 */
export const getProcessStatistics = async (): Promise<
  ApiResponse<ProcessStatistics>
> => {
  return request.get(BASE_URL)
}

/**
 * 按流程定义统计
 */
export const getStatisticsByDefinition = async (): Promise<
  ApiResponse<DefinitionStatistics[]>
> => {
  return request.get(`${BASE_URL}/by-definition`)
}
