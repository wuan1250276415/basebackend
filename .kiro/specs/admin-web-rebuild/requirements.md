# Requirements Document

## Introduction

使用 Ant Design Pro 全新重构 `basebackend-admin-web` 模块，构建现代化后台管理系统。该系统对接现有的 `basebackend-user-api`（认证、用户、角色、个人中心）、`basebackend-system-api`（部门、字典、权限/菜单、日志、监控）和 `basebackend-chat-api`（消息、群组、在线状态）三个后端微服务，提供系统管理、系统监控、聊天管理和数据可视化等功能。

技术栈：Vite + React 18 + TypeScript（严格模式）、Ant Design 5.x + Pro Components（ProTable / ProForm / ProLayout）、Zustand 状态管理、React Router 6、Axios HTTP 客户端、@ant-design/charts 图表库。

## Glossary

- **Admin_Web**: 基于 Ant Design Pro 的后台管理前端应用，即 `basebackend-admin-web` 模块
- **Auth_Module**: 认证模块，负责登录、登出、Token 刷新和当前用户信息获取，对接 `/api/user/auth/*`
- **User_API**: 用户管理后端服务，提供用户 CRUD、角色分配、状态切换等接口，路径前缀 `/api/user/users`
- **Role_API**: 角色管理后端服务，提供角色 CRUD、菜单分配、权限分配、数据权限等接口，路径前缀 `/api/user/roles`
- **Dept_API**: 部门管理后端服务，提供部门树、CRUD 等接口，路径前缀 `/api/system/depts`
- **Dict_API**: 字典管理后端服务，提供字典类型和字典数据 CRUD 接口，路径前缀 `/api/system/dicts`
- **Permission_API**: 权限/菜单管理后端服务，提供权限列表、CRUD、按用户/角色查询等接口，路径前缀 `/api/system/permissions`
- **Log_API**: 日志管理后端服务，提供登录日志和操作日志的分页查询、删除、清空等接口，路径前缀 `/api/system/logs`
- **Monitor_API**: 系统监控后端服务，提供在线用户、服务器信息、缓存信息、系统统计等接口，路径前缀 `/api/system/monitor`
- **Chat_API**: 聊天管理后端服务，提供消息、群组、在线状态等接口，路径前缀 `/api/chat/*`
- **Profile_API**: 个人中心后端服务，提供个人资料查看/更新和密码修改接口，路径前缀 `/api/user/profile`
- **ProLayout**: Ant Design Pro 提供的企业级布局组件，支持侧边栏、面包屑、多种布局模式
- **ProTable**: Ant Design Pro 提供的高级表格组件，内置搜索表单、分页、工具栏
- **ProForm**: Ant Design Pro 提供的高级表单组件，支持抽屉表单、模态表单等
- **Router_Guard**: 路由守卫，根据用户登录状态和菜单权限控制页面访问
- **HTTP_Client**: 基于 Axios 封装的 HTTP 客户端，负责请求/响应拦截、Token 附加和错误处理
- **Auth_Store**: 基于 Zustand 的认证状态管理，存储 Token、用户信息和权限数据
- **Tab_Manager**: 多标签页管理器，以标签形式展示已打开的页面，支持关闭和切换

## Requirements

### Requirement 1: Project Scaffolding and Build Configuration

**User Story:** As a developer, I want a properly configured Vite + React 18 + TypeScript project with ESLint and Prettier, so that I can develop with modern tooling and consistent code quality.

#### Acceptance Criteria

1. THE Admin_Web SHALL use Vite as the build tool with React 18, TypeScript strict mode, Ant Design 5.x, Ant Design Pro Components, Zustand, React Router 6, Axios, and @ant-design/charts as core dependencies
2. THE Admin_Web SHALL provide a `vite.config.ts` that proxies all `/api` requests to `http://localhost:8080` during development
3. THE Admin_Web SHALL include `.env.development` and `.env.production` environment configuration files with `VITE_API_BASE_URL` variable
4. THE Admin_Web SHALL include ESLint and Prettier configuration files enforcing TypeScript strict rules and consistent formatting
5. THE Admin_Web SHALL produce a successful build output when executing `npm run build` with zero TypeScript compilation errors
6. THE Admin_Web SHALL use Chinese comments throughout all source code files

### Requirement 2: HTTP Client and API Layer

**User Story:** As a developer, I want a centralized HTTP client with interceptors and modular API services, so that all backend communication is consistent and maintainable.

#### Acceptance Criteria

