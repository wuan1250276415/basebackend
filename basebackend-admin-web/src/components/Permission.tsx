import type { ReactNode } from 'react';
import { useAuth } from '@/hooks/useAuth';

/**
 * 声明式权限控制组件
 *
 * 根据用户权限决定是否渲染子组件，无权限时渲染 fallback 或 null
 *
 * @example
 * <Permission code="system:user:add">
 *   <Button type="primary">新增用户</Button>
 * </Permission>
 *
 * <Permission code="system:user:delete" fallback={<span>无权限</span>}>
 *   <Button danger>删除</Button>
 * </Permission>
 */
interface PermissionProps {
  /** 权限标识码 */
  code: string;
  /** 有权限时渲染的子组件 */
  children: ReactNode;
  /** 无权限时渲染的备选内容，默认为 null */
  fallback?: ReactNode;
}

export function Permission({ code, children, fallback = null }: PermissionProps) {
  const hasPermission = useAuth(code);

  return hasPermission ? <>{children}</> : <>{fallback}</>;
}
