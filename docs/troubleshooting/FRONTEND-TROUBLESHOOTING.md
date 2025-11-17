# 前端页面白屏问题排查指南

## 问题已解决

开发服务器端口被占用，已自动切换到 **3001** 端口。

### 正确访问地址
- ✅ http://localhost:3001/system/application（应用管理）
- ✅ http://localhost:3001/system/application-resource（资源管理）

---

## 如果仍然白屏，请按以下步骤排查

### 1. 检查开发服务器是否正常运行
```bash
cd basebackend-admin-web
npm run dev
```

查看输出，确认端口号（可能是3000或3001）

### 2. 检查浏览器控制台错误

打开浏览器开发者工具（F12），查看 Console 和 Network 标签：

#### Console 标签常见错误

**错误1: 路由不存在**
```
No routes matched location "/system/application"
```
**解决方案**: 检查 `src/router/index.tsx` 是否正确添加了路由

**错误2: 模块导入失败**
```
Failed to resolve module specifier "@/pages/System/Application"
```
**解决方案**: 检查文件是否存在，路径是否正确

**错误3: 组件报错**
```
Cannot read property 'xxx' of undefined
```
**解决方案**: 检查组件中的数据访问逻辑

#### Network 标签常见问题

**问题1: API 404错误**
```
GET http://localhost:3001/api/admin/application/list 404
```
**解决方案**:
- 确认后端服务已启动（端口8081）
- 检查 vite.config.mts 中的 proxy 配置

**问题2: API 401错误**
```
GET http://localhost:3001/api/admin/application/list 401
```
**解决方案**:
- 需要先登录系统
- 访问 http://localhost:3001/login

### 3. 检查文件是否正确创建

```bash
cd basebackend-admin-web

# 检查应用管理页面
ls -la src/pages/System/Application/index.tsx

# 检查应用资源管理页面
ls -la src/pages/System/ApplicationResource/index.tsx

# 检查API文件
ls -la src/api/application.ts

# 检查路由配置
grep -A 5 "ApplicationManagement" src/router/index.tsx
```

### 4. 检查后端服务是否正常

```bash
# 检查后端是否运行
curl http://localhost:8081/api/admin/application/list

# 如果返回401，需要先获取token
# 登录获取token
curl -X POST http://localhost:8081/api/admin/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 使用token访问
curl http://localhost:8081/api/admin/application/list \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 5. 清理缓存重新构建

```bash
cd basebackend-admin-web

# 清理node_modules
rm -rf node_modules package-lock.json

# 重新安装依赖
npm install

# 重新启动
npm run dev
```

### 6. 检查 vite.config.mts 代理配置

文件路径: `basebackend-admin-web/vite.config.mts`

确保包含以下配置：
```typescript
export default defineConfig({
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        // rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
  // ...
})
```

---

## 常见白屏原因总结

### 前端问题
1. ❌ 路由未配置或配置错误
2. ❌ 组件导入路径错误
3. ❌ 组件内部JavaScript错误
4. ❌ 缺少必要的依赖包
5. ❌ TypeScript类型错误

### 后端问题
6. ❌ 后端服务未启动
7. ❌ API接口路径错误
8. ❌ 未登录（需要先访问登录页）
9. ❌ CORS跨域问题
10. ❌ 后端返回数据格式不符合前端预期

---

## 快速验证步骤

### Step 1: 验证路由配置
```bash
cd basebackend-admin-web
grep -n "ApplicationManagement\|ApplicationResource" src/router/index.tsx
```

应该看到：
```typescript
import ApplicationManagement from '@/pages/System/Application'
import ApplicationResourceManagement from '@/pages/System/ApplicationResource'
...
<Route path="system/application" element={<ApplicationManagement />} />
<Route path="system/application-resource" element={<ApplicationResourceManagement />} />
```

### Step 2: 验证文件存在
```bash
ls -la src/pages/System/Application/index.tsx
ls -la src/pages/System/ApplicationResource/index.tsx
ls -la src/api/application.ts
```

### Step 3: 验证后端服务
```bash
curl http://localhost:8081/api/admin/application/enabled
```

### Step 4: 访问登录页面
先确保已登录：http://localhost:3001/login
- 用户名: admin
- 密码: admin123（根据实际情况）

### Step 5: 访问目标页面
登录后访问：http://localhost:3001/system/application

---

## 调试技巧

### 1. 添加调试信息
在组件中添加 console.log：

```typescript
const ApplicationManagement = () => {
  console.log('ApplicationManagement component mounted')

  useEffect(() => {
    console.log('useEffect triggered')
    loadData()
  }, [])

  const loadData = async () => {
    console.log('loadData called')
    try {
      const response = await getApplicationList()
      console.log('API response:', response)
      setDataSource(response.data)
    } catch (error) {
      console.error('Load data error:', error)
    }
  }

  return <div>...</div>
}
```

### 2. 使用 React DevTools
安装 React Developer Tools 浏览器扩展，可以查看：
- 组件树
- Props 和 State
- 组件是否正确渲染

### 3. 使用 Network 面板
- 查看所有API请求
- 检查请求头（Authorization）
- 检查响应状态码和数据

---

## 完整的测试流程

```bash
# 1. 确认端口和启动服务
cd /home/wuan/IdeaProjects/basebackend/basebackend-admin-web
npm run dev
# 记下端口号（例如3001）

# 2. 打开浏览器
# 访问: http://localhost:3001

# 3. 登录系统
# 访问: http://localhost:3001/login
# 输入用户名密码登录

# 4. 访问应用管理
# 访问: http://localhost:3001/system/application

# 5. 如果白屏，打开控制台（F12）
# 查看 Console 错误信息
# 查看 Network 请求状态

# 6. 根据错误信息进行排查
```

---

## 联系支持

如果以上步骤都无法解决问题，请提供：
1. 浏览器控制台的错误截图
2. Network 面板的请求列表截图
3. 开发服务器的输出日志
4. 后端服务的运行状态

这些信息将帮助快速定位问题。
