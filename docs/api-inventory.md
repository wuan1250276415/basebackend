# BaseBackend API 接口清单

> 供 admin-web 前端对接使用

## 一、user-api 服务（basebackend-user-api）

### 1. 认证模块 AuthController `/api/user/auth`
| 方法 | 路径 | 功能 |
|------|------|------|
| POST | `/api/user/auth/login` | 用户登录 |
| POST | `/api/user/auth/logout` | 用户登出 |
| POST | `/api/user/auth/refresh` | 刷新Token |
| GET | `/api/user/auth/info` | 获取当前用户信息（含权限、角色） |
| PUT | `/api/user/auth/password` | 修改密码 |
| POST | `/api/user/auth/wechat-login` | 微信登录 |

### 2. 用户管理 UserController `/api/user/users`
| 方法 | 路径 | 功能 |
|------|------|------|
| GET | `/api/user/users` | 用户列表（分页） |
| GET | `/api/user/users/{id}` | 用户详情 |
| POST | `/api/user/users` | 新增用户 |
| PUT | `/api/user/users/{id}` | 编辑用户 |
| DELETE | `/api/user/users/{id}` | 删除用户 |
| DELETE | `/api/user/users/batch` | 批量删除 |
| PUT | `/api/user/users/{id}/reset-password` | 重置密码 |
| PUT | `/api/user/users/{id}/roles` | 分配角色 |
| PUT | `/api/user/users/{id}/status` | 启用/禁用 |
| GET | `/api/user/users/export` | 导出用户 |
| GET | `/api/user/users/{id}/roles` | 获取用户角色 |
| GET | `/api/user/users/check-username` | 校验用户名 |
| GET | `/api/user/users/check-email` | 校验邮箱 |
| GET | `/api/user/users/check-phone` | 校验手机号 |
| GET | `/api/user/users/by-username` | 按用户名查询 |
| GET | `/api/user/users/by-phone` | 按手机号查询 |
| GET | `/api/user/users/by-email` | 按邮箱查询 |
| GET | `/api/user/users/batch` | 批量查询 |
| GET | `/api/user/users/by-dept` | 按部门查询 |
| GET | `/api/user/users/active-ids` | 活跃用户ID |

### 3. 角色管理 RoleController `/api/user/roles`
| 方法 | 路径 | 功能 |
|------|------|------|
| GET | `/api/user/roles` | 角色列表 |
| GET | `/api/user/roles/{id}` | 角色详情 |
| POST | `/api/user/roles` | 新增角色 |
| PUT | `/api/user/roles/{id}` | 编辑角色 |
| DELETE | `/api/user/roles/{id}` | 删除角色 |
| PUT | `/api/user/roles/{id}/menus` | 分配菜单权限 |
| PUT | `/api/user/roles/{id}/permissions` | 分配权限 |
| GET | `/api/user/roles/{id}/menus` | 获取角色菜单 |
| GET | `/api/user/roles/{id}/permissions` | 获取角色权限 |
| GET | `/api/user/roles/check-role-name` | 校验角色名 |
| GET | `/api/user/roles/check-role-key` | 校验角色Key |
| GET | `/api/user/roles/tree` | 角色树 |
| GET | `/api/user/roles/{id}/users` | 角色下的用户 |
| POST | `/api/user/roles/{id}/users` | 批量分配用户 |
| DELETE | `/api/user/roles/{roleId}/users/{userId}` | 取消分配用户 |
| PUT | `/api/user/roles/{id}/resources` | 分配资源权限 |
| GET | `/api/user/roles/{id}/resources` | 获取角色资源 |
| PUT | `/api/user/roles/{id}/list-operations` | 分配列表操作权限 |
| GET | `/api/user/roles/{id}/list-operations` | 获取列表操作权限 |
| PUT | `/api/user/roles/{id}/data-permissions` | 数据权限 |

### 4. 个人中心 ProfileController `/api/user/profile`
| 方法 | 路径 | 功能 |
|------|------|------|
| GET | `/api/user/profile/info` | 获取个人信息 |
| PUT | `/api/user/profile/info` | 修改个人信息 |
| PUT | `/api/user/profile/password` | 修改密码 |

