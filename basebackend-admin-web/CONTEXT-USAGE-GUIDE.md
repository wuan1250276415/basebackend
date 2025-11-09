# React Context 全局状态管理使用指南

## 📖 概述

浮浮酱为项目创建了一套完整的 React Context 全局状态管理系统，让您能够快速访问用户信息、字典数据、部门信息等常用数据 ฅ'ω'ฅ

## 🎯 核心功能

### 1. **UserContext** - 用户信息和权限管理
- 用户信息获取和刷新
- 权限检查（`hasPermission`）
- 角色检查（`hasRole`）
- 权限控制 HOC

### 2. **DictContext** - 字典数据缓存
- 字典数据自动缓存（5分钟过期）
- 按类型获取字典数据
- 字典标签转换
- 字典选项格式化（用于 Select 组件）

### 3. **DeptContext** - 部门数据缓存
- 部门树数据（10分钟过期）
- 部门列表数据
- 根据ID查询部门
- 获取部门路径
- 获取子部门列表

### 4. **AppContext** - 统一入口
- 整合所有子 Context
- 提供统一的访问接口
- 全局刷新方法

---

## 🚀 快速开始

### 安装（已完成）

AppProvider 已经在 `src/App.tsx` 中集成，无需额外配置！

```tsx
// src/App.tsx (已配置)
import { AppProvider } from './contexts'

<ConfigProvider>
  <AppProvider>
    <QueryClientProvider>
      {/* Your App */}
    </QueryClientProvider>
  </AppProvider>
</ConfigProvider>
```

### 基础使用

#### 方式 1: 使用 useApp（推荐）

```tsx
import { useApp } from '@/contexts'

function MyComponent() {
  const app = useApp()

  // 访问用户信息
  const { userInfo, hasPermission } = app.user

  // 访问字典数据
  const getDictOptions = app.dict.getDictOptions

  // 访问部门数据
  const deptTree = app.dept.deptTree

  return <div>...</div>
}
```

#### 方式 2: 使用单独的 Hook

```tsx
import { useUser, useDict, useDept } from '@/contexts'

function MyComponent() {
  const { userInfo, hasPermission } = useUser()
  const { getDictByType, getDictLabel } = useDict()
  const { deptTree, getDeptById } = useDept()

  return <div>...</div>
}
```

---

## 💡 使用示例

### 示例 1: 用户信息和权限检查

```tsx
import { useUser } from '@/contexts'
import { Button } from 'antd'

function UserProfile() {
  const { userInfo, hasPermission, refreshUserInfo } = useUser()

  // 检查权限
  const canEditUser = hasPermission('system:user:edit')
  const canDeleteUser = hasPermission('system:user:delete')

  return (
    <div>
      <h2>欢迎, {userInfo?.nickname}</h2>
      <p>邮箱: {userInfo?.email}</p>
      <p>部门: {userInfo?.deptName}</p>

      {canEditUser && <Button>编辑用户</Button>}
      {canDeleteUser && <Button danger>删除用户</Button>}

      <Button onClick={refreshUserInfo}>刷新用户信息</Button>
    </div>
  )
}
```

### 示例 2: 使用字典数据

```tsx
import { useDictOptions } from '@/contexts'
import { Select } from 'antd'

function UserStatusSelect() {
  // 方式 1: 使用便捷 Hook（推荐）
  const [options, loading] = useDictOptions('sys_user_status')

  return (
    <Select
      placeholder="选择用户状态"
      options={options}
      loading={loading}
    />
  )
}

// 方式 2: 手动获取
function UserStatusSelect2() {
  const { getDictOptions } = useDict()
  const [options, setOptions] = useState([])

  useEffect(() => {
    getDictOptions('sys_user_status').then(setOptions)
  }, [getDictOptions])

  return <Select options={options} />
}

// 方式 3: 显示字典标签
function UserStatusLabel({ status }: { status: string }) {
  const { getDictLabel } = useDict()

  return <span>{getDictLabel('sys_user_status', status)}</span>
}
```

### 示例 3: 使用部门树

```tsx
import { useDeptTreeOptions, useDeptInfo } from '@/contexts'
import { TreeSelect } from 'antd'

// 部门选择器
function DeptTreeSelect() {
  const [treeData, loading] = useDeptTreeOptions()

  return (
    <TreeSelect
      treeData={treeData}
      placeholder="选择部门"
      loading={loading}
      fieldNames={{
        label: 'deptName',
        value: 'id',
        children: 'children',
      }}
    />
  )
}

// 显示部门信息
function DeptInfo({ deptId }: { deptId: string }) {
  const dept = useDeptInfo(deptId)

  if (!dept) return <span>部门不存在</span>

  return (
    <div>
      <p>部门名称: {dept.deptName}</p>
      <p>负责人: {dept.leader}</p>
      <p>联系电话: {dept.phone}</p>
    </div>
  )
}

// 获取部门路径
function DeptPath({ deptId }: { deptId: string }) {
  const { getDeptPath } = useDept()
  const path = getDeptPath(deptId)

  return (
    <span>
      {path.map((dept, index) => (
        <span key={dept.id}>
          {dept.deptName}
          {index < path.length - 1 && ' > '}
        </span>
      ))}
    </span>
  )
}
```

