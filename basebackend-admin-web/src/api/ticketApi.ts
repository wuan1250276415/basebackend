import request from '@/api/request';

// ==================== Types ====================

export interface TicketCreateDTO {
  title: string;
  description?: string;
  categoryId: number;
  priority?: number;
  source?: string;
  assigneeId?: number;
  assigneeName?: string;
  reporterId?: number;
  reporterName?: string;
  deptId?: number;
  tags?: string;
  attachmentIds?: number[];
}

export interface TicketUpdateDTO {
  title?: string;
  description?: string;
  categoryId?: number;
  priority?: number;
  tags?: string;
}

export interface TicketQueryDTO {
  status?: string;
  priority?: number;
  categoryId?: number;
  assigneeId?: number;
  reporterId?: number;
  deptId?: number;
  keyword?: string;
  startDate?: string;
  endDate?: string;
}

export interface TicketListItem {
  id: number;
  ticketNo: string;
  title: string;
  status: string;
  priority: number;
  categoryId: number;
  categoryName: string;
  assigneeId: number;
  assigneeName: string;
  reporterId: number;
  reporterName: string;
  deptId: number;
  source: string;
  tags: string;
  commentCount: number;
  attachmentCount: number;
  slaBreached: number;
  slaDeadline: string;
  createTime: string;
  updateTime: string;
}

export interface TicketDetail extends TicketListItem {
  description: string;
  processInstanceId: string;
  resolvedAt: string;
  closedAt: string;
  comments: CommentItem[];
  attachments: AttachmentItem[];
  statusLogs: StatusLogItem[];
  approvals: ApprovalItem[];
  ccList: CcItem[];
}

export interface CommentItem {
  id: number;
  content: string;
  type: string;
  isInternal: number;
  parentId: number;
  creatorName: string;
  createTime: string;
}

export interface AttachmentItem {
  id: number;
  fileId: number;
  fileName: string;
  fileSize: number;
  fileType: string;
  fileUrl: string;
  createTime: string;
}

export interface StatusLogItem {
  id: number;
  fromStatus: string;
  toStatus: string;
  operatorName: string;
  remark: string;
  createTime: string;
}

export interface ApprovalItem {
  id: number;
  taskName: string;
  approverName: string;
  action: string;
  opinion: string;
  delegateToName: string;
  createTime: string;
}

export interface CcItem {
  id: number;
  userName: string;
  isRead: number;
  readTime: string;
  createTime: string;
}

export interface ActiveTaskItem {
  taskId: string;
  taskName: string;
  assignee: string;
  createTime: string;
  processInstanceId: string;
}

export interface TicketOverview {
  totalCount: number;
  openCount: number;
  inProgressCount: number;
  pendingApprovalCount: number;
  resolvedCount: number;
  closedCount: number;
  rejectedCount: number;
  slaBreachedCount: number;
}

export interface TicketCategoryTree {
  id: number;
  name: string;
  parentId: number;
  icon: string;
  sortOrder: number;
  description: string;
  slaHours: number;
  status: number;
  children: TicketCategoryTree[];
}

export interface PageResult<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

// ==================== Phase 2 Types ====================

export interface TrendPoint {
  date: string;
  openCount: number;
  resolvedCount: number;
  closedCount: number;
}

export interface ResolutionTimeStats {
  avgHours: number;
  medianHours: number;
  p90Hours: number;
}

export interface SlaCompliance {
  totalCount: number;
  breachedCount: number;
  complianceRate: number;
}

export interface AssigneeRank {
  assigneeId: number;
  assigneeName: string;
  resolvedCount: number;
  avgResolutionHours: number;
}

export interface TicketClassifyResult {
  categoryId: number;
  categoryName: string;
  confidence: number;
  reasoning: string;
}

export interface SearchHit<T> {
  id: string;
  score: number;
  source: T;
  highlights: Record<string, string[]>;
}

export interface SearchResult<T> {
  hits: SearchHit<T>[];
  totalHits: number;
  tookMs: number;
}

