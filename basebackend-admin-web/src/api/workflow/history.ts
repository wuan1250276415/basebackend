import request from '@/utils/request'
import type {
    ApiResponse,
    PageResult,
    HistoricProcessInstance,
    HistoricActivityInstance,
    HistoricProcessInstanceQuery,
    SimplePageQuery,
    UserOperationLog,
    HistoricProcessInstanceStatus,
    HistoricProcessInstanceDetail
} from '@/types/workflow'

/**
 * 基础路径
 */
const BASE_URL = '/api/camunda/historic/process-instances'

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
