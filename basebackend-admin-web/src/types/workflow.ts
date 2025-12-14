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
  resourceName?: string
  diagramResourceName?: string | null
  suspended: boolean
  tenantId: string | null
  versionTag?: string | null
  description: string | null
  deploymentTime?: string
}

export interface ProcessDefinitionQueryParams {
  key?: string
  name?: string
  latestVersion?: boolean
  suspended?: boolean
  current?: number
  size?: number
  tenantId?: string
}

// ==================== 流程实例 ====================

export interface ProcessInstance {
  id: string
  businessKey: string
  processDefinitionId: string
  processDefinitionKey?: string // Doc doesn't show this in response but it's common.
  processDefinitionName?: string
  suspended?: boolean
  ended?: boolean // Doc doesn't explicitly show.
  startTime?: string
  endTime?: string | null
  durationInMillis?: number | null
  tenantId: string | null
  variables?: Record<string, any>
  startUserId?: string | null
}

export interface StartProcessInstanceParams {
  processDefinitionId?: string
  processDefinitionKey?: string
  businessKey?: string
  variables?: Record<string, any>
  tenantId?: string
}

export interface ProcessInstanceQueryParams {
  processDefinitionKey?: string
  businessKey?: string
  suspended?: boolean
  active?: boolean // Custom helper if needed
  current?: number
  size?: number
  withVariables?: boolean
  tenantId?: string
}

// ==================== 任务 ====================

export interface Task {
  id: string
  name: string
  taskDefinitionKey?: string
  processInstanceId: string
  processDefinitionId: string
  executionId?: string
  assignee: string | null
  owner?: string
  candidateUsers?: string | null
  candidateGroups?: string | null
  createTime: string
  dueDate: string | null
  followUpDate: string | null
  priority: number
  description: string | null
  tenantId: string | null
  variables?: Record<string, any>
}

export interface TaskQueryParams {
  assignee?: string
  candidateUser?: string
  candidateGroup?: string
  processInstanceId?: string
  processDefinitionKey?: string
  name?: string
  current?: number
  size?: number
  tenantId?: string
}

export interface CompleteTaskParams {
  variables?: Record<string, any>
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
  type: string
  content: string // JSON content
  tenantId: string | null
  version: number
  status?: number // 1 enabled, 0 disabled
  createTime?: string
  updateTime?: string
}

export interface FormTemplateQueryParams {
  name?: string
  keyword?: string
  type?: string
  status?: number
  current?: number
  size?: number
  tenantId?: string
}

export interface FormTemplateCreateParams {
  name: string
  description?: string
  type: string
  content: string
  tenantId?: string
}

export interface FormTemplateUpdateParams {
  name?: string
  description?: string
  content?: string
}

// ==================== 流程历史 ====================

export interface HistoricProcessInstance {
  id: string
  businessKey?: string
  processDefinitionId: string
  processDefinitionKey: string
  processDefinitionName: string
  processDefinitionVersion: number
  startTime: string
  endTime: string | null
  durationInMillis: number | null
  startUserId: string | null
  deleteReason: string | null
  tenantId: string | null
  state: string
}

export interface HistoricProcessInstanceQuery {
  processDefinitionKey?: string
  processDefinitionId?: string
  businessKey?: string
  startedBy?: string
  finished?: boolean
  tenantId?: string
  current?: number
  size?: number
}

// ==================== 流程统计 ====================

export interface ProcessDefinitionsStatistics {
  totalDeployments: number
  totalDefinitions: number
  activeDefinitions: number
  suspendedDefinitions: number
}

export interface InstanceStatistics {
  runningInstances: number
  completedInstances: number
  terminatedInstances: number
}

export interface TaskStatistics {
  pendingTasks: number
  completedTasks: number
  overdueTasks: number
}

export interface WorkflowOverview {
  processDefinitions: ProcessDefinitionsStatistics
  instances: InstanceStatistics
  tasks: TaskStatistics
}

// ==================== API 响应 ====================

export interface ApiResponse<T = any> {
  code: number // Camunda wrapper usually returns code 200
  message: string
  data: T
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
}

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

export interface HelperPageQuery {
  current?: number
  size?: number
}

// Alias for SimplePageQuery if used
export type SimplePageQuery = HelperPageQuery

export interface HistoricProcessInstanceStatus {
  instanceId: string
  status: 'COMPLETED' | 'EXTERNALLY_TERMINATED' | 'INTERNALLY_TERMINATED' | 'ACTIVE' | 'SUSPENDED'
  startTime: string
  endTime?: string
  durationInMillis?: number
}

export interface HistoricProcessInstanceDetail extends HistoricProcessInstance {
  variables: Record<string, any>
  activities: HistoricActivityInstance[]
}
