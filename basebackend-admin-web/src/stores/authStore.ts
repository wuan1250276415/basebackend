import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { RouteObject } from 'react-router-dom';
import { authApi } from '@/api/authApi';
import { menuApi } from '@/api/menuApi';
import type { UserInfo, MenuItem } from '@/types';

/**
 * 认证状态接口
 * 管理 token、用户信息、权限、角色、菜单和动态路由
 */
export interface AuthState {
  /** JWT 访问令牌 */
  token: string | null;
  /** 当前用户信息 */
  userInfo: UserInfo | null;
  /** 用户权限标识集合 */
  permissions: Set<string>;
  /** 用户角色标识集合 */
  roles: Set<string>;
  /** 后端返回的原始菜单数据 */
  menus: MenuItem[];
  /** 生成的动态路由（由 Task 7.1 实现 generateRoutes 后填充） */
  dynamicRoutes: RouteObject[];

  /** 用户登录：调用登录接口，存储 token，拉取用户信息 */
  login: (username: string, password: string) => Promise<void>;
  /** 用户登出：调用登出接口，清除状态，跳转登录页 */
  logout: () => Promise<void>;
  /** 拉取用户信息：获取权限、角色、菜单，生成动态路由 */
  fetchUserInfo: () => Promise<void>;
  /** 权限判断：支持通配符 `*:*:*` 和 `*` */
  hasPermission: (perm: string) => boolean;
  /** 重置状态到初始值 */
  reset: () => void;
}

/** 初始状态 */
const initialState = {
  token: null as string | null,
  userInfo: null as UserInfo | null,
  permissions: new Set<string>(),
  roles: new Set<string>(),
  menus: [] as MenuItem[],
  dynamicRoutes: [] as RouteObject[],
};

/**
 * 认证状态管理 Store
 * 使用 zustand persist 中间件持久化到 localStorage
 * key: 'auth-storage'（与 request.ts 中读取 token 的 key 保持一致）
 */
export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      ...initialState,

      login: async (username: string, password: string) => {
        // 调用登录接口
        const result = await authApi.login({ username, password });

        // 存储 token 和登录时返回的基本信息
        set({
          token: result.accessToken,
          userInfo: result.userInfo,
          permissions: new Set(result.permissions),
          roles: new Set(result.roles),
        });

        // 拉取完整用户信息（含菜单）
        await get().fetchUserInfo();
      },

      logout: async () => {
        try {
          await authApi.logout();
        } catch {
          // 登出接口失败不阻塞前端清理
        }

        // 清除所有状态
        set({ ...initialState, permissions: new Set(), roles: new Set() });

        // 跳转登录页
        window.location.href = '/login';
      },

      fetchUserInfo: async () => {
        // 获取用户上下文（含权限、角色）
        const userContext = await authApi.getUserInfo();

        // 获取用户菜单
        const menus = await menuApi.getByUser(userContext.userId);

        // TODO: Task 7.1 实现 generateRoutes 后替换
        const dynamicRoutes: RouteObject[] = [];

        set({
          userInfo: {
            userId: userContext.userId,
            username: userContext.username,
            nickname: userContext.nickname,
            email: userContext.email,
            phone: userContext.phone,
            avatar: userContext.avatar,
            gender: userContext.gender,
            deptId: userContext.deptId,
            deptName: userContext.deptName,
            userType: userContext.userType,
            status: userContext.status,
          },
          permissions: new Set(userContext.permissions),
          roles: new Set(userContext.roles),
          menus,
          dynamicRoutes,
        });
      },

      hasPermission: (perm: string) => {
        const { permissions } = get();

        // 超级管理员通配符：拥有所有权限
        if (permissions.has('*:*:*') || permissions.has('*')) {
          return true;
        }

        return permissions.has(perm);
      },

      reset: () => {
        set({ ...initialState, permissions: new Set(), roles: new Set() });
      },
    }),
    {
      name: 'auth-storage',
      /**
       * 自定义序列化/反序列化
       * Set 不能直接 JSON 序列化，需要转换为数组存储
       */
      storage: {
        getItem: (name: string) => {
          const raw = localStorage.getItem(name);
          if (!raw) return null;
          try {
            const parsed = JSON.parse(raw);
            // 将数组还原为 Set
            if (parsed?.state) {
              if (Array.isArray(parsed.state.permissions)) {
                parsed.state.permissions = new Set(parsed.state.permissions);
              }
              if (Array.isArray(parsed.state.roles)) {
                parsed.state.roles = new Set(parsed.state.roles);
              }
            }
            return parsed;
          } catch {
            return null;
          }
        },
        setItem: (name: string, value: unknown) => {
          const data = value as { state: Record<string, unknown> };
          // 将 Set 转换为数组以便 JSON 序列化
          const serializable = {
            ...data,
            state: {
              ...data.state,
              permissions: data.state.permissions instanceof Set
                ? Array.from(data.state.permissions)
                : data.state.permissions,
              roles: data.state.roles instanceof Set
                ? Array.from(data.state.roles)
                : data.state.roles,
              // 不持久化动态路由（包含函数，无法序列化）
              dynamicRoutes: [],
            },
          };
          localStorage.setItem(name, JSON.stringify(serializable));
        },
        removeItem: (name: string) => {
          localStorage.removeItem(name);
        },
      },
      /**
       * 只持久化必要的状态字段
       * dynamicRoutes 包含 React.lazy 组件，无法序列化
       */
      partialize: (state: AuthState) => ({
        token: state.token,
        userInfo: state.userInfo,
        permissions: state.permissions,
        roles: state.roles,
        menus: state.menus,
      }),
    },
  ),
);
