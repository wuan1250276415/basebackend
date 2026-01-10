# 工作流前端代码示例

## 概述

本文档提供完整的前端代码示例，展示如何对接工作流 API 并构建用户界面。

## 📁 项目结构

```
frontend/
├── src/
│   ├── api/                    # API 接口
│   │   ├── workflow/
│   │   │   ├── processDefinition.ts
│   │   │   ├── processInstance.ts
│   │   │   └── task.ts
│   │   └── client.ts           # HTTP 客户端
│   ├── components/             # 通用组件
│   │   ├── WorkflowTable.vue
│   │   ├── ProcessViewer.vue
│   │   └── TaskForm.vue
│   ├── views/                  # 页面组件
│   │   ├── workflow/
│   │   │   ├── ProcessDefinitionList.vue
│   │   │   ├── ProcessInstanceList.vue
│   │   │   └── TaskCenter.vue
│   │   └── layout/
│   │       ├── Layout.vue
│   │       └── Sidebar.vue
│   ├── stores/                 # 状态管理
│   │   └── workflow.ts
│   ├── types/                  # 类型定义
│   │   └── workflow.ts
│   └── router/                 # 路由配置
│       └── index.ts
```

## 🔧 基础配置

### 1. HTTP 客户端配置

**文件**: `src/api/client.ts`
```typescript
import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

interface ResponseData<T = any> {
  success: boolean
  message: string
  data: T
  code: number
  timestamp: string
}

class HttpClient {
  private client: AxiosInstance

  constructor() {
    this.client = axios.create({
      baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8089',
      timeout: 30000,
      headers: {
        'Content-Type': 'application/json'
      }
    })

    this.setupInterceptors()
  }

  private setupInterceptors() {
    // 请求拦截器
    this.client.interceptors.request.use(
      (config) => {
        const token = localStorage.getItem('access_token')
        if (token) {
          config.headers.Authorization = `Bearer ${token}`
        }
        return config
      },
      (error) => {
        return Promise.reject(error)
      }
    )

    // 响应拦截器
    this.client.interceptors.response.use(
      (response: AxiosResponse<ResponseData>) => {
        const { data } = response

        // 业务状态码处理
        if (data.code === 401) {
          ElMessageBox.alert('登录已过期，请重新登录', '提示', {
            confirmButtonText: '确定',
            callback: () => {
              localStorage.removeItem('access_token')
              window.location.href = '/login'
            }
          })
          return Promise.reject(new Error('未授权'))
        }

        if (data.code === 403) {
          ElMessage.error('没有权限访问该资源')
          return Promise.reject(new Error('禁止访问'))
        }

        return data
      },
      (error) => {
        const message = error.response?.data?.message || error.message || '请求失败'
        ElMessage.error(message)
        return Promise.reject(error)
      }
    )
  }

  public get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return this.client.get(url, config).then(res => res.data)
  }

  public post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return this.client.post(url, data, config).then(res => res.data)
  }

  public put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return this.client.put(url, data, config).then(res => res.data)
  }

  public delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return this.client.delete(url, config).then(res => res.data)
  }
}

export const httpClient = new HttpClient()
export default httpClient
```

### 2. API 接口封装

**文件**: `src/api/workflow/processDefinition.ts`
```typescript
import httpClient from '../client'

// 类型定义
export interface ProcessDefinition {
  id: string
  key: string
  name: string
  version: number
  deploymentId: string
  resourceName: string
  tenantId: string
  suspended: boolean
  createTime: string
}

export interface ProcessDefinitionQuery {
  pageNo: number
  pageSize: number
  key?: string
  name?: string
  version?: number
  latestVersion?: boolean
  suspended?: boolean
  tenantId?: string
}

export interface DeployRequest {
  file: File
  tenantId?: string
  deploymentName?: string
}

export interface StartProcessRequest {
  processDefinitionId?: string
  processDefinitionKey?: string
  businessKey?: string
  variables?: Record<string, any>
  tenantId?: string
}

export const processDefinitionApi = {
  /**
   * 分页查询流程定义
   */
  list(params: ProcessDefinitionQuery) {
    return httpClient.get<{
      success: boolean
      data: {
        total: number
        pageNo: number
        pageSize: number
        data: ProcessDefinition[]
      }
    }>('/api/camunda/process-definitions', { params })
  },

  /**
   * 部署流程定义
   */
  async deploy(data: DeployRequest) {
    const formData = new FormData()
    formData.append('file', data.file)
    if (data.tenantId) formData.append('tenantId', data.tenantId)
    if (data.deploymentName) formData.append('deploymentName', data.deploymentName)

    return httpClient.post<{
      success: boolean
      message: string
      data: string
    }>('/api/camunda/process-definitions/deployments', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  /**
   * 启动流程实例
   */
  start(data: StartProcessRequest) {
    return httpClient.post<{
      success: boolean
      message: string
      data: ProcessInstance
    }>('/api/camunda/process-definitions/start', data)
  },

  /**
   * 获取流程定义详情
   */
  getDetail(definitionId: string) {
    return httpClient.get<{
      success: boolean
      data: ProcessDefinition
    }>(`/api/camunda/process-definitions/${definitionId}`)
  },

  /**
   * 挂起/激活流程定义
   */
  suspend(definitionId: string, suspended: boolean) {
    return httpClient.put(`/api/camunda/process-definitions/${definitionId}/suspend`, {
      suspended,
      includeInstance: false,
      executionDate: null
    })
  },

  /**
   * 删除流程定义
   */
  delete(definitionId: string, cascade: boolean = true) {
    return httpClient.delete(`/api/camunda/process-definitions/${definitionId}`, {
      params: {
        cascade,
        skipCustomListeners: false
      }
    })
  },

  /**
   * 下载 BPMN 文件
   */
  downloadBpmn(definitionId: string) {
    return httpClient.get(`/api/camunda/process-definitions/${definitionId}/bpmn`, {
      responseType: 'blob'
    })
  }
}
```

