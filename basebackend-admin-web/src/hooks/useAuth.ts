import { useAuthStore } from '@/stores/authStore';

/**
 * 权限判断 Hook
 *
 * 单权限模式：传入权限字符串，返回是否拥有该权限
 * 多权限模式：传入权限数组和匹配模式（any/all），返回是否满足条件
 *
 * @example
 * // 单权限判断
 * const canAdd = useAuth('system:user:add');
 *
 * // 多权限 - 满足任一即可（默认）
 * const canOperate = useAuth(['system:user:add', 'system:user:edit']);
 *
 * // 多权限 - 必须全部满足
 * const canManage = useAuth(['system:user:add', 'system:user:edit'], 'all');
 */
export function useAuth(permission: string): boolean;
export function useAuth(permissions: string[], mode?: 'any' | 'all'): boolean;
export function useAuth(
  permissionOrPermissions: string | string[],
  mode: 'any' | 'all' = 'any',
): boolean {
  const hasPermission = useAuthStore((state) => state.hasPermission);

  // 单权限模式
  if (typeof permissionOrPermissions === 'string') {
    return hasPermission(permissionOrPermissions);
  }

  // 多权限模式
  if (mode === 'all') {
    return permissionOrPermissions.every((p) => hasPermission(p));
  }

  // 默认 any 模式
  return permissionOrPermissions.some((p) => hasPermission(p));
}
