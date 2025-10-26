import dayjs, { Dayjs } from 'dayjs'

/**
 * 计算两个日期之间的天数
 */
export const calculateDaysBetween = (start: Dayjs | string, end: Dayjs | string): number => {
  const startDate = typeof start === 'string' ? dayjs(start) : start
  const endDate = typeof end === 'string' ? dayjs(end) : end
  return endDate.diff(startDate, 'day') + 1
}

/**
 * 计算持续时间（返回友好格式）
 */
export const calculateDuration = (
  startTime: string | Date,
  endTime?: string | Date
): string => {
  const start = dayjs(startTime)
  const end = endTime ? dayjs(endTime) : dayjs()
  const duration = end.diff(start, 'minute')

  if (duration < 60) {
    return `${duration}分钟`
  } else if (duration < 1440) {
    const hours = Math.floor(duration / 60)
    const minutes = duration % 60
    return minutes > 0 ? `${hours}小时${minutes}分钟` : `${hours}小时`
  } else {
    const days = Math.floor(duration / 1440)
    const hours = Math.floor((duration % 1440) / 60)
    return hours > 0 ? `${days}天${hours}小时` : `${days}天`
  }
}

/**
 * 计算持续时间（返回分钟数）
 */
export const calculateDurationInMinutes = (
  startTime: string | Date,
  endTime?: string | Date
): number => {
  const start = dayjs(startTime)
  const end = endTime ? dayjs(endTime) : dayjs()
  return end.diff(start, 'minute')
}

/**
 * 格式化日期时间
 */
export const formatDateTime = (
  date: string | Date,
  format: string = 'YYYY-MM-DD HH:mm:ss'
): string => {
  return dayjs(date).format(format)
}

/**
 * 格式化相对时间
 */
export const formatRelativeTime = (date: string | Date): string => {
  return dayjs(date).fromNow()
}

/**
 * 检查日期是否即将到期（24小时内）
 */
export const isApproachingDue = (dueDate: string | Date): boolean => {
  const now = dayjs()
  const due = dayjs(dueDate)
  const hoursDiff = due.diff(now, 'hour')
  return hoursDiff >= 0 && hoursDiff < 24
}

/**
 * 检查日期是否已超时
 */
export const isOverdue = (dueDate: string | Date): boolean => {
  const now = dayjs()
  const due = dayjs(dueDate)
  return due.isBefore(now)
}

/**
 * 获取日期范围的开始和结束时间
 */
export const getDateRangeTimestamps = (
  startDate: Dayjs,
  endDate: Dayjs
): { start: string; end: string } => {
  return {
    start: startDate.startOf('day').toISOString(),
    end: endDate.endOf('day').toISOString(),
  }
}

/**
 * 检查日期是否在范围内
 */
export const isDateInRange = (
  date: string | Date,
  startDate: string | Date,
  endDate: string | Date
): boolean => {
  const target = dayjs(date)
  const start = dayjs(startDate)
  const end = dayjs(endDate)
  return target.isAfter(start) && target.isBefore(end)
}
