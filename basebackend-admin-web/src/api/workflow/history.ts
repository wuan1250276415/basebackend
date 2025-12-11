import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types/workflow'

/**
 * 基础路径
 */
const BASE_URL = '/api/workflow/history/process-instances'

/**
 * 历史流程实例状态
 */
export interface HistoricProcessInstanceStatus {
    instanceId: string
    status: 'COMPLETED' | 'EXTERNALLY_TERMINATED' | 'INTERNALLY_TERMINATED' | 'ACTIVE' | 'SUSPENDED'
    startTime: string
    endTime?: string
    durationInMillis?: number
}

/**
 * 历史流程实例
 */
export interface HistoricProcessInstance {
    id: string
    businessKey?: string
    processDefinitionKey: string
    processDefinitionId: string
    processDefinitionName: string
    processDefinitionVersion: number
    startTime: string
    endTime?: string
    durationInMillis?: number
    startUserId?: string
    startActivityId?: string
    deleteReason?: string
    rootProcessInstanceId?: string
    superProcessInstanceId?: string
    tenantId?: string
    state: string
}

/**
 * 历史流程实例详情
 */
export interface HistoricProcessInstanceDetail extends HistoricProcessInstance {
    variables: Record<string, any>
    activities: HistoricActivityInstance[]
}

/**
 * 历史活动实例
 */
export interface HistoricActivityInstance {
    id: string
    activityId: string
    activityName: string
    activityType: string
    startTime: string
    endTime?: string
    durationInMillis?: number
    taskId?: string
    assignee?: string
    completeScope: boolean
    canceled: boolean
}

/**
 * 用户操作日志
 */
export interface UserOperationLog {
    id: string
    deploymentId?: string
    processDefinitionId?: string
    processDefinitionKey?: string
    processInstanceId?: string
    executionId?: string
    caseDefinitionId?: string
    caseInstanceId?: string
    caseExecutionId?: string
    taskId?: string
    jobId?: string
    jobDefinitionId?: string
    batchId?: string
    userId?: string
    timestamp: string
    operationId: string
    operationType: string
    entityType: string
    property?: string
    orgValue?: string
    newValue?: string
}

/**
 * 历史流程实例查询参数
 */
export interface HistoricProcessInstanceQuery {
    page?: number
    size?: number
    processDefinitionKey?: string
    processDefinitionId?: string
    businessKey?: string
    startedBy?: string
    finished?: boolean
    unfinished?: boolean
    withIncidents?: boolean
    tenantId?: string
}

/**
 * 简单分页查询参数
 */
export interface SimplePageQuery {
    current?: number
    size?: number
}

/**
 * 分页查询历史流程实例
 */
export const listHistoricProcessInstances = async (
    params?: HistoricProcessInstanceQuery
): Promise<ApiResponse<PageResult<HistoricProcessInstance>>> => {
    return request.get(BASE_URL, { params })
}

/**
 * 获取历史流程实例详情
 */
export const getHistoricProcessInstanceDetail = async (
    id: string
): Promise<ApiResponse<HistoricProcessInstanceDetail>> => {
    return request.get(`${BASE_URL}/${id}`)
}

/**
 * 查询历史流程实例状态
 */
export const getHistoricProcessInstanceStatus = async (
    id: string
): Promise<ApiResponse<HistoricProcessInstanceStatus>> => {
    return request.get(`${BASE_URL}/${id}/status`)
}

/**
 * 查询历史流程实例活动历史
 */
export const listHistoricActivities = async (
    id: string,
    params?: SimplePageQuery
): Promise<ApiResponse<PageResult<HistoricActivityInstance>>> => {
    return request.get(`${BASE_URL}/${id}/activities`, { params })
}

/**
 * 查询历史流程实例审计日志
 */
export const listHistoricAuditLogs = async (
    id: string,
    params?: SimplePageQuery
): Promise<ApiResponse<PageResult<UserOperationLog>>> => {
    return request.get(`${BASE_URL}/${id}/audit-logs`, { params })
}
