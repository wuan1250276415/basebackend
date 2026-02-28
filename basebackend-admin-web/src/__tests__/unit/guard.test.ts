/**
 * RouterGuard 单元测试
 * 验证路由守卫的核心逻辑：白名单判断、菜单路径匹配
 */
import { describe, it, expect } from 'vitest';
import type { MenuItem } from '@/types/menu';
import { isPathInMenus, WHITE_LIST } from '@/router/guard';

/** 创建测试用的菜单项 */
function createMenuItem(overrides: Partial<MenuItem> = {}): MenuItem {
  return {
    id: 1,
    parentId: 0,
    name: '测试菜单',
    icon: 'setting',
    path: '/test',
    permissionKey: 'test',
    type: 1,
    orderNum: 1,
    status: 1,
    ...overrides,
  };
}

describe('WHITE_LIST', () => {
  it('应包含 /login, /403, /404, /500', () => {
    expect(WHITE_LIST).toContain('/login');
    expect(WHITE_LIST).toContain('/403');
    expect(WHITE_LIST).toContain('/404');
    expect(WHITE_LIST).toContain('/500');
  });
});

describe('isPathInMenus', () => {
  it('应在空菜单中返回 false', () => {
    expect(isPathInMenus([], '/system/user')).toBe(false);
  });

  it('应匹配顶层菜单路径（type=1）', () => {
    const menus: MenuItem[] = [
      createMenuItem({ path: '/dashboard', type: 1 }),
    ];
    expect(isPathInMenus(menus, '/dashboard')).toBe(true);
  });

  it('应匹配目录路径（type=0）', () => {
    const menus: MenuItem[] = [
      createMenuItem({ path: '/system', type: 0 }),
    ];
    expect(isPathInMenus(menus, '/system')).toBe(true);
  });

  it('应排除按钮类型（type=2）', () => {
    const menus: MenuItem[] = [
      createMenuItem({ path: '/system/user/add', type: 2, permissionKey: 'system:user:add' }),
    ];
    expect(isPathInMenus(menus, '/system/user/add')).toBe(false);
  });

  it('应递归搜索嵌套菜单', () => {
    const menus: MenuItem[] = [
      createMenuItem({
        id: 1,
        path: '/system',
        type: 0,
        children: [
          createMenuItem({ id: 2, parentId: 1, path: '/system/user', type: 1 }),
          createMenuItem({ id: 3, parentId: 1, path: '/system/role', type: 1 }),
        ],
      }),
    ];
    expect(isPathInMenus(menus, '/system/user')).toBe(true);
    expect(isPathInMenus(menus, '/system/role')).toBe(true);
    expect(isPathInMenus(menus, '/system/dept')).toBe(false);
  });

  it('应在深层嵌套中找到菜单', () => {
    const menus: MenuItem[] = [
      createMenuItem({
        id: 1,
        path: '/system',
        type: 0,
        children: [
          createMenuItem({
            id: 2,
            parentId: 1,
            path: '/system/advanced',
            type: 0,
            children: [
              createMenuItem({ id: 3, parentId: 2, path: '/system/advanced/config', type: 1 }),
            ],
          }),
        ],
      }),
    ];
    expect(isPathInMenus(menus, '/system/advanced/config')).toBe(true);
  });

  it('应对不存在的路径返回 false', () => {
    const menus: MenuItem[] = [
      createMenuItem({ path: '/dashboard', type: 1 }),
    ];
    expect(isPathInMenus(menus, '/nonexistent')).toBe(false);
  });

  it('应忽略嵌套在菜单中的按钮项', () => {
    const menus: MenuItem[] = [
      createMenuItem({
        id: 1,
        path: '/system',
        type: 0,
        children: [
          createMenuItem({ id: 2, parentId: 1, path: '/system/user', type: 1 }),
          createMenuItem({ id: 3, parentId: 1, path: '/system/user/add', type: 2 }),
        ],
      }),
    ];
    expect(isPathInMenus(menus, '/system/user')).toBe(true);
    expect(isPathInMenus(menus, '/system/user/add')).toBe(false);
  });
});
