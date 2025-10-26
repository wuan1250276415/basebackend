/**
 * 工作流常量配置
 */

/**
 * 流程状态
 */
export const PROCESS_STATUS = {
  ACTIVE: 'active',
  SUSPENDED: 'suspended',
  COMPLETED: 'completed',
  TERMINATED: 'terminated',
} as const

/**
 * 任务状态
 */
export const TASK_STATUS = {
  PENDING: 'pending',
  IN_PROGRESS: 'in_progress',
  COMPLETED: 'completed',
  OVERDUE: 'overdue',
} as const

/**
 * 审批决定
 */
export const APPROVAL_DECISION = {
  APPROVE: 'approve',
  REJECT: 'reject',
  RETURN: 'return',
} as const

/**
 * 审批决定文本
 */
export const APPROVAL_DECISION_TEXT = {
  [APPROVAL_DECISION.APPROVE]: '通过',
  [APPROVAL_DECISION.REJECT]: '驳回',
  [APPROVAL_DECISION.RETURN]: '退回',
} as const

/**
 * 优先级定义
 */
export const PRIORITY = {
  LOW: 30,
  NORMAL: 50,
  HIGH: 70,
  URGENT: 90,
} as const

/**
 * 优先级文本
 */
export const PRIORITY_TEXT = {
  [PRIORITY.LOW]: '低',
  [PRIORITY.NORMAL]: '普通',
  [PRIORITY.HIGH]: '重要',
  [PRIORITY.URGENT]: '紧急',
} as const

/**
 * 优先级颜色
 */
export const PRIORITY_COLOR = {
  [PRIORITY.LOW]: '#d9d9d9',
  [PRIORITY.NORMAL]: '#1890ff',
  [PRIORITY.HIGH]: '#faad14',
  [PRIORITY.URGENT]: '#f5222d',
} as const

/**
 * 流程类型
 */
export const PROCESS_TYPE = {
  LEAVE: 'leave',
  EXPENSE: 'expense',
  PURCHASE: 'purchase',
  OTHER: 'other',
} as const

/**
 * 流程类型文本
 */
export const PROCESS_TYPE_TEXT = {
  [PROCESS_TYPE.LEAVE]: '请假审批',
  [PROCESS_TYPE.EXPENSE]: '报销审批',
  [PROCESS_TYPE.PURCHASE]: '采购审批',
  [PROCESS_TYPE.OTHER]: '其他流程',
} as const

/**
 * 流程类型颜色
 */
export const PROCESS_TYPE_COLOR = {
  [PROCESS_TYPE.LEAVE]: '#1890ff',
  [PROCESS_TYPE.EXPENSE]: '#52c41a',
  [PROCESS_TYPE.PURCHASE]: '#faad14',
  [PROCESS_TYPE.OTHER]: '#d9d9d9',
} as const

/**
 * 请假类型
 */
export const LEAVE_TYPE = {
  ANNUAL: 'annual',
  SICK: 'sick',
  PERSONAL: 'personal',
  MARRIAGE: 'marriage',
  MATERNITY: 'maternity',
  OTHER: 'other',
} as const

/**
 * 请假类型文本
 */
export const LEAVE_TYPE_TEXT = {
  [LEAVE_TYPE.ANNUAL]: '年假',
  [LEAVE_TYPE.SICK]: '病假',
  [LEAVE_TYPE.PERSONAL]: '事假',
  [LEAVE_TYPE.MARRIAGE]: '婚假',
  [LEAVE_TYPE.MATERNITY]: '产假',
  [LEAVE_TYPE.OTHER]: '其他',
} as const

/**
 * 报销类型
 */
export const EXPENSE_TYPE = {
  TRANSPORTATION: 'transportation',
  ACCOMMODATION: 'accommodation',
  MEAL: 'meal',
  COMMUNICATION: 'communication',
  ENTERTAINMENT: 'entertainment',
  OFFICE: 'office',
  OTHER: 'other',
} as const

/**
 * 报销类型文本
 */
export const EXPENSE_TYPE_TEXT = {
  [EXPENSE_TYPE.TRANSPORTATION]: '交通费',
  [EXPENSE_TYPE.ACCOMMODATION]: '住宿费',
  [EXPENSE_TYPE.MEAL]: '餐饮费',
  [EXPENSE_TYPE.COMMUNICATION]: '通讯费',
  [EXPENSE_TYPE.ENTERTAINMENT]: '招待费',
  [EXPENSE_TYPE.OFFICE]: '办公费',
  [EXPENSE_TYPE.OTHER]: '其他',
} as const

/**
 * 采购类型
 */
export const PURCHASE_TYPE = {
  EQUIPMENT: 'equipment',
  OFFICE: 'office',
  SOFTWARE: 'software',
  SERVICE: 'service',
  OTHER: 'other',
} as const

