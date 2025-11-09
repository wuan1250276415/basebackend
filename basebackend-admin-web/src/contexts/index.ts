/**
 * Contexts 统一导出
 * 提供便捷的导入方式
 */

// 主要的 AppProvider 和 useApp Hook
export { AppProvider, useApp } from './AppContext'

// 单独的 Context Providers
export { UserProvider, DictProvider, DeptProvider } from './AppContext'

// 单独的 Context Hooks
export { useUser, useDict, useDept } from './AppContext'

// 便捷 Hooks
export {
  withPermission,
  withRole,
  useDictData,
  useDictOptions,
  useDeptTreeOptions,
  useDeptInfo,
} from './AppContext'

/**
 * 使用指南：
 *
 * 1. 在应用入口使用 AppProvider 包裹整个应用：
 *    import { AppProvider } from '@/contexts'
 *    <AppProvider>
 *      <App />
 *    </AppProvider>
 *
 * 2. 在组件中使用 useApp Hook 访问所有功能：
 *    import { useApp } from '@/contexts'
 *    const app = useApp()
 *    const { userInfo } = app.user
 *
 * 3. 也可以单独使用各个 Context：
 *    import { useUser, useDict, useDept } from '@/contexts'
 *    const { userInfo } = useUser()
 *    const { getDictByType } = useDict()
 *    const { deptTree } = useDept()
 *
 * 4. 使用便捷 Hooks：
 *    import { useDictOptions, useDeptTreeOptions } from '@/contexts'
 *    const [options, loading] = useDictOptions('sys_user_status')
 *
 * 5. 使用权限控制 HOC：
 *    import { withPermission } from '@/contexts'
 *    const ProtectedComponent = withPermission('system:user:add')(MyComponent)
 */
