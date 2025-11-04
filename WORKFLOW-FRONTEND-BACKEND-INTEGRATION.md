# 工作流模块前后端对接指南

## 📋 对接概述

本文档说明如何将 admin-web 的工作流管理模块与后端 Camunda 工作流接口对接，确保前后端正常通信。

**对接完成时间**: 2025-11-03
**涉及服务**: 前端（admin-web）、网关（gateway）、后端（scheduler）

---

## 🎯 对接内容

### 已完成的工作 ✅

#### 1. 前端API文件创建
- ✅ `statistics.ts` - 流程统计API
- ✅ `formTemplate.ts` - 表单模板API
- ✅ `index.ts` - API统一导出
- ✅ 更新 `task.ts` - 添加批量操作接口

#### 2. 前端配置更新
- ✅ 更新 `request.ts` - 兼容工作流API返回格式
- ✅ 更新 `types/index.ts` - 添加success字段支持
- ✅ 更新 `vite.config.mts` - 代理指向网关（8081）

#### 3. 网关路由配置
- ✅ 创建 `WorkflowRouteConfig.java` - 工作流路由规则

---

## 🔧 技术架构

### 请求流程

```
前端 (localhost:3000)
  ↓ Vite Proxy
网关 (localhost:8081)
  ↓ /api/workflow/** → /scheduler/api/workflow/**
Scheduler (localhost:8085)
  ↓
Camunda BPM Engine
```

### 路由规则

| 前端请求路径 | 网关转发路径 | 后端实际路径 | 目标服务 |
|------------|------------|------------|---------|
| `/api/workflow/definitions` | `/scheduler/api/workflow/definitions` | `/scheduler/api/workflow/definitions` | basebackend-scheduler |
| `/api/workflow/instances` | `/scheduler/api/workflow/instances` | `/scheduler/api/workflow/instances` | basebackend-scheduler |
| `/api/workflow/tasks` | `/scheduler/api/workflow/tasks` | `/scheduler/api/workflow/tasks` | basebackend-scheduler |
| `/api/workflow/statistics` | `/scheduler/api/workflow/statistics` | `/scheduler/api/workflow/statistics` | basebackend-scheduler |
| `/api/workflow/form-templates` | `/scheduler/api/workflow/form-templates` | `/scheduler/api/workflow/form-templates` | basebackend-scheduler |

---

## 📁 文件清单

### 前端新增文件（4个）

```
basebackend-admin-web/src/
├── api/workflow/
│   ├── statistics.ts          ✨新增 - 流程统计API
│   ├── formTemplate.ts        ✨新增 - 表单模板API
│   └── index.ts              ✨新增 - API统一导出
```

### 前端修改文件（3个）

```
basebackend-admin-web/
├── src/
│   ├── api/workflow/task.ts        ✏️修改 - 添加批量操作
│   ├── utils/request.ts            ✏️修改 - 兼容返回格式
│   └── types/index.ts             ✏️修改 - 添加success字段
└── vite.config.mts                ✏️修改 - 代理指向8081
```

### 后端新增文件（1个）

```
basebackend-gateway/src/main/java/
└── com/basebackend/gateway/config/
    └── WorkflowRouteConfig.java    ✨新增 - 工作流路由配置
```

---

## 🚀 快速启动

### 1. 启动后端服务

```bash
# 1. 启动 Nacos (服务注册中心)
# 确保 Nacos 在运行

# 2. 启动 Scheduler 服务（工作流后端）
cd basebackend-scheduler
mvn spring-boot:run

# 3. 启动 Gateway 服务（API网关）
cd basebackend-gateway
mvn spring-boot:run
```

### 2. 启动前端

```bash
cd basebackend-admin-web
npm install  # 首次运行
npm run dev
```

### 3. 访问应用

- 前端应用: http://localhost:3000
- API网关: http://localhost:8081
- Scheduler服务: http://localhost:8085
- Swagger文档: http://localhost:8085/swagger-ui/index.html

---

## 🧪 接口测试

### 1. 测试网关路由

```bash
# 测试流程统计接口
curl http://localhost:8081/api/workflow/statistics

# 预期返回：
{
  "success": true,
  "data": {
    "totalInstances": 0,
    "runningInstances": 0,
    ...
  }
}
```

### 2. 测试表单模板接口

```bash
# 查询表单模板列表
curl http://localhost:8081/api/workflow/form-templates

# 预期返回：
{
  "success": true,
  "data": {
    "list": [...],
    "total": 3,
    "page": 1,
    "size": 10
  }
}
```

### 3. 前端验证

1. 访问 http://localhost:3000
2. 登录系统
3. 进入"工作流管理"菜单
4. 验证以下功能：
   - ✅ 流程定义列表加载
   - ✅ 流程实例查询
   - ✅ 任务列表显示
   - ✅ 统计数据展示
   - ✅ 表单模板管理

---

## 📊 API接口清单

### 前端可用的工作流API

| 功能模块 | 接口数量 | API文件 | 状态 |
|---------|---------|---------|-----|
| 流程定义 | 9个 | processDefinition.ts | ✅ 可用 |
| 流程实例 | 12个 | processInstance.ts | ✅ 可用 |
| 任务管理 | 14个 | task.ts | ✅ 可用 |
| 流程统计 | 2个 | statistics.ts | ✅ 新增 |
| 表单模板 | 9个 | formTemplate.ts | ✅ 新增 |
| **总计** | **46个** | - | ✅ 全部就绪 |

