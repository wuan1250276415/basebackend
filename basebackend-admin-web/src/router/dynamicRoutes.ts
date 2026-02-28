/**
 * 动态路由生成模块
 * 将后端返回的菜单数据转换为 React Router 6 的 RouteObject[]
 *
 * 菜单类型映射规则：
 * - type=0 (目录) → 路由分组，包含子路由
 * - type=1 (菜单) → 懒加载页面组件
 * - type=2 (按钮) → 不生成路由，仅用于权限判断
 */
import React from 'react';
import type { RouteObject } from 'react-router-dom';
import type { MenuItem } from '@/types/menu';

/**
 * 页面模块映射表类型
 * key 为模块路径，value 为动态 import 函数
 */
export type PageModuleMap = Record<
  string,
  () => Promise<{ default: React.ComponentType }>
>;

/**
 * 页面模块映射表
 * 使用 Vite 的 import.meta.glob 预扫描 pages 目录下所有 index.tsx 文件
 */
const pageModules: PageModuleMap = import.meta.glob(
  '../pages/**/index.tsx'
) as PageModuleMap;

/**
 * 根据菜单路径查找对应的页面组件懒加载函数
 * 菜单路径如 /system/user → 尝试匹配 ../pages/system/user/index.tsx
 *
 * @param menuPath - 菜单路由路径，如 /system/user
 * @param modules - 页面模块映射表
 * @returns 匹配到的模块加载函数，未找到返回 undefined
 */
function findPageModule(
  menuPath: string,
  modules: PageModuleMap
): (() => Promise<{ default: React.ComponentType }>) | undefined {
  // 去掉开头的斜杠，得到相对路径段
  const relativePath = menuPath.startsWith('/') ? menuPath.slice(1) : menuPath;
  // 构造 glob 匹配的 key，如 ../pages/system/user/index.tsx
  const moduleKey = `../pages/${relativePath}/index.tsx`;

  // 先尝试精确匹配（小写路径）
  if (modules[moduleKey]) {
    return modules[moduleKey];
  }

  // 再尝试大小写不敏感匹配（兼容 PascalCase 目录名）
  const lowerKey = moduleKey.toLowerCase();
  for (const key of Object.keys(modules)) {
    if (key.toLowerCase() === lowerKey) {
      return modules[key];
    }
  }

  return undefined;
}

/**
 * 为菜单项创建懒加载组件
 *
 * @param menuPath - 菜单路由路径
 * @param modules - 页面模块映射表
 * @returns React.lazy 包装的懒加载组件，未找到模块时返回 undefined
 */
function createLazyComponent(
  menuPath: string,
  modules: PageModuleMap
): React.LazyExoticComponent<React.ComponentType> | undefined {
  const moduleLoader = findPageModule(menuPath, modules);
  if (!moduleLoader) {
    return undefined;
  }
  return React.lazy(moduleLoader);
}

/**
 * 按 orderNum 升序排序菜单项
 *
 * @param menus - 待排序的菜单数组
 * @returns 排序后的新数组（不修改原数组）
 */
function sortMenus(menus: MenuItem[]): MenuItem[] {
  return [...menus].sort((a, b) => a.orderNum - b.orderNum);
}


/**
 * 将后端菜单数据递归转换为 React Router 6 的 RouteObject[]
 * 内部实现，接受模块映射表参数以支持测试注入
 *
 * 转换规则：
 * 1. 过滤掉 type=2（按钮）的菜单项
 * 2. type=0（目录）→ 路由分组，path 为目录路径，children 为子路由
 * 3. type=1（菜单）→ 懒加载页面路由，使用 React.lazy 实现代码分割
 * 4. 所有菜单按 orderNum 升序排列
 *
 * @param menus - 后端返回的菜单树数据
 * @param modules - 页面模块映射表（可选，默认使用 Vite glob 扫描结果）
 * @returns React Router 6 的 RouteObject 数组
 */
export function generateRoutes(
  menus: MenuItem[],
  modules: PageModuleMap = pageModules
): RouteObject[] {
  // 按排序号排序
  const sorted = sortMenus(menus);
  const routes: RouteObject[] = [];

  for (const menu of sorted) {
    // 跳过按钮类型和无路径的菜单项
    if (menu.type === 2 || !menu.path) {
      continue;
    }

    // 去掉路径开头的斜杠，React Router 嵌套路由使用相对路径
    const routePath = menu.path.startsWith('/')
      ? menu.path.slice(1)
      : menu.path;

    if (menu.type === 0) {
      // 目录类型：创建路由分组，递归处理子菜单
      const children = menu.children
        ? generateRoutes(menu.children, modules)
        : [];

      routes.push({
        path: routePath,
        children,
      });
    } else if (menu.type === 1) {
      // 菜单类型：创建懒加载页面路由
      const LazyComponent = createLazyComponent(menu.path, modules);

      if (LazyComponent) {
        routes.push({
          path: routePath,
          element: React.createElement(LazyComponent),
        });
      } else {
        // 页面组件未找到时，仍然生成路由占位（避免路由丢失）
        // 实际渲染时会显示空内容，开发阶段可通过控制台发现问题
        if (import.meta.env.DEV) {
          console.warn(
            `[dynamicRoutes] 未找到菜单 "${menu.name}" 对应的页面组件，路径: ${menu.path}`
          );
        }
        routes.push({
          path: routePath,
          element: null,
        });
      }
    }
  }

  return routes;
}
