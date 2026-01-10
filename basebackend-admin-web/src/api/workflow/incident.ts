import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types/workflow'

const BASE_URL = '/api/camunda/ops/incidents'

/**
 * 异常事件信息
 */
export interface Incident {
    id: string
    incidentType: string
    incidentMessage: string
    incidentTimestamp: string
    processDefinitionId: string
    processInstanceId: string
    executionId: string
    activityId: string
    failedActivityId?: string
    jobDefinitionId?: string
    configuration?: string
    rootCauseIncidentId?: string
    causeIncidentId?: string
    tenantId?: string
    resolved?: boolean
    resolvedTimestamp?: string
    annotation?: string
}

/**
 * 异常事件查询参数
 */
export interface IncidentQueryParams {
    current?: number
    size?: number
    incidentType?: string
    processInstanceId?: string
    processDefinitionId?: string
    processDefinitionKey?: string
    activityId?: string
    executionId?: string
    jobDefinitionId?: string
    tenantId?: string
    incidentTimestampBefore?: string
    incidentTimestampAfter?: string
    sortBy?: string
    sortOrder?: 'asc' | 'desc'
}

/**
 * 异常事件统计信息
 */
export interface IncidentStatistics {
    totalCount: number
    byType: Record<string, number>
    byProcessDefinition: Record<string, number>
}

// ========== 查询接口 ==========

/**
 * 分页查询异常事件
 */
export const listIncidents = async (
    params?: IncidentQueryParams
): Promise<ApiResponse<PageResult<Incident>>> => {
    return request.get(BASE_URL, { params })
}

/**
 * 获取异常事件详情
 */
export const getIncidentById = async (
    incidentId: string
): Promise<ApiResponse<Incident>> => {
    return request.get(`${BASE_URL}/${incidentId}`)
}

/**
 * 获取流程实例的异常事件列表
 */
export const listIncidentsByProcessInstance = async (
    processInstanceId: string
): Promise<ApiResponse<Incident[]>> => {
    return request.get(`${BASE_URL}/process-instance/${processInstanceId}`)
}

/**
 * 获取最近的异常事件列表
 */
export const listRecentIncidents = async (
    maxResults: number = 20
): Promise<ApiResponse<Incident[]>> => {
    return request.get(`${BASE_URL}/recent`, { params: { maxResults } })
}

// ========== 操作接口 ==========

/**
 * 解决异常事件
 */
export const resolveIncident = async (
    incidentId: string
): Promise<ApiResponse<string>> => {
    return request.post(`${BASE_URL}/${incidentId}/resolve`)
}

/**
 * 设置异常事件注解
 */
export const setIncidentAnnotation = async (
    incidentId: string,
    annotation: string
): Promise<ApiResponse<string>> => {
    return request.put(`${BASE_URL}/${incidentId}/annotation`, null, {
        params: { annotation }
    })
}

/**
 * 清除异常事件注解
 */
export const clearIncidentAnnotation = async (
    incidentId: string
): Promise<ApiResponse<string>> => {
    return request.delete(`${BASE_URL}/${incidentId}/annotation`)
}

// ========== 统计接口 ==========

/**
 * 获取异常事件统计信息
 */
export const getIncidentStatistics = async (): Promise<ApiResponse<IncidentStatistics>> => {
    return request.get(`${BASE_URL}/statistics`)
}

/**
 * 统计异常事件总数
 */
export const countIncidents = async (): Promise<ApiResponse<number>> => {
    return request.get(`${BASE_URL}/statistics/count`)
}

/**
 * 按类型统计异常事件
 */
export const countIncidentsByType = async (): Promise<ApiResponse<Record<string, number>>> => {
    return request.get(`${BASE_URL}/statistics/by-type`)
}

/**
 * 按流程定义统计异常事件
 */
export const countIncidentsByProcessDefinition = async (): Promise<
    ApiResponse<Record<string, number>>
> => {
    return request.get(`${BASE_URL}/statistics/by-definition`)
}