**文件**: `src/api/workflow/processInstance.ts`
```typescript
import httpClient from '../client'

export interface ProcessInstance {
  id: string
  processDefinitionId: string
  processDefinitionKey: string
  processDefinitionName: string
  businessKey?: string
  tenantId: string
  state: string
  startTime: string
  endTime?: string
  durationInMillis?: number
}

export interface ProcessInstanceQuery {
  pageNo: number
  pageSize: number
  processDefinitionId?: string
  processDefinitionKey?: string
  businessKey?: string
  state?: string
  tenantId?: string
  startTimeFrom?: string
  startTimeTo?: string
}

export const processInstanceApi = {
  /**
   * 分页查询流程实例
   */
  list(params: ProcessInstanceQuery) {
    return httpClient.get<{
      success: boolean
      data: {
        total: number
        pageNo: number
        pageSize: number
        data: ProcessInstance[]
      }
    }>('/api/camunda/process-instances', { params })
  },

  /**
   * 获取流程实例详情
   */
  getDetail(instanceId: string, withVariables: boolean = true) {
    return httpClient.get(`/api/camunda/process-instances/${instanceId}`, {
      params: { withVariables }
    })
  },

  /**
   * 终止流程实例
   */
  terminate(instanceId: string, reason?: string) {
    return httpClient.post(`/api/camunda/process-instances/${instanceId}/terminate`, {
      reason: reason || '用户终止'
    })
  },

  /**
   * 挂起流程实例
   */
  suspend(instanceId: string, suspended: boolean = true) {
    return httpClient.put(`/api/camunda/process-instances/${instanceId}/suspend`, {
      suspended,
      executionDate: null
    })
  },

  /**
   * 删除流程实例
   */
  delete(instanceId: string, reason?: string) {
    return httpClient.delete(`/api/camunda/process-instances/${instanceId}`, {
      params: {
        deleteReason: reason || '用户删除',
        skipCustomListeners: false
      }
    })
  },

  /**
   * 获取流程变量
   */
  getVariables(instanceId: string, local: boolean = false) {
    return httpClient.get(`/api/camunda/process-instances/${instanceId}/variables`, {
      params: { local }
    })
  },

  /**
   * 设置流程变量
   */
  setVariables(instanceId: string, variables: Record<string, any>) {
    return httpClient.put(`/api/camunda/process-instances/${instanceId}/variables`, {
      variables
    })
  }
}
```

