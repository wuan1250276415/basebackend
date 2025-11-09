import React, { createContext, useContext, useEffect, useState, useCallback } from 'react'
import { useAuthStore } from '@/stores/auth'
import { getCurrentUserInfo } from '@/api/auth'
import { UserInfo } from '@/types'

/**
 * 用户上下文接口
 */
interface UserContextType {
  // 用户信息
  userInfo: UserInfo | null
  token: string | null
  permissions: string[]
  roles: string[]

  // 加载状态
  loading: boolean
  error: string | null

  // 操作方法
  refreshUserInfo: () => Promise<void>
  hasPermission: (permission: string) => boolean
  hasRole: (role: string) => boolean
  hasAnyPermission: (permissions: string[]) => boolean
  hasAnyRole: (roles: string[]) => boolean
  logout: () => void
}

const UserContext = createContext<UserContextType | undefined>(undefined)

/**
 * 用户上下文 Provider
 */
export const UserProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const authStore = useAuthStore()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  /**
   * 刷新用户信息
   */
  const refreshUserInfo = useCallback(async () => {
    if (!authStore.token) {
      return
    }

    setLoading(true)
    setError(null)

    try {
      const response = await getCurrentUserInfo()
      if (response.data.code === 200 && response.data.data) {
        authStore.setUserInfo(response.data.data)

        // 这里可以同时获取权限和角色
        // 假设后端返回了权限和角色信息
        // authStore.setPermissions(response.data.data.permissions || [])
        // authStore.setRoles(response.data.data.roles || [])
      }
    } catch (err: any) {
      setError(err.message || '获取用户信息失败')
      console.error('刷新用户信息失败:', err)
    } finally {
      setLoading(false)
    }
  }, [authStore])

  /**
   * 检查是否有指定权限
   */
  const hasPermission = useCallback(
    (permission: string): boolean => {
      return authStore.permissions.includes(permission)
    },
    [authStore.permissions]
  )

  /**
   * 检查是否有指定角色
   */
  const hasRole = useCallback(
    (role: string): boolean => {
      return authStore.roles.includes(role)
    },
    [authStore.roles]
  )

  /**
   * 检查是否有任意一个权限
   */
  const hasAnyPermission = useCallback(
    (permissions: string[]): boolean => {
      return permissions.some((permission) => authStore.permissions.includes(permission))
    },
    [authStore.permissions]
  )

  /**
   * 检查是否有任意一个角色
   */
  const hasAnyRole = useCallback(
    (roles: string[]): boolean => {
      return roles.some((role) => authStore.roles.includes(role))
    },
    [authStore.roles]
  )

  /**
   * 登出
   */
  const logout = useCallback(() => {
    authStore.logout()
  }, [authStore])

  // 组件挂载时刷新用户信息
  useEffect(() => {
    if (authStore.token && !authStore.userInfo) {
      refreshUserInfo()
    }
  }, [authStore.token, authStore.userInfo, refreshUserInfo])

  const value: UserContextType = {
    userInfo: authStore.userInfo,
    token: authStore.token,
    permissions: authStore.permissions,
    roles: authStore.roles,
    loading,
    error,
    refreshUserInfo,
    hasPermission,
    hasRole,
    hasAnyPermission,
    hasAnyRole,
    logout,
  }

  return <UserContext.Provider value={value}>{children}</UserContext.Provider>
}

/**
 * 使用用户上下文的 Hook
 * @example
 * const { userInfo, hasPermission } = useUser()
 */
export const useUser = (): UserContextType => {
  const context = useContext(UserContext)
  if (!context) {
    throw new Error('useUser 必须在 UserProvider 内部使用')
  }
  return context
}

/**
 * 高阶组件：权限检查
 * @param permission 需要的权限
 * @example
 * const ProtectedComponent = withPermission('system:user:add')(MyComponent)
 */
export const withPermission = (permission: string) => {
  return <P extends object>(Component: React.ComponentType<P>) => {
    const WrappedComponent: React.FC<P> = (props) => {
      const { hasPermission } = useUser()

      if (!hasPermission(permission)) {
        return <div>您没有权限访问此内容</div>
      }

      return <Component {...props} />
    }

    WrappedComponent.displayName = `withPermission(${Component.displayName || Component.name})`
    return WrappedComponent
  }
}

/**
 * 高阶组件：角色检查
 * @param role 需要的角色
 * @example
 * const ProtectedComponent = withRole('admin')(MyComponent)
 */
export const withRole = (role: string) => {
  return <P extends object>(Component: React.ComponentType<P>) => {
    const WrappedComponent: React.FC<P> = (props) => {
      const { hasRole } = useUser()

      if (!hasRole(role)) {
        return <div>您的角色无权访问此内容</div>
      }

      return <Component {...props} />
    }

    WrappedComponent.displayName = `withRole(${Component.displayName || Component.name})`
    return WrappedComponent
  }
}
