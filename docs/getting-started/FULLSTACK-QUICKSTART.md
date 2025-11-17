# BaseBackend 全栈后台管理系统快速启动指南

## 🎉 项目概述

恭喜！你现在拥有一个完整的企业级后台管理系统：

### **后端 (Backend)** ✅
- **技术栈**: Java 17 + Spring Boot 3.1.5 + MyBatis Plus + MySQL + Redis
- **架构**: 微服务架构，基于 Nacos 服务发现和配置中心
- **功能**: 完整的 RBAC 权限体系，包含用户、角色、菜单、权限、部门管理
- **端口**: 8082 (admin-api), 8180 (gateway)

### **前端 (Frontend)** ✅
- **技术栈**: React 18 + TypeScript + Ant Design 5 + Vite
- **功能**: 用户管理、角色管理、菜单管理、部门管理等
- **端口**: 3000

## 🚀 完整启动流程

### 步骤 1: 启动基础设施

#### 1.1 启动 MySQL
```bash
# 确保 MySQL 服务正在运行
sudo systemctl start mysql
# 或
sudo service mysql start
```

#### 1.2 启动 Redis
```bash
# 确保 Redis 服务正在运行
sudo systemctl start redis
# 或
sudo service redis start
```

#### 1.3 启动 Nacos
```bash
cd /home/wuan/IdeaProjects/basebackend
./start-nacos.sh
```

等待 Nacos 启动完成，访问: `http://localhost:8848/nacos`
- 用户名: `nacos`
- 密码: `nacos`

### 步骤 2: 初始化数据库

```bash
cd /home/wuan/IdeaProjects/basebackend

# 创建数据库并执行SQL脚本
mysql -u root -p <<EOF
CREATE DATABASE IF NOT EXISTS basebackend_admin CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE basebackend_admin;
source basebackend-admin-api/src/main/resources/db/schema.sql;
source basebackend-admin-api/src/main/resources/db/data.sql;
EOF
```

或使用提供的脚本：
```bash
./init-admin-database.sh
```

### 步骤 3: 启动后端服务

#### 3.1 启动网关
```bash
cd basebackend-gateway
mvn spring-boot:run
```

#### 3.2 启动后台管理API
```bash
# 新开一个终端
cd basebackend-admin-api
mvn spring-boot:run
```

等待服务启动完成，验证：
- 健康检查: `curl http://localhost:8082/actuator/health`
- API文档: `http://localhost:8082/doc.html`

### 步骤 4: 启动前端

```bash
# 新开一个终端
cd basebackend-admin-web

# 首次运行需要安装依赖
npm install

# 启动开发服务器
npm run dev
```

或使用提供的脚本：
```bash
./start-frontend.sh
```

## 🎯 访问系统

### 前端访问
- **URL**: `http://localhost:3000`
- **用户名**: `admin`
- **密码**: `admin123`

### 后端访问
- **API文档**: `http://localhost:8082/doc.html`
- **网关**: `http://localhost:8180`
- **Nacos控制台**: `http://localhost:8848/nacos`

## 📝 功能说明

### 1. 登录功能
1. 打开浏览器访问 `http://localhost:3000`
2. 输入用户名 `admin` 和密码 `admin123`
3. 点击登录按钮
4. 成功后跳转到仪表盘

### 2. 用户管理
- **查看用户列表**: 分页、搜索、过滤
- **新增用户**: 填写用户信息、选择部门、分配角色
- **编辑用户**: 修改用户信息
- **删除用户**: 逻辑删除用户
- **重置密码**: 重置为默认密码 `123456`
- **启用/禁用**: 快速切换用户状态

### 3. 角色管理
- **查看角色列表**: 分页、搜索
- **新增角色**: 填写角色信息、配置数据范围
- **编辑角色**: 修改角色信息
- **删除角色**: 删除角色
- **分配菜单**: 树形结构选择菜单权限

### 4. 菜单管理
- **查看菜单列表**: 完整的菜单列表
- **新增菜单**: 创建目录、菜单或按钮
- **编辑菜单**: 修改菜单配置
- **删除菜单**: 删除菜单项

### 5. 部门管理
- **查看部门列表**: 部门层级结构
- **新增部门**: 创建新部门
- **编辑部门**: 修改部门信息
- **删除部门**: 删除部门

## 🔧 开发指南

### 添加新功能模块

#### 1. 创建 API 接口
```typescript
// src/api/example.ts
import request from '@/utils/request'

export const getExampleList = () => {
  return request.get('/admin/examples')
}
```

#### 2. 创建页面组件
```typescript
// src/pages/Example/index.tsx
import { Card } from 'antd'

const Example = () => {
  return <Card title="示例页面">内容</Card>
}

export default Example
```

#### 3. 添加路由
```typescript
// src/router/index.tsx
import Example from '@/pages/Example'

<Route path="example" element={<Example />} />
```

#### 4. 添加菜单项
```typescript
// src/layouts/BasicLayout/index.tsx
{
  key: '/example',
  label: '示例页面',
  onClick: () => navigate('/example'),
}
```

## 📦 生产部署

### 构建前端
```bash
cd basebackend-admin-web
npm run build
```

产物在 `dist` 目录，部署到 Nginx 或其他 Web 服务器。

### 构建后端
```bash
cd /home/wuan/IdeaProjects/basebackend
mvn clean package -DskipTests
```

JAR 文件在各模块的 `target` 目录。

## 🎨 UI 截图说明

### 登录页面
- 渐变背景设计
- 居中登录卡片
- Material Design 风格

### 主界面
- 左侧导航栏
- 顶部用户信息栏
- 内容区域

### 用户管理
- 搜索栏：支持多条件筛选
- 操作按钮：新增、编辑、删除、重置密码
- 表格展示：分页、排序

## 🐛 常见问题

### 1. 前端无法连接后端
- 检查后端服务是否启动: `curl http://localhost:8082/actuator/health`
- 检查代理配置: `vite.config.ts`
- 检查网络请求: 浏览器开发者工具

### 2. 登录失败
- 检查数据库是否初始化
- 检查用户名密码是否正确
- 查看后端日志

### 3. Token 失效
- Token 有效期 24 小时
- 过期后自动跳转登录页
- 可以实现 Token 自动刷新

### 4. 跨域问题
- 开发环境通过 Vite 代理解决
- 生产环境配置 Nginx 反向代理

## 📊 系统架构

```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│   浏览器    │────▶│  前端(3000)  │────▶│ 后端(8082)  │
│   Chrome    │     │ React + Antd │     │ Spring Boot │
└─────────────┘     └──────────────┘     └─────────────┘
                                                 │
                                                 ▼
                                          ┌─────────────┐
                                          │   MySQL     │
                                          │   Redis     │
                                          │   Nacos     │
                                          └─────────────┘
```

## 🎯 下一步

1. **完善字典管理页面** - 实现完整的字典管理功能
2. **实现系统监控页面** - 在线用户、服务器监控
3. **实现日志查看页面** - 登录日志、操作日志
4. **个人中心功能** - 修改密码、个人信息
5. **权限按钮控制** - 根据权限显示/隐藏按钮
6. **Excel 导入导出** - 用户、角色数据导入导出
7. **数据可视化** - 图表展示统计数据

## 🎊 恭喜

你现在拥有了一个完整的、生产级别的后台管理系统！

**后端**: 11个数据表 + 50+个API接口 + 完整RBAC权限
**前端**: 10+个页面 + 响应式布局 + 现代化UI

开始你的开发之旅吧！🚀