1. THE HTTP_Client SHALL attach the JWT access token from Auth_Store to the `Authorization` header of every outgoing request as a Bearer token
2. WHEN the HTTP_Client receives a 401 response, THE HTTP_Client SHALL clear the Auth_Store and redirect the user to the login page
3. WHEN the HTTP_Client receives a 403 response, THE HTTP_Client SHALL display a notification message indicating insufficient permissions
4. IF the HTTP_Client encounters a network error or timeout, THEN THE HTTP_Client SHALL display an error notification with a descriptive message
5. THE Admin_Web SHALL organize API calls into separate module files: `userApi`, `roleApi`, `menuApi`, `deptApi`, `dictApi`, `logApi`, `monitorApi`, `chatApi`, `profileApi`, and `authApi`
6. THE `authApi` SHALL provide methods for login (`POST /api/user/auth/login`), logout (`POST /api/user/auth/logout`), refresh token (`POST /api/user/auth/refresh`), get current user info (`GET /api/user/auth/info`), and change password (`PUT /api/user/auth/password`)
7. THE `userApi` SHALL provide methods for paginated user list (`GET /api/user/users`), create user (`POST /api/user/users`), update user (`PUT /api/user/users/{id}`), delete user (`DELETE /api/user/users/{id}`), reset password (`PUT /api/user/users/{id}/reset-password`), assign roles (`PUT /api/user/users/{id}/roles`), and toggle status (`PUT /api/user/users/{id}/status`)
8. THE `roleApi` SHALL provide methods for paginated role list (`GET /api/user/roles`), create/update/delete role, assign menus (`PUT /api/user/roles/{id}/menus`), assign permissions (`PUT /api/user/roles/{id}/permissions`), and manage data permissions (`PUT /api/user/roles/{id}/data-permissions`)
9. THE `deptApi` SHALL provide methods for department tree (`GET /api/system/depts/tree`), department list, create/update/delete department, and check name uniqueness
10. THE `dictApi` SHALL provide methods for paginated dict type list (`GET /api/system/dicts`), dict data by type (`GET /api/system/dicts/data/type/{dictType}`), and CRUD for both dict types and dict data
11. THE `menuApi` SHALL provide methods for permission list (`GET /api/system/permissions`), create/update/delete permission, and query permissions by user or role
12. THE `logApi` SHALL provide methods for paginated login logs (`GET /api/system/logs/login`), paginated operation logs (`GET /api/system/logs/operation`), log detail by ID, delete, batch delete, and clean operations
13. THE `monitorApi` SHALL provide methods for online users (`GET /api/system/monitor/online`), force logout (`DELETE /api/system/monitor/online/{token}`), server info (`GET /api/system/monitor/server`), cache info (`GET /api/system/monitor/cache`), and system stats (`GET /api/system/monitor/stats`)
14. THE `chatApi` SHALL provide methods for message list (`GET /api/chat/messages`), group list, group info, group members, dissolve group (`DELETE /api/chat/groups/{groupId}`), and message deletion

### Requirement 3: Authentication and Login Page

**User Story:** As a user, I want a visually appealing login page and secure authentication flow, so that I can access the admin system safely.

#### Acceptance Criteria

1. THE Admin_Web SHALL display a login page with a centered login card over an animated background, containing username and password fields and a submit button
2. WHEN the user submits valid credentials, THE Auth_Module SHALL call `POST /api/user/auth/login` and store the returned JWT access token and refresh token in Auth_Store
3. WHEN login succeeds, THE Admin_Web SHALL fetch the current user info via `GET /api/user/auth/info` and store user details (userId, username, nickname, avatar, roles, permissions, menus) in Auth_Store
4. WHEN login succeeds, THE Admin_Web SHALL redirect the user to the Dashboard page
5. IF the login request returns an error, THEN THE Admin_Web SHALL display the error message from the server response on the login form
6. WHEN the user clicks the logout option, THE Auth_Module SHALL call `POST /api/user/auth/logout`, clear Auth_Store, and redirect to the login page
7. THE Auth_Store SHALL persist token data to localStorage so that the session survives page refresh

### Requirement 4: Layout and Navigation

**User Story:** As a user, I want a professional layout with sidebar navigation, breadcrumbs, user menu, and multi-tab support, so that I can navigate the system efficiently.

#### Acceptance Criteria