**文件**: `src/api/workflow/task.ts`
```typescript
import httpClient from '../client'

export interface Task {
  id: string
  name: string
  assignee?: string
  owner?: string
  description?: string
  processInstanceId: string
  processDefinitionId: string
  processDefinitionName: string
  taskDefinitionKey: string
  priority: number
  createTime: string
  dueDate?: string
  followUpDate?: string
  tenantId: string
  state: string
}

export interface TaskQuery {
  pageNo: number
  pageSize: number
  assignee?: string
  candidateUser?: string
  candidateGroup?: string
  processInstanceId?: string
  processDefinitionKey?: string
  taskName?: string
  priority?: number
  state?: string
  tenantId?: string
  createdFrom?: string
  createdTo?: string
}

export interface CompleteTaskRequest {
  variables?: Record<string, any>
  completeTask?: boolean
}

export const taskApi = {
  /**
   * 分页查询任务
   */
  list(params: TaskQuery) {
    return httpClient.get<{
      success: boolean
      data: {
        total: number
        pageNo: number
        pageSize: number
        data: Task[]
      }
    }>('/api/camunda/tasks', { params })
  },

  /**
   * 获取任务详情
   */
  getDetail(taskId: string) {
    return httpClient.get(`/api/camunda/tasks/${taskId}`)
  },

  /**
   * 认领任务
   */
  claim(taskId: string, userId: string) {
    return httpClient.post(`/api/camunda/tasks/${taskId}/claim`, {
      userId
    })
  },

  /**
   * 释放任务
   */
  release(taskId: string) {
    return httpClient.post(`/api/camunda/tasks/${taskId}/release`)
  },

  /**
   * 完成任务
   */
  complete(taskId: string, data: CompleteTaskRequest) {
    return httpClient.post(`/api/camunda/tasks/${taskId}/complete`, data)
  },

  /**
   * 委托任务
   */
  delegate(taskId: string, userId: string) {
    return httpClient.post(`/api/camunda/tasks/${taskId}/delegate`, {
      userId
    })
  },

  /**
   * 获取任务变量
   */
  getVariables(taskId: string, local: boolean = true) {
    return httpClient.get(`/api/camunda/tasks/${taskId}/variables`, {
      params: { local }
    })
  },

  /**
   * 设置任务变量
   */
  setVariables(taskId: string, variables: Record<string, any>) {
    return httpClient.put(`/api/camunda/tasks/${taskId}/variables`, {
      variables
    })
  },

  /**
   * 获取任务评论
   */
  getComments(taskId: string) {
    return httpClient.get(`/api/camunda/tasks/${taskId}/comments`)
  },

  /**
   * 添加任务评论
   */
  addComment(taskId: string, message: string) {
    return httpClient.post(`/api/camunda/tasks/${taskId}/comments`, {
      message
    })
  }
}
```

## 🎨 页面组件示例

### 1. 流程定义列表页

**文件**: `src/views/workflow/ProcessDefinitionList.vue`
```vue
<template>
  <div class="process-definition-list">
    <!-- 搜索栏 -->
    <div class="toolbar">
      <el-form :model="searchForm" inline>
        <el-form-item label="流程名称">
          <el-input
            v-model="searchForm.name"
            placeholder="请输入流程名称"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="流程Key">
          <el-input
            v-model="searchForm.key"
            placeholder="请输入流程Key"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="租户">
          <el-select
            v-model="searchForm.tenantId"
            placeholder="请选择租户"
            clearable
            style="width: 150px"
          >
            <el-option label="默认租户" value="default" />
            <el-option label="租户A" value="tenantA" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 操作栏 -->
    <div class="actions">
      <el-button type="primary" @click="handleDeploy">
        <el-icon><Upload /></el-icon>
        部署流程
      </el-button>
      <el-button @click="handleRefresh">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <!-- 表格 -->
    <el-table
      v-loading="loading"
      :data="tableData"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" />
      <el-table-column prop="name" label="流程名称" min-width="150" />
      <el-table-column prop="key" label="流程Key" width="120" />
      <el-table-column prop="version" label="版本" width="80">
        <template #default="{ row }">
          <el-tag type="success">v{{ row.version }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="tenantId" label="租户" width="100" />
      <el-table-column prop="suspended" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.suspended ? 'danger' : 'success'">
            {{ row.suspended ? '已挂起' : '运行中' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="部署时间" width="180">
        <template #default="{ row }">
          {{ formatDateTime(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="handleView(row)">查看</el-button>
          <el-button
            size="small"
            type="primary"
            @click="handleStart(row)"
          >
            启动
          </el-button>
          <el-button
            size="small"
            :type="row.suspended ? 'success' : 'warning'"
            @click="handleToggleStatus(row)"
          >
            {{ row.suspended ? '激活' : '挂起' }}
          </el-button>
          <el-dropdown>
            <el-button size="small">
              更多<el-icon><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleDownloadBpmn(row)">
                  下载BPMN
                </el-dropdown-item>
                <el-dropdown-item @click="handleViewInstances(row)">
                  查看实例
                </el-dropdown-item>
                <el-dropdown-item
                  divided
                  @click="handleDelete(row)"
                >
                  删除
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-model:current-page="pagination.pageNo"
      v-model:page-size="pagination.pageSize"
      :total="pagination.total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />

    <!-- 部署对话框 -->
    <DeployDialog
      v-model="deployVisible"
      @success="handleDeploySuccess"
    />

    <!-- 启动对话框 -->
    <StartProcessDialog
      v-model="startVisible"
      :definition="selectedDefinition"
      @success="handleStartSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, Refresh, ArrowDown } from '@element-plus/icons-vue'
import { processDefinitionApi, type ProcessDefinition, type ProcessDefinitionQuery } from '@/api/workflow/processDefinition'
import DeployDialog from './components/DeployDialog.vue'
import StartProcessDialog from './components/StartProcessDialog.vue'

// 数据状态
const loading = ref(false)
const tableData = ref<ProcessDefinition[]>([])
const selectedRows = ref<ProcessDefinition[]>([])

// 搜索表单
const searchForm = reactive<Partial<ProcessDefinitionQuery>>({
  name: '',
  key: '',
  tenantId: 'default'
})

// 分页信息
const pagination = reactive({
  pageNo: 1,
  pageSize: 20,
  total: 0
})

// 对话框状态
const deployVisible = ref(false)
const startVisible = ref(false)
const selectedDefinition = ref<ProcessDefinition | null>(null)

// 获取列表数据
const fetchData = async () => {
  loading.value = true
  try {
    const params: ProcessDefinitionQuery = {
      pageNo: pagination.pageNo,
      pageSize: pagination.pageSize,
      ...searchForm
    }
    const response = await processDefinitionApi.list(params)
    if (response.success) {
      tableData.value = response.data.data
      pagination.total = response.data.total
    }
  } catch (error) {
    console.error('获取流程定义列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.pageNo = 1
  fetchData()
}

// 重置
const handleReset = () => {
  Object.assign(searchForm, {
    name: '',
    key: '',
    tenantId: 'default'
  })
  pagination.pageNo = 1
  fetchData()
}

// 刷新
const handleRefresh = () => {
  fetchData()
}

// 部署流程
const handleDeploy = () => {
  deployVisible.value = true
}

// 部署成功
const handleDeploySuccess = () => {
  deployVisible.value = false
  fetchData()
  ElMessage.success('流程部署成功')
}

// 启动流程
const handleStart = (row: ProcessDefinition) => {
  selectedDefinition.value = row
  startVisible.value = true
}

// 启动成功
const handleStartSuccess = () => {
  startVisible.value = false
  ElMessage.success('流程启动成功')
}

// 切换状态
const handleToggleStatus = async (row: ProcessDefinition) => {
  try {
    await ElMessageBox.confirm(
      `确定要${row.suspended ? '激活' : '挂起'}该流程定义吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await processDefinitionApi.suspend(row.id, !row.suspended)
    ElMessage.success(`${row.suspended ? '激活' : '挂起'}成功`)
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('操作失败:', error)
    }
  }
}

