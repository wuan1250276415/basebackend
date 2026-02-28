/**
 * 动态路由生成属性基测试
 * 使用 fast-check 验证 generateRoutes 函数的核心属性
 */
import { describe, it, expect } from 'vitest';
import fc from 'fast-check';
import type { MenuItem } from '@/types/menu';
import type { PageModuleMap } from '@/router/dynamicRoutes';
import { generateRoutes } from '@/router/dynamicRoutes';
import type { RouteObject } from 'react-router-dom';

// Feature: admin-web-rebuild, Property 7: Dynamic route generation from menu data
// **Validates: Requirements 5.1**

/**
 * 生成随机路径段的 arbitrary
 * 路径段为 1-10 个小写字母
 */
const pathSegmentArb = fc.stringMatching(/^[a-z]{1,10}$/);

/**
 * 生成随机菜单路径的 arbitrary（1-3 段，以 / 开头）
 */
const menuPathArb = fc
  .array(pathSegmentArb, { minLength: 1, maxLength: 3 })
  .map((segments) => '/' + segments.join('/'));

/**
 * 生成随机菜单名称的 arbitrary
 */
const menuNameArb = fc.stringMatching(/^[\u4e00-\u9fa5a-zA-Z]{1,8}$/);

/**
 * 生成随机权限标识的 arbitrary
 */
const permKeyArb = fc
  .array(fc.stringMatching(/^[a-z]{2,6}$/), { minLength: 2, maxLength: 3 })
  .map((parts) => parts.join(':'));

/**
 * 生成包含混合类型（0、1、2）的扁平菜单数组 arbitrary
 * 确保 type=2 的按钮项路径为空
 */
const mixedMenuArrayArb: fc.Arbitrary<MenuItem[]> = fc
  .array(
    fc.record({
      id: fc.nat({ max: 10000 }),
      parentId: fc.constant(0),
      name: menuNameArb,
      icon: fc.constant('setting'),
      path: menuPathArb,
      permissionKey: permKeyArb,
      type: fc.oneof(fc.constant(0), fc.constant(1), fc.constant(2)),
      orderNum: fc.nat({ max: 100 }),
      status: fc.constant(1),
    }),
    { minLength: 0, maxLength: 15 },
  )
  .map((items) =>
    // 为 type=2（按钮）清空路径，按钮不需要路径；确保每项有唯一 id
    items.map((item, idx) => ({
      ...item,
      id: idx + 1,
      path: item.type === 2 ? '' : item.path,
    })),
  );


/**
 * 根据菜单数组创建对应的模拟模块映射表
 * 为所有 type=1 的菜单项创建模拟页面模块
 */
function createModulesForMenus(menus: MenuItem[]): PageModuleMap {
  const modules: PageModuleMap = {};
  for (const menu of menus) {
    if (menu.type === 1 && menu.path) {
      const relativePath = menu.path.startsWith('/') ? menu.path.slice(1) : menu.path;
      const key = `../pages/${relativePath}/index.tsx`;
      modules[key] = () => Promise.resolve({ default: (() => null) as React.FC });
    }
    if (menu.children) {
      Object.assign(modules, createModulesForMenus(menu.children));
    }
  }
  return modules;
}

/**
 * 递归统计菜单数组中 type=1（菜单）的数量
 */
function countType1Menus(menus: MenuItem[]): number {
  let count = 0;
  for (const menu of menus) {
    if (menu.type === 1) {
      count++;
    }
    if (menu.children) {
      count += countType1Menus(menu.children);
    }
  }
  return count;
}

/**
 * 递归统计路由数组中的叶子路由数量
 * 叶子路由：没有 children 属性或 children 为空的路由
 */
function countLeafRoutes(routes: RouteObject[]): number {
  let count = 0;
  for (const route of routes) {
    if (!route.children || route.children.length === 0) {
      count++;
    } else {
      count += countLeafRoutes(route.children);
    }
  }
  return count;
}

/**
 * 递归统计菜单中 type=0 且过滤 type=2 后无有效子菜单的空目录数
 */
