/**
 * 格式化金额（添加千分位）
 */
export const formatCurrency = (amount: number): string => {
  return amount.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })
}

/**
 * 格式化金额（带货币符号）
 */
export const formatCurrencyWithSymbol = (amount: number, symbol: string = '¥'): string => {
  return `${symbol} ${formatCurrency(amount)}`
}

/**
 * 解析金额字符串为数字
 */
export const parseCurrency = (value: string): number => {
  return parseFloat(value.replace(/[^\d.-]/g, '')) || 0
}

/**
 * 计算数组中的总金额
 */
export const calculateTotalAmount = (items: Array<{ amount?: number }>): number => {
  return items.reduce((sum, item) => sum + (item.amount || 0), 0)
}

/**
 * 验证金额是否有效
 */
export const isValidAmount = (amount: number): boolean => {
  return !isNaN(amount) && amount >= 0 && amount <= 999999999.99
}

/**
 * 四舍五入到指定小数位
 */
export const roundAmount = (amount: number, decimals: number = 2): number => {
  return Math.round(amount * Math.pow(10, decimals)) / Math.pow(10, decimals)
}
