import request from '@/utils/request'
import type { ApiResponse } from '@/types/workflow'

const BASE_URL = '/api/camunda/historic/process-instances'

/**
 * 活动历史详情
 */
export interface ActivityHistory {
    id: string
    activityId: string
    activityName: string
    activityType: string
    assignee?: string
    taskId?: string
    startTime: string
    endTime?: string
    durationInMillis?: number
    ended: boolean
    canceled: boolean
}

/**
 * 流程跟踪信息
 */
export interface ProcessTracking {
    processInstanceId: string
    processDefinitionId: string
    processDefinitionKey: string
    processInstanceName?: string
    businessKey?: string
    ended: boolean
    suspended: boolean
    startTime: string
    endTime?: string
    bpmnXml: string
    activeActivityIds: string[]
    completedActivityIds: string[]
    pendingActivityIds?: string[]
    failedActivityIds?: string[]
    activityHistories: ActivityHistory[]
}

/**
 * 获取流程跟踪信息
 */
export const getProcessTracking = async (
    instanceId: string
): Promise<ApiResponse<ProcessTracking>> => {
    return request.get(`${BASE_URL}/${instanceId}/tracking`)
}