1. THE Admin_Web SHALL use ProLayout with a dark-themed sidebar and white content area as the main application layout
2. THE Admin_Web SHALL display a breadcrumb trail in the top content area reflecting the current navigation path
3. THE Admin_Web SHALL display a user avatar dropdown menu in the top-right corner with options for "个人中心" (profile), "修改密码" (change password), and "退出登录" (logout)
4. THE Admin_Web SHALL generate the left sidebar menu dynamically based on the menu data returned from the user's permissions in Auth_Store
5. THE Tab_Manager SHALL display each opened page as a closable tab below the top bar, allowing the user to switch between open pages and close individual tabs
6. THE Admin_Web SHALL provide a theme toggle button that switches between light mode and dark mode, applying the selected theme to the entire application
7. THE Admin_Web SHALL provide a fullscreen toggle button in the top bar
8. THE Admin_Web SHALL use `#1677ff` as the primary theme color with rounded card corners and subtle shadow effects throughout the application

### Requirement 5: Route Permission Control

**User Story:** As an administrator, I want route-level and button-level permission control, so that users can only access features they are authorized to use.

#### Acceptance Criteria

1. THE Router_Guard SHALL dynamically generate application routes based on the menu list stored in Auth_Store after login
2. WHEN an unauthenticated user attempts to access a protected route, THE Router_Guard SHALL redirect the user to the login page
3. WHEN an authenticated user attempts to access a route not present in the user's menu list, THE Admin_Web SHALL display a 403 Forbidden error page
4. THE Admin_Web SHALL provide a `useAuth` hook that accepts a permission string and returns a boolean indicating whether the current user has that permission
5. THE Admin_Web SHALL use the `useAuth` hook to conditionally render action buttons (create, edit, delete) in management pages based on the user's permissions
6. THE Admin_Web SHALL provide static error pages for 403 (Forbidden), 404 (Not Found), and 500 (Server Error) scenarios

### Requirement 6: User Management Page

**User Story:** As an administrator, I want to manage system users including creating, editing, disabling, and assigning roles, so that I can control who has access to the system.

#### Acceptance Criteria

1. THE Admin_Web SHALL display a user management page with a ProTable listing users with columns for username, nickname, department, phone, email, status, and creation time, with server-side pagination and search filters
2. WHEN the administrator clicks the "新增" (add) button, THE Admin_Web SHALL open a ProForm drawer for creating a new user with fields for username, nickname, password, department (tree select), phone, email, and status
3. WHEN the administrator clicks the "编辑" (edit) icon button on a user row, THE Admin_Web SHALL open a ProForm drawer pre-filled with the user's current data for editing
4. WHEN the administrator clicks the "分配角色" (assign roles) action on a user row, THE Admin_Web SHALL open a modal displaying all available roles as checkboxes, pre-selecting the user's current roles, and submit changes via `PUT /api/user/users/{id}/roles`
5. WHEN the administrator clicks the "重置密码" (reset password) action on a user row, THE Admin_Web SHALL display a confirmation dialog and upon confirmation call `PUT /api/user/users/{id}/reset-password`
6. WHEN the administrator toggles the status switch on a user row, THE Admin_Web SHALL call `PUT /api/user/users/{id}/status` to enable or disable the user account
7. THE Admin_Web SHALL use icon buttons (edit, delete, more) in the table action column for a compact, modern appearance

### Requirement 7: Role Management Page

**User Story:** As an administrator, I want to manage roles and assign permissions, so that I can define access control policies for different user groups.

#### Acceptance Criteria

1. THE Admin_Web SHALL display a role management page with a ProTable listing roles with columns for role name, role key, sort order, status, and creation time
2. WHEN the administrator creates or edits a role, THE Admin_Web SHALL provide a form with fields for role name, role key, sort order, status, and remark
3. WHEN the administrator clicks "分配权限" (assign permissions) on a role, THE Admin_Web SHALL display a tree-structured checkbox list of all permissions fetched from `GET /api/system/permissions`, pre-selecting the role's current permissions from `GET /api/user/roles/{id}/permissions`
4. WHEN the administrator clicks "分配菜单" (assign menus) on a role, THE Admin_Web SHALL display a tree-structured checkbox list of all menus, pre-selecting the role's current menus from `GET /api/user/roles/{id}/menus`
5. WHEN the administrator clicks "数据权限" (data permissions) on a role, THE Admin_Web SHALL provide a configuration interface for setting the role's data scope via `PUT /api/user/roles/{id}/data-permissions`

### Requirement 8: Menu/Permission Management Page

**User Story:** As an administrator, I want to manage the system menu structure and permissions, so that I can control navigation and access granularity.

#### Acceptance Criteria