### 5. 偏好设置 PreferenceController `/api/user/preference`
| 方法 | 路径 | 功能 |
|------|------|------|
| GET | `/api/user/preference` | 获取偏好设置 |
| PUT | `/api/user/preference` | 更新偏好设置 |

### 6. 安全管理 SecurityController `/api/user/security`
| 方法 | 路径 | 功能 |
|------|------|------|
| GET | `/api/user/security/devices` | 登录设备列表 |
| DELETE | `/api/user/security/devices/{deviceId}` | 移除设备 |
| PUT | `/api/user/security/devices/{deviceId}/trust` | 信任设备 |
| GET | `/api/user/security/operation-logs` | 安全操作日志 |
| GET | `/api/user/security/2fa` | 获取2FA状态 |
| POST | `/api/user/security/2fa/enable` | 启用2FA |
| POST | `/api/user/security/2fa/disable` | 禁用2FA |

---

## 二、system-api 服务（basebackend-system-api）

### 7. 菜单/资源管理 ApplicationResourceController `/api/system/application/resource`
| 方法 | 路径 | 功能 |
|------|------|------|
| GET | `/api/system/application/resource/tree` | 菜单树 |
| GET | `/api/system/application/resource` | 资源列表 |
| GET | `/api/system/application/resource/routes` | 路由列表 |
| GET | `/api/system/application/resource/user/{userId}` | 用户菜单 |
| GET | `/api/system/application/resource/check-menu-name` | 校验菜单名 |
| PUT | `/api/system/application/resource/{id}` | 编辑资源 |
| GET | `/api/system/application/resource/tree/{appId}` | 应用菜单树 |
| GET | `/api/system/application/resource/user/tree/{appId}` | 用户应用菜单树 |
| GET | `/api/system/application/resource/{id}` | 资源详情 |
| POST | `/api/system/application/resource` | 新增资源 |
| PUT | `/api/system/application/resource` | 更新资源 |
| DELETE | `/api/system/application/resource/{id}` | 删除资源 |
| GET | `/api/system/application/resource/role/{roleId}` | 角色资源 |
| POST | `/api/system/application/resource/role/{roleId}/assign` | 分配角色资源 |
| GET | `/api/system/application/resource/current-user` | 当前用户资源 |

### 8. 部门管理 DeptController `/api/system/depts`
| 方法 | 路径 | 功能 |
|------|------|------|
| GET | `/api/system/depts/tree` | 部门树 |
| GET | `/api/system/depts` | 部门列表 |
| GET | `/api/system/depts/{id}` | 部门详情 |
| POST | `/api/system/depts` | 新增部门 |
| PUT | `/api/system/depts/{id}` | 编辑部门 |
| DELETE | `/api/system/depts/{id}` | 删除部门 |
| GET | `/api/system/depts/{id}/children` | 子部门 |
| GET | `/api/system/depts/{id}/children-ids` | 子部门ID |
| GET | `/api/system/depts/check-dept-name` | 校验部门名 |
| GET | `/api/system/depts/by-name` | 按名称查询 |
| GET | `/api/system/depts/by-code` | 按编码查询 |
| GET | `/api/system/depts/batch` | 批量查询 |
| GET | `/api/system/depts/by-parent` | 按父部门查询 |

### 9. 字典管理 DictController `/api/system/dicts`
| 方法 | 路径 | 功能 |
|------|------|------|
| GET | `/api/system/dicts` | 字典类型列表 |
| GET | `/api/system/dicts/{id}` | 字典类型详情 |
| POST | `/api/system/dicts` | 新增字典类型 |
| PUT | `/api/system/dicts/{id}` | 编辑字典类型 |
| DELETE | `/api/system/dicts/{id}` | 删除字典类型 |
| GET | `/api/system/dicts/data/type/{dictType}` | 按类型获取字典数据 |
| GET | `/api/system/dicts/data` | 字典数据列表 |
| GET | `/api/system/dicts/data/{id}` | 字典数据详情 |
| POST | `/api/system/dicts/data` | 新增字典数据 |
| PUT | `/api/system/dicts/data/{id}` | 编辑字典数据 |
| DELETE | `/api/system/dicts/data/{id}` | 删除字典数据 |
| POST | `/api/system/dicts/refresh-cache` | 刷新字典缓存 |

