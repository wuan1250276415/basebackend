[根目录](../../CLAUDE.md) > **basebackend-admin-web**

# basebackend-admin-web

## 模块职责

后台管理系统前端，基于 React 18 + TypeScript + Ant Design 5 + Vite 构建的单页应用(SPA)。提供完整的系统管理、监控、工作流、文件管理等功能界面。

## 入口与启动

- **入口文件**: `src/main.tsx`
- **路由**: `src/router/index.tsx` (集中式路由配置)
- **布局**: `src/layouts/BasicLayout/index.tsx`
- **启动**: `npm run dev` (Vite开发服务器)
- **构建**: `npm run build`

## 对外接口

### 页面模块

| 路由 | 页面 | 说明 |
|------|------|------|
| /login | Login | 登录页 |
| /dashboard | Dashboard | 仪表盘(欢迎/指标/监控/快捷操作/活动) |
| /system/* | User/Role/Menu/Dept/Dict/Application | 系统管理 |
| /monitor/* | OnlineUser/ServerMonitor/LoginLog/OperationLog | 系统监控 |
| /monitor/observability/* | Overview/LogQuery/TraceQuery/AlertManagement | 可观测性 |
| /integration/* | MessageMonitor/WebhookConfig/EventLog/DeadLetter | 消息集成 |
| /workflow/* | TodoList/ProcessInstance/ProcessDefinition/BpmnDesigner/Statistics | 工作流 |
| /file/* | FileList/RecycleBin | 文件管理 |
| /user/profile | Profile | 个人中心(基本信息/偏好/安全) |
| /notification | NotificationCenter | 通知中心 |
| /developer/api-docs | ApiDocs | Swagger API文档 |

### API层

API调用封装在 `src/api/` 目录，使用 Axios，请求拦截器自动附加 JWT Token。

### 状态管理

| Store | 文件 | 职责 |
|-------|------|------|
| useAuthStore | stores/auth.ts | 认证状态/Token管理 |
| useMenuStore | stores/menu.ts | 动态菜单 |
| useThemeStore | stores/theme.ts | 主题配置 |
| useWorkflowStore | stores/workflow.ts | 工作流状态 |
| useNotificationStore | stores/notification.ts | 通知状态 |

### Context

AppContext, UserContext, DeptContext, DictContext -- 全局数据共享。

## 关键依赖与配置

- UI: Ant Design 5, @ant-design/pro-components, @ant-design/icons
- 图表: @ant-design/charts
- 工作流设计器: @antv/x6 (自定义), bpmn-js (BPMN标准)
- 表单引擎: @formily/core + @formily/react + @formily/antd-v5
- 国际化: i18next + react-i18next (zh-CN / en-US)
- 状态管理: Zustand 4
- HTTP: Axios 1.6
- API文档: swagger-ui-react

## 测试与质量

- 暂无自动化测试
- ESLint + Prettier 代码风格检查
- TypeScript 严格模式

## 相关文件清单

- 入口: `src/main.tsx`, `src/App.tsx`
- 路由: `src/router/index.tsx`
- API层: `src/api/*.ts`
- 页面: `src/pages/`
- 组件: `src/components/`
- 状态: `src/stores/`
- 工具: `src/utils/`
- 国际化: `src/i18n/`
- 类型: `src/types/`
- 构建: `vite.config.mts`, `tsconfig.json`

## 变更记录

| 时间 | 操作 | 说明 |
|------|------|------|
| 2026-02-20 13:17:55 | 初始创建 | 全量扫描生成 |
