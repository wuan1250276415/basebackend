/**
 * 生成唯一的业务键
 */
export const generateBusinessKey = (prefix: string): string => {
  const timestamp = Date.now()
  const random = Math.floor(Math.random() * 10000)
  return `${prefix}-${timestamp}-${random}`
}

/**
 * 生成流程业务键（基于流程类型）
 */
export const generateProcessBusinessKey = (processType: string): string => {
  const typeMap: Record<string, string> = {
    leave: 'LEAVE',
    expense: 'EXPENSE',
    purchase: 'PURCHASE',
  }
  const prefix = typeMap[processType.toLowerCase()] || 'PROCESS'
  return generateBusinessKey(prefix)
}

/**
 * 解析业务键获取流程类型
 */
export const parseBusinessKey = (businessKey: string): {
  type: string
  timestamp: number
  id: string
} | null => {
  const parts = businessKey.split('-')
  if (parts.length >= 3) {
    return {
      type: parts[0],
      timestamp: parseInt(parts[1], 10),
      id: parts.slice(2).join('-'),
    }
  }
  return null
}

/**
 * 生成任务键
 */
export const generateTaskKey = (): string => {
  return `TASK-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`
}

/**
 * 生成UUID（简化版）
 */
export const generateUUID = (): string => {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}
