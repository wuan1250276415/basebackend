import request from '@/utils/request'
import type {
  ProcessInstance,
  StartProcessInstanceParams,
  ProcessInstanceQueryParams,
  ApiResponse,
  PageResult,
} from '@/types/workflow'

const BASE_URL = '/basebackend-scheduler/api/camunda/process-instances'

/**
 * 启动流程实例
 */
export const startProcessInstance = async (
  data: StartProcessInstanceParams
): Promise<ApiResponse<ProcessInstance>> => {
  return request.post(`${BASE_URL}/start`, data)
}

/**
 * 查询运行中的流程实例
 */
export const listRunningProcessInstances = async (
  params?: ProcessInstanceQueryParams
): Promise<ApiResponse<PageResult<ProcessInstance>>> => {
  return request.get(`${BASE_URL}/running`, { params })
}

/**
 * 根据流程定义Key查询运行中的流程实例
 */
export const listRunningProcessInstancesByKey = async (
  key: string
): Promise<ApiResponse<PageResult<ProcessInstance>>> => {
  return request.get(`${BASE_URL}/running/key/${key}`)
}

/**
 * 根据ID查询流程实例
 */
export const getProcessInstanceById = async (
  id: string
): Promise<ApiResponse<ProcessInstance>> => {
  return request.get(`${BASE_URL}/${id}`)
}

/**
 * 根据业务键查询流程实例
 */
export const getProcessInstanceByBusinessKey = async (
  businessKey: string
): Promise<ApiResponse<ProcessInstance>> => {
  return request.get(`${BASE_URL}/business-key/${businessKey}`)
}

/**
 * 挂起流程实例
 */
export const suspendProcessInstance = async (
  id: string
): Promise<ApiResponse> => {
  return request.put(`${BASE_URL}/${id}/suspend`)
}

/**
 * 激活流程实例
 */
export const activateProcessInstance = async (
  id: string
): Promise<ApiResponse> => {
  return request.put(`${BASE_URL}/${id}/activate`)
}

/**
 * 删除流程实例
 */
export const deleteProcessInstance = async (
  id: string,
  deleteReason?: string
): Promise<ApiResponse> => {
  return request.delete(`${BASE_URL}/${id}`, {
    params: { deleteReason },
  })
}

/**
 * 设置流程变量
 */
export const setProcessVariables = async (
  id: string,
  variables: Record<string, any>
): Promise<ApiResponse> => {
  return request.put(`${BASE_URL}/${id}/variables`, variables)
}

/**
 * 获取流程变量
 */
export const getProcessVariables = async (
  id: string
): Promise<ApiResponse<Record<string, any>>> => {
  return request.get(`${BASE_URL}/${id}/variables`)
}

/**
 * 查询所有流程实例（包括历史）
 */
export const listProcessInstances = async (
  params?: ProcessInstanceQueryParams
): Promise<ApiResponse<PageResult<ProcessInstance>>> => {
  return request.get(`${BASE_URL}`, { params })
}


/**
 * 获取流程实例变量（包括历史）
 */
export const getProcessInstanceVariables = async (
  id: string
): Promise<ApiResponse<Record<string, any>>> => {
  return request.get(`${BASE_URL}/${id}/variables`)
}