### 示例 4: 权限控制 HOC

```tsx
import { withPermission, withRole } from '@/contexts'
import { Button } from 'antd'

// 需要权限的组件
const AddUserButton = () => <Button type="primary">新增用户</Button>

// 使用 HOC 包装
const ProtectedAddUserButton = withPermission('system:user:add')(AddUserButton)

// 在页面中使用
function UserManagePage() {
  return (
    <div>
      <h1>用户管理</h1>

      {/* 有权限时才显示按钮 */}
      <ProtectedAddUserButton />
    </div>
  )
}

// 也可以使用角色控制
const AdminPanel = () => <div>管理员面板</div>
const ProtectedAdminPanel = withRole('admin')(AdminPanel)
```

### 示例 5: 完整的表单示例

```tsx
import { useApp, useDictOptions, useDeptTreeOptions } from '@/contexts'
import { Form, Input, Select, TreeSelect, Button } from 'antd'

function UserForm() {
  const app = useApp()
  const [form] = Form.useForm()

  // 获取字典选项
  const [genderOptions] = useDictOptions('sys_user_gender')
  const [statusOptions] = useDictOptions('sys_user_status')
  const [userTypeOptions] = useDictOptions('sys_user_type')

  // 获取部门树
  const [deptTree] = useDeptTreeOptions()

  const onSubmit = async (values: any) => {
    console.log('表单值:', values)
    console.log('当前用户:', app.user.userInfo)
  }

  return (
    <Form form={form} onFinish={onSubmit} layout="vertical">
      <Form.Item label="用户名" name="username" rules={[{ required: true }]}>
        <Input />
      </Form.Item>

      <Form.Item label="昵称" name="nickname">
        <Input />
      </Form.Item>

      <Form.Item label="性别" name="gender">
        <Select options={genderOptions} />
      </Form.Item>

      <Form.Item label="用户类型" name="userType">
        <Select options={userTypeOptions} />
      </Form.Item>

      <Form.Item label="状态" name="status">
        <Select options={statusOptions} />
      </Form.Item>

      <Form.Item label="所属部门" name="deptId">
        <TreeSelect
          treeData={deptTree}
          fieldNames={{
            label: 'deptName',
            value: 'id',
            children: 'children',
          }}
        />
      </Form.Item>

      <Form.Item>
        <Button type="primary" htmlType="submit">
          提交
        </Button>
      </Form.Item>
    </Form>
  )
}
```

### 示例 6: 刷新所有缓存

```tsx
import { useApp } from '@/contexts'
import { Button } from 'antd'

function RefreshButton() {
  const app = useApp()
  const [loading, setLoading] = useState(false)

  const handleRefresh = async () => {
    setLoading(true)
    try {
      // 刷新所有缓存数据
      await app.refreshAll()
      message.success('刷新成功')
    } catch (error) {
      message.error('刷新失败')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Button onClick={handleRefresh} loading={loading}>
      刷新所有数据
    </Button>
  )
}

// 或者单独刷新某个数据
function RefreshDictButton() {
  const { refreshAllDict, refreshDict } = useDict()

  return (
    <>
      <Button onClick={() => refreshAllDict()}>刷新所有字典</Button>
      <Button onClick={() => refreshDict('sys_user_status')}>
        刷新用户状态字典
      </Button>
    </>
  )
}
```

---

## 📚 API 参考

### useUser()

| 属性/方法 | 类型 | 说明 |
|-----------|------|------|
| `userInfo` | `UserInfo \| null` | 当前用户信息 |
| `token` | `string \| null` | 访问令牌 |
| `permissions` | `string[]` | 用户权限列表 |
| `roles` | `string[]` | 用户角色列表 |
| `loading` | `boolean` | 加载状态 |
| `error` | `string \| null` | 错误信息 |
| `refreshUserInfo()` | `() => Promise<void>` | 刷新用户信息 |
| `hasPermission(perm)` | `(permission: string) => boolean` | 检查是否有指定权限 |
| `hasRole(role)` | `(role: string) => boolean` | 检查是否有指定角色 |
| `hasAnyPermission(perms)` | `(permissions: string[]) => boolean` | 检查是否有任意一个权限 |
| `hasAnyRole(roles)` | `(roles: string[]) => boolean` | 检查是否有任意一个角色 |
| `logout()` | `() => void` | 登出 |

### useDict()

| 属性/方法 | 类型 | 说明 |
|-----------|------|------|
| `dictCache` | `DictCache` | 字典缓存对象 |
| `loading` | `Record<string, boolean>` | 各字典类型的加载状态 |
| `getDictByType(type, force?)` | `(dictType: string, forceRefresh?: boolean) => Promise<DictData[]>` | 获取字典数据 |
| `getDictLabel(type, value)` | `(dictType: string, dictValue: string) => string` | 获取字典标签 |
| `getDictOptions(type)` | `(dictType: string) => Promise<Option[]>` | 获取字典选项 |
| `refreshDict(type)` | `(dictType: string) => Promise<void>` | 刷新指定字典 |
| `refreshAllDict()` | `() => Promise<void>` | 刷新所有字典 |
| `clearCache()` | `() => void` | 清空本地缓存 |

