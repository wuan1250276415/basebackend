# BaseBackend Admin Web - 后台管理系统前端

基于 React 18 + Ant Design 5 + TypeScript 的现代化后台管理系统前端。

## 技术栈

- **React 18** - UI 框架
- **TypeScript 5** - 类型系统
- **Ant Design 5** - UI 组件库
- **Vite 5** - 构建工具
- **React Router 6** - 路由管理
- **Zustand** - 状态管理
- **Axios** - HTTP 请求
- **React Query** - 数据请求管理
- **Dayjs** - 日期处理

## 功能特性

### 核心功能
- ✅ 用户登录/登出
- ✅ Token 认证
- ✅ 路由权限控制
- ✅ 响应式布局

### 系统管理
- ✅ 用户管理 - CRUD、分配角色、重置密码
- ✅ 角色管理 - CRUD、分配菜单、分配权限
- ✅ 菜单管理 - 树形结构、路由配置
- ✅ 部门管理 - 树形结构、层级管理
- 🔄 字典管理 - 待完善

### 系统监控
- 🔄 在线用户 - 待完善
- 🔄 服务器监控 - 待完善
- 🔄 登录日志 - 待完善
- 🔄 操作日志 - 待完善

## 快速开始

### 前置要求
- Node.js 16+
- npm 或 yarn 或 pnpm

### 安装依赖
```bash
cd basebackend-admin-web
npm install
# 或
yarn install
# 或
pnpm install
```

### 开发模式
```bash
npm run dev
```

访问 `http://localhost:3000`

### 生产构建
```bash
npm run build
```

构建产物在 `dist` 目录

### 预览生产构建
```bash
npm run preview
```

## 项目结构

```
basebackend-admin-web/
├── public/              # 静态资源
├── src/
│   ├── api/            # API 接口
│   │   ├── auth.ts     # 认证接口
│   │   ├── user.ts     # 用户接口
│   │   ├── role.ts     # 角色接口
│   │   ├── menu.ts     # 菜单接口
│   │   └── dept.ts     # 部门接口
│   ├── assets/         # 资源文件
│   ├── components/     # 公共组件
│   ├── layouts/        # 布局组件
│   │   └── BasicLayout/ # 基础布局
│   ├── pages/          # 页面组件
│   │   ├── Login/      # 登录页
│   │   ├── Dashboard/  # 仪表盘
│   │   ├── System/     # 系统管理
│   │   └── Monitor/    # 系统监控
│   ├── router/         # 路由配置
│   ├── stores/         # 状态管理
│   │   └── auth.ts     # 认证状态
│   ├── types/          # 类型定义
│   ├── utils/          # 工具函数
│   │   └── request.ts  # HTTP 请求封装
│   ├── App.tsx         # 根组件
│   └── main.tsx        # 入口文件
├── index.html          # HTML 模板
├── package.json        # 项目配置
├── tsconfig.json       # TypeScript 配置
├── vite.config.ts      # Vite 配置
└── README.md           # 项目说明
```

## 接口对接

### API 基础配置
- **开发环境**: `http://localhost:8082`
- **生产环境**: 配置在 `vite.config.ts` 中

### 认证机制
- Token 存储在 `localStorage`
- 请求头携带: `Authorization: Bearer <token>`
- Token 过期自动跳转登录页

### 响应格式
```typescript
{
  code: 200,
  message: "操作成功",
  data: {...},
  timestamp: 1234567890
}
```

## 默认账号

- **用户名**: admin
- **密码**: admin123

## 开发说明

### 添加新页面
1. 在 `src/pages` 创建页面组件
2. 在 `src/router/index.tsx` 添加路由
3. 在 `src/api` 创建对应的 API 接口
4. 更新菜单配置（如需要）

### 状态管理
使用 Zustand 进行全局状态管理，当前已实现：
- 认证状态 (`auth.ts`)

### 样式方案
- Ant Design 主题定制
- CSS Modules（可选）
- 全局样式在 `index.css`

## 浏览器支持

- Chrome（推荐）
- Firefox
- Safari
- Edge

## License

MIT
