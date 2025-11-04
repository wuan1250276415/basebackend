import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types/workflow'

const BASE_URL = '/api/workflow/form-templates'

/**
 * 表单模板
 */
export interface FormTemplate {
  id: number
  name: string
  description: string
  formKey: string
  processDefinitionKey: string
  schemaJson: string
  status: number
  version: number
  createTime: string
  updateTime: string
  createBy: number
  updateBy: number
}

/**
 * 表单模板查询参数
 */
export interface FormTemplateQueryParams {
  page?: number
  size?: number
  name?: string
  processDefinitionKey?: string
  status?: number
}

/**
 * 创建/更新表单模板参数
 */
export interface FormTemplateParams {
  name: string
  description?: string
  formKey: string
  processDefinitionKey?: string
  schemaJson: string
  status?: number
}

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
  id: number
): Promise<ApiResponse<FormTemplate>> => {
  return request.get(`${BASE_URL}/${id}`)
}

/**
 * 根据表单Key查询表单模板
 */
export const getFormTemplateByKey = async (
  formKey: string
): Promise<ApiResponse<FormTemplate>> => {
  return request.get(`${BASE_URL}/by-key/${formKey}`)
}

/**
 * 根据流程定义Key查询表单模板列表
 */
export const getFormTemplatesByProcessKey = async (
  processDefinitionKey: string
): Promise<ApiResponse<FormTemplate[]>> => {
  return request.get(`${BASE_URL}/by-process/${processDefinitionKey}`)
}

/**
 * 查询所有启用的表单模板
 */
export const getEnabledFormTemplates = async (): Promise<
  ApiResponse<FormTemplate[]>
> => {
  return request.get(`${BASE_URL}/enabled`)
}

/**
 * 创建表单模板
 */
export const createFormTemplate = async (
  data: FormTemplateParams
): Promise<ApiResponse<FormTemplate>> => {
  return request.post(BASE_URL, data)
}

/**
 * 更新表单模板
 */
export const updateFormTemplate = async (
  id: number,
  data: FormTemplateParams
): Promise<ApiResponse<FormTemplate>> => {
  return request.put(`${BASE_URL}/${id}`, data)
}

/**
 * 删除表单模板
 */
export const deleteFormTemplate = async (id: number): Promise<ApiResponse> => {
  return request.delete(`${BASE_URL}/${id}`)
}

/**
 * 更新表单模板状态
 */
export const updateFormTemplateStatus = async (
  id: number,
  status: number
): Promise<ApiResponse> => {
  return request.put(`${BASE_URL}/${id}/status`, null, {
    params: { status },
  })
}