1. THE Admin_Web SHALL display a menu management page with a tree-structured table showing all permissions/menus with columns for name, icon, permission key, type, sort order, and status
2. WHEN the administrator creates or edits a menu item, THE Admin_Web SHALL provide a form with fields for parent menu (tree select), menu name, icon (with an icon selector component), permission key, type (directory/menu/button), route path, sort order, and status
3. THE Admin_Web SHALL render the menu tree with proper indentation and expand/collapse controls
4. WHEN the administrator deletes a menu item that has child items, THE Admin_Web SHALL display a warning confirmation before proceeding

### Requirement 9: Department Management Page

**User Story:** As an administrator, I want to manage the organizational department structure, so that I can maintain the company hierarchy.

#### Acceptance Criteria

1. THE Admin_Web SHALL display a department management page with a tree-structured table showing departments with columns for department name, department code, sort order, status, and creation time
2. WHEN the administrator creates or edits a department, THE Admin_Web SHALL provide a form with fields for parent department (tree select), department name, department code, sort order, and status
3. THE Admin_Web SHALL fetch the department tree from `GET /api/system/depts/tree` and render it with expand/collapse controls
4. WHEN creating a department, THE Admin_Web SHALL validate department name uniqueness within the same parent via `GET /api/system/depts/check-dept-name`

### Requirement 10: Dictionary Management Page

**User Story:** As an administrator, I want to manage system dictionaries, so that I can maintain configurable enumeration values used across the application.

#### Acceptance Criteria

1. THE Admin_Web SHALL display a dictionary type management page with a ProTable listing dictionary types with columns for dict name, dict type, status, remark, and creation time, with server-side pagination
2. WHEN the administrator clicks on a dictionary type row, THE Admin_Web SHALL navigate to or display a sub-table showing the dictionary data entries for that type, fetched from `GET /api/system/dicts/data?dictType={type}`
3. WHEN the administrator creates or edits a dictionary type, THE Admin_Web SHALL provide a form with fields for dict name, dict type, status, and remark
4. WHEN the administrator creates or edits a dictionary data entry, THE Admin_Web SHALL provide a form with fields for dict type, dict label, dict value, sort order, status, and remark
5. THE Admin_Web SHALL provide a "刷新缓存" (refresh cache) button that calls `POST /api/system/dicts/refresh-cache`

### Requirement 11: System Configuration Page

**User Story:** As an administrator, I want to manage system parameters, so that I can adjust application behavior without code changes.

#### Acceptance Criteria

1. THE Admin_Web SHALL display a system configuration page with a ProTable listing configuration parameters with columns for parameter name, parameter key, parameter value, type, and remark
2. WHEN the administrator creates or edits a configuration parameter, THE Admin_Web SHALL provide a form with fields for parameter name, parameter key, parameter value, type (system/custom), and remark

### Requirement 12: Online User Monitoring Page

**User Story:** As an administrator, I want to view currently online users and force logout if necessary, so that I can manage active sessions.

#### Acceptance Criteria

1. THE Admin_Web SHALL display an online user monitoring page with a ProTable listing online users with columns for username, IP address, login location, browser, OS, and login time, fetched from `GET /api/system/monitor/online`
2. WHEN the administrator clicks the "强制下线" (force logout) button on a user row, THE Admin_Web SHALL display a confirmation dialog and upon confirmation call `DELETE /api/system/monitor/online/{token}`
3. WHEN the force logout operation succeeds, THE Admin_Web SHALL refresh the online user list

### Requirement 13: Operation Log Page

**User Story:** As an administrator, I want to view operation logs, so that I can audit user actions in the system.

#### Acceptance Criteria

1. THE Admin_Web SHALL display an operation log page with a ProTable listing operation logs with columns for operation description, operator username, request method, status, operation time, and duration, with server-side pagination and search filters for username, operation, status, and date range
2. WHEN the administrator clicks a log row, THE Admin_Web SHALL display a detail modal or drawer showing the full log information including request URL, request parameters, response result, and error message (if any), fetched from `GET /api/system/logs/operation/{id}`
3. THE Admin_Web SHALL support batch deletion of operation logs via `DELETE /api/system/logs/operation/batch` and a "清空" (clean all) button via `DELETE /api/system/logs/operation/clean`

### Requirement 14: Login Log Page

**User Story:** As an administrator, I want to view login logs, so that I can monitor authentication activity.

#### Acceptance Criteria

