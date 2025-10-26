/**
 * 工作流工具函数库
 * 统一导出所有工作流相关工具函数
 */

// 日期时间工具
export {
  calculateDaysBetween,
  calculateDuration,
  calculateDurationInMinutes,
  formatDateTime,
  formatRelativeTime,
  isApproachingDue,
  isOverdue,
  getDateRangeTimestamps,
  isDateInRange,
} from './dateUtils'

// 键值生成工具
export {
  generateBusinessKey,
  generateProcessBusinessKey,
  parseBusinessKey,
  generateTaskKey,
  generateUUID,
} from './keyGenerator'

// 状态工具
export {
  getPriorityColor,
  getPriorityText,
  getProcessStatusColor,
  getProcessStatusText,
  getTaskStatusColor,
  getTaskStatusText,
  getProcessTypeColor,
  getProcessTypeText,
} from './statusUtils'

// 金额工具
export {
  formatCurrency,
  formatCurrencyWithSymbol,
  parseCurrency,
  calculateTotalAmount,
  isValidAmount,
  roundAmount,
} from './amountUtils'