/**
 * 采购类型文本
 */
export const PURCHASE_TYPE_TEXT = {
  [PURCHASE_TYPE.EQUIPMENT]: '设备采购',
  [PURCHASE_TYPE.OFFICE]: '办公用品',
  [PURCHASE_TYPE.SOFTWARE]: '软件采购',
  [PURCHASE_TYPE.SERVICE]: '服务采购',
  [PURCHASE_TYPE.OTHER]: '其他',
} as const

/**
 * 审批人类型
 */
export const APPROVER_TYPE = {
  MANAGER: 'manager',
  DIRECTOR: 'director',
  HR: 'hr',
  FINANCE: 'finance',
} as const

/**
 * 审批人类型文本
 */
export const APPROVER_TYPE_TEXT = {
  [APPROVER_TYPE.MANAGER]: '直属经理',
  [APPROVER_TYPE.DIRECTOR]: '部门总监',
  [APPROVER_TYPE.HR]: '人力资源',
  [APPROVER_TYPE.FINANCE]: '财务主管',
} as const

/**
 * 部门列表
 */
export const DEPARTMENTS = [
  '研发部',
  '市场部',
  '销售部',
  '财务部',
  '人力资源部',
  '行政部',
] as const

/**
 * 单位列表
 */
export const UNITS = ['个', '台', '套', '件', '箱', '批'] as const

/**
 * 日期格式
 */
export const DATE_FORMAT = {
  DATE: 'YYYY-MM-DD',
  TIME: 'HH:mm:ss',
  DATETIME: 'YYYY-MM-DD HH:mm:ss',
  DATETIME_SHORT: 'MM-DD HH:mm',
} as const

/**
 * 分页配置
 */
export const PAGINATION = {
  DEFAULT_PAGE_SIZE: 10,
  PAGE_SIZE_OPTIONS: [10, 20, 50, 100],
  SHOW_SIZE_CHANGER: true,
  SHOW_QUICK_JUMPER: true,
} as const

/**
 * 文件上传配置
 */
export const UPLOAD_CONFIG = {
  MAX_FILE_SIZE: 10 * 1024 * 1024, // 10MB
  MAX_FILE_COUNT: 10,
  ACCEPT_IMAGE: 'image/*',
  ACCEPT_DOCUMENT: '.pdf,.doc,.docx,.xls,.xlsx',
  ACCEPT_BPMN: '.bpmn,.xml',
  ACCEPT_ALL: '*',
} as const

/**
 * API 端点
 */
export const API_ENDPOINTS = {
  PROCESS_DEFINITIONS: '/api/workflow/definitions',
  PROCESS_INSTANCES: '/api/workflow/instances',
  TASKS: '/api/workflow/tasks',
} as const

/**
 * 权限标识
 */
export const PERMISSIONS = {
  // 任务权限
  TASK_LIST: 'workflow:task:list',
  TASK_VIEW: 'workflow:task:view',
  TASK_CLAIM: 'workflow:task:claim',
  TASK_COMPLETE: 'workflow:task:complete',

  // 流程实例权限
  INSTANCE_MY_LIST: 'workflow:instance:mylist',
  INSTANCE_LIST: 'workflow:instance:list',
  INSTANCE_VIEW: 'workflow:instance:view',
  INSTANCE_SUSPEND: 'workflow:instance:suspend',
  INSTANCE_ACTIVATE: 'workflow:instance:activate',
  INSTANCE_DELETE: 'workflow:instance:delete',

  // 流程模板权限
  TEMPLATE_LIST: 'workflow:template:list',
  TEMPLATE_START: 'workflow:template:start',

  // 流程定义权限
  DEFINITION_LIST: 'workflow:definition:list',
  DEFINITION_DEPLOY: 'workflow:definition:deploy',
  DEFINITION_DELETE: 'workflow:definition:delete',
  DEFINITION_SUSPEND: 'workflow:definition:suspend',
  DEFINITION_ACTIVATE: 'workflow:definition:activate',

  // 流程历史权限
  HISTORY_LIST: 'workflow:history:list',
  HISTORY_VIEW: 'workflow:history:view',
} as const

/**
 * 路由路径
 */
export const ROUTES = {
  TODO: '/workflow/todo',
  TODO_DETAIL: '/workflow/todo/:taskId',
  INITIATED: '/workflow/initiated',
  TEMPLATE: '/workflow/template',
  TEMPLATE_LEAVE: '/workflow/template/leave',
  TEMPLATE_EXPENSE: '/workflow/template/expense',
  TEMPLATE_PURCHASE: '/workflow/template/purchase',
  INSTANCE: '/workflow/instance',
  INSTANCE_DETAIL: '/workflow/instance/:instanceId',
  DEFINITION: '/workflow/definition',
  HISTORY: '/workflow/history',
} as const