function countEmptyDirs(menus: MenuItem[]): number {
  let count = 0;
  for (const menu of menus) {
    if (menu.type === 0) {
      const nonButtonChildren = (menu.children || []).filter((c) => c.type !== 2);
      if (nonButtonChildren.length === 0) {
        count++;
      }
    }
  }
  return count;
}

/**
 * 生成带有子菜单的目录+菜单+按钮混合树结构 arbitrary
 */
const menuTreeArb: fc.Arbitrary<MenuItem[]> = fc
  .array(
    fc.record({
      basePath: pathSegmentArb,
      leafCount: fc.nat({ max: 4 }),
      buttonCount: fc.nat({ max: 3 }),
      orderNum: fc.nat({ max: 100 }),
    }),
    { minLength: 0, maxLength: 5 },
  )
  .chain((dirs) => {
    if (dirs.length === 0) {
      return fc.constant([] as MenuItem[]);
    }
    // 为每个目录生成子菜单路径和按钮名称
    return fc
      .tuple(
        ...dirs.map((dir) =>
          fc.tuple(
            fc.array(pathSegmentArb, {
              minLength: dir.leafCount,
              maxLength: dir.leafCount || 1,
            }),
            fc.array(menuNameArb, {
              minLength: dir.buttonCount,
              maxLength: dir.buttonCount || 1,
            }),
          ),
        ),
      )
      .map((childrenData) => {
        let idCounter = 1;
        const menus: MenuItem[] = [];

        dirs.forEach((dir, dirIdx) => {
          const [leafPaths, buttonNames] = childrenData[dirIdx];
          const dirId = idCounter++;
          const children: MenuItem[] = [];

          // 添加 type=1 叶子菜单
          for (let i = 0; i < dir.leafCount && i < leafPaths.length; i++) {
            children.push({
              id: idCounter++,
              parentId: dirId,
              name: `菜单${i}`,
              icon: 'file',
              path: `/${dir.basePath}/${leafPaths[i]}`,
              permissionKey: `${dir.basePath}:${leafPaths[i]}:list`,
              type: 1,
              orderNum: i,
              status: 1,
            });
          }

          // 添加 type=2 按钮
          for (let i = 0; i < dir.buttonCount && i < buttonNames.length; i++) {
            children.push({
              id: idCounter++,
              parentId: dirId,
              name: buttonNames[i] || `按钮${i}`,
              icon: '',
              path: '',
              permissionKey: `${dir.basePath}:btn${i}`,
              type: 2,
              orderNum: 100 + i,
              status: 1,
            });
          }

          menus.push({
            id: dirId,
            parentId: 0,
            name: `目录${dirIdx}`,
            icon: 'folder',
            path: `/${dir.basePath}`,
            permissionKey: dir.basePath,
            type: 0,
            orderNum: dir.orderNum,
            status: 1,
            children,
          });
        });

        return menus;
      });
  });