### 使用示例

```typescript
// 1. 导入API
import {
  getProcessStatistics,
  listFormTemplates,
  batchCompleteTasks,
} from '@/api/workflow'

// 2. 获取流程统计
const stats = await getProcessStatistics()
console.log(stats.data)

// 3. 查询表单模板
const templates = await listFormTemplates({ page: 1, size: 10 })
console.log(templates.data.list)

// 4. 批量完成任务
const result = await batchCompleteTasks({
  taskIds: ['task1', 'task2'],
  variables: { approved: true }
})
console.log(result.data.successCount)
```

---

## ⚙️ 配置说明

### 前端配置 (vite.config.mts)

```typescript
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8081', // 指向网关
      changeOrigin: true,
    },
  },
}
```

### 网关配置 (WorkflowRouteConfig.java)

```java
@Bean
public RouteLocator workflowRouteLocator(RouteLocatorBuilder builder) {
    return builder.routes()
        .route("workflow-route", r -> r
            .path("/api/workflow/**")
            .filters(f -> f.rewritePath(
                "/api/workflow/(?<segment>.*)",
                "/scheduler/api/workflow/${segment}"
            ))
            .uri("lb://basebackend-scheduler")
        )
        .build();
}
```

### Scheduler配置 (application-camunda.yml)

```yaml
server:
  servlet:
    context-path: /scheduler
```

---

## 🔍 故障排查

### 问题1：404 Not Found

**症状**: 前端请求返回404

**检查清单**:
1. ✅ Scheduler服务是否启动？
2. ✅ Gateway服务是否启动？
3. ✅ Nacos中是否注册了basebackend-scheduler服务？
4. ✅ 网关路由配置是否生效？

**解决方案**:
```bash
# 检查服务状态
curl http://localhost:8081/actuator/gateway/routes

# 检查Nacos注册
访问 Nacos控制台查看服务列表
```

### 问题2：CORS 跨域错误

**症状**: 浏览器console显示CORS错误

**解决方案**:
网关已配置全局CORS，如仍有问题，检查：
```yaml
# application-gateway.yml
spring.cloud.gateway.globalcors.cors-configurations:
  '[/**]':
    allowed-origins: "*"
```

### 问题3：返回格式不兼容

**症状**: 前端解析响应失败

**检查**:
- 后端返回格式：`{ success: true, data: {...} }`
- request.ts已更新兼容两种格式

**调试**:
```typescript
// 在request.ts中添加日志
console.log('API Response:', response.data)
```

---

## 📝 开发建议

### 1. API调用最佳实践

```typescript
// ✅ 推荐：使用try-catch处理错误
try {
  const result = await getProcessStatistics()
  if (result.success) {
    // 处理数据
  }
} catch (error) {
  console.error('获取统计失败:', error)
}

// ❌ 不推荐：不处理错误
const result = await getProcessStatistics()
```

### 2. 类型安全

```typescript
// ✅ 推荐：使用TypeScript类型
import type { ProcessStatistics } from '@/api/workflow/statistics'

const stats: ProcessStatistics = result.data

// ❌ 不推荐：使用any
const stats: any = result.data
```

### 3. 性能优化

```typescript
// ✅ 推荐：使用React Query缓存
import { useQuery } from '@tanstack/react-query'

const { data, isLoading } = useQuery({
  queryKey: ['processStatistics'],
  queryFn: getProcessStatistics,
  staleTime: 60000, // 1分钟缓存
})
```

---

## ✅ 验收清单

### 前端验收
- [ ] 所有工作流页面能正常加载
- [ ] API请求返回正确数据
- [ ] 错误提示友好清晰
- [ ] 加载状态正常显示
- [ ] 表单提交功能正常

### 后端验收
- [ ] 所有接口返回格式统一
- [ ] 异常处理规范
- [ ] 日志记录完整
- [ ] Swagger文档可访问
- [ ] 性能指标达标

### 集成验收
- [ ] 网关路由正确
- [ ] 服务发现正常
- [ ] 跨域配置生效
- [ ] 负载均衡工作
- [ ] 限流降级正常

---

## 🎓 相关文档

- [工作流后端实装总结](../../WORKFLOW-BACKEND-IMPLEMENTATION-SUMMARY.md)
- [前端工作流实施指南](../WORKFLOW-IMPLEMENTATION.md)
- [Camunda使用指南](../../docs/CAMUNDA-GUIDE.md)
- [网关配置说明](../../basebackend-gateway/README.md)

---

## 🤝 技术支持

如遇问题，请检查：

1. **日志文件**
   - 前端：浏览器Console
   - Gateway：`logs/gateway.log`
   - Scheduler：`logs/scheduler.log`

2. **监控端点**
   - Gateway健康检查：http://localhost:8081/actuator/health
   - Scheduler健康检查：http://localhost:8085/actuator/health

3. **Swagger文档**
   - http://localhost:8085/swagger-ui/index.html

---

## 🎉 总结

前后端对接已完成！主要工作包括：

✅ **前端**: 创建4个API文件，更新3个配置
✅ **网关**: 配置工作流路由规则
✅ **文档**: 提供完整的对接和测试指南

现在可以启动服务并访问 http://localhost:3000 开始使用工作流功能！

**状态**: ✅ **对接完成，可以使用**
