# 应用管理系统前端实现指南

## 概述

本文档说明如何在前端实现应用管理系统，包括应用管理、应用资源管理，以及如何更新角色管理和字典管理以支持应用隔离。

## 已完成的文件

### 1. API接口文件

**文件**: `src/api/application.ts`

提供以下API接口：
- 应用管理：列表查询、创建、更新、删除、状态修改
- 应用资源管理：资源树查询、创建、更新、删除
- 角色资源分配：查询角色资源、分配资源

### 2. 类型定义

**文件**: `src/types/index.ts`（已更新）

新增类型：
```typescript
// 应用信息类型
export interface Application {
  id?: string
  appName: string
  appCode: string
  appType: string
  appIcon?: string
  appUrl?: string
  status?: number
  orderNum?: number
  remark?: string
  createTime?: string
  updateTime?: string
}

// 应用资源类型
export interface ApplicationResource {
  id?: string
  appId: string
  resourceName: string
  parentId?: string
  resourceType: string
  path?: string
  component?: string
  perms?: string
  icon?: string
  visible?: number
  openType?: string
  orderNum?: number
  status?: number
  remark?: string
  children?: ApplicationResource[]
  appName?: string
  createTime?: string
  updateTime?: string
}
```

更新类型（添加appId字段）：
- `Role`: 添加 `appId?` 和 `resourceIds?`
- `Dict`: 添加 `appId?`
- `DictData`: 添加 `appId?`

### 3. 页面组件

#### 应用管理页面
**文件**: `src/pages/System/Application/index.tsx`

功能：
- ✅ 应用列表展示
- ✅ 新增/编辑应用
- ✅ 删除应用
- ✅ 启用/禁用应用（Switch开关）
- ✅ 跳转到资源管理
- ✅ 搜索和筛选

#### 应用资源管理页面
**文件**: `src/pages/System/ApplicationResource/index.tsx`

功能：
- ✅ 树形资源列表展示
- ✅ 新增/编辑资源（支持父子层级）
- ✅ 删除资源
- ✅ 资源类型标签（目录/菜单/按钮）
- ✅ 返回应用列表
- ✅ 支持设置打开方式（当前页/新窗口）

## 需要手动更新的文件

### 1. 角色管理页面

**文件**: `src/pages/System/Role/index.tsx`

需要添加的功能：

1. **添加应用选择器**（在搜索栏）：
```typescript
import { getEnabledApplications } from '@/api/application'

// 在组件中添加应用列表状态
const [applications, setApplications] = useState<Application[]>([])

// 加载应用列表
const loadApplications = async () => {
  try {
    const response = await getEnabledApplications()
    setApplications(response.data)
  } catch (error) {
    console.error('加载应用列表失败', error)
  }
}

useEffect(() => {
  loadData()
  loadMenuTree()
  loadApplications() // 添加这行
}, [])
```

2. **在搜索表单中添加应用选择**：
```tsx
<Form form={searchForm} layout="inline" style={{ marginBottom: 16 }}>
  <Form.Item name="appId" label="所属应用">
    <Select
      placeholder="请选择应用"
      allowClear
      style={{ width: 200 }}
      options={[
        { label: '全部', value: '' },
        ...applications.map(app => ({
          label: app.appName,
          value: app.id
        }))
      ]}
    />
  </Form.Item>
  {/* ... 其他搜索项 ... */}
</Form>
```

3. **在新增/编辑表单中添加应用选择**：
```tsx
<Form form={form} labelCol={{ span: 6 }} wrapperCol={{ span: 16 }}>
  <Form.Item
    label="所属应用"
    name="appId"
    rules={[{ required: true, message: '请选择所属应用' }]}
  >
    <Select placeholder="请选择所属应用">
      {applications.map(app => (
        <Select.Option key={app.id} value={app.id}>
          {app.appName}
        </Select.Option>
      ))}
    </Select>
  </Form.Item>
  {/* ... 其他表单项 ... */}
</Form>
```

4. **在表格中显示应用名称**：
```tsx
{
  title: '所属应用',
  dataIndex: 'appId',
  key: 'appId',
  width: 120,
  render: (appId: string) => {
    const app = applications.find(a => a.id === appId)
    return app ? app.appName : '-'
  },
}
```

5. **修改资源分配功能**：

将原来的菜单分配改为应用资源分配：

