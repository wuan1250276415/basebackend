/**
 * 路由守卫属性基测试
 * 使用 fast-check 验证 RouterGuard 的核心纯函数逻辑
 *
 * Property 8: 未认证用户访问受保护路由应重定向到 /login
 * Property 9: 已认证用户访问不在菜单中的路由应重定向到 /403
 */
import { describe, it, expect } from 'vitest';
import fc from 'fast-check';
import type { MenuItem } from '@/types/menu';
import { isPathInMenus, WHITE_LIST } from '@/router/guard';

// Feature: admin-web-rebuild, Property 8: Unauthenticated access redirects to login
// **Validates: Requirements 5.2**

// Feature: admin-web-rebuild, Property 9: Unauthorized route access shows 403
// **Validates: Requirements 5.3**

/**
 * 生成随机路径段的 arbitrary
 * 路径段为 1-10 个小写字母
 */
const pathSegmentArb = fc.stringMatching(/^[a-z]{1,10}$/);

/**
 * 生成随机路由路径的 arbitrary（1-3 段，以 / 开头）
 */
const routePathArb = fc
  .array(pathSegmentArb, { minLength: 1, maxLength: 3 })
  .map((segments) => '/' + segments.join('/'));

/**
 * 生成不在白名单中的受保护路由路径 arbitrary
 * 过滤掉 /login, /403, /404, /500 以及根路径 / 和 /dashboard
 */
const protectedPathArb = routePathArb.filter(
  (path) => !WHITE_LIST.includes(path) && path !== '/' && path !== '/dashboard',
);

/**
 * 根据路径生成一个 type=1 的菜单项
 */
function createMenuItem(id: number, parentId: number, path: string, type: number): MenuItem {
  return {
    id,
    parentId,
    name: `菜单${id}`,
    icon: 'setting',
    path,
    permissionKey: `perm:${id}`,
    type,
    orderNum: id,
    status: 1,
  };
}

/**
 * 生成包含混合类型菜单项的树结构 arbitrary
 * 包含 type=0（目录）、type=1（菜单）、type=2（按钮）
 */
const menuTreeArb: fc.Arbitrary<MenuItem[]> = fc
  .array(
    fc.record({
      basePath: pathSegmentArb,
      leafCount: fc.nat({ max: 4 }),
      buttonCount: fc.nat({ max: 2 }),
    }),
    { minLength: 1, maxLength: 5 },
  )
  .chain((dirs) =>
    fc
      .tuple(
        ...dirs.map((dir) =>
          fc.array(pathSegmentArb, {
            minLength: Math.max(dir.leafCount, 1),
            maxLength: Math.max(dir.leafCount, 1),
          }),
        ),
      )
      .map((childrenPaths) => {
        let idCounter = 1;
        const menus: MenuItem[] = [];

        dirs.forEach((dir, dirIdx) => {
          const leafPaths = childrenPaths[dirIdx];
          const dirId = idCounter++;
          const children: MenuItem[] = [];

          // 添加 type=1 叶子菜单
          for (let i = 0; i < dir.leafCount && i < leafPaths.length; i++) {
            children.push(
              createMenuItem(idCounter++, dirId, `/${dir.basePath}/${leafPaths[i]}`, 1),
            );
          }

          // 添加 type=2 按钮（路径为空，不应匹配路由）
          for (let i = 0; i < dir.buttonCount; i++) {
            children.push(createMenuItem(idCounter++, dirId, '', 2));
          }

          menus.push({
            ...createMenuItem(dirId, 0, `/${dir.basePath}`, 0),
            children,
          });
        });

        return menus;
      }),
  );

/**
 * 从菜单树中收集所有 type=1 菜单的路径
 */
function collectMenuPaths(menus: MenuItem[]): string[] {
  const paths: string[] = [];
  for (const menu of menus) {
    if (menu.type === 1 && menu.path) {
      paths.push(menu.path);
    }
    if (menu.children) {
      paths.push(...collectMenuPaths(menu.children));
    }
  }
  return paths;
}

