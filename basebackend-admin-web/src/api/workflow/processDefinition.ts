import request from '@/utils/request'
import type {
  ProcessDefinition,
  ProcessDefinitionQueryParams,
  ApiResponse,
  PageResult,
} from '@/types/workflow'

const BASE_URL = '/basebackend-scheduler/api/camunda/process-definitions'

/**
 * 查询所有流程定义
 */
export const listProcessDefinitions = async (
  params?: ProcessDefinitionQueryParams
): Promise<ApiResponse<PageResult<ProcessDefinition>>> => {
  return request.get(BASE_URL, { params })
}

/**
 * 根据Key查询流程定义
 */
export const getProcessDefinitionByKey = async (
  key: string
): Promise<ApiResponse<ProcessDefinition>> => {
  return request.get(`${BASE_URL}/key/${key}`)
}

/**
 * 根据ID查询流程定义
 */
export const getProcessDefinitionById = async (
  id: string
): Promise<ApiResponse<ProcessDefinition>> => {
  return request.get(`${BASE_URL}/${id}`)
}

/**
 * 部署流程定义
 * POST /api/camunda/process-definitions/deployments
 */
export const deployProcessDefinition = async (data: {
  file: File
  deploymentName?: string
  name?: string
  tenantId?: string
  category?: string
  enableDuplicateFilter?: boolean
  deployChangedOnly?: boolean
}): Promise<ApiResponse<string>> => {
  const formData = new FormData()
  formData.append('file', data.file)
  if (data.deploymentName) formData.append('deploymentName', data.deploymentName)
  if (data.name) formData.append('name', data.name)
  if (data.tenantId) formData.append('tenantId', data.tenantId)
  if (data.category) formData.append('category', data.category)
  if (data.enableDuplicateFilter !== undefined) formData.append('enableDuplicateFilter', String(data.enableDuplicateFilter))
  if (data.deployChangedOnly !== undefined) formData.append('deployChangedOnly', String(data.deployChangedOnly))

  return request.post(`${BASE_URL}/deployments`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}

/**
 * 挂起流程定义
 */
export const suspendProcessDefinition = async (
  id: string,
  data: { includeProcessInstances?: boolean; executeAt?: string } = { includeProcessInstances: true }
): Promise<ApiResponse> => {
  return request.post(`${BASE_URL}/${id}/suspend`, data)
}

/**
 * 激活流程定义
 */
export const activateProcessDefinition = async (
  id: string,
  data: { includeProcessInstances?: boolean; executeAt?: string } = { includeProcessInstances: true }
): Promise<ApiResponse> => {
  return request.post(`${BASE_URL}/${id}/activate`, data)
}

/**
 * 删除部署
 */
export const deleteDeployment = async (
  deploymentId: string,
  cascade: boolean = false
): Promise<ApiResponse> => {
  return request.delete(`${BASE_URL}/deployments/${deploymentId}`, {
    params: { cascade },
  })
}

/**
 * 获取流程定义XML
 */
export const getProcessDefinitionXml = async (
  id: string
): Promise<ApiResponse<{ xml: string }>> => {
  return request.get(`${BASE_URL}/${id}/xml-content`)
}

/**
 * 获取流程图（PNG）
 */
export const getProcessDefinitionDiagram = async (id: string): Promise<Blob> => {
  const response = await request.get(`${BASE_URL}/${id}/diagram`, {
    responseType: 'blob',
  })
  return response
}
