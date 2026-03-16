/**
 * 动态路由生成单元测试
 * 验证 generateRoutes 函数的菜单到路由转换逻辑
 */
import { describe, it, expect } from 'vitest';
import type { MenuItem } from '@/types/menu';
import type { PageModuleMap } from '@/router/dynamicRoutes';
import { generateRoutes } from '@/router/dynamicRoutes';

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

/** 创建模拟的页面模块映射表 */
function createMockModules(paths: string[]): PageModuleMap {
  const modules: PageModuleMap = {};
  for (const p of paths) {
    const key = `../pages/${p}/index.tsx`;
    modules[key] = () =>
      Promise.resolve({ default: (() => null) as React.FC });
  }
  return modules;
}

describe('generateRoutes', () => {
  it('应为空菜单数组返回空路由', () => {
    const routes = generateRoutes([], {});
    expect(routes).toEqual([]);
  });

  it('应为 type=1（菜单）生成懒加载路由', () => {
    const menus: MenuItem[] = [
      createMenuItem({ path: '/system/user', type: 1 }),
    ];
    const modules = createMockModules(['system/user']);
    const routes = generateRoutes(menus, modules);

    expect(routes).toHaveLength(1);
    expect(routes[0].path).toBe('system/user');
    // 懒加载组件会生成 element
    expect(routes[0].element).not.toBeNull();
  });

  it('应为 type=0（目录）生成路由分组并递归处理子菜单', () => {
    const menus: MenuItem[] = [
      createMenuItem({
        id: 1,
        path: '/system',
        type: 0,
        name: '系统管理',
        children: [
          createMenuItem({ id: 2, parentId: 1, path: '/system/user', type: 1, name: '用户管理' }),
          createMenuItem({ id: 3, parentId: 1, path: '/system/role', type: 1, name: '角色管理' }),
        ],
      }),
    ];
    const modules = createMockModules(['system/user', 'system/role']);
    const routes = generateRoutes(menus, modules);

    expect(routes).toHaveLength(1);
    expect(routes[0].path).toBe('system');
    expect(routes[0].children).toHaveLength(2);
    expect(routes[0].children![0].path).toBe('system/user');
    expect(routes[0].children![1].path).toBe('system/role');
  });

  it('应完全过滤掉 type=2（按钮）菜单项', () => {
    const menus: MenuItem[] = [
      createMenuItem({ id: 1, path: '/system/user', type: 1, name: '用户管理' }),
      createMenuItem({ id: 2, path: '', type: 2, name: '新增用户', permissionKey: 'system:user:add' }),
      createMenuItem({ id: 3, path: '', type: 2, name: '删除用户', permissionKey: 'system:user:delete' }),
    ];
    const modules = createMockModules(['system/user']);
    const routes = generateRoutes(menus, modules);

    expect(routes).toHaveLength(1);
    expect(routes[0].path).toBe('system/user');
  });

  it('应过滤目录子菜单中的按钮项', () => {
    const menus: MenuItem[] = [
      createMenuItem({
        id: 1,
        path: '/system',
        type: 0,
        name: '系统管理',
        children: [
          createMenuItem({ id: 2, parentId: 1, path: '/system/user', type: 1, name: '用户管理' }),
          createMenuItem({ id: 3, parentId: 1, path: '', type: 2, name: '新增按钮' }),
        ],
      }),
    ];
    const modules = createMockModules(['system/user']);
    const routes = generateRoutes(menus, modules);

    expect(routes).toHaveLength(1);
    expect(routes[0].children).toHaveLength(1);
    expect(routes[0].children![0].path).toBe('system/user');
  });

  it('应按 orderNum 升序排列路由', () => {
    const menus: MenuItem[] = [
      createMenuItem({ id: 1, path: '/monitor', type: 1, name: '监控', orderNum: 3 }),
      createMenuItem({ id: 2, path: '/system', type: 1, name: '系统', orderNum: 1 }),
      createMenuItem({ id: 3, path: '/chat', type: 1, name: '聊天', orderNum: 2 }),
    ];
    const modules = createMockModules(['monitor', 'system', 'chat']);
    const routes = generateRoutes(menus, modules);

    expect(routes).toHaveLength(3);
    expect(routes[0].path).toBe('system');
    expect(routes[1].path).toBe('chat');
    expect(routes[2].path).toBe('monitor');
  });

  it('应对未找到页面组件的菜单生成 element=null 的占位路由', () => {
    const menus: MenuItem[] = [
      createMenuItem({ path: '/nonexistent/page', type: 1, name: '不存在的页面' }),
    ];
    // 空模块映射，不包含任何页面
    const routes = generateRoutes(menus, {});

    expect(routes).toHaveLength(1);
    expect(routes[0].path).toBe('nonexistent/page');
    expect(routes[0].element).toBeNull();
  });

  it('应支持大小写不敏感的模块路径匹配', () => {
    const menus: MenuItem[] = [
      createMenuItem({ path: '/system/user', type: 1 }),
    ];
    // 模拟 PascalCase 目录名
    const modules: PageModuleMap = {
      '../pages/System/User/index.tsx': () =>
        Promise.resolve({ default: (() => null) as React.FC }),
    };
    const routes = generateRoutes(menus, modules);

    expect(routes).toHaveLength(1);
    expect(routes[0].element).not.toBeNull();
  });

  it('应正确处理无子菜单的目录', () => {
    const menus: MenuItem[] = [
      createMenuItem({ path: '/empty', type: 0, name: '空目录' }),
    ];
    const routes = generateRoutes(menus, {});

    expect(routes).toHaveLength(1);
    expect(routes[0].path).toBe('empty');
    expect(routes[0].children).toEqual([]);
  });

  it('应正确处理多层嵌套菜单', () => {
    const menus: MenuItem[] = [
      createMenuItem({
        id: 1,
        path: '/system',
        type: 0,
        name: '系统管理',
        children: [
          createMenuItem({
            id: 2,
            parentId: 1,
            path: '/system/advanced',
            type: 0,
            name: '高级设置',
            children: [
              createMenuItem({
                id: 3,
                parentId: 2,
                path: '/system/advanced/config',
                type: 1,
                name: '系统配置',
              }),
            ],
          }),
        ],
      }),
    ];
    const modules = createMockModules(['system/advanced/config']);
    const routes = generateRoutes(menus, modules);

    expect(routes).toHaveLength(1);
    expect(routes[0].path).toBe('system');
    expect(routes[0].children).toHaveLength(1);
    expect(routes[0].children![0].path).toBe('system/advanced');
    expect(routes[0].children![0].children).toHaveLength(1);
    expect(routes[0].children![0].children![0].path).toBe('system/advanced/config');
  });

  it('应去掉路径开头的斜杠', () => {
    const menus: MenuItem[] = [
      createMenuItem({ path: '/dashboard', type: 1 }),
    ];
    const modules = createMockModules(['dashboard']);
    const routes = generateRoutes(menus, modules);

    expect(routes[0].path).toBe('dashboard');
  });

  it('应处理没有前导斜杠的路径', () => {
    const menus: MenuItem[] = [
      createMenuItem({ path: 'dashboard', type: 1 }),
    ];
    const modules = createMockModules(['dashboard']);
    const routes = generateRoutes(menus, modules);

    expect(routes[0].path).toBe('dashboard');
  });
});