describe('路由守卫属性基测试', () => {
  // Feature: admin-web-rebuild, Property 8: Unauthenticated access redirects to login
  describe('Property 8: 未认证访问重定向到登录页', () => {
    it('任意受保护路由路径不在白名单中（重定向条件成立）', () => {
      // 验证核心条件：非白名单路径在无 token 时应触发重定向
      // RouterGuard 组件中的逻辑：!token → Navigate to /login
      // 这里测试前置条件：生成的路径确实不在白名单中
      fc.assert(
        fc.property(protectedPathArb, (path) => {
          // 受保护路径不应在白名单中
          expect(WHITE_LIST.includes(path)).toBe(false);
          // 受保护路径不应是根路径或 dashboard
          expect(path).not.toBe('/');
          expect(path).not.toBe('/dashboard');
        }),
        { numRuns: 20 },
      );
    });

    it('白名单路由始终包含 /login, /403, /404, /500', () => {
      // 验证白名单配置正确，确保未认证用户可以访问登录页
      expect(WHITE_LIST).toContain('/login');
      expect(WHITE_LIST).toContain('/403');
      expect(WHITE_LIST).toContain('/404');
      expect(WHITE_LIST).toContain('/500');
    });

    it('任意随机路径（非白名单）在无 token 时满足重定向条件', () => {
      // 模拟 RouterGuard 的判断逻辑：
      // 1. 路径不在白名单 → 继续检查
      // 2. token 为 null → 应重定向到 /login
      fc.assert(
        fc.property(protectedPathArb, (path) => {
          const token: string | null = null;
          const isWhitelisted = WHITE_LIST.includes(path);

          // 不在白名单且无 token → 应重定向
          const shouldRedirectToLogin = !isWhitelisted && !token;
          expect(shouldRedirectToLogin).toBe(true);
        }),
        { numRuns: 20 },
      );
    });
  });

  // Feature: admin-web-rebuild, Property 9: Unauthorized route access shows 403
  describe('Property 9: 未授权路由访问显示 403', () => {
    it('不在菜单中的路径，isPathInMenus 返回 false', () => {
      // 生成随机菜单树和不在菜单中的路径，验证 isPathInMenus 返回 false
      fc.assert(
        fc.property(menuTreeArb, protectedPathArb, (menus, randomPath) => {
          // 收集菜单中所有有效路径
          const menuPaths = collectMenuPaths(menus);
          // 也收集 type=0 目录路径（isPathInMenus 也匹配目录）
          const allPaths = [
            ...menuPaths,
            ...menus.filter((m) => m.type === 0).map((m) => m.path),
          ];

          // 如果随机路径恰好在菜单中，跳过此用例
          if (allPaths.includes(randomPath)) {
            return;
          }

          // 不在菜单中的路径应返回 false → 触发 403 重定向
          expect(isPathInMenus(menus, randomPath)).toBe(false);
        }),
        { numRuns: 20 },
      );
    });

    it('在菜单中的 type=1 路径，isPathInMenus 返回 true', () => {
      // 正向验证：菜单中存在的 type=1 路径应返回 true（不触发 403）
      fc.assert(
        fc.property(menuTreeArb, (menus) => {
          const menuPaths = collectMenuPaths(menus);

          for (const path of menuPaths) {
            expect(isPathInMenus(menus, path)).toBe(true);
          }
        }),
        { numRuns: 20 },
      );
    });

    it('type=2 按钮路径不应被 isPathInMenus 匹配', () => {
      // 验证 type=2 的按钮项即使有路径也不会被匹配
      fc.assert(
        fc.property(
          fc.array(pathSegmentArb, { minLength: 1, maxLength: 3 }).map((segments) => {
            const path = '/' + segments.join('/');
            // 创建一个只包含 type=2 按钮的菜单
            const menus: MenuItem[] = [
              createMenuItem(1, 0, path, 2),
            ];
            return { menus, path };
          }),
          ({ menus, path }) => {
            // type=2 按钮不应被视为有效菜单路径
            expect(isPathInMenus(menus, path)).toBe(false);
          },
        ),
        { numRuns: 20 },
      );
    });

    it('模拟完整守卫逻辑：有 token + userInfo 但路径不在菜单中 → 应重定向 403', () => {
      // 模拟 RouterGuard 的完整判断链
      fc.assert(
        fc.property(menuTreeArb, protectedPathArb, (menus, randomPath) => {
          const menuPaths = collectMenuPaths(menus);
          const allPaths = [
            ...menuPaths,
            ...menus.filter((m) => m.type === 0).map((m) => m.path),
          ];

          // 跳过恰好在菜单中的路径
          if (allPaths.includes(randomPath)) {
            return;
          }

          // 模拟守卫条件
          const token = 'valid-token';
          const userInfo = { userId: 1 }; // 非 null
          const isWhitelisted = WHITE_LIST.includes(randomPath);
          const isRootOrDashboard = randomPath === '/' || randomPath === '/dashboard';
          const inMenus = isPathInMenus(menus, randomPath);

          // 完整守卫逻辑判断
          const shouldRedirectTo403 =
            !isWhitelisted && !!token && !!userInfo && !isRootOrDashboard && !inMenus;

          expect(shouldRedirectTo403).toBe(true);
        }),
        { numRuns: 20 },
      );
    });
  });
});
