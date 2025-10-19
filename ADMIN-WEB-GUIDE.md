# BaseBackend 前端管理系统部署指南

## 📋 项目概述

已成功创建了一个完整的后台管理系统前端项目，基于 **React 18 + Ant Design 5 + TypeScript**。

## 🎯 已实现的功能

### 1. **核心功能** ✅
- ✅ 用户登录/登出
- ✅ JWT Token 认证
- ✅ 路由权限控制
- ✅ 响应式布局设计
- ✅ 统一的 API 请求封装
- ✅ 全局状态管理（Zustand）

### 2. **系统管理** ✅
- ✅ **用户管理**: 完整的CRUD、分页、搜索、分配角色、重置密码、启用/禁用
- ✅ **角色管理**: 完整的CRUD、分页、搜索、分配菜单权限
- ✅ **菜单管理**: 树形结构、CRUD操作
- ✅ **部门管理**: 树形结构、CRUD操作
- 🔄 **字典管理**: 页面占位符（待完善）

### 3. **系统监控** 🔄
- 🔄 在线用户监控（页面占位符）
- 🔄 服务器监控（页面占位符）
- 🔄 登录日志（页面占位符）
- 🔄 操作日志（页面占位符）

### 4. **个人中心** 🔄
- 🔄 个人信息（页面占位符）

## 🚀 快速启动

### 步骤 1: 安装依赖
```bash
cd basebackend-admin-web
npm install
```

### 步骤 2: 启动开发服务器
```bash
npm run dev
```

访问: `http://localhost:3000`

### 步骤 3: 登录系统
- **用户名**: `admin`
- **密码**: `admin123`

## 📁 项目结构

```
basebackend-admin-web/
├── src/
│   ├── api/                    # API 接口层
│   │   ├── auth.ts            # 认证接口
│   │   ├── user.ts            # 用户管理接口
│   │   ├── role.ts            # 角色管理接口
│   │   ├── menu.ts            # 菜单管理接口
│   │   └── dept.ts            # 部门管理接口
│   ├── layouts/               # 布局组件
│   │   └── BasicLayout/       # 基础布局（侧边栏+顶栏）
│   ├── pages/                 # 页面组件
│   │   ├── Login/             # 登录页
│   │   ├── Dashboard/         # 仪表盘
│   │   ├── System/            # 系统管理模块
│   │   │   ├── User/          # 用户管理
│   │   │   ├── Role/          # 角色管理
│   │   │   ├── Menu/          # 菜单管理
│   │   │   ├── Dept/          # 部门管理
│   │   │   └── Dict/          # 字典管理
│   │   ├── Monitor/           # 系统监控模块
│   │   │   ├── LoginLog/      # 登录日志
│   │   │   ├── OperationLog/  # 操作日志
│   │   │   ├── OnlineUser/    # 在线用户
│   │   │   └── ServerMonitor/ # 服务器监控
│   │   └── User/              # 个人中心
│   │       └── Profile/       # 个人信息
│   ├── router/                # 路由配置
│   │   └── index.tsx          # 路由入口
│   ├── stores/                # 状态管理
│   │   └── auth.ts            # 认证状态
│   ├── types/                 # TypeScript 类型定义
│   │   └── index.ts           # 全局类型
│   ├── utils/                 # 工具函数
│   │   └── request.ts         # HTTP 请求封装
│   ├── App.tsx                # 根组件
│   ├── main.tsx               # 应用入口
│   └── index.css              # 全局样式
├── package.json               # 项目配置
├── vite.config.ts             # Vite 配置
├── tsconfig.json              # TypeScript 配置
└── README.md                  # 项目说明
```

## 🔌 API 对接

### 请求拦截器
- 自动添加 `Authorization` 头
- Token 从 `localStorage` 获取

### 响应拦截器
- 统一处理错误响应
- 401 自动跳转登录页
- 显示错误提示消息

### 代理配置
开发环境通过 Vite 代理转发请求到后端：
```typescript
proxy: {
  '/api': {
    target: 'http://localhost:8082',
    changeOrigin: true,
  },
}
```

## 🎨 核心功能说明

### 1. 登录页面
- 优雅的渐变背景
- 表单验证
- 记住密码功能
- 错误提示

### 2. 用户管理
- **列表展示**: 分页、搜索、过滤
- **新增/编辑**: 表单验证、部门选择、角色分配
- **删除**: 二次确认
- **重置密码**: 一键重置为默认密码
- **启用/禁用**: 快速切换状态

### 3. 角色管理
- **列表展示**: 分页、搜索、过滤
- **新增/编辑**: 表单验证、数据范围配置
- **删除**: 二次确认
- **分配菜单**: 树形结构选择

### 4. 菜单管理
- **列表展示**: 完整菜单列表
- **新增/编辑**: 父菜单选择、类型配置
- **删除**: 二次确认

### 5. 部门管理
- **树形展示**: 部门层级结构
- **新增/编辑**: 父部门选择
- **删除**: 二次确认

## 📝 开发说明

### 添加新页面
1. 在 `src/pages` 创建页面组件
2. 在 `src/api` 创建对应的 API 接口
3. 在 `src/router/index.tsx` 添加路由
4. 在布局菜单中添加导航项

### 状态管理
```typescript
// 使用 Zustand
import { useAuthStore } from '@/stores/auth'

const { token, userInfo, logout } = useAuthStore()
```

### API 调用
```typescript
import { getUserPage } from '@/api/user'

const response = await getUserPage({ current: 1, size: 10 })
```

## 🔧 配置说明

### 环境变量
- `.env.development` - 开发环境
- `.env.production` - 生产环境

### 主题定制
在 `src/main.tsx` 中配置 Ant Design 主题

### 路由配置
在 `src/router/index.tsx` 中配置路由

## 🌐 部署

### Nginx 配置示例
```nginx
server {
    listen 80;
    server_name yourdomain.com;
    
    root /var/www/basebackend-admin-web/dist;
    index index.html;
    
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    location /api {
        proxy_pass http://localhost:8082;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## 📞 技术支持

如有问题，请查看：
- 后端API文档: `http://localhost:8082/doc.html`
- 项目 README
- 联系开发团队

## 🎉 已完成的页面

1. ✅ **登录页面** - 完整的登录功能
2. ✅ **仪表盘** - 数据统计展示
3. ✅ **用户管理** - 完整的用户管理功能
4. ✅ **角色管理** - 完整的角色管理功能
5. ✅ **菜单管理** - 完整的菜单管理功能
6. ✅ **部门管理** - 完整的部门管理功能

## 🔄 待完善的功能

1. 字典管理完整功能
2. 在线用户监控
3. 服务器性能监控
4. 登录日志查看
5. 操作日志查看
6. 个人中心功能
7. 更多高级功能...

## 🎨 UI 设计特点

- **现代化设计**: 清爽简洁的界面
- **响应式布局**: 适配各种屏幕尺寸
- **流畅动画**: 优雅的过渡效果
- **一致性**: 遵循 Ant Design 设计规范
- **可访问性**: 良好的键盘导航支持