### 10. 日志管理 LogController `/api/system/logs`
| 方法 | 路径 | 功能 |
|------|------|------|
| GET | `/api/system/logs/login` | 登录日志列表 |
| GET | `/api/system/logs/operation` | 操作日志列表 |
| GET | `/api/system/logs/login/{id}` | 登录日志详情 |
| GET | `/api/system/logs/operation/{id}` | 操作日志详情 |
| DELETE | `/api/system/logs/login/{id}` | 删除登录日志 |
| DELETE | `/api/system/logs/operation/{id}` | 删除操作日志 |
| DELETE | `/api/system/logs/login/batch` | 批量删除登录日志 |
| DELETE | `/api/system/logs/operation/batch` | 批量删除操作日志 |
| DELETE | `/api/system/logs/login/clean` | 清空登录日志 |
| DELETE | `/api/system/logs/operation/clean` | 清空操作日志 |

### 11. 系统监控 MonitorController `/api/system/monitor`
| 方法 | 路径 | 功能 |
|------|------|------|
| GET | `/api/system/monitor/online` | 在线用户列表 |
| DELETE | `/api/system/monitor/online/{token}` | 强制下线 |
| GET | `/api/system/monitor/server` | 服务器信息 |
| GET | `/api/system/monitor/cache` | 缓存信息 |
| DELETE | `/api/system/monitor/cache/{cacheName}` | 清除指定缓存 |
| DELETE | `/api/system/monitor/cache` | 清除全部缓存 |
| GET | `/api/system/monitor/stats` | 系统统计 |

### 12. 权限管理 PermissionController `/api/system/permissions`
| 方法 | 路径 | 功能 |
|------|------|------|
| GET | `/api/system/permissions` | 权限列表 |
| GET | `/api/system/permissions/type/{permissionType}` | 按类型查询 |
| GET | `/api/system/permissions/{id}` | 权限详情 |
| POST | `/api/system/permissions` | 新增权限 |
| PUT | `/api/system/permissions/{id}` | 编辑权限 |
| DELETE | `/api/system/permissions/{id}` | 删除权限 |
| GET | `/api/system/permissions/user/{userId}` | 用户权限 |
| GET | `/api/system/permissions/role/{roleId}` | 角色权限 |
| GET | `/api/system/permissions/check-permission-key` | 校验权限Key |

### 13. 应用管理 ApplicationController `/api/system/application`
| 方法 | 路径 | 功能 |
|------|------|------|
| GET | `/api/system/application/list` | 应用列表 |
| GET | `/api/system/application/enabled` | 已启用应用 |
| GET | `/api/system/application/{id}` | 应用详情 |
| GET | `/api/system/application/code/{appCode}` | 按编码查询 |
| POST | `/api/system/application` | 新增应用 |
| PUT | `/api/system/application` | 编辑应用 |
| DELETE | `/api/system/application/{id}` | 删除应用 |
| PUT | `/api/system/application/{id}/status/{status}` | 修改状态 |

---

## 三、前端 API 路径对照表

前端直连（非网关）时，API 路径规则：
- user-api 接口：`/api/user/...`（端口 8081）
- system-api 接口：`/api/system/...`（端口 8082）

经网关时，路径加服务前缀：
- `/basebackend-user-api/api/user/...`
- `/basebackend-system-api/api/system/...`

### Vite 代理配置建议
```typescript
// vite.config.ts
proxy: {
  '/api/user': {
    target: 'http://localhost:8081',
    changeOrigin: true,
  },
  '/api/system': {
    target: 'http://localhost:8082',
    changeOrigin: true,
  },
}
```

### 关键提醒
- **菜单接口不在 user-api**：菜单是 `ApplicationResource`，在 system-api
- 获取当前用户菜单：`GET /api/system/application/resource/current-user`
- 菜单树：`GET /api/system/application/resource/tree`
- 用户菜单树：`GET /api/system/application/resource/user/{userId}`
