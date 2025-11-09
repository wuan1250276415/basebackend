import React, { createContext, useContext, useState, useCallback, useEffect } from 'react'
import { getDictDataByType, refreshDictCache } from '@/api/dict'
import { DictData } from '@/types'

/**
 * 字典数据缓存接口
 */
interface DictCache {
  [dictType: string]: {
    data: DictData[]
    timestamp: number
  }
}

/**
 * 字典上下文接口
 */
interface DictContextType {
  // 字典缓存
  dictCache: DictCache

  // 加载状态
  loading: Record<string, boolean>

  // 操作方法
  getDictByType: (dictType: string, forceRefresh?: boolean) => Promise<DictData[]>
  getDictLabel: (dictType: string, dictValue: string) => string
  getDictOptions: (dictType: string) => Promise<Array<{ label: string; value: string }>>
  refreshDict: (dictType: string) => Promise<void>
  refreshAllDict: () => Promise<void>
  clearCache: () => void
}

const DictContext = createContext<DictContextType | undefined>(undefined)

// 缓存过期时间（5分钟）
const CACHE_EXPIRE_TIME = 5 * 60 * 1000

/**
 * 字典上下文 Provider
 */
export const DictProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [dictCache, setDictCache] = useState<DictCache>({})
  const [loading, setLoading] = useState<Record<string, boolean>>({})

  /**
   * 检查缓存是否过期
   */
  const isCacheExpired = useCallback((dictType: string): boolean => {
    const cache = dictCache[dictType]
    if (!cache) return true
    return Date.now() - cache.timestamp > CACHE_EXPIRE_TIME
  }, [dictCache])

  /**
   * 根据字典类型获取字典数据
   * @param dictType 字典类型
   * @param forceRefresh 是否强制刷新
   */
  const getDictByType = useCallback(
    async (dictType: string, forceRefresh = false): Promise<DictData[]> => {
      // 检查缓存
      if (!forceRefresh && !isCacheExpired(dictType) && dictCache[dictType]) {
        return dictCache[dictType].data
      }

      // 防止重复请求
      if (loading[dictType]) {
        return dictCache[dictType]?.data || []
      }

      setLoading((prev) => ({ ...prev, [dictType]: true }))

      try {
        const response = await getDictDataByType(dictType)
        if (response.data.code === 200 && response.data.data) {
          const data = response.data.data

          // 更新缓存
          setDictCache((prev) => ({
            ...prev,
            [dictType]: {
              data,
              timestamp: Date.now(),
            },
          }))

          return data
        }
        return []
      } catch (error) {
        console.error(`获取字典数据失败 [${dictType}]:`, error)
        return dictCache[dictType]?.data || []
      } finally {
        setLoading((prev) => ({ ...prev, [dictType]: false }))
      }
    },
    [dictCache, loading, isCacheExpired]
  )

  /**
   * 根据字典值获取字典标签
   * @param dictType 字典类型
   * @param dictValue 字典值
   */
  const getDictLabel = useCallback(
    (dictType: string, dictValue: string): string => {
      const cache = dictCache[dictType]
      if (!cache) return dictValue

      const item = cache.data.find((d) => d.dictValue === dictValue)
      return item?.dictLabel || dictValue
    },
    [dictCache]
  )

  /**
   * 获取字典选项（用于下拉框）
   * @param dictType 字典类型
   */
  const getDictOptions = useCallback(
    async (dictType: string): Promise<Array<{ label: string; value: string }>> => {
      const data = await getDictByType(dictType)
      return data
        .filter((item) => item.status === 1) // 只返回启用的
        .sort((a, b) => (a.dictSort || 0) - (b.dictSort || 0)) // 按排序号排序
        .map((item) => ({
          label: item.dictLabel,
          value: item.dictValue,
        }))
    },
    [getDictByType]
  )

  /**
   * 刷新指定字典
   * @param dictType 字典类型
   */
  const refreshDict = useCallback(
    async (dictType: string) => {
      await getDictByType(dictType, true)
    },
    [getDictByType]
  )

  /**
   * 刷新所有字典缓存
   */
  const refreshAllDict = useCallback(async () => {
    try {
      await refreshDictCache()

      // 清空本地缓存
      setDictCache({})

      console.log('字典缓存已刷新')
    } catch (error) {
      console.error('刷新字典缓存失败:', error)
    }
  }, [])

  /**
   * 清空本地缓存
   */
  const clearCache = useCallback(() => {
    setDictCache({})
  }, [])

  const value: DictContextType = {
    dictCache,
    loading,
    getDictByType,
    getDictLabel,
    getDictOptions,
    refreshDict,
    refreshAllDict,
    clearCache,
  }

  return <DictContext.Provider value={value}>{children}</DictContext.Provider>
}

/**
 * 使用字典上下文的 Hook
 * @example
 * const { getDictByType, getDictLabel } = useDict()
 *
 * // 获取字典数据
 * const statusDict = await getDictByType('sys_user_status')
 *
 * // 获取字典标签
 * const statusLabel = getDictLabel('sys_user_status', '1')
 *
 * // 获取字典选项（用于 Select）
 * const options = await getDictOptions('sys_user_status')
 */
export const useDict = (): DictContextType => {
  const context = useContext(DictContext)
  if (!context) {
    throw new Error('useDict 必须在 DictProvider 内部使用')
  }
  return context
}

/**
 * 便捷Hook：直接获取指定类型的字典数据
 * @param dictType 字典类型
 * @example
 * const [statusDict, loading] = useDictData('sys_user_status')
 */
export const useDictData = (dictType: string): [DictData[], boolean] => {
  const { getDictByType, loading: allLoading } = useDict()
  const [data, setData] = useState<DictData[]>([])

  useEffect(() => {
    getDictByType(dictType).then(setData)
  }, [dictType, getDictByType])

  return [data, allLoading[dictType] || false]
}

/**
 * 便捷Hook：直接获取指定类型的字典选项
 * @param dictType 字典类型
 * @example
 * const [options, loading] = useDictOptions('sys_user_status')
 * // 直接用于 <Select options={options} loading={loading} />
 */
export const useDictOptions = (
  dictType: string
): [Array<{ label: string; value: string }>, boolean] => {
  const { getDictOptions, loading: allLoading } = useDict()
  const [options, setOptions] = useState<Array<{ label: string; value: string }>>([])

  useEffect(() => {
    getDictOptions(dictType).then(setOptions)
  }, [dictType, getDictOptions])

  return [options, allLoading[dictType] || false]
}
