import request from '@/utils/request'
import type {
  ProcessDefinition,
  ProcessDefinitionQueryParams,
  ApiResponse,
  PageResult,
} from '@/types/workflow'

const BASE_URL = '/api/workflow/definitions'

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
 */
export const deployProcessDefinition = async (data: {
  name: string
  file: File
}): Promise<ApiResponse<{ deploymentId: string }>> => {
  const formData = new FormData()
  formData.append('name', data.name)
  formData.append('file', data.file)

  return request.post(BASE_URL, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}

/**
 * 挂起流程定义
 */
export const suspendProcessDefinition = async (
  id: string
): Promise<ApiResponse> => {
  return request.put(`${BASE_URL}/${id}/suspend`)
}

/**
 * 激活流程定义
 */
export const activateProcessDefinition = async (
  id: string
): Promise<ApiResponse> => {
  return request.put(`${BASE_URL}/${id}/activate`)
}

/**
 * 删除部署
 */
export const deleteDeployment = async (
  deploymentId: string,
  cascade: boolean = false
): Promise<ApiResponse> => {
  return request.delete(`${BASE_URL}/deployment/${deploymentId}`, {
    params: { cascade },
  })
}

/**
 * 获取流程定义XML
 */
export const getProcessDefinitionXml = async (
  id: string
): Promise<ApiResponse<{ xml: string }>> => {
  return request.get(`${BASE_URL}/${id}/xml`)
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