// 查看详情
const handleView = (row: ProcessDefinition) => {
  // 跳转到详情页
  window.open(`/workflow/process-definitions/detail/${row.id}`, '_blank')
}

// 查看实例
const handleViewInstances = (row: ProcessDefinition) => {
  // 跳转到实例列表页，并筛选当前流程
  window.open(`/workflow/process-instances?definitionId=${row.id}`, '_blank')
}

// 下载BPMN
const handleDownloadBpmn = async (row: ProcessDefinition) => {
  try {
    const response = await processDefinitionApi.downloadBpmn(row.id)
    const blob = new Blob([response], { type: 'application/xml' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${row.key}-v${row.version}.bpmn`
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('下载成功')
  } catch (error) {
    console.error('下载失败:', error)
  }
}

// 删除
const handleDelete = async (row: ProcessDefinition) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除该流程定义吗？此操作不可恢复！',
      '警告',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'error'
      }
    )

    await processDefinitionApi.delete(row.id, true)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

// 选中项变化
const handleSelectionChange = (selection: ProcessDefinition[]) => {
  selectedRows.value = selection
}

// 分页大小变化
const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  fetchData()
}

// 页码变化
const handleCurrentChange = (page: number) => {
  pagination.pageNo = page
  fetchData()
}

// 格式化日期时间
const formatDateTime = (dateString: string) => {
  return new Date(dateString).toLocaleString('zh-CN')
}

// 初始化
onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.process-definition-list {
  padding: 20px;
}

.toolbar {
  margin-bottom: 20px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;
}

.actions {
  margin-bottom: 16px;
}

.el-table {
  margin-bottom: 16px;
}
</style>
```

### 2. 任务中心页

**文件**: `src/views/workflow/TaskCenter.vue`
```vue
<template>
  <div class="task-center">
    <!-- 任务统计 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-number">{{ taskStats.myTasks }}</div>
            <div class="stat-label">我的任务</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-number">{{ taskStats.todoTasks }}</div>
            <div class="stat-label">待认领</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-number">{{ taskStats.overdueTasks }}</div>
            <div class="stat-label">已逾期</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-number">{{ taskStats.completedToday }}</div>
            <div class="stat-label">今日完成</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 标签页 -->
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="我的任务" name="my-tasks">
        <TaskList
          :assignee="currentUserId"
          @refresh="fetchTaskStats"
        />
      </el-tab-pane>
      <el-tab-pane label="待认领" name="todo">
        <TaskList
          :candidate-user="currentUserId"
          @refresh="fetchTaskStats"
        />
      </el-tab-pane>
      <el-tab-pane label="全部任务" name="all">
        <TaskList @refresh="fetchTaskStats" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import TaskList from './components/TaskList.vue'
import { taskApi } from '@/api/workflow/task'

// 当前用户ID（从用户状态获取）
const currentUserId = ref('zhangsan')

// 活跃标签页
const activeTab = ref('my-tasks')

// 任务统计
const taskStats = reactive({
  myTasks: 0,
  todoTasks: 0,
  overdueTasks: 0,
  completedToday: 0
})

// 获取任务统计
const fetchTaskStats = async () => {
  try {
    // 这里可以根据需要实现具体的统计API
    // 暂时使用模拟数据
    taskStats.myTasks = 5
    taskStats.todoTasks = 3
    taskStats.overdueTasks = 1
    taskStats.completedToday = 2
  } catch (error) {
    console.error('获取任务统计失败:', error)
  }
}

// 标签页切换
const handleTabChange = (tabName: string) => {
  activeTab.value = tabName
}

// 初始化
onMounted(() => {
  fetchTaskStats()
})
</script>

<style scoped>
.task-center {
  padding: 20px;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
}

.stat-content {
  padding: 10px 0;
}

.stat-number {
  font-size: 28px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #666;
}
</style>
```

### 3. 流程实例详情页

**文件**: `src/views/workflow/ProcessInstanceDetail.vue`
```vue
<template>
  <div class="process-instance-detail">
    <el-page-header @back="goBack">
      <template #content>
        <span class="page-title">流程实例详情</span>
      </template>
    </el-page-header>

    <div class="content">
      <el-row :gutter="20">
        <!-- 左侧：流程图 -->
        <el-col :span="16">
          <el-card class="diagram-card">
            <template #header>
              <span>流程图</span>
            </template>
            <ProcessViewer
              :xml="processDiagram"
              :highlighted-nodes="currentActivities"
              style="height: 500px"
            />
          </el-card>
        </el-col>

        <!-- 右侧：实例信息 -->
        <el-col :span="8">
          <el-card class="info-card">
            <template #header>
              <span>实例信息</span>
            </template>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="实例ID">
                {{ instance.id }}
              </el-descriptions-item>
              <el-descriptions-item label="流程名称">
                {{ instance.processDefinitionName }}
              </el-descriptions-item>
              <el-descriptions-item label="业务Key">
                {{ instance.businessKey || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="getStateType(instance.state)">
                  {{ instance.state }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="开始时间">
                {{ formatDateTime(instance.startTime) }}
              </el-descriptions-item>
              <el-descriptions-item label="结束时间">
                {{ instance.endTime ? formatDateTime(instance.endTime) : '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="耗时">
                {{ formatDuration(instance.durationInMillis) }}
              </el-descriptions-item>
            </el-descriptions>

            <div class="actions" style="margin-top: 16px">
              <el-button
                v-if="instance.state === 'running'"
                type="warning"
                @click="handleSuspend"
              >
                挂起
              </el-button>
              <el-button
                v-if="instance.state === 'suspended'"
                type="success"
                @click="handleActivate"
              >
                激活
              </el-button>
              <el-button
                v-if="instance.state === 'running'"
                type="danger"
                @click="handleTerminate"
              >
                终止
              </el-button>
            </div>
          </el-card>

          <!-- 当前活动 -->
          <el-card class="activities-card" style="margin-top: 16px">
            <template #header>
              <span>当前活动</span>
            </template>
            <el-timeline>
              <el-timeline-item
                v-for="activity in currentActivitiesList"
                :key="activity.id"
                :timestamp="formatDateTime(activity.startTime)"
                placement="top"
              >
                <el-card>
                  <h4>{{ activity.activityName }}</h4>
                  <p>类型：{{ activity.activityType }}</p>
                  <p>开始时间：{{ formatDateTime(activity.startTime) }}</p>
                </el-card>
              </el-timeline-item>
            </el-timeline>
          </el-card>
        </el-col>
      </el-row>

      <!-- 流程变量 -->
      <el-card class="variables-card" style="margin-top: 20px">
        <template #header>
          <span>流程变量</span>
        </template>
        <el-table :data="variables" stripe>
          <el-table-column prop="name" label="变量名" width="200" />
          <el-table-column prop="value" label="值">
            <template #default="{ row }">
              <span v-if="row.type === 'Boolean'">
                <el-tag :type="row.value ? 'success' : 'info'">
                  {{ row.value ? '是' : '否' }}
                </el-tag>
              </span>
              <span v-else-if="row.type === 'Date'">
                {{ formatDateTime(row.value) }}
              </span>
              <span v-else>
                {{ row.value }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="type" label="类型" width="120" />
          <el-table-column prop="scope" label="作用域" width="100" />
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import ProcessViewer from '@/components/ProcessViewer.vue'
import { processInstanceApi } from '@/api/workflow/processInstance'

const route = useRoute()
const router = useRouter()

// 实例ID
const instanceId = route.params.id as string

// 数据状态
const instance = reactive<any>({})
const processDiagram = ref('')
const currentActivities = ref<string[]>([])
const currentActivitiesList = ref<any[]>([])
const variables = ref<any[]>([])

// 获取实例详情
const fetchInstanceDetail = async () => {
  try {
    const response = await processInstanceApi.getDetail(instanceId, true)
    if (response.success) {
      Object.assign(instance, response.data)

      // 获取流程图和当前活动
      // 这里需要根据实际API调整
      processDiagram.value = '' // 需要从后端获取BPMN XML
      currentActivities.value = [instance.currentActivityId].filter(Boolean)
      currentActivitiesList.value = instance.activities || []

      // 获取变量
      if (response.data.variables) {
        variables.value = response.data.variables
      }
    }
  } catch (error) {
    console.error('获取实例详情失败:', error)
  }
}

// 挂起
const handleSuspend = async () => {
  try {
    await ElMessageBox.confirm('确定要挂起该流程实例吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await processInstanceApi.suspend(instanceId, true)
    ElMessage.success('挂起成功')
    fetchInstanceDetail()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('挂起失败:', error)
    }
  }
}

// 激活
const handleActivate = async () => {
  try {
    await ElMessageBox.confirm('确定要激活该流程实例吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await processInstanceApi.suspend(instanceId, false)
    ElMessage.success('激活成功')
    fetchInstanceDetail()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('激活失败:', error)
    }
  }
}

// 终止
const handleTerminate = async () => {
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入终止原因', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPlaceholder: '请输入终止原因',
      inputType: 'textarea',
      inputValidator: (value) => {
        if (!value || value.trim() === '') {
          return '请输入终止原因'
        }
        return true
      }
    })

    await processInstanceApi.terminate(instanceId, reason)
    ElMessage.success('终止成功')
    fetchInstanceDetail()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('终止失败:', error)
    }
  }
}

// 返回
const goBack = () => {
  router.go(-1)
}

// 格式化状态类型
const getStateType = (state: string) => {
  const stateMap: Record<string, string> = {
    running: 'success',
    suspended: 'warning',
    completed: 'info',
    terminated: 'danger'
  }
  return stateMap[state] || 'info'
}

// 格式化日期时间
const formatDateTime = (dateString: string) => {
  return new Date(dateString).toLocaleString('zh-CN')
}

// 格式化持续时间
const formatDuration = (millis?: number) => {
  if (!millis) return '-'
  const hours = Math.floor(millis / 3600000)
  const minutes = Math.floor((millis % 3600000) / 60000)
  return `${hours}小时${minutes}分钟`
}

// 初始化
onMounted(() => {
  fetchInstanceDetail()
})
</script>

<style scoped>
.process-instance-detail {
  padding: 20px;
}

.page-title {
  font-size: 18px;
  font-weight: bold;
}

.content {
  margin-top: 20px;
}

.diagram-card,
.info-card,
.activities-card,
.variables-card {
  margin-bottom: 0;
}

.actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
```

## 📦 通用组件

### 1. 流程图查看器

**文件**: `src/components/ProcessViewer.vue`
```vue
<template>
  <div class="process-viewer" ref="container">
    <div ref="canvas" class="canvas" />

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-overlay">
      <el-icon class="is-loading"><Loading /></el-icon>
      <p>加载流程图中...</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, nextTick } from 'vue'
import { Loading } from '@element-plus/icons-vue'

const props = defineProps<{
  xml: string
  highlightedNodes?: string[]
}>()

const container = ref<HTMLElement>()
const canvas = ref<HTMLElement>()
const loading = ref(false)
let bpmnModeler: any = null

const initBpmnModeler = async () => {
  if (!canvas.value) return

  try {
    // 动态导入 bpmn-js
    const BpmnModeler = (await import('bpmn-js/lib/Modeler')).default

    bpmnModeler = new BpmnModeler({
      container: canvas.value,
      width: '100%',
      height: '100%'
    })

    // 注册高亮样式
    bpmnModeler.get('canvas').addMarker('highlight', 'highlight')

    if (props.xml) {
      await loadDiagram(props.xml)
    }
  } catch (error) {
    console.error('初始化BPMN查看器失败:', error)
  }
}

const loadDiagram = async (xml: string) => {
  if (!bpmnModeler) return

  loading.value = true
  try {
    await bpmnModeler.importXML(xml)

    // 高亮节点
    if (props.highlightedNodes?.length) {
      highlightElements(props.highlightedNodes)
    }

    // 自适应视图
    const canvas = bpmnModeler.get('canvas')
    canvas.zoom('fit-viewport')
  } catch (error) {
    console.error('加载流程图失败:', error)
  } finally {
    loading.value = false
  }
}

const highlightElements = (nodeIds: string[]) => {
  if (!bpmnModeler) return

  const elementRegistry = bpmnModeler.get('elementRegistry')

  nodeIds.forEach(nodeId => {
    const element = elementRegistry.get(nodeId)
    if (element) {
      bpmnModeler.get('canvas').addMarker(nodeId, 'highlight')
    }
  })
}

// 监听XML变化
watch(() => props.xml, async (newXml) => {
  if (newXml && bpmnModeler) {
    await loadDiagram(newXml)
  }
})

// 监听高亮节点变化
watch(() => props.highlightedNodes, (newNodes) => {
  if (newNodes && bpmnModeler) {
    highlightElements(newNodes)
  }
})

onMounted(async () => {
  await nextTick()
  await initBpmnModeler()
})
</script>

<style scoped>
.process-viewer {
  width: 100%;
  height: 100%;
  position: relative;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}

.canvas {
  width: 100%;
  height: 100%;
}

.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.9);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.loading-overlay p {
  margin-top: 12px;
  color: #606266;
}
</style>

<style>
/* BPMN.js 高亮样式 */
.bjs-container .highlight .djs-visual > :nth-child(1) {
  stroke: #67c23a !important;
  stroke-width: 3px !important;
}

.bjs-container .highlight .djs-visual > :nth-child(2) {
  stroke: #67c23a !important;
  stroke-width: 3px !important;
}

.bjs-container .highlight .djs-visual > :nth-child(3) {
  stroke: #67c23a !important;
  stroke-width: 3px !important;
}
</style>
```

### 2. 动态表单组件

**文件**: `src/components/TaskForm.vue`
```vue
<template>
  <el-form
    ref="formRef"
    :model="formData"
    :rules="formRules"
    label-width="120px"
    @submit.prevent="handleSubmit"
  >
    <template v-for="field in formFields" :key="field.name">
      <el-form-item
        :label="field.label"
        :prop="field.name"
        v-if="!field.hidden"
      >
        <!-- 文本输入 -->
        <el-input
          v-if="field.type === 'text'"
          v-model="formData[field.name]"
          :placeholder="field.placeholder || `请输入${field.label}`"
          :readonly="field.readonly"
          clearable
        />

        <!-- 多行文本 -->
        <el-input
          v-else-if="field.type === 'textarea'"
          v-model="formData[field.name]"
          type="textarea"
          :rows="field.rows || 3"
          :placeholder="field.placeholder || `请输入${field.label}`"
          :readonly="field.readonly"
        />

        <!-- 数字输入 -->
        <el-input-number
          v-else-if="field.type === 'number'"
          v-model="formData[field.name]"
          :min="field.min"
          :max="field.max"
          :step="field.step || 1"
          :precision="field.precision"
          :readonly="field.readonly"
          style="width: 100%"
        />

        <!-- 日期选择 -->
        <el-date-picker
          v-else-if="field.type === 'date'"
          v-model="formData[field.name]"
          type="date"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          :placeholder="`请选择${field.label}`"
          :readonly="field.readonly"
          style="width: 100%"
        />

        <!-- 日期时间选择 -->
        <el-date-picker
          v-else-if="field.type === 'datetime'"
          v-model="formData[field.name]"
          type="datetime"
          format="YYYY-MM-DD HH:mm:ss"
          value-format="YYYY-MM-DD HH:mm:ss"
          :placeholder="`请选择${field.label}`"
          :readonly="field.readonly"
          style="width: 100%"
        />

        <!-- 下拉选择 -->
        <el-select
          v-else-if="field.type === 'select'"
          v-model="formData[field.name]"
          :placeholder="`请选择${field.label}`"
          :readonly="field.readonly"
          clearable
          style="width: 100%"
        >
          <el-option
            v-for="option in field.options || []"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>

        <!-- 单选按钮 -->
        <el-radio-group
          v-else-if="field.type === 'radio'"
          v-model="formData[field.name]"
          :disabled="field.readonly"
        >
          <el-radio
            v-for="option in field.options || []"
            :key="option.value"
            :label="option.value"
          >
            {{ option.label }}
          </el-radio>
        </el-radio-group>

        <!-- 复选框 -->
        <el-checkbox-group
          v-else-if="field.type === 'checkbox'"
          v-model="formData[field.name]"
          :disabled="field.readonly"
        >
          <el-checkbox
            v-for="option in field.options || []"
            :key="option.value"
            :label="option.value"
          >
            {{ option.label }}
          </el-checkbox>
        </el-checkbox-group>

        <!-- 开关 -->
        <el-switch
          v-else-if="field.type === 'switch'"
          v-model="formData[field.name]"
          :disabled="field.readonly"
        />

        <!-- 文件上传 -->
        <el-upload
          v-else-if="field.type === 'file'"
          v-model:file-list="formData[field.name]"
          :action="uploadAction"
          :headers="uploadHeaders"
          :before-upload="beforeUpload"
          :on-success="handleUploadSuccess"
          :on-error="handleUploadError"
          :limit="field.limit || 1"
          :accept="field.accept"
        >
          <el-button type="primary">上传文件</el-button>
        </el-upload>

        <!-- 未知类型 -->
        <span v-else>
          {{ formData[field.name] }}
        </span>

        <!-- 字段说明 -->
        <div v-if="field.help" class="field-help">
          {{ field.help }}
        </div>
      </el-form-item>
    </template>

    <!-- 提交按钮 -->
    <el-form-item>
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitting">
        确定
      </el-button>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'

export interface FormField {
  type: 'text' | 'textarea' | 'number' | 'date' | 'datetime' | 'select' | 'radio' | 'checkbox' | 'switch' | 'file'
  label: string
  name: string
  required?: boolean
  readonly?: boolean
  hidden?: boolean
  placeholder?: string
  defaultValue?: any
  min?: number
  max?: number
  step?: number
  precision?: number
  rows?: number
  options?: { label: string; value: any }[]
  limit?: number
  accept?: string
  help?: string
}

const props = defineProps<{
  fields: FormField[]
  initialData?: Record<string, any>
  submitUrl?: string
}>()

const emit = defineEmits<{
  submit: [data: Record<string, any>]
  cancel: []
}>()

const formRef = ref()
const formData = reactive<Record<string, any>>({})
const formRules = reactive<Record<string, any>>({})
const formFields = ref<FormField[]>([])
const submitting = ref(false)

const uploadAction = '/api/upload' // 上传地址
const uploadHeaders = {
  Authorization: `Bearer ${localStorage.getItem('access_token')}`
}

// 初始化表单
const initForm = () => {
  formFields.value = props.fields

  // 初始化表单数据
  props.fields.forEach(field => {
    const value = props.initialData?.[field.name] ?? field.defaultValue
    formData[field.name] = value

    // 设置校验规则
    if (field.required) {
      formRules[field.name] = [
        {
          required: true,
          message: `请输入${field.label}`,
          trigger: field.type === 'select' ? 'change' : 'blur'
        }
      ]
    }
  })
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    submitting.value = true

    // 调用提交回调
    emit('submit', { ...formData })

  } catch (error) {
    console.error('表单验证失败:', error)
  } finally {
    submitting.value = false
  }
}

// 取消
const handleCancel = () => {
  emit('cancel')
}

// 上传前校验
const beforeUpload = (file: File) => {
  const isValidType = true // 根据需要添加文件类型校验
  const isLt10M = file.size / 1024 / 1024 < 10

  if (!isValidType) {
    ElMessage.error('文件格式不正确')
  }
  if (!isLt10M) {
    ElMessage.error('文件大小不能超过10MB')
  }

  return isValidType && isLt10M
}

// 上传成功
const handleUploadSuccess = (response: any, file: File) => {
  ElMessage.success('文件上传成功')
}

// 上传失败
const handleUploadError = () => {
  ElMessage.error('文件上传失败')
}

// 监听初始数据变化
watch(() => props.initialData, (newData) => {
  if (newData) {
    Object.assign(formData, newData)
  }
}, { deep: true })

// 初始化
initForm()
</script>

<style scoped>
.field-help {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
```

## 📚 完整文档列表

1. **API_INTEGRATION_GUIDE.md** - API接口对接指南
2. **WORKFLOW_UI_PLAN.md** - 工作流页面完善实施计划
3. **CODE_EXAMPLES.md** - 完整代码示例（本文档）

## 🚀 下一步行动

1. **立即开始**：根据 CODE_EXAMPLES.md 创建前端项目结构
2. **API测试**：使用 Postman 或 curl 测试所有 API 接口
3. **页面开发**：按照 WORKFLOW_UI_PLAN.md 的阶段计划逐步开发
4. **持续集成**：配置 CI/CD 流水线

## 📞 技术支持

如有问题，请参考：
- API 文档：http://localhost:8089/swagger-ui/index.html
- 后端团队联系方式
- Camunda 官方文档：https://docs.camunda.org/
