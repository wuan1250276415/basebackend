# 部门树形选择修复

## 问题描述

在admin-web模块中，新增或编辑用户时，部门选择器只显示根目录的部门，而不显示子节点的部门。这是因为原来的实现只是简单地遍历部门列表，没有处理树形结构。

## 解决方案

### 1. 创建部门树形选择组件

创建了 `DeptTreeSelect` 组件 (`/src/components/DeptTreeSelect.tsx`)，该组件：

- 使用 Ant Design 的 `TreeSelect` 组件
- 自动将部门树形数据转换为 TreeSelect 需要的格式
- 支持搜索功能
- 支持清空选择
- 支持禁用状态

### 2. 更新用户管理页面

修改了 `/src/pages/System/User/index.tsx`：

- 导入 `DeptTreeSelect` 组件
- 将原来的 `Select` 组件替换为 `DeptTreeSelect`
- 传入部门树形数据

### 3. 更新部门管理页面

修改了 `/src/pages/System/Dept/index.tsx`：

- 导入 `DeptTreeSelect` 组件
- 将上级部门选择器也替换为树形选择器

## 技术实现

### 数据转换逻辑

```typescript
const convertDeptToTreeData = (depts: Dept[], level = 0): any[] => {
  return depts.map(dept => ({
    title: dept.deptName,
    value: dept.id,
    key: dept.id,
    children: dept.children ? convertDeptToTreeData(dept.children, level + 1) : undefined,
  }))
}
```

### 组件使用

```tsx
<DeptTreeSelect 
  placeholder="请选择部门" 
  treeData={deptList}
  value={formValue}
  onChange={handleChange}
/>
```

## 功能特性

1. **树形结构显示**：正确显示部门层级关系
2. **搜索功能**：支持按部门名称搜索
3. **清空功能**：支持清空已选择的部门
4. **禁用状态**：支持禁用选择器
5. **响应式设计**：适配不同屏幕尺寸

## 测试

创建了测试组件 `DeptTreeSelectTest.tsx` 用于验证功能是否正常工作。

## 影响范围

- ✅ 用户管理页面的部门选择
- ✅ 部门管理页面的上级部门选择
- ✅ 其他可能使用部门选择的地方

## 注意事项

1. 确保后端API返回的部门数据包含 `children` 字段
2. 部门数据应该是树形结构，而不是平铺的列表
3. 如果部门数据发生变化，需要重新加载数据

## 后续优化

1. 可以添加部门图标显示
2. 可以添加部门状态显示（启用/禁用）
3. 可以添加部门负责人信息显示
4. 可以添加部门层级缩进显示