describe('动态路由生成属性基测试', () => {
  // Feature: admin-web-rebuild, Property 7: Dynamic route generation from menu data
  describe('Property 7: 叶子路由数量等于 type=1 菜单数量', () => {
    it('对于任意扁平菜单数组，叶子路由数应等于 type=1 菜单项数加上空目录数', () => {
      fc.assert(
        fc.property(mixedMenuArrayArb, (menus) => {
          const modules = createModulesForMenus(menus);
          const routes = generateRoutes(menus, modules);

          const type1Count = countType1Menus(menus);
          const leafCount = countLeafRoutes(routes);

          // 扁平菜单中 type=0 无 children 属性，generateRoutes 会生成 children=[] 的路由
          // 这些空目录路由也是叶子路由
          const type0Count = menus.filter((m) => m.type === 0).length;

          expect(leafCount).toBe(type1Count + type0Count);
        }),
        { numRuns: 20 },
      );
    });
  });

  // Feature: admin-web-rebuild, Property 7: Dynamic route generation from menu data
  describe('Property 7: type=2 按钮不产生任何路由', () => {
    it('对于任意菜单数组，生成的路由总数应等于非按钮菜单项数', () => {
      fc.assert(
        fc.property(mixedMenuArrayArb, (menus) => {
          const modules = createModulesForMenus(menus);
          const routes = generateRoutes(menus, modules);

          // 路由总数不应包含 type=2 的项
          const nonButtonCount = menus.filter((m) => m.type !== 2).length;
          expect(routes.length).toBe(nonButtonCount);
        }),
        { numRuns: 20 },
      );
    });
  });

  // Feature: admin-web-rebuild, Property 7: Dynamic route generation from menu data
  describe('Property 7: 树形菜单的叶子路由数等于 type=1 总数加空目录数', () => {
    it('对于任意树形菜单结构，递归叶子路由数应等于所有 type=1 菜单项总数加上空目录数', () => {
      fc.assert(
        fc.property(menuTreeArb, (menus) => {
          const modules = createModulesForMenus(menus);
          const routes = generateRoutes(menus, modules);

          const type1Count = countType1Menus(menus);
          const leafCount = countLeafRoutes(routes);
          const emptyDirCount = countEmptyDirs(menus);

          expect(leafCount).toBe(type1Count + emptyDirCount);
        }),
        { numRuns: 20 },
      );
    });
  });

  // Feature: admin-web-rebuild, Property 7: Dynamic route generation from menu data
  describe('Property 7: type=0 目录映射为包含 children 的路由组', () => {
    it('对于任意菜单数组，每个 type=0 目录应映射为一个带 children 属性的路由', () => {
      fc.assert(
        fc.property(mixedMenuArrayArb, (menus) => {
          const modules = createModulesForMenus(menus);
          const routes = generateRoutes(menus, modules);

          // 收集所有 type=0 目录的路径（去掉前导斜杠）
          const dirPaths = menus
            .filter((m) => m.type === 0)
            .map((m) => (m.path.startsWith('/') ? m.path.slice(1) : m.path));

          // 验证每个目录路径对应的路由都有 children 属性
          for (const dirPath of dirPaths) {
            const route = routes.find((r) => r.path === dirPath);
            if (route) {
              expect(route).toHaveProperty('children');
              expect(Array.isArray(route.children)).toBe(true);
            }
          }
        }),
        { numRuns: 20 },
      );
    });
  });

  // Feature: admin-web-rebuild, Property 7: Dynamic route generation from menu data
  describe('Property 7: type=1 菜单映射为带 element 的叶子路由', () => {
    it('对于任意菜单数组，每个 type=1 菜单应映射为一个带 element 的路由（无 children）', () => {
      fc.assert(
        fc.property(mixedMenuArrayArb, (menus) => {
          const modules = createModulesForMenus(menus);
          const routes = generateRoutes(menus, modules);

          // 收集所有 type=1 菜单的路径（去掉前导斜杠）
          const menuPaths = menus
            .filter((m) => m.type === 1)
            .map((m) => (m.path.startsWith('/') ? m.path.slice(1) : m.path));

          // 验证每个菜单路径对应的路由有 element 且无 children
          for (const menuPath of menuPaths) {
            const route = routes.find((r) => r.path === menuPath);
            if (route) {
              expect(route).toHaveProperty('element');
              expect(route.children).toBeUndefined();
            }
          }
        }),
        { numRuns: 20 },
      );
    });
  });
});

// Feature: admin-web-rebuild, Property 5: Sidebar menu generation from AuthStore menus
// **Validates: Requirements 4.4**

import { convertMenus } from '@/layouts/BasicLayout';
import type { MenuDataItem } from '@ant-design/pro-components';

/**
 * 递归统计 MenuDataItem 数组中的所有节点数量（包含子节点）
 */
function countMenuDataItems(items: MenuDataItem[]): number {
  let count = 0;
  for (const item of items) {
    count++;
    if (item.children) {
      count += countMenuDataItems(item.children);
    }
  }
  return count;
}

/**
 * 递归统计 MenuItem 数组中 type=0 和 type=1 的节点总数
 */
function countNonButtonItems(menus: MenuItem[]): number {
  let count = 0;
  for (const menu of menus) {
    if (menu.type === 0 || menu.type === 1) {
      count++;
    }
    if (menu.children) {
      count += countNonButtonItems(menu.children);
    }
  }
  return count;
}

/**
 * 递归收集 MenuDataItem 数组中所有节点的 path
 */