```typescript
import { getResourceTree, getResourceIdsByRoleId, assignRoleResources } from '@/api/application'

// 打开资源分配弹窗
const handleOpenResourceModal = async (record: Role) => {
  setCurrentRoleId(record.id!)
  if (!record.appId) {
    message.warning('该角色未关联应用')
    return
  }
  try {
    // 加载应用资源树
    const treeResponse = await getResourceTree(record.appId)
    setResourceTree(treeResponse.data)

    // 加载已分配的资源
    const roleResourceResponse = await getResourceIdsByRoleId(record.id!)
    setSelectedResourceKeys(roleResourceResponse.data)
    setResourceModalVisible(true)
  } catch (error) {
    message.error('加载角色资源失败')
  }
}

// 提交资源分配
const handleSubmitResources = async () => {
  try {
    await assignRoleResources(currentRoleId!, selectedResourceKeys)
    message.success('资源分配成功')
    setResourceModalVisible(false)
  } catch (error) {
    message.error('资源分配失败')
  }
}
```

### 2. 字典管理页面

**文件**: `src/pages/System/Dict/index.tsx`

需要添加的功能：

1. **添加应用选择器**（与角色管理类似）：
```typescript
import { getEnabledApplications } from '@/api/application'

const [applications, setApplications] = useState<Application[]>([])

const loadApplications = async () => {
  try {
    const response = await getEnabledApplications()
    setApplications(response.data)
  } catch (error) {
    console.error('加载应用列表失败', error)
  }
}
```

2. **在搜索栏添加应用筛选**：
```tsx
<Form.Item name="appId" label="所属应用">
  <Select
    placeholder="请选择应用"
    allowClear
    style={{ width: 200 }}
    options={[
      { label: '全部', value: '' },
      { label: '系统字典', value: 'system' },
      ...applications.map(app => ({
        label: app.appName,
        value: app.id
      }))
    ]}
  />
</Form.Item>
```

3. **在新增/编辑表单中添加应用选择**：
```tsx
<Form.Item label="所属应用" name="appId">
  <Select placeholder="请选择所属应用（不选则为系统字典）" allowClear>
    {applications.map(app => (
      <Select.Option key={app.id} value={app.id}>
        {app.appName}
      </Select.Option>
    ))}
  </Select>
</Form.Item>
```

4. **在表格中显示应用名称**：
```tsx
{
  title: '所属应用',
  dataIndex: 'appId',
  key: 'appId',
  width: 120,
  render: (appId: string) => {
    if (!appId) return <Tag>系统字典</Tag>
    const app = applications.find(a => a.id === appId)
    return app ? <Tag color="blue">{app.appName}</Tag> : '-'
  },
}
```

### 3. 路由配置

**文件**: `src/router/index.tsx`（或相应的路由文件）

添加新路由：

```typescript
import ApplicationManagement from '@/pages/System/Application'
import ApplicationResourceManagement from '@/pages/System/ApplicationResource'

// 在路由配置中添加
{
  path: '/system/application',
  element: <ApplicationManagement />,
  meta: {
    title: '应用管理',
    requiresAuth: true,
  }
},
{
  path: '/system/application-resource',
  element: <ApplicationResourceManagement />,
  meta: {
    title: '应用资源管理',
    requiresAuth: true,
  }
}
```

### 4. 菜单配置

在后台添加应用管理相关菜单：

```sql
-- 应用管理菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, remark)
VALUES ('应用管理', 1, 6, 'application', 'System/Application', 'C', 1, 1, 'system:application:list', 'AppstoreOutlined', '应用管理菜单');

-- 应用资源管理菜单（隐藏，通过应用管理页面跳转访问）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, remark)
VALUES ('应用资源管理', 1, 7, 'application-resource', 'System/ApplicationResource', 'C', 0, 1, 'system:application:resource:list', 'MenuOutlined', '应用资源管理菜单');
```

## 完整实现步骤

### Step 1: 安装依赖（如果需要）
```bash
cd basebackend-admin-web
npm install
```

### Step 2: 复制已创建的文件

已创建的文件：
- ✅ `src/api/application.ts`
- ✅ `src/types/index.ts`（已更新）
- ✅ `src/pages/System/Application/index.tsx`
- ✅ `src/pages/System/ApplicationResource/index.tsx`

### Step 3: 更新现有文件

需要手动更新的文件：
- ⚠️ `src/pages/System/Role/index.tsx` - 添加应用选择和资源分配功能
- ⚠️ `src/pages/System/Dict/index.tsx` - 添加应用选择功能
- ⚠️ `src/router/index.tsx` - 添加路由配置

### Step 4: 初始化数据库

