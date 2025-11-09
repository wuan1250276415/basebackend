import React, { createContext, useContext, useState, useCallback, useEffect } from 'react'
import { getDeptTree, getDeptList } from '@/api/dept'
import { Dept } from '@/types'

/**
 * 部门上下文接口
 */
interface DeptContextType {
  // 部门数据
  deptTree: Dept[]
  deptList: Dept[]

  // 加载状态
  loading: boolean
  treeLoading: boolean
  listLoading: boolean

  // 操作方法
  loadDeptTree: (forceRefresh?: boolean) => Promise<Dept[]>
  loadDeptList: (forceRefresh?: boolean) => Promise<Dept[]>
  getDeptById: (deptId: string) => Dept | undefined
  getDeptPath: (deptId: string) => Dept[]
  getDeptChildren: (deptId: string) => Dept[]
  refreshDept: () => Promise<void>
  clearCache: () => void
}

const DeptContext = createContext<DeptContextType | undefined>(undefined)

// 缓存过期时间（10分钟）
const CACHE_EXPIRE_TIME = 10 * 60 * 1000

/**
 * 部门上下文 Provider
 */
export const DeptProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [deptTree, setDeptTree] = useState<Dept[]>([])
  const [deptList, setDeptList] = useState<Dept[]>([])
  const [treeLoading, setTreeLoading] = useState(false)
  const [listLoading, setListLoading] = useState(false)
  const [treeTimestamp, setTreeTimestamp] = useState(0)
  const [listTimestamp, setListTimestamp] = useState(0)

  /**
   * 检查树形缓存是否过期
   */
  const isTreeCacheExpired = useCallback((): boolean => {
    return Date.now() - treeTimestamp > CACHE_EXPIRE_TIME
  }, [treeTimestamp])

  /**
   * 检查列表缓存是否过期
   */
  const isListCacheExpired = useCallback((): boolean => {
    return Date.now() - listTimestamp > CACHE_EXPIRE_TIME
  }, [listTimestamp])

  /**
   * 加载部门树
   * @param forceRefresh 是否强制刷新
   */
  const loadDeptTree = useCallback(
    async (forceRefresh = false): Promise<Dept[]> => {
      // 检查缓存
      if (!forceRefresh && !isTreeCacheExpired() && deptTree.length > 0) {
        return deptTree
      }

      setTreeLoading(true)

      try {
        const response = await getDeptTree()
        const data = response.data || []

        setDeptTree(data)
        setTreeTimestamp(Date.now())

        return data
      } catch (error) {
        console.error('加载部门树失败:', error)
        return deptTree
      } finally {
        setTreeLoading(false)
      }
    },
    [deptTree, isTreeCacheExpired]
  )

  /**
   * 加载部门列表
   * @param forceRefresh 是否强制刷新
   */
  const loadDeptList = useCallback(
    async (forceRefresh = false): Promise<Dept[]> => {
      // 检查缓存
      if (!forceRefresh && !isListCacheExpired() && deptList.length > 0) {
        return deptList
      }

      setListLoading(true)

      try {
        const response = await getDeptList()
        const data = response.data || []

        setDeptList(data)
        setListTimestamp(Date.now())

        return data
      } catch (error) {
        console.error('加载部门列表失败:', error)
        return deptList
      } finally {
        setListLoading(false)
      }
    },
    [deptList, isListCacheExpired]
  )

  /**
   * 根据ID获取部门
   * @param deptId 部门ID
   */
  const getDeptById = useCallback(
    (deptId: string): Dept | undefined => {
      // 递归查找
      const findDept = (depts: Dept[]): Dept | undefined => {
        for (const dept of depts) {
          if (dept.id === deptId) {
            return dept
          }
          if (dept.children && dept.children.length > 0) {
            const found = findDept(dept.children)
            if (found) return found
          }
        }
        return undefined
      }

      return findDept(deptTree) || deptList.find((d) => d.id === deptId)
    },
    [deptTree, deptList]
  )

  /**
   * 获取部门路径（从根到指定部门）
   * @param deptId 部门ID
   * @example
   * // 返回: [根部门, 父部门, 当前部门]
   * const path = getDeptPath('dept123')
   */
  const getDeptPath = useCallback(
    (deptId: string): Dept[] => {
      const path: Dept[] = []

      const findPath = (depts: Dept[], targetId: string): boolean => {
        for (const dept of depts) {
          if (dept.id === targetId) {
            path.push(dept)
            return true
          }

          if (dept.children && dept.children.length > 0) {
            path.push(dept)
            if (findPath(dept.children, targetId)) {
              return true
            }
            path.pop()
          }
        }
        return false
      }

      findPath(deptTree, deptId)
      return path
    },
    [deptTree]
  )

  /**
   * 获取指定部门的所有子部门（扁平化）
   * @param deptId 部门ID
   */
  const getDeptChildren = useCallback(
    (deptId: string): Dept[] => {
      const dept = getDeptById(deptId)
      if (!dept || !dept.children) return []

      const children: Dept[] = []

      const collectChildren = (depts: Dept[]) => {
        depts.forEach((d) => {
          children.push(d)
          if (d.children && d.children.length > 0) {
            collectChildren(d.children)
          }
        })
      }

      collectChildren(dept.children)
      return children
    },
    [getDeptById]
  )

  /**
   * 刷新部门数据
   */
  const refreshDept = useCallback(async () => {
    await Promise.all([loadDeptTree(true), loadDeptList(true)])
  }, [loadDeptTree, loadDeptList])

  /**
   * 清空缓存
   */
  const clearCache = useCallback(() => {
    setDeptTree([])
    setDeptList([])
    setTreeTimestamp(0)
    setListTimestamp(0)
  }, [])

  // 组件挂载时自动加载部门树
  useEffect(() => {
    loadDeptTree()
  }, [])

  const value: DeptContextType = {
    deptTree,
    deptList,
    loading: treeLoading || listLoading,
    treeLoading,
    listLoading,
    loadDeptTree,
    loadDeptList,
    getDeptById,
    getDeptPath,
    getDeptChildren,
    refreshDept,
    clearCache,
  }

  return <DeptContext.Provider value={value}>{children}</DeptContext.Provider>
}

/**
 * 使用部门上下文的 Hook
 * @example
 * const { deptTree, getDeptById } = useDept()
 *
 * // 获取部门树
 * const tree = deptTree
 *
 * // 根据ID获取部门
 * const dept = getDeptById('1')
 *
 * // 获取部门路径
 * const path = getDeptPath('123') // [根部门, 父部门, 当前部门]
 */
export const useDept = (): DeptContextType => {
  const context = useContext(DeptContext)
  if (!context) {
    throw new Error('useDept 必须在 DeptProvider 内部使用')
  }
  return context
}

/**
 * 便捷Hook：获取部门树选项（用于TreeSelect）
 * @example
 * const [options, loading] = useDeptTreeOptions()
 * // 直接用于 <TreeSelect options={options} loading={loading} />
 */
export const useDeptTreeOptions = (): [Dept[], boolean] => {
  const { deptTree, treeLoading, loadDeptTree } = useDept()

  useEffect(() => {
    loadDeptTree()
  }, [loadDeptTree])

  return [deptTree, treeLoading]
}

/**
 * 便捷Hook：获取指定部门信息
 * @param deptId 部门ID
 * @example
 * const dept = useDeptInfo('123')
 */
export const useDeptInfo = (deptId: string | undefined): Dept | undefined => {
  const { getDeptById } = useDept()

  if (!deptId) return undefined
  return getDeptById(deptId)
}
