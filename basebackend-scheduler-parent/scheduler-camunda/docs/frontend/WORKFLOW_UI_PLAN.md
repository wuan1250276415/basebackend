# 工作流页面完善实施计划

## 概述

本计划提供从零开始构建工作流管理页面的完整实施方案，包括页面结构、功能模块和开发步骤。

## 📐 页面架构设计

### 整体布局
```
┌─────────────────────────────────────────────────────────┐
│                      顶部导航栏                           │
│  Logo | 工作流管理 | 流程定义 | 任务中心 | 监控面板        │
├─────────────────────────────────────────────────────────┤
│                  面包屑导航                              │
├─────────────────────────────────────────────────────────┤
│                                                         │
│                    主内容区域                            │
│                  (根据路由动态加载)                       │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### 页面路由结构
```
/workflow
├── /workflow/dashboard          # 工作流仪表板
├── /workflow/process-definitions # 流程定义管理
│   ├── /list                    # 流程定义列表
│   ├── /deploy                  # 流程部署
│   └── /detail/:id              # 流程详情
├── /workflow/process-instances  # 流程实例管理
│   ├── /list                    # 实例列表
│   ├── /detail/:id              # 实例详情
│   └── /track/:id               # 实例跟踪
└── /workflow/tasks              # 任务中心
    ├── /my-tasks                # 我的任务
    ├── /todo                    # 待办任务
    └── /task-detail/:id         # 任务详情
```

## 🎯 分阶段实施计划

### 阶段1：基础框架搭建 (第1-2周)

#### 1.1 项目初始化
**任务清单**：
- [ ] 创建前端项目结构（推荐使用 Vue3 + TypeScript + Vite）
- [ ] 集成 UI 组件库（推荐 Element Plus 或 Ant Design Vue）
- [ ] 配置路由系统（Vue Router）
- [ ] 配置状态管理（Pinia）
- [ ] 配置 HTTP 客户端（Axios）
- [ ] 配置 TypeScript 和 ESLint

**技术选型**：
```typescript
// 推荐技术栈
- Framework: Vue 3 + TypeScript
- Build Tool: Vite
- UI Library: Element Plus / Ant Design Vue
- Router: Vue Router 4
- State Management: Pinia
- HTTP Client: Axios
- CSS Preprocessor: SCSS
- Icons: @element-plus/icons-vue
```

#### 1.2 基础组件开发
**任务清单**：
- [ ] 创建全局布局组件 (`Layout`)
- [ ] 创建侧边栏导航组件 (`Sidebar`)
- [ ] 创建顶部导航栏组件 (`Navbar`)
- [ ] 创建面包屑导航组件 (`Breadcrumb`)
- [ ] 创建通用表格组件 (`DataTable`)
- [ ] 创建通用表单组件 (`FormDialog`)
- [ ] 创建加载状态组件 (`Loading`)
- [ ] 创建空状态组件 (`Empty`)

#### 1.3 API 层封装
**任务清单**：
- [ ] 创建 HTTP 客户端封装 (`api/client.ts`)
- [ ] 创建响应拦截器
- [ ] 创建错误处理机制
- [ ] 创建 API 类型定义 (`types/api.ts`)
- [ ] 创建认证 Token 管理

**示例代码**：
```typescript
// api/client.ts
import axios from 'axios'
import { ElMessage } from 'element-plus'

const client = axios.create({
  baseURL: 'http://localhost:8089',
  timeout: 10000
})