function collectPaths(items: MenuDataItem[]): string[] {
  const paths: string[] = [];
  for (const item of items) {
    if (item.path) paths.push(item.path);
    if (item.children) {
      paths.push(...collectPaths(item.children));
    }
  }
  return paths;
}

/**
 * 递归收集 MenuItem 数组中 type=0 和 type=1 的 path
 */
function collectNonButtonPaths(menus: MenuItem[]): string[] {
  const paths: string[] = [];
  for (const menu of menus) {
    if ((menu.type === 0 || menu.type === 1) && menu.path) {
      paths.push(menu.path);
    }
    if (menu.children) {
      paths.push(...collectNonButtonPaths(menu.children));
    }
  }
  return paths;
}

/**
 * 生成带有子菜单的混合树结构 arbitrary（用于 Property 5）
 * 包含 type=0（目录）、type=1（菜单）、type=2（按钮）
 */
const sidebarMenuTreeArb: fc.Arbitrary<MenuItem[]> = fc
  .array(
    fc.record({
      basePath: pathSegmentArb,
      menuCount: fc.nat({ max: 4 }),
      buttonCount: fc.nat({ max: 3 }),
      orderNum: fc.nat({ max: 100 }),
    }),
    { minLength: 0, maxLength: 5 },
  )
  .chain((dirs) => {
    if (dirs.length === 0) {
      return fc.constant([] as MenuItem[]);
    }
    return fc
      .tuple(
        ...dirs.map((dir) =>
          fc.tuple(
            fc.array(pathSegmentArb, {
              minLength: dir.menuCount,
              maxLength: dir.menuCount || 1,
            }),
            fc.array(menuNameArb, {
              minLength: dir.buttonCount,
              maxLength: dir.buttonCount || 1,
            }),
            fc.array(fc.nat({ max: 100 }), {
              minLength: dir.menuCount,
              maxLength: dir.menuCount || 1,
            }),
          ),
        ),
      )
      .map((childrenData) => {
        let idCounter = 1;
        const menus: MenuItem[] = [];

        dirs.forEach((dir, dirIdx) => {
          const [leafPaths, buttonNames, leafOrders] = childrenData[dirIdx];
          const dirId = idCounter++;
          const children: MenuItem[] = [];

          // 添加 type=1 菜单子项
          for (let i = 0; i < dir.menuCount && i < leafPaths.length; i++) {
            children.push({
              id: idCounter++,
              parentId: dirId,
              name: `菜单${i}`,
              icon: 'file',
              path: `/${dir.basePath}/${leafPaths[i]}`,
              permissionKey: `${dir.basePath}:${leafPaths[i]}:list`,
              type: 1,
              orderNum: leafOrders[i] ?? i,
              status: 1,
            });
          }

          // 添加 type=2 按钮子项
          for (let i = 0; i < dir.buttonCount && i < buttonNames.length; i++) {
            children.push({
              id: idCounter++,
              parentId: dirId,
              name: buttonNames[i] || `按钮${i}`,
              icon: '',
              path: '',
              permissionKey: `${dir.basePath}:btn${i}`,
              type: 2,
              orderNum: 200 + i,
              status: 1,
            });
          }

          menus.push({
            id: dirId,
            parentId: 0,
            name: `目录${dirIdx}`,
            icon: 'folder',
            path: `/${dir.basePath}`,
            permissionKey: dir.basePath,
            type: 0,
            orderNum: dir.orderNum,
            status: 1,
            children,
          });
        });

        return menus;
      });
  });

/**
 * 生成扁平的混合类型菜单数组 arbitrary（用于 Property 5 扁平场景）
 */
const flatMixedMenuArb: fc.Arbitrary<MenuItem[]> = fc
  .array(
    fc.record({
      id: fc.nat({ max: 10000 }),
      parentId: fc.constant(0),
      name: menuNameArb,
      icon: fc.constant('setting'),
      path: menuPathArb,
      permissionKey: permKeyArb,
      type: fc.oneof(fc.constant(0), fc.constant(1), fc.constant(2)) as fc.Arbitrary<number>,
      orderNum: fc.nat({ max: 100 }),
      status: fc.constant(1),
    }),
    { minLength: 0, maxLength: 15 },
  )
  .map((items) =>
    items.map((item, idx) => ({
      ...item,
      id: idx + 1,
      // 按钮不需要路径
      path: item.type === 2 ? '' : item.path,
    })),
  );