### useDept()

| 属性/方法 | 类型 | 说明 |
|-----------|------|------|
| `deptTree` | `Dept[]` | 部门树数据 |
| `deptList` | `Dept[]` | 部门列表数据 |
| `loading` | `boolean` | 加载状态 |
| `loadDeptTree(force?)` | `(forceRefresh?: boolean) => Promise<Dept[]>` | 加载部门树 |
| `loadDeptList(force?)` | `(forceRefresh?: boolean) => Promise<Dept[]>` | 加载部门列表 |
| `getDeptById(id)` | `(deptId: string) => Dept \| undefined` | 根据ID获取部门 |
| `getDeptPath(id)` | `(deptId: string) => Dept[]` | 获取部门路径 |
| `getDeptChildren(id)` | `(deptId: string) => Dept[]` | 获取子部门列表 |
| `refreshDept()` | `() => Promise<void>` | 刷新部门数据 |
| `clearCache()` | `() => void` | 清空缓存 |

### useApp()

| 属性 | 类型 | 说明 |
|------|------|------|
| `user` | `UserContextType` | useUser() 的返回值 |
| `dict` | `DictContextType` | useDict() 的返回值 |
| `dept` | `DeptContextType` | useDept() 的返回值 |
| `refreshAll()` | `() => Promise<void>` | 刷新所有缓存数据 |

---

## 🔧 便捷 Hooks

### useDictData(dictType)

直接获取指定类型的字典数据

```tsx
const [statusDict, loading] = useDictData('sys_user_status')
```

### useDictOptions(dictType)

直接获取指定类型的字典选项（用于 Select）

```tsx
const [options, loading] = useDictOptions('sys_user_status')
<Select options={options} loading={loading} />
```

### useDeptTreeOptions()

获取部门树选项（用于 TreeSelect）

```tsx
const [treeData, loading] = useDeptTreeOptions()
<TreeSelect treeData={treeData} loading={loading} />
```

### useDeptInfo(deptId)

获取指定部门信息

```tsx
const dept = useDeptInfo('123')
```

---

## 🎨 高阶组件 (HOC)

### withPermission(permission)

权限控制 HOC

```tsx
const ProtectedComponent = withPermission('system:user:add')(MyComponent)
```

### withRole(role)

角色控制 HOC

```tsx
const AdminOnlyComponent = withRole('admin')(MyComponent)
```

---

## ⚡ 性能优化

### 1. 自动缓存

- 字典数据：5分钟过期
- 部门数据：10分钟过期
- 自动防止重复请求

### 2. 按需加载

```tsx
// 只在需要时才加载数据
const { getDictByType } = useDict()

// 组件挂载时加载
useEffect(() => {
  getDictByType('sys_user_status')
}, [])
```

### 3. 强制刷新

```tsx
// 强制刷新字典数据
await getDictByType('sys_user_status', true)

// 强制刷新部门树
await loadDeptTree(true)
```

---

## 🐛 常见问题

### Q1: Hook 必须在 Provider 内部使用

**错误：** `useUser 必须在 UserProvider 内部使用`

**解决：** 确保您的组件在 `AppProvider` 内部

```tsx
// ✅ 正确
<AppProvider>
  <YourComponent />
</AppProvider>

// ❌ 错误
<YourComponent /> // 没有包裹在 AppProvider 内
```

### Q2: 数据未及时更新

**解决：** 使用强制刷新或清空缓存

```tsx
// 强制刷新
await getDictByType('sys_user_status', true)

// 清空缓存后重新加载
clearCache()
await getDictByType('sys_user_status')
```

### Q3: TypeScript 类型错误

**解决：** 确保导入了正确的类型

```tsx
import { UserInfo, Dept, DictData } from '@/types'
```

---

## 📦 文件结构

```
src/contexts/
├── UserContext.tsx      # 用户上下文
├── DictContext.tsx      # 字典上下文
├── DeptContext.tsx      # 部门上下文
├── AppContext.tsx       # 应用根上下文
└── index.ts             # 统一导出
```

---

## 🎉 总结

浮浮酱创建的 Context 系统提供了：

✅ **统一的状态管理** - 一个地方管理所有全局数据
✅ **自动缓存** - 减少不必要的 API 请求
✅ **类型安全** - 完整的 TypeScript 支持
✅ **便捷的 Hooks** - 简化代码，提高效率
✅ **权限控制** - 内置权限和角色检查
✅ **灵活使用** - 支持统一入口和单独使用

现在您可以在任何组件中轻松访问用户信息、字典数据和部门信息了喵～ ฅ'ω'ฅ

---

**创建者：** Claude Code (浮浮酱) φ(≧ω≦*)♪
**创建时间：** 2025-11-09
**版本：** v1.0.0
