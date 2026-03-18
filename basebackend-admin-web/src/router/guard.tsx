/**
 * 路由守卫组件
 * 根据用户认证状态和菜单权限控制页面访问
 *
 * 逻辑流程：
 * 1. 白名单路由（/login, /403, /404, /500）直接放行
 * 2. 无 token → 重定向 /login
 * 3. 有 token 但无 userInfo → 调用 fetchUserInfo（显示加载状态）
 * 4. 有 token + userInfo → 检查当前路由是否在用户菜单中
 *    - 在 → 放行
 *    - 不在 → 重定向 /403
 */
import { useEffect, useState } from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { Spin } from 'antd';
import { useAuthStore } from '@/stores/authStore';
import { findInTree } from '@/utils/tree';
import type { MenuItem } from '@/types';
import { filterUnsupportedMenus } from './menuAvailability';

/** 白名单路由列表，无需认证即可访问 */
const WHITE_LIST = ['/login', '/403', '/404', '/500'];

/**
 * 检查指定路径是否存在于用户菜单树中
 * 递归搜索菜单树，匹配 type=0（目录）和 type=1（菜单）的路径
 *
 * @param menus - 用户菜单树
 * @param path - 当前路由路径
 * @returns 是否在菜单中找到匹配路径
 */
function isPathInMenus(menus: MenuItem[], path: string): boolean {
  return (
    findInTree(
      menus,
      (node) => node.path === path && node.type !== 2,
      'children',
    ) !== null
  );
}

/**
 * 路由守卫组件
 * 包裹在需要认证的路由外层，控制访问权限
 */
const RouterGuard: React.FC = () => {
  const location = useLocation();
  const { token, userInfo, menus } = useAuthStore();
  const supportedMenus = filterUnsupportedMenus(menus);

  // 1. 白名单路由直接放行
  if (WHITE_LIST.includes(location.pathname)) {
    return <Outlet />;
  }

  // 2. 无 token → 重定向到登录页
  if (!token) {
    return <Navigate to="/login" replace />;
  }

  // 3. 有 token 但无 userInfo → 拉取用户信息
  if (!userInfo) {
    return <FetchUserInfoWrapper />;
  }

  // 4. 有 token + userInfo → 检查路由权限
  // 根路径和 dashboard 默认放行
  if (location.pathname === '/' || location.pathname === '/dashboard') {
    return <Outlet />;
  }

  // 检查当前路由是否在用户菜单中
  if (!isPathInMenus(supportedMenus, location.pathname)) {
    return <Navigate to="/403" replace />;
  }

  return <Outlet />;
};

/**
 * 用户信息加载包装组件
 * 在 token 存在但 userInfo 为空时，异步拉取用户信息
 * 加载期间显示全屏 loading 状态
 */
const FetchUserInfoWrapper: React.FC = () => {
  const { fetchUserInfo } = useAuthStore();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  useEffect(() => {
    let cancelled = false;

    const loadUserInfo = async () => {
      try {
        await fetchUserInfo();
      } catch {
        // 拉取用户信息失败，跳转登录页
        if (!cancelled) {
          setError(true);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    loadUserInfo();

    return () => {
      cancelled = true;
    };
  }, [fetchUserInfo]);

  // 加载失败，重定向到登录页
  if (error) {
    return <Navigate to="/login" replace />;
  }

  // 加载中，显示全屏 loading
  if (loading) {
    return (
      <div
        style={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          height: '100vh',
        }}
      >
        <Spin size="large" tip="加载中..." />
      </div>
    );
  }

  // 加载完成，渲染子路由
  return <Outlet />;
};

export default RouterGuard;
export { isPathInMenus, WHITE_LIST };