describe('侧边栏菜单生成属性基测试', () => {
  // Feature: admin-web-rebuild, Property 5: Sidebar menu generation from AuthStore menus
  describe('Property 5: 输出不包含 type=2 按钮项', () => {
    it('对于任意菜单数组，convertMenus 输出的节点总数应等于输入中 type=0 和 type=1 的节点总数', () => {
      fc.assert(
        fc.property(sidebarMenuTreeArb, (menus) => {
          const result = convertMenus(menus);
          const outputCount = countMenuDataItems(result);
          const expectedCount = countNonButtonItems(menus);
          expect(outputCount).toBe(expectedCount);
        }),
        { numRuns: 20 },
      );
    });
  });

  // Feature: admin-web-rebuild, Property 5: Sidebar menu generation from AuthStore menus
  describe('Property 5: 扁平菜单中 type=2 被过滤', () => {
    it('对于任意扁平菜单数组，convertMenus 输出长度应等于 type!=2 的项数', () => {
      fc.assert(
        fc.property(flatMixedMenuArb, (menus) => {
          const result = convertMenus(menus);
          const expectedCount = menus.filter((m) => m.type !== 2).length;
          expect(result.length).toBe(expectedCount);
        }),
        { numRuns: 20 },
      );
    });
  });

  // Feature: admin-web-rebuild, Property 5: Sidebar menu generation from AuthStore menus
  describe('Property 5: 层级结构保持一致', () => {
    it('对于任意树形菜单，convertMenus 输出的路径集合应等于输入中 type=0 和 type=1 的路径集合', () => {
      fc.assert(
        fc.property(sidebarMenuTreeArb, (menus) => {
          const result = convertMenus(menus);
          const outputPaths = new Set(collectPaths(result));
          const expectedPaths = new Set(collectNonButtonPaths(menus));
          expect(outputPaths).toEqual(expectedPaths);
        }),
        { numRuns: 20 },
      );
    });
  });

  // Feature: admin-web-rebuild, Property 5: Sidebar menu generation from AuthStore menus
  describe('Property 5: 排序顺序按 orderNum 升序', () => {
    it('对于任意菜单数组，convertMenus 输出的顶层项应按 orderNum 升序排列', () => {
      fc.assert(
        fc.property(sidebarMenuTreeArb, (menus) => {
          const result = convertMenus(menus);

          // 获取输入中非按钮项的 orderNum 排序后的 path 顺序
          const sortedInputPaths = menus
            .filter((m) => m.type === 0 || m.type === 1)
            .sort((a, b) => a.orderNum - b.orderNum)
            .map((m) => m.path);

          const outputPaths = result.map((item) => item.path);
          expect(outputPaths).toEqual(sortedInputPaths);
        }),
        { numRuns: 20 },
      );
    });
  });

  // Feature: admin-web-rebuild, Property 5: Sidebar menu generation from AuthStore menus
  describe('Property 5: 子菜单排序顺序按 orderNum 升序', () => {
    it('对于任意树形菜单，每个目录的子菜单应按 orderNum 升序排列', () => {
      fc.assert(
        fc.property(sidebarMenuTreeArb, (menus) => {
          const result = convertMenus(menus);

          // 验证每个有 children 的节点，其 children 的 path 顺序与按 orderNum 排序后一致
          for (let i = 0; i < menus.length; i++) {
            const menu = menus[i];
            if (menu.type === 0 && menu.children && menu.children.length > 0) {
              const expectedChildPaths = menu.children
                .filter((c) => c.type === 0 || c.type === 1)
                .sort((a, b) => a.orderNum - b.orderNum)
                .map((c) => c.path);

              const resultItem = result.find((r) => r.path === menu.path);
              if (resultItem && resultItem.children) {
                const actualChildPaths = resultItem.children.map((c) => c.path);
                expect(actualChildPaths).toEqual(expectedChildPaths);
              }
            }
          }
        }),
        { numRuns: 20 },
      );
    });
  });
});
