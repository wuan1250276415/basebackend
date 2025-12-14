import request from '@/utils/request'
import type {
  ApiResponse,
  PageResult,
  FormTemplate,
  FormTemplateQueryParams,
  FormTemplateCreateParams,
  FormTemplateUpdateParams
} from '@/types/workflow'

const BASE_URL = '/basebackend-scheduler/api/camunda/form-templates'

/**
 * 查询表单模板列表（分页）
 */
export const listFormTemplates = async (
  params?: FormTemplateQueryParams
): Promise<ApiResponse<PageResult<FormTemplate>>> => {
  return request.get(BASE_URL, { params })
}

/**
 * 根据ID查询表单模板
 */
export const getFormTemplateById = async (
  id: string
): Promise<ApiResponse<FormTemplate>> => {
  return request.get(`${BASE_URL}/${id}`)
}

/**
 * 创建表单模板
 */
export const createFormTemplate = async (
  data: FormTemplateCreateParams
): Promise<ApiResponse<FormTemplate>> => {
  return request.post(BASE_URL, data)
}

/**
 * 更新表单模板
 */
export const updateFormTemplate = async (
  id: string,
  data: FormTemplateUpdateParams
): Promise<ApiResponse<FormTemplate>> => {
  return request.put(`${BASE_URL}/${id}`, data)
}

/**
 * 删除表单模板
 */
export const deleteFormTemplate = async (id: string): Promise<ApiResponse> => {
  return request.delete(`${BASE_URL}/${id}`)
}