1. THE Admin_Web SHALL display a login log page with a ProTable listing login records with columns for username, IP address, login location, browser, OS, status (success/failure), and login time, with server-side pagination and search filters for username, IP address, status, and date range
2. THE Admin_Web SHALL support batch deletion of login logs via `DELETE /api/system/logs/login/batch` and a "清空" (clean all) button via `DELETE /api/system/logs/login/clean`

### Requirement 15: Chat Statistics Dashboard

**User Story:** As an administrator, I want to view chat system statistics, so that I can monitor messaging activity and user engagement.

#### Acceptance Criteria

1. THE Admin_Web SHALL display a chat statistics page with dashboard cards showing today's message count, active user count, and group count
2. THE Admin_Web SHALL display a line chart using @ant-design/charts showing messaging trends over the past 7 days
3. THE Admin_Web SHALL fetch statistics data from the Chat_API endpoints

### Requirement 16: Chat Message Management Page

**User Story:** As an administrator, I want to manage chat messages, so that I can search for and remove sensitive or inappropriate content.

#### Acceptance Criteria

1. THE Admin_Web SHALL display a message management page with a ProTable listing messages with columns for sender, conversation, message type, content preview, send time, and status, with search filters for sender, content keyword, and date range
2. WHEN the administrator clicks "删除" (delete) on a message row, THE Admin_Web SHALL display a confirmation dialog and upon confirmation delete the message via the Chat_API
3. WHEN the administrator clicks a message row, THE Admin_Web SHALL display the full message content in a detail drawer

### Requirement 17: Chat Group Management Page

**User Story:** As an administrator, I want to manage chat groups, so that I can oversee group activities and dissolve groups when necessary.

#### Acceptance Criteria

1. THE Admin_Web SHALL display a group management page with a ProTable listing groups with columns for group name, owner, member count, creation time, and status, fetched from the Chat_API
2. WHEN the administrator clicks "查看成员" (view members) on a group row, THE Admin_Web SHALL display a modal listing group members with their roles, fetched from `GET /api/chat/groups/{groupId}/members`
3. WHEN the administrator clicks "解散群组" (dissolve group) on a group row, THE Admin_Web SHALL display a confirmation dialog and upon confirmation call `DELETE /api/chat/groups/{groupId}`

### Requirement 18: Dashboard Home Page

**User Story:** As a user, I want a dashboard home page with key metrics and system information, so that I can get a quick overview of the system status.

#### Acceptance Criteria

1. THE Admin_Web SHALL display a dashboard page with statistic cards showing today's user count, message count, request count, and online user count, using Ant Design Statistic components with icons
2. THE Admin_Web SHALL display a line chart using @ant-design/charts showing trends over the past 7 days for key metrics (users, messages, requests)
3. THE Admin_Web SHALL display a system information panel showing JVM info, memory usage, and disk usage, fetched from `GET /api/system/monitor/server`
4. THE Admin_Web SHALL use rounded card containers with shadow effects for each dashboard section, following the `#1677ff` primary color scheme

### Requirement 19: Profile and Personal Center

**User Story:** As a user, I want to view and update my personal information and change my password, so that I can manage my account.

#### Acceptance Criteria

1. THE Admin_Web SHALL display a personal center page with the user's avatar, nickname, username, department, email, and phone, fetched from `GET /api/user/profile/info`
2. WHEN the user edits their profile, THE Admin_Web SHALL provide a form for updating nickname, email, and phone, submitting changes via `PUT /api/user/profile/info`
3. WHEN the user changes their password, THE Admin_Web SHALL provide a form with fields for old password, new password, and confirm password, submitting via `PUT /api/user/profile/password`
4. THE Admin_Web SHALL validate that the new password and confirm password fields match before submitting the password change request

### Requirement 20: Responsive Design and Visual Style

**User Story:** As a user, I want the admin system to look modern and work well on different screen sizes, so that I can use it comfortably.

#### Acceptance Criteria

1. THE Admin_Web SHALL use `#1677ff` as the primary color throughout the application, applied to buttons, links, active menu items, and chart elements
2. THE Admin_Web SHALL apply rounded corners (border-radius 8px) and subtle box shadows to all card containers
3. THE Admin_Web SHALL use icon buttons in table action columns instead of text links for a compact, modern appearance
4. THE Admin_Web SHALL adapt the layout responsively: on screens narrower than 768px, the sidebar SHALL collapse to icon-only mode automatically
5. THE Admin_Web SHALL support dark mode and light mode themes, toggled via the theme switch button, persisting the user's preference in localStorage
