import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types/workflow'

const BASE_URL = '/api/camunda/ops/jobs'

/**
 * 作业信息
 */
export interface Job {
    id: string
    processInstanceId: string
    processDefinitionId: string
    processDefinitionKey: string
    executionId: string
    jobType?: string
    jobHandlerType: string
    duedate: string
    createTime: string
    retries: number
    exceptionMessage?: string
    priority: number
    tenantId?: string
    suspended: boolean
    deploymentId?: string
    activityId?: string
    failed: boolean
}

/**
 * 作业详情
 */
export interface JobDetail extends Job {
    jobHandlerConfiguration?: string
    exceptionStacktrace?: string
    activityName?: string
    executable: boolean
    failedActivityId?: string
}

/**
 * 作业查询参数
 */
export interface JobQueryParams {
    current?: number
    size?: number
    processInstanceId?: string
    processDefinitionId?: string
    processDefinitionKey?: string
    activityId?: string
    executionId?: string
    tenantId?: string
    failedOnly?: boolean
    suspendedOnly?: boolean
    executableOnly?: boolean
    timersOnly?: boolean
    messagesOnly?: boolean
    withException?: boolean
    duedateBefore?: string
    duedateAfter?: string
    sortBy?: string
    sortOrder?: 'asc' | 'desc'
}

/**
 * 作业重试请求
 */
export interface JobRetryRequest {
    retries?: number
    duedate?: string
}

/**
 * 作业统计信息
 */
export interface JobStatistics {
    failedJobCount: number
    executableJobCount: number
    failedJobsByProcessDefinition: Record<string, number>
}

// ========== 查询接口 ==========

/**
 * 分页查询作业
 */
export const listJobs = async (
    params?: JobQueryParams
): Promise<ApiResponse<PageResult<Job>>> => {
    return request.get(BASE_URL, { params })
}

/**
 * 获取作业详情
 */
export const getJobById = async (
    jobId: string
): Promise<ApiResponse<JobDetail>> => {
    return request.get(`${BASE_URL}/${jobId}`)
}

/**
 * 获取作业异常堆栈
 */
export const getJobExceptionStacktrace = async (
    jobId: string
): Promise<ApiResponse<string>> => {
    return request.get(`${BASE_URL}/${jobId}/exception-stacktrace`)
}

/**
 * 查询失败作业列表
 */
export const listFailedJobs = async (
    maxResults: number = 100
): Promise<ApiResponse<Job[]>> => {
    return request.get(`${BASE_URL}/failed`, { params: { maxResults } })
}

// ========== 操作接口 ==========

/**
 * 重试作业
 */
export const retryJob = async (
    jobId: string,
    data?: JobRetryRequest
): Promise<ApiResponse<string>> => {
    return request.post(`${BASE_URL}/${jobId}/retry`, data || {})
}

/**
 * 立即执行作业
 */
export const executeJob = async (
    jobId: string
): Promise<ApiResponse<string>> => {
    return request.post(`${BASE_URL}/${jobId}/execute`)
}

/**
 * 删除作业
 */
export const deleteJob = async (
    jobId: string
): Promise<ApiResponse<string>> => {
    return request.delete(`${BASE_URL}/${jobId}`)
}

/**
 * 修改作业截止时间
 */
export const setJobDuedate = async (
    jobId: string,
    duedate: string
): Promise<ApiResponse<string>> => {
    return request.put(`${BASE_URL}/${jobId}/duedate`, null, {
        params: { duedate }
    })
}

/**
 * 修改作业优先级
 */
export const setJobPriority = async (
    jobId: string,
    priority: number
): Promise<ApiResponse<string>> => {
    return request.put(`${BASE_URL}/${jobId}/priority`, null, {
        params: { priority }
    })
}

/**
 * 修改作业重试次数
 */
export const setJobRetries = async (
    jobId: string,
    retries: number
): Promise<ApiResponse<string>> => {
    return request.put(`${BASE_URL}/${jobId}/retries`, null, {
        params: { retries }
    })
}

/**
 * 挂起作业
 */
export const suspendJob = async (
    jobId: string
): Promise<ApiResponse<string>> => {
    return request.post(`${BASE_URL}/${jobId}/suspend`)
}

/**
 * 激活作业
 */
export const activateJob = async (
    jobId: string
): Promise<ApiResponse<string>> => {
    return request.post(`${BASE_URL}/${jobId}/activate`)
}

// ========== 批量操作接口 ==========

/**
 * 批量重试作业
 */
export const batchRetryJobs = async (
    jobIds: string[],
    retries?: number
): Promise<ApiResponse<{ total: number; success: number; failed: number }>> => {
    return request.post(`${BASE_URL}/batch-retry`, jobIds, {
        params: { retries: retries || 3 }
    })
}

/**
 * 批量删除作业
 */
export const batchDeleteJobs = async (
    jobIds: string[]
): Promise<ApiResponse<{ total: number; success: number; failed: number }>> => {
    return request.post(`${BASE_URL}/batch-delete`, jobIds)
}

// ========== 统计接口 ==========

/**
 * 获取作业统计信息
 */
export const getJobStatistics = async (): Promise<ApiResponse<JobStatistics>> => {
    return request.get(`${BASE_URL}/statistics`)
}

/**
 * 统计失败作业数量
 */
export const countFailedJobs = async (): Promise<ApiResponse<number>> => {
    return request.get(`${BASE_URL}/statistics/failed-count`)
}

/**
 * 按流程定义统计失败作业
 */
export const countFailedJobsByProcessDefinition = async (): Promise<
    ApiResponse<Record<string, number>>
> => {
    return request.get(`${BASE_URL}/statistics/failed-by-definition`)
}
