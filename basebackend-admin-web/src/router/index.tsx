/**
 * 路由配置
 * 定义静态路由（登录、错误页）和受保护的动态路由
 *
 * 路由结构：
 * - /login          → 登录页（公开）
 * - /403            → 禁止访问页（公开）
 * - /404            → 页面未找到（公开）
 * - /500            → 服务器错误（公开）
 * - /               → RouterGuard 包裹的受保护路由组
 *   - /             → Dashboard 首页
 *   - ...dynamicRoutes → 动态路由（来自 AuthStore）
 * - *               → 重定向到 /404
 */
import React, { Suspense } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { Spin } from 'antd';
import RouterGuard from './guard';
import { useAuthStore } from '@/stores/authStore';

/** 懒加载页面组件 */
const Login = React.lazy(() => import('@/pages/Login'));
const Forbidden = React.lazy(() => import('@/pages/error/403'));
const NotFound = React.lazy(() => import('@/pages/error/404'));
const ServerError = React.lazy(() => import('@/pages/error/500'));
const Dashboard = React.lazy(() => import('@/pages/Dashboard'));

/** 全局加载状态组件 */
const Loading: React.FC = () => (
  <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
    <Spin size="large" tip="加载中..." />
  </div>
);

/**
 * 递归渲染动态路由
 * 将 RouteObject[] 转换为 <Route> 元素树
 */
function renderDynamicRoutes(routes: ReturnType<typeof useAuthStore.getState>['dynamicRoutes']): React.ReactNode {
  return routes.map((route, index) => {
    if (route.children && route.children.length > 0) {
      return (
        <Route key={route.path || index} path={route.path}>
          {renderDynamicRoutes(route.children)}
        </Route>
      );
    }
    return (
      <Route
        key={route.path || index}
        path={route.path}
        element={route.element ? <Suspense fallback={<Loading />}>{route.element}</Suspense> : null}
      />
    );
  });
}

/**
 * 应用路由组件
 * 组合静态路由和动态路由，使用 RouterGuard 保护需要认证的路由
 */
const AppRoutes: React.FC = () => {
  const { dynamicRoutes } = useAuthStore();

  return (
    <Suspense fallback={<Loading />}>
      <Routes>
        {/* 公开路由：无需认证 */}
        <Route path="/login" element={<Login />} />
        <Route path="/403" element={<Forbidden />} />
        <Route path="/404" element={<NotFound />} />
        <Route path="/500" element={<ServerError />} />

        {/* 受保护路由：RouterGuard 作为布局路由，子路由通过 Outlet 渲染 */}
        <Route path="/" element={<RouterGuard />}>
          <Route index element={<Suspense fallback={<Loading />}><Dashboard /></Suspense>} />
          {renderDynamicRoutes(dynamicRoutes)}
        </Route>

        {/* 兜底路由：未匹配的路径重定向到 404 */}
        <Route path="*" element={<Navigate to="/404" replace />} />
      </Routes>
    </Suspense>
  );
};

export default AppRoutes;
