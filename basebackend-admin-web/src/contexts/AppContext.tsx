import React, { createContext, useContext, ReactNode } from 'react'
import { UserProvider, useUser } from './UserContext'
import { DictProvider, useDict } from './DictContext'
import { DeptProvider, useDept } from './DeptContext'

/**
 * 应用全局上下文类型
 * 整合了所有子 Context 的功能
 */
interface AppContextType {
  // 用户相关
  user: ReturnType<typeof useUser>

  // 字典相关
  dict: ReturnType<typeof useDict>

  // 部门相关
  dept: ReturnType<typeof useDept>

  // 全局刷新方法
  refreshAll: () => Promise<void>
}

const AppContext = createContext<AppContextType | undefined>(undefined)

/**
 * 内部 Provider - 用于访问所有子 Context
 */
const AppContextProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const user = useUser()
  const dict = useDict()
  const dept = useDept()

  /**
   * 刷新所有缓存数据
   */
  const refreshAll = async () => {
    await Promise.all([
      user.refreshUserInfo(),
      dict.refreshAllDict(),
      dept.refreshDept(),
    ])
  }

  const value: AppContextType = {
    user,
    dict,
    dept,
    refreshAll,
  }

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>
}

/**
 * 应用根 Provider
 * 包装了所有子 Provider，确保正确的嵌套顺序
 */
export const AppProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  return (
    <UserProvider>
      <DictProvider>
        <DeptProvider>
          <AppContextProvider>{children}</AppContextProvider>
        </DeptProvider>
      </DictProvider>
    </UserProvider>
  )
}

/**
 * 使用应用全局上下文的 Hook
 * 提供对所有子 Context 的统一访问
 *
 * @example
 * // 基础用法
 * const app = useApp()
 *
 * // 访问用户信息
 * const { userInfo, hasPermission } = app.user
 * if (hasPermission('system:user:add')) {
 *   // 有权限
 * }
 *
 * // 访问字典数据
 * const statusOptions = await app.dict.getDictOptions('sys_user_status')
 *
 * // 访问部门数据
 * const deptTree = app.dept.deptTree
 *
 * // 刷新所有缓存
 * await app.refreshAll()
 */
export const useApp = (): AppContextType => {
  const context = useContext(AppContext)
  if (!context) {
    throw new Error('useApp 必须在 AppProvider 内部使用')
  }
  return context
}

/**
 * 便捷导出：直接从 AppContext 访问子 Context
 * 这样可以让用户选择使用 useApp() 或单独的 useUser/useDict/useDept
 */
export { useUser, useDict, useDept }

// 导出所有 Provider 以便单独使用
export { UserProvider, DictProvider, DeptProvider }

// 导出便捷 Hooks
export { withPermission, withRole } from './UserContext'
export { useDictData, useDictOptions } from './DictContext'
export { useDeptTreeOptions, useDeptInfo } from './DeptContext'
