# BaseBackend 接口文档

本文档包含了 `user-api` (用户服务) 和 `system-api` (系统服务) 的接口定义、传参及回参说明。

---

## 📅 公共说明

### 响应格式 (Result<T>)

所有接口统一返回以下 JSON 结构：

| 字段名 | 类型 | 说明 |
| :--- | :--- | :--- |
| code | Integer | 状态码（200 表示成功，其他为错误码） |
| msg | String | 提示消息（成功或失败的原因） |
| data | T | 具体的业务数据（对象、列表或分页结果） |

### 分页结构 (PageResult<T>)

分页查询接口的 `data` 字段结构如下：

| 字段名 | 类型 | 说明 |
| :--- | :--- | :--- |
| current | Long | 当前页码 |
| size | Long | 每页大小 |
| total | Long | 总记录数 |
| pages | Long | 总页数 |
| records | List<T> | 当前显示的记录列表 |

---

## 1. 用户服务 (user-api)

### 1.1 认证管理 (`AuthController`)

**基础路径**: `/api/user/auth`

#### 用户登录

- **路径**: `POST /login`
- **请求体 (LoginRequest)**:
| 参数名 | 类型 | 必选 | 说明 |
| :--- | :--- | :--- | :--- |
| username | String | 是 | 用户名 |
| password | String | 是 | 密码 |
| captcha | String | 否 | 验证码 |
| captchaId | String | 否 | 验证码标识 |
| rememberMe | Boolean | 否 | 记住我 |
- **响应 (LoginResponse)**:
| 字段名 | 类型 | 说明 |
| :--- | :--- | :--- |
| accessToken | String | 访问令牌 |
| tokenType | String | 令牌类型 (默认 Bearer) |
| expiresIn | Long | 过期时间（秒） |
| userInfo | Object | 用户基础信息 (id, username, nickname 等) |
| permissions | List<String> | 权限标识列表 |
| roles | List<String> | 角色标识列表 |

#### 修改密码

- **路径**: `PUT /password`
- **请求体 (PasswordChangeDTO)**:
| 参数名 | 类型 | 必选 | 说明 |
| :--- | :--- | :--- | :--- |
| oldPassword | String | 是 | 旧密码 |
| newPassword | String | 是 | 新密码 |

### 1.2 用户管理 (`UserController`)

**基础路径**: `/api/user`

#### 分页查询用户

- **路径**: `GET /`
- **查询参数 (UserQueryDTO)**:
| 参数名 | 类型 | 说明 |
| :--- | :--- | :--- |
| current | Integer | 当前页 (默认 1) |
| size | Integer | 每页条数 (默认 10) |
| username | String | 用户名（模糊匹配） |
| nickname | String | 昵称（模糊匹配） |
| phone | String | 手机号 |
| deptId | Long | 部门 ID |
| status | Integer | 状态 (0-禁用, 1-启用) |
- **响应**: `PageResult<UserDTO>`

#### 创建用户

- **路径**: `POST /`
- **请求体 (UserCreateDTO)**:
| 参数名 | 类型 | 必选 | 说明 |
| :--- | :--- | :--- | :--- |
| username | String | 是 | 用户名 (2-20字符) |
| password | String | 是 | 密码 (6-20字符) |
| nickname | String | 是 | 昵称 |
| email | String | 否 | 邮箱 |
| phone | String | 否 | 手机号 |
| gender | Integer | 否 | 性别 (0-未知, 1-男, 2-女) |
| deptId | Long | 否 | 部门 ID |
| roleIds | List<Long> | 否 | 关联角色 ID 列表 |

### 1.3 角色管理 (`RoleController`)

**基础路径**: `/api/user/roles`

#### 创建/更新角色

- **路径**: `POST /` 或 `PUT /{id}`
- **请求体 (RoleDTO)**:
| 参数名 | 类型 | 必选 | 说明 |
| :--- | :--- | :--- | :--- |
| roleName | String | 是 | 角色名称 |
| roleKey | String | 是 | 角色标识符 (如 admin, user) |
| roleSort | Integer | 否 | 显示顺序 |
| dataScope | Integer | 否 | 数据权限范围 (1-全部, 2-本部门等) |
| status | Integer | 否 | 状态 (0-禁用, 1-启用) |
| menuIds | List<Long> | 否 | 关联菜单 ID 列表 |

---

## 2. 系统服务 (system-api)

### 2.1 应用管理 (`ApplicationController`)

**基础路径**: `/api/system/application`

#### 创建应用

- **路径**: `POST /`
- **请求体 (ApplicationDTO)**:
| 参数名 | 类型 | 必选 | 说明 |
| :--- | :--- | :--- | :--- |
| appName | String | 是 | 应用名称 |
| appCode | String | 是 | 应用编码 |
| appType | String | 是 | 应用类型 |
| appIcon | String | 否 | 图标 |
| appUrl | String | 否 | 地址 |
| status | Integer | 是 | 状态 |

### 2.2 资源菜单管理 (`ApplicationResourceController`)

**基础路径**: `/api/system/application/resource`

#### 创建/更新资源

- **路径**: `POST /` 或 `PUT /`
- **请求体 (ApplicationResourceDTO)**:
| 参数名 | 类型 | 必选 | 说明 |
| :--- | :--- | :--- | :--- |
| appId | Long | 是 | 所属应用 ID |
| resourceName | String | 是 | 资源/菜单名称 |
| parentId | Long | 否 | 父级 ID (根节点为 0) |
| resourceType | String | 是 | 类型 (M-目录, C-菜单, F-按钮) |
| path | String | 否 | 路由地址 |
| component | String | 否 | 前端组件路径 |
| perms | String | 否 | 权限标识 |
| icon | String | 否 | 图标 |
| visible | Integer | 否 | 是否可见 (0-否, 1-是) |

### 2.3 部门管理 (`DeptController`)

**基础路径**: `/api/system/depts`

#### 部门 DTO (DeptDTO)

| 参数名 | 类型 | 必选 | 说明 |
| :--- | :--- | :--- | :--- |
| deptName | String | 是 | 部门名称 |
| parentId | Long | 否 | 上级部门 ID |
| orderNum | Integer | 否 | 排序号 |
| leader | String | 否 | 负责人 |
| status | Integer | 否 | 状态 (0-禁用, 1-启用) |

### 2.4 字典管理 (`DictController`)

**基础路径**: `/api/system/dicts`

#### 字典数据 (DictDataDTO)

| 参数名 | 类型 | 必选 | 说明 |
| :--- | :--- | :--- | :--- |
| dictType | String | 是 | 字典类型编码 |
| dictLabel | String | 是 | 字典标签 (展示值) |
| dictValue | String | 是 | 字典键值 (实际存储值) |
| cssClass | String | 否 | 回显样式 |
| listClass | String | 否 | 表格回显样式 |
| status | Integer | 否 | 状态 (0-禁用, 1-启用) |

---

## 3. 常见返回码说明

| 代码 | 说明 |
| :--- | :--- |
| 200 | 请求成功 |
| 401 | 未授权 / Token 已失效 |
| 403 | 权限不足 |
| 404 | 资源未找到 |
| 500 | 服务器内部异常 |
| B0001 | 业务逻辑错误 (例如：用户名已存在) |
| V0001 | 参数校验不通过 |