执行数据库初始化脚本：
```bash
mysql -u root -p basebackend < init-application-management.sql
```

### Step 5: 启动后端服务

```bash
cd basebackend-admin-api
mvn spring-boot:run
```

### Step 6: 启动前端服务

```bash
cd basebackend-admin-web
npm run dev
```

### Step 7: 访问应用管理

访问地址：`http://localhost:5173/#/system/application`

## 功能测试清单

### 应用管理
- [ ] 查看应用列表
- [ ] 新增应用
- [ ] 编辑应用
- [ ] 删除应用
- [ ] 启用/禁用应用
- [ ] 搜索应用
- [ ] 跳转到资源管理

### 应用资源管理
- [ ] 查看资源树
- [ ] 新增顶级资源
- [ ] 新增子资源
- [ ] 编辑资源
- [ ] 删除资源
- [ ] 返回应用列表

### 角色管理（更新后）
- [ ] 按应用筛选角色
- [ ] 创建角色时选择应用
- [ ] 为角色分配应用资源
- [ ] 查看角色的应用归属

### 字典管理（更新后）
- [ ] 按应用筛选字典
- [ ] 创建字典时选择应用
- [ ] 区分系统字典和应用字典
- [ ] 查看字典的应用归属

## 注意事项

1. **应用编码唯一性**: 应用编码必须唯一，且只能包含小写字母、数字和下划线
2. **资源删除顺序**: 删除资源前必须先删除其所有子资源
3. **系统字典**: appId为NULL的字典为系统级字典，所有应用都可以使用
4. **角色隔离**: 不同应用的角色是独立的，互不干扰

## API文档

### 应用管理API

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 查询应用列表 | GET | `/api/admin/application/list` | 查询所有应用 |
| 查询启用的应用 | GET | `/api/admin/application/enabled` | 查询启用的应用 |
| 根据ID查询 | GET | `/api/admin/application/{id}` | 根据ID查询应用 |
| 创建应用 | POST | `/api/admin/application` | 创建新应用 |
| 更新应用 | PUT | `/api/admin/application` | 更新应用信息 |
| 删除应用 | DELETE | `/api/admin/application/{id}` | 删除应用 |
| 修改状态 | PUT | `/api/admin/application/{id}/status/{status}` | 修改应用状态 |

### 应用资源API

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 查询资源树 | GET | `/api/admin/application/resource/tree/{appId}` | 查询应用的资源树 |
| 查询用户资源树 | GET | `/api/admin/application/resource/user/tree/{appId}` | 查询用户的资源树 |
| 根据ID查询 | GET | `/api/admin/application/resource/{id}` | 根据ID查询资源 |
| 创建资源 | POST | `/api/admin/application/resource` | 创建新资源 |
| 更新资源 | PUT | `/api/admin/application/resource` | 更新资源信息 |
| 删除资源 | DELETE | `/api/admin/application/resource/{id}` | 删除资源 |
| 查询角色资源 | GET | `/api/admin/application/resource/role/{roleId}` | 查询角色的资源ID列表 |
| 分配角色资源 | POST | `/api/admin/application/resource/role/{roleId}/assign` | 分配角色资源 |

## 效果预览

### 应用管理页面
- 列表展示：应用名称、编码、类型、图标、地址、排序、状态
- 操作按钮：编辑、删除、资源管理
- 搜索功能：按名称和编码搜索
- 状态开关：实时切换启用/禁用状态

### 应用资源管理页面
- 树形展示：层级缩进显示资源结构
- 类型标签：目录（蓝色）、菜单（绿色）、按钮（橙色）
- 操作按钮：编辑、删除
- 返回按钮：返回应用列表

### 角色管理页面（更新后）
- 应用筛选：按应用查看角色
- 应用归属：创建角色时选择所属应用
- 资源分配：为角色分配应用资源（替代原来的菜单分配）

### 字典管理页面（更新后）
- 应用筛选：按应用查看字典
- 系统字典：支持系统级字典（所有应用共享）
- 应用归属：创建字典时可选择所属应用

## 总结

本次实现完成了：
1. ✅ 应用管理完整功能
2. ✅ 应用资源管理完整功能
3. ✅ API接口定义
4. ✅ TypeScript类型定义
5. ⚠️ 角色管理更新指南（需手动实现）
6. ⚠️ 字典管理更新指南（需手动实现）
7. ⚠️ 路由配置指南（需手动实现）

所有核心功能已实现，剩余的更新工作请参考上述指南进行手动修改。