export interface TicketSearchDoc {
  id: string;
  title: string;
  description: string;
  ticketNo: string;
  status: string;
  priority: number;
  categoryName: string;
  reporterName: string;
  assigneeName: string;
  tags: string;
  createTime: string;
}

// ==================== API ====================

export const ticketApi = {
  // 工单 CRUD
  create: (data: TicketCreateDTO): Promise<TicketDetail> =>
    request.post('/api/ticket/tickets', data),

  page: (params: TicketQueryDTO & { current: number; size: number }): Promise<PageResult<TicketListItem>> =>
    request.get('/api/ticket/tickets', { params }),

  getDetail: (id: number): Promise<TicketDetail> =>
    request.get(`/api/ticket/tickets/${id}/detail`),

  update: (id: number, data: TicketUpdateDTO): Promise<void> =>
    request.put(`/api/ticket/tickets/${id}`, data),

  changeStatus: (id: number, data: { toStatus: string; remark?: string }): Promise<void> =>
    request.put(`/api/ticket/tickets/${id}/status`, null, { params: data }),

  assign: (id: number, data: { assigneeId: number; assigneeName: string }): Promise<void> =>
    request.put(`/api/ticket/tickets/${id}/assign`, null, { params: data }),

  close: (id: number, remark?: string): Promise<void> =>
    request.put(`/api/ticket/tickets/${id}/close`, null, { params: { remark } }),

  delete: (id: number): Promise<void> =>
    request.delete(`/api/ticket/tickets/${id}`),

  // 评论
  getComments: (ticketId: number): Promise<CommentItem[]> =>
    request.get(`/api/ticket/tickets/${ticketId}/comments`),

  addComment: (ticketId: number, data: { content: string; type?: string; isInternal?: number; parentId?: number }): Promise<void> =>
    request.post(`/api/ticket/tickets/${ticketId}/comments`, data),

  deleteComment: (ticketId: number, commentId: number): Promise<void> =>
    request.delete(`/api/ticket/tickets/${ticketId}/comments/${commentId}`),

  // 附件
  getAttachments: (ticketId: number): Promise<AttachmentItem[]> =>
    request.get(`/api/ticket/tickets/${ticketId}/attachments`),

  addAttachment: (ticketId: number, data: { fileId: number; fileName: string; fileSize: number; fileType: string; fileUrl: string }): Promise<void> =>
    request.post(`/api/ticket/tickets/${ticketId}/attachments`, data),

  deleteAttachment: (ticketId: number, attachmentId: number): Promise<void> =>
    request.delete(`/api/ticket/tickets/${ticketId}/attachments/${attachmentId}`),

  // 审批
  submitApproval: (ticketId: number, data: { approver1: string; approver2?: string }): Promise<string> =>
    request.post(`/api/ticket/tickets/${ticketId}/approvals/submit`, data),

  approve: (ticketId: number, params: { taskId?: string; taskName?: string; opinion?: string }): Promise<void> =>
    request.post(`/api/ticket/tickets/${ticketId}/approvals/approve`, null, { params }),

  reject: (ticketId: number, params: { taskId?: string; taskName?: string; opinion?: string }): Promise<void> =>
    request.post(`/api/ticket/tickets/${ticketId}/approvals/reject`, null, { params }),

  returnTicket: (ticketId: number, params: { taskId?: string; taskName?: string; opinion?: string }): Promise<void> =>
    request.post(`/api/ticket/tickets/${ticketId}/approvals/return`, null, { params }),

  delegate: (ticketId: number, params: { taskId?: string; delegateUserId: string; opinion?: string }): Promise<void> =>
    request.post(`/api/ticket/tickets/${ticketId}/approvals/delegate`, null, { params }),

  getActiveTasks: (ticketId: number): Promise<ActiveTaskItem[]> =>
    request.get(`/api/ticket/tickets/${ticketId}/approvals/tasks`),

  // 分类
  getCategoryTree: (): Promise<TicketCategoryTree[]> =>
    request.get('/api/ticket/categories'),

  createCategory: (data: { name: string; parentId?: number; icon?: string; sortOrder?: number; description?: string; slaHours?: number; status?: number }): Promise<void> =>
    request.post('/api/ticket/categories', data),

  updateCategory: (id: number, data: { name?: string; parentId?: number; icon?: string; sortOrder?: number; description?: string; slaHours?: number; status?: number }): Promise<void> =>
    request.put(`/api/ticket/categories/${id}`, data),

  deleteCategory: (id: number): Promise<void> =>
    request.delete(`/api/ticket/categories/${id}`),

  // 统计
  getOverview: (): Promise<TicketOverview> =>
    request.get('/api/ticket/statistics/overview'),

  countByCategory: (): Promise<Record<string, number>[]> =>
    request.get('/api/ticket/statistics/by-category'),

  countByStatus: (): Promise<Record<string, number>[]> =>
    request.get('/api/ticket/statistics/by-status'),

  // 幂等 Token
  getIdempotentToken: (): Promise<string> =>
    request.get('/api/idempotent/token'),

  // 创建工单（带幂等 token）
  createWithToken: (data: TicketCreateDTO, token: string): Promise<TicketDetail> =>
    request.post('/api/ticket/tickets', data, {
      headers: { 'X-Idempotent-Token': token },
    }),

  // ==================== Phase 2: 统计增强 ====================

  getTrend: (days = 30): Promise<TrendPoint[]> =>
    request.get('/api/ticket/statistics/trend', { params: { days } }),

  getResolutionTime: (): Promise<ResolutionTimeStats> =>
    request.get('/api/ticket/statistics/resolution-time'),

  getSlaCompliance: (): Promise<SlaCompliance> =>
    request.get('/api/ticket/statistics/sla-compliance'),

  getTopAssignees: (limit = 10): Promise<AssigneeRank[]> =>
    request.get('/api/ticket/statistics/top-assignees', { params: { limit } }),

  // ==================== Phase 2: 全文搜索 ====================

  searchTickets: (keyword: string, filters: Partial<TicketQueryDTO> = {}, page = 1, size = 10): Promise<SearchResult<TicketSearchDoc>> =>
    request.get('/api/ticket/search', { params: { keyword, ...filters, page, size } }),

  reindexSearch: (): Promise<void> =>
    request.post('/api/ticket/search/reindex'),

  // ==================== Phase 2: AI 智能 ====================

  aiClassify: (title: string, description?: string): Promise<TicketClassifyResult> =>
    request.post('/api/ticket/ai/classify', { title, description }),

  aiSummarize: (ticketId: number): Promise<string> =>
    request.get(`/api/ticket/ai/summary/${ticketId}`),

  aiSuggestReply: (ticketId: number): Promise<string[]> =>
    request.get(`/api/ticket/ai/suggest-reply/${ticketId}`),

  // ==================== Phase 2: 导出 ====================

  exportTickets: (params: Partial<TicketQueryDTO>, format: 'csv' | 'excel' = 'excel'): Promise<Blob> =>
    request.get('/api/ticket/export', {
      params: { ...params, format },
      responseType: 'blob',
    }),

  asyncExport: (params: Partial<TicketQueryDTO>, format: 'csv' | 'excel' = 'excel'): Promise<string> =>
    request.get('/api/ticket/export/async', { params: { ...params, format } }),

  getExportStatus: (taskId: string): Promise<{ taskId: string; status: string }> =>
    request.get(`/api/ticket/export/status/${taskId}`),

  downloadExport: (taskId: string): Promise<Blob> =>
    request.get(`/api/ticket/export/download/${taskId}`, { responseType: 'blob' }),

  // ==================== Phase 2: 实时推送 ====================

  subscribeRealtime: (ticketId: number): Promise<void> =>
    request.post(`/api/ticket/realtime/subscribe/${ticketId}`),

  unsubscribeRealtime: (ticketId: number): Promise<void> =>
    request.delete(`/api/ticket/realtime/unsubscribe/${ticketId}`),
};