// 请求拦截器
client.interceptors.request.use(config => {
  const token = localStorage.getItem('access_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器
client.interceptors.response.use(
  response => response.data,
  error => {
    ElMessage.error(error.response?.data?.message || '请求失败')
    return Promise.reject(error)
  }
)

export default client
```

#### 1.4 API 接口封装
**任务清单**：
- [ ] 创建流程定义 API (`api/processDefinition.ts`)
- [ ] 创建流程实例 API (`api/processInstance.ts`)
- [ ] 创建任务 API (`api/task.ts`)
- [ ] 创建历史查询 API (`api/historic.ts`)

**示例代码**：
```typescript
// api/processDefinition.ts
import client from './client'

export const processDefinitionApi = {
  // 分页查询流程定义
  list(params: ProcessDefinitionQuery) {
    return client.get('/api/camunda/process-definitions', { params })
  },

  // 部署流程
  deploy(data: DeployRequest) {
    const formData = new FormData()
    formData.append('file', data.file)
    if (data.tenantId) formData.append('tenantId', data.tenantId)
    if (data.deploymentName) formData.append('deploymentName', data.deploymentName)
    return client.post('/api/camunda/process-definitions/deployments', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  // 启动流程实例
  start(data: StartProcessRequest) {
    return client.post('/api/camunda/process-definitions/start', data)
  },

  // 获取详情
  getDetail(definitionId: string) {
    return client.get(`/api/camunda/process-definitions/${definitionId}`)
  },

  // 挂起/激活
  suspend(definitionId: string, suspended: boolean) {
    return client.put(`/api/camunda/process-definitions/${definitionId}/suspend`, {
      suspended,
      includeInstance: false
    })
  }
}
```

### 阶段2：核心功能开发 (第3-5周)

#### 2.1 流程定义管理页面
**页面功能**：
- [ ] 流程定义列表页 (`/workflow/process-definitions/list`)
- [ ] 流程部署页 (`/workflow/process-definitions/deploy`)
- [ ] 流程详情页 (`/workflow/process-definitions/detail/:id`)

**详细需求**：

**A. 列表页功能**：
```vue
<!-- 功能点 -->
- 分页查询和排序
- 多条件筛选（版本、状态、租户）
- 批量操作（挂起/激活/删除）
- 搜索功能（流程名称/Key）
- 导出功能
- 部署按钮
```

**B. 部署页功能**：
```vue
<!-- 功能点 -->
- 文件上传组件（BPMN 文件）
- 租户选择
- 部署名称输入
- 上传进度显示
- 部署结果反馈
```

**C. 详情页功能**：
```vue
<!-- 功能点 -->
- 流程基本信息展示
- BPMN 图形显示（使用 bpmn-js）
- 流程版本信息
- 部署信息
- 操作按钮（挂起/激活/删除/下载）
- 相关实例列表
```

#### 2.2 流程实例管理页面
**页面功能**：
- [ ] 流程实例列表页 (`/workflow/process-instances/list`)
- [ ] 流程实例详情页 (`/workflow/process-instances/detail/:id`)
- [ ] 流程跟踪页 (`/workflow/process-instances/track/:id`)

**详细需求**：

**A. 列表页功能**：
```vue
<!-- 功能点 -->
- 分页查询（状态、租户、时间范围）
- 高级筛选（流程定义、业务Key、发起人）
- 状态标签（运行中/已结束/已挂起）
- 操作按钮（查看详情/终止/删除/变量管理）
- 导出功能
```

**B. 详情页功能**：
```vue
<!-- 功能点 -->
- 实例基本信息
- 流程变量展示和编辑
- 当前活动节点高亮
- 操作按钮（挂起/激活/终止/删除）
- 活动历史时间线
```

**C. 跟踪页功能**：
```vue
<!-- 功能点 -->
- 流程图实时高亮当前节点
- 节点点击查看详情
- 时间线展示
- 变量快照
```

#### 2.3 任务中心页面
**页面功能**：
- [ ] 我的任务页 (`/workflow/tasks/my-tasks`)
- [ ] 待办任务页 (`/workflow/tasks/todo`)
- [ ] 任务详情页 (`/workflow/tasks/task-detail/:id`)

**详细需求**：

**A. 任务列表功能**：
```vue
<!-- 功能点 -->
- 分页查询（分配人、候选组、状态）
- 任务分组（按流程/按优先级/按截止时间）
- 批量操作（认领/完成/委托）
- 任务筛选（紧急/今日到期/逾期）
- 任务搜索（任务名称/流程名称）
```

**B. 任务详情功能**：
```vue
<!-- 功能点 -->
- 任务基本信息
- 流程变量展示和编辑
- 动态表单渲染
- 任务操作（认领/释放/完成/委托）
- 评论和附件
- 流程图高亮
```

**C. 动态表单组件**：
```typescript
// 动态表单渲染示例
interface FormField {
  type: 'text' | 'textarea' | 'number' | 'date' | 'select' | 'radio' | 'checkbox'
  label: string
  name: string
  required: boolean
  options?: { label: string; value: any }[]
  defaultValue?: any
}

const renderFormField = (field: FormField) => {
  switch (field.type) {
    case 'text':
      return <el-input v-model={formData[field.name]} />
    case 'number':
      return <el-input-number v-model={formData[field.name]} />
    case 'select':
      return (
        <el-select v-model={formData[field.name]}>
          {field.options?.map(option => (
            <el-option label={option.label} value={option.value} />
          ))}
        </el-select>
      )
    // ... 其他类型
  }
}
```

### 阶段3：高级功能开发 (第6-7周)

#### 3.1 工作流仪表板
**页面功能**：
- [ ] 统计卡片（运行实例数、待办任务数、失败任务数）
- [ ] 流程实例趋势图
- [ ] 任务分布饼图
- [ ] 最近活动列表
- [ ] 失败任务列表

#### 3.2 监控运维页面
**页面功能**：
- [ ] 引擎状态监控
- [ ] Job 执行情况
- [ ] 性能指标展示
- [ ] 异常日志查询

#### 3.3 历史查询页面
**页面功能**：
- [ ] 历史实例查询
- [ ] 流程实例追踪
- [ ] 任务完成统计
- [ ] 数据导出

### 阶段4：体验优化 (第8周)

#### 4.1 性能优化
- [ ] 虚拟滚动（长列表）
- [ ] 表格懒加载
- [ ] 路由懒加载
- [ ] 组件缓存
- [ ] 图片懒加载

#### 4.2 用户体验优化
- [ ] 骨架屏加载
- [ ] 空状态页面
- [ ] 操作反馈（Toast/Confirm）
- [ ] 快捷键支持
- [ ] 主题切换

#### 4.3 移动端适配
- [ ] 响应式布局
- [ ] 移动端菜单
- [ ] 触摸手势支持
- [ ] 移动端优化

## 🎨 组件设计规范

### 通用组件清单

#### 1. DataTable 组件
```vue
<template>
  <div class="data-table">
    <!-- 搜索栏 -->
    <div class="table-toolbar">
      <slot name="toolbar">
        <el-button type="primary" @click="$emit('add')">新增</el-button>
      </slot>
    </div>

    <!-- 表格 -->
    <el-table
      :data="data"
      :loading="loading"
      @selection-change="handleSelectionChange"
    >
      <slot />
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-if="showPagination"
      :current-page="pageNo"
      :page-size="pageSize"
      :total="total"
      @current-change="handlePageChange"
    />
  </div>
</template>
```

#### 2. FormDialog 组件
```vue
<template>
  <el-dialog
    v-model="visible"
    :title="title"
    :width="width"
    @close="handleClose"
  >
    <el-form ref="formRef" :model="form" :rules="rules">
      <slot />
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitting">
        确定
      </el-button>
    </template>
  </el-dialog>
</template>
```

#### 3. ProcessViewer 组件（流程图查看器）
```vue
<template>
  <div class="process-viewer" ref="container">
    <div ref="canvas" class="canvas" />
  </div>
</template>

<script setup lang="ts">
import BpmnModeler from 'bpmn-js/lib/Modeler'
import { ref, onMounted, watch } from 'vue'

const props = defineProps<{
  xml: string
  highlightedNodes?: string[]
}>()

const container = ref<HTMLElement>()
const canvas = ref<HTMLElement>()
let modeler: BpmnModeler

onMounted(() => {
  modeler = new BpmnModeler({
    container: canvas.value
  })
  loadDiagram()
})

const loadDiagram = async () => {
  try {
    await modeler.importXML(props.xml)
    highlightElements()
  } catch (error) {
    console.error('加载流程图失败:', error)
  }
}

const highlightElements = () => {
  if (!props.highlightedNodes?.length) return

  const elementRegistry = modeler.get('elementRegistry')
  props.highlightedNodes.forEach(nodeId => {
    const element = elementRegistry.get(nodeId)
    if (element) {
      modeler.get('canvas').addMarker(nodeId, 'highlight')
    }
  })
}
</script>
```

## 📱 响应式设计

### 断点设置
```scss
$breakpoints: (
  xs: 0,
  sm: 576px,
  md: 768px,
  lg: 992px,
  xl: 1200px,
  xxl: 1600px
);

@mixin respond-to($breakpoint) {
  @media (min-width: map-get($breakpoints, $breakpoint)) {
    @content;
  }
}
```

### 布局适配
```vue
<template>
  <div class="workflow-layout">
    <!-- 桌面端侧边栏 -->
    <el-aside class="sidebar desktop-only" :width="240">
      <WorkflowSidebar />
    </el-aside>

    <!-- 移动端抽屉 -->
    <el-drawer
      v-model="mobileDrawerVisible"
      direction="ltr"
      class="mobile-only"
    >
      <WorkflowSidebar @navigate="handleNavigate" />
    </el-drawer>

    <!-- 主内容区 -->
    <el-main class="main-content">
      <router-view />
    </el-main>
  </div>
</template>
```

## 🔒 权限控制

### 路由守卫
```typescript
// router/guards.ts
import { useUserStore } from '@/stores/user'

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  // 检查登录状态
  if (!userStore.isLoggedIn) {
    next('/login')
    return
  }

  // 检查权限
  const requiredPermission = to.meta.permission as string
  if (requiredPermission && !userStore.hasPermission(requiredPermission)) {
    ElMessage.warning('没有权限访问该页面')
    next('/403')
    return
  }

  next()
})
```

### 指令控制
```typescript
// directives/permission.ts
import { useUserStore } from '@/stores/user'

export default {
  mounted(el: HTMLElement, binding: any) {
    const userStore = useUserStore()
    const permission = binding.value

    if (permission && !userStore.hasPermission(permission)) {
      el.style.display = 'none'
    }
  }
}
```

## 📊 数据流设计

### Pinia 状态管理
```typescript
// stores/workflow.ts
import { defineStore } from 'pinia'
import { processDefinitionApi } from '@/api/workflow'

export const useWorkflowStore = defineStore('workflow', {
  state: () => ({
    processDefinitions: [] as ProcessDefinition[],
    loading: false,
    pagination: {
      pageNo: 1,
      pageSize: 20,
      total: 0
    }
  }),

  actions: {
    async fetchProcessDefinitions(params: any) {
      this.loading = true
      try {
        const result = await processDefinitionApi.list(params)
        this.processDefinitions = result.data.data
        this.pagination = {
          pageNo: result.data.pageNo,
          pageSize: result.data.pageSize,
          total: result.data.total
        }
      } finally {
        this.loading = false
      }
    },

    async deployProcess(data: DeployRequest) {
      await processDefinitionApi.deploy(data)
      this.fetchProcessDefinitions(this.pagination)
    }
  }
})
```

## 🧪 测试策略

### 单元测试
```typescript
// tests/api/workflow.test.ts
import { processDefinitionApi } from '@/api/workflow'
import client from '@/api/client'

jest.mock('@/api/client')

describe('Workflow API', () => {
  test('should fetch process definitions', async () => {
    const mockResponse = {
      success: true,
      data: {
        total: 1,
        data: [{ id: '1', name: 'Test Process' }]
      }
    }
    ;(client.get as jest.Mock).mockResolvedValue(mockResponse)

    const result = await processDefinitionApi.list({ pageNo: 1 })
    expect(result.data.data).toHaveLength(1)
  })
})
```

### E2E 测试
```typescript
// tests/e2e/workflow.spec.ts
import { test, expect } from '@playwright/test'

test('should deploy process successfully', async ({ page }) => {
  await page.goto('/workflow/process-definitions/deploy')

  // 上传文件
  const fileInput = page.locator('input[type="file"]')
  await fileInput.setInputFiles('test/process.bpmn')

  // 点击部署按钮
  await page.click('button:has-text("部署")')

  // 验证成功消息
  await expect(page.locator('.el-message')).toContainText('部署成功')
})
```

## 📈 监控和日志

### 埋点统计
```typescript
// utils/analytics.ts
export const trackPageView = (pageName: string) => {
  // 发送页面访问统计
  console.log('Page View:', pageName)
}

export const trackEvent = (eventName: string, properties?: any) => {
  // 发送事件统计
  console.log('Event:', eventName, properties)
}
```

### 错误监控
```typescript
// utils/errorHandler.ts
window.addEventListener('error', (event) => {
  console.error('Global Error:', event.error)
  // 发送错误到监控平台
})

window.addEventListener('unhandledrejection', (event) => {
  console.error('Unhandled Promise Rejection:', event.reason)
  // 发送错误到监控平台
})
```

## 📝 开发规范

### Git 提交规范
```
feat: 新功能
fix: 修复bug
docs: 文档更新
style: 代码格式调整
refactor: 代码重构
test: 测试相关
chore: 构建或辅助工具变动
```

### 代码审查清单
- [ ] 功能实现完整性
- [ ] 代码质量和可读性
- [ ] 错误处理
- [ ] 性能优化
- [ ] 安全性检查
- [ ] 测试覆盖率
- [ ] 文档更新

## 🎯 里程碑计划

| 阶段 | 时间 | 交付物 |
|------|------|--------|
| 阶段1 | 第1-2周 | 基础框架、API封装、通用组件 |
| 阶段2 | 第3-5周 | 流程定义、流程实例、任务中心页面 |
| 阶段3 | 第6-7周 | 仪表板、监控、历史查询 |
| 阶段4 | 第8周 | 性能优化、移动端适配、测试 |

## 📞 技术支持

开发过程中遇到问题，可以：
1. 查阅 API 文档：`/docs/frontend/API_INTEGRATION_GUIDE.md`
2. 参考代码示例
3. 联系后端团队
4. 查看 Camunda 官方文档
