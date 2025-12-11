/**
 * 工作流相关类型定义
 */

// ==================== 流程定义 ====================

export interface ProcessDefinition {
  id: string
  key: string
  name: string
  version: number
  deploymentId: string
  resourceName: string
  diagramResourceName: string | null
  suspended: boolean
  tenantId: string | null
  versionTag: string | null
  description: string | null
  deploymentTime: string
}

export interface ProcessDefinitionQueryParams {
  key?: string
  name?: string
  latest?: boolean
  suspended?: boolean
  page?: number
  size?: number
}

// ==================== 流程实例 ====================

export interface ProcessInstance {
  id: string
  businessKey: string
  processDefinitionId: string
  processDefinitionKey: string
  processDefinitionName: string
  suspended: boolean
  ended: boolean
  startTime: string
  endTime: string | null
  durationInMillis: number | null
  tenantId: string | null
  variables: Record<string, any>
  startUserId: string | null
}

export interface StartProcessInstanceParams {
  processDefinitionKey: string
  businessKey: string
  variables: Record<string, any>
}

export interface ProcessInstanceQueryParams {
  processDefinitionKey?: string
  businessKey?: string
  suspended?: boolean
  active?: boolean
  page?: number
  size?: number
}

// ==================== 任务 ====================

export interface Task {
  id: string
  name: string
  taskDefinitionKey: string
  processInstanceId: string
  processDefinitionId: string
  executionId: string
  assignee: string | null
  candidateUsers: string | null
  candidateGroups: string | null
  createTime: string
  dueDate: string | null
  followUpDate: string | null
  priority: number
  description: string | null
  tenantId: string | null
  variables: Record<string, any>
}

export interface TaskQueryParams {
  assignee?: string
  candidateUser?: string
  processInstanceId?: string
  processDefinitionKey?: string
  taskDefinitionKey?: string
  page?: number
  size?: number
}

export interface CompleteTaskParams {
  variables: Record<string, any>
}

export interface ClaimTaskParams {
  userId: string
}

export interface DelegateTaskParams {
  userId: string
}

// ==================== 表单模板 ====================

export interface FormTemplate {
  id: string
  name: string
  description: string | null
  formKey: string
  processDefinitionKey: string | null
  schema: FormSchema
  createTime: string
  updateTime: string
}

export interface FormSchema {
  type: 'object'
  properties: Record<string, FormField>
  required?: string[]
}

export interface FormField {
  type: string
  title: string
  description?: string
  default?: any
  enum?: any[]
  format?: string
  minimum?: number
  maximum?: number
  pattern?: string
  minLength?: number
  maxLength?: number
  'x-component'?: string
  'x-component-props'?: Record<string, any>
  'x-decorator'?: string
  'x-decorator-props'?: Record<string, any>
  'x-reactions'?: any
}

export interface FormTemplateQueryParams {
  name?: string
  processDefinitionKey?: string
  page?: number
  size?: number
}

// ==================== 流程历史 ====================

export interface HistoricProcessInstance {
  id: string
  businessKey: string
  processDefinitionId: string
  processDefinitionKey: string
  processDefinitionName: string
  startTime: string
  endTime: string | null
  durationInMillis: number | null
  startUserId: string | null
  deleteReason: string | null
  tenantId: string | null
}

export interface HistoricTaskInstance {
  id: string
  processInstanceId: string
  taskDefinitionKey: string
  name: string
  assignee: string | null
  startTime: string
  endTime: string | null
  durationInMillis: number | null
  deleteReason: string | null
  priority: number
  dueDate: string | null
}

export interface ApprovalHistory {
  taskId: string
  taskName: string
  assignee: string
  assigneeName: string
  startTime: string
  endTime: string | null
  durationInMillis: number | null
  approved: boolean | null
  comment: string | null
}

export interface HistoricQueryParams {
  processDefinitionKey?: string
  businessKey?: string
  startedAfter?: string
  startedBefore?: string
  finishedAfter?: string
  finishedBefore?: string
  page?: number
  size?: number
}

// ==================== 流程模板 ====================

export enum ProcessTemplateType {
  LEAVE = 'leave',
  EXPENSE = 'expense',
  PURCHASE = 'purchase',
}

export interface ProcessTemplate {
  type: ProcessTemplateType
  name: string
  description: string
  icon: string
  processDefinitionKey: string
  formKey: string
  schema: FormSchema
}

// ==================== 流程统计 ====================

export interface ProcessStatistics {
  totalProcesses: number
  runningProcesses: number
  completedProcesses: number
  failedProcesses: number
  avgDurationInMillis: number
  processCountByDay: Array<{
    date: string
    count: number
  }>
  processCountByType: Array<{
    type: string
    count: number
  }>
}

// ==================== API 响应 ====================

export interface ApiResponse<T = any> {
  success: boolean
  data: T
  message?: string
  total?: number
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
}
