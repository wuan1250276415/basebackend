# 工作流模块菜单配置指南

## 📋 菜单结构

工作流模块建议的菜单结构如下：

```
工作流管理
├── 待办任务 (/workflow/todo)
├── 我发起的 (/workflow/initiated)
├── 流程申请 (/workflow/template)
│   ├── 请假申请 (/workflow/template/leave)
│   ├── 报销申请 (/workflow/template/expense)
│   └── 采购申请 (/workflow/template/purchase)
├── 流程监控 (/workflow/instance)
└── 流程管理 (/workflow/definition)
```

---

## 🔧 配置方式

根据你的系统架构，有以下几种配置方式：

### 方式一：直接在数据库配置（推荐）

如果你的系统菜单存储在数据库中，需要执行以下 SQL 插入菜单数据：

```sql
-- 1. 插入工作流管理主菜单
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_time)
VALUES (2000, '工作流管理', 0, 5, 'workflow', NULL, 'M', '0', '0', '', 'workflow', NOW());

-- 2. 插入待办任务菜单
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_time)
VALUES (2001, '待办任务', 2000, 1, 'todo', 'workflow/TaskManagement/TodoList', 'C', '0', '0', 'workflow:task:list', 'form', NOW());

-- 3. 插入我发起的菜单
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_time)
VALUES (2002, '我发起的', 2000, 2, 'initiated', 'workflow/TaskManagement/MyInitiated', 'C', '0', '0', 'workflow:instance:mylist', 'profile', NOW());

-- 4. 插入流程申请主菜单
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_time)
VALUES (2003, '流程申请', 2000, 3, 'template', 'workflow/ProcessTemplate/index', 'C', '0', '0', 'workflow:template:list', 'plus', NOW());

-- 5. 插入流程监控菜单
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_time)
VALUES (2004, '流程监控', 2000, 4, 'instance', 'workflow/ProcessInstance/index', 'C', '0', '0', 'workflow:instance:list', 'eye', NOW());

-- 6. 插入流程管理菜单（管理员专用）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_time)
VALUES (2005, '流程管理', 2000, 5, 'definition', 'workflow/ProcessDefinition/index', 'C', '0', '0', 'workflow:definition:list', 'setting', NOW());
```

**注意事项：**
- `menu_id` 需要根据你的系统实际情况调整，避免冲突
- `parent_id` 为父菜单ID，0表示顶级菜单
- `component` 路径要与实际文件路径匹配
- `perms` 权限标识要与后端权限配置一致
- 表结构可能与你的系统不同，请根据实际情况调整

### 方式二：在前端路由文件中配置

如果你的系统使用前端静态配置菜单，可以在菜单配置文件中添加：

```typescript
// src/config/menu.ts 或 src/layouts/menu.ts
export const menuConfig = [
  // ... 其他菜单
  {
    key: 'workflow',
    title: '工作流管理',
    icon: <AppstoreOutlined />,
    children: [
      {
        key: 'workflow-todo',
        title: '待办任务',
        icon: <FormOutlined />,
        path: '/workflow/todo',
        badge: true, // 显示待办数量徽标
      },
      {
        key: 'workflow-initiated',
        title: '我发起的',
        icon: <ProfileOutlined />,
        path: '/workflow/initiated',
      },
      {
        key: 'workflow-template',
        title: '流程申请',
        icon: <PlusOutlined />,
        path: '/workflow/template',
      },
      {
        key: 'workflow-instance',
        title: '流程监控',
        icon: <EyeOutlined />,
        path: '/workflow/instance',
        permission: 'workflow:instance:list', // 权限控制
      },
      {
        key: 'workflow-definition',
        title: '流程管理',
        icon: <SettingOutlined />,
        path: '/workflow/definition',
        permission: 'workflow:definition:list', // 仅管理员可见
      },
    ],
  },
]
```

### 方式三：使用 Ant Design Pro 的菜单配置

如果使用 Ant Design Pro，在 `config/routes.ts` 中配置：

```typescript
{
  path: '/workflow',
  name: 'workflow',
  icon: 'workflow',
  routes: [
    {
      path: '/workflow/todo',
      name: 'todo',
      icon: 'form',
      component: './Workflow/TaskManagement/TodoList',
    },
    {
      path: '/workflow/initiated',
      name: 'initiated',
      icon: 'profile',
      component: './Workflow/TaskManagement/MyInitiated',
    },
    {
      path: '/workflow/template',
      name: 'template',
      icon: 'plus',
      component: './Workflow/ProcessTemplate',
    },
    {
      path: '/workflow/instance',
      name: 'instance',
      icon: 'eye',
      component: './Workflow/ProcessInstance',
      access: 'canViewInstance',
    },
    {
      path: '/workflow/definition',
      name: 'definition',
      icon: 'setting',
      component: './Workflow/ProcessDefinition',
      access: 'canManageDefinition',
    },
  ],
},
```

---

## 🎨 图标建议

推荐使用的 Ant Design 图标：

| 菜单项 | 图标 | 图标组件 |
|--------|------|----------|
| 工作流管理 | workflow | `<AppstoreOutlined />` |
| 待办任务 | form | `<FormOutlined />` |
| 我发起的 | profile | `<ProfileOutlined />` |
| 流程申请 | plus | `<PlusOutlined />` |
| 流程监控 | eye | `<EyeOutlined />` |
| 流程管理 | setting | `<SettingOutlined />` |

---

## 🔐 权限配置

### 权限标识符建议

```typescript
// 基础权限
workflow:task:list        // 查看待办任务
workflow:task:view        // 查看任务详情
workflow:task:claim       // 认领任务
workflow:task:complete    // 完成任务

workflow:instance:mylist  // 查看我发起的流程
workflow:instance:list    // 查看所有流程实例（管理员）
workflow:instance:view    // 查看流程实例详情
workflow:instance:suspend // 挂起流程实例
workflow:instance:activate // 激活流程实例
workflow:instance:delete  // 删除流程实例

workflow:template:list    // 查看流程模板
workflow:template:start   // 发起流程

workflow:definition:list  // 查看流程定义
workflow:definition:deploy // 部署流程定义
workflow:definition:delete // 删除流程定义
```

### 角色权限分配示例

**普通用户角色：**
- workflow:task:list
- workflow:task:view
- workflow:task:claim
- workflow:task:complete
- workflow:instance:mylist
- workflow:template:list
- workflow:template:start

**流程管理员角色：**
- 包含普通用户所有权限
- workflow:instance:list
- workflow:instance:view
- workflow:instance:suspend
- workflow:instance:activate
- workflow:definition:list

**系统管理员角色：**
- 包含流程管理员所有权限
- workflow:instance:delete
- workflow:definition:deploy
- workflow:definition:delete

---

## 🔔 待办任务徽标（Badge）

为了提升用户体验，建议在"待办任务"菜单项上显示待办数量徽标：

### 实现方式

#### 1. 在布局组件中获取待办数量

```typescript
// src/layouts/BasicLayout/index.tsx
import { useWorkflowStore } from '@/stores/workflow'
import { listPendingTasks } from '@/api/workflow/task'
import { useAuthStore } from '@/stores/auth'

const BasicLayout = () => {
  const { user } = useAuthStore()
  const { pendingTaskCount, setPendingTaskCount } = useWorkflowStore()

  // 定期获取待办任务数量
  useEffect(() => {
    const fetchPendingCount = async () => {
      if (user) {
        const response = await listPendingTasks(user.username)
        if (response.success) {
          setPendingTaskCount(response.data?.list?.length || 0)
        }
      }
    }

    fetchPendingCount()

    // 每30秒刷新一次
    const interval = setInterval(fetchPendingCount, 30000)

    return () => clearInterval(interval)
  }, [user])

  // ... 渲染菜单时使用 pendingTaskCount
}
```

#### 2. 在菜单渲染中显示徽标

```typescript
// 渲染菜单项时
<Menu.Item key="workflow-todo" icon={<FormOutlined />}>
  <Link to="/workflow/todo">
    待办任务
    {pendingTaskCount > 0 && (
      <Badge
        count={pendingTaskCount}
        offset={[10, 0]}
        style={{ marginLeft: 8 }}
      />
    )}
  </Link>
</Menu.Item>
```

---

## 📱 响应式菜单

建议在移动端显示简化菜单：

```typescript
const isMobile = window.innerWidth < 768

const mobileMenuConfig = [
  {
    key: 'workflow-todo',
    title: '待办',
    path: '/workflow/todo',
  },
  {
    key: 'workflow-template',
    title: '申请',
    path: '/workflow/template',
  },
  {
    key: 'workflow-initiated',
    title: '我的',
    path: '/workflow/initiated',
  },
]
```

---

## 🎯 菜单高亮

确保路由匹配时菜单项正确高亮：

```typescript
// 根据当前路由设置选中的菜单项
const location = useLocation()
const [selectedKeys, setSelectedKeys] = useState<string[]>([])

useEffect(() => {
  const path = location.pathname

  if (path.startsWith('/workflow/todo')) {
    setSelectedKeys(['workflow-todo'])
  } else if (path.startsWith('/workflow/initiated')) {
    setSelectedKeys(['workflow-initiated'])
  } else if (path.startsWith('/workflow/template')) {
    setSelectedKeys(['workflow-template'])
  } else if (path.startsWith('/workflow/instance')) {
    setSelectedKeys(['workflow-instance'])
  } else if (path.startsWith('/workflow/definition')) {
    setSelectedKeys(['workflow-definition'])
  }
}, [location.pathname])
```

---

## ✅ 配置检查清单

完成菜单配置后，请检查以下事项：

- [ ] 菜单项在导航栏中正确显示
- [ ] 点击菜单项可以正确跳转
- [ ] 菜单高亮状态正确
- [ ] 待办任务徽标正常显示
- [ ] 权限控制生效（无权限的菜单不显示）
- [ ] 移动端菜单正常显示
- [ ] 菜单图标正确显示
- [ ] 菜单排序符合预期

---

## 🚀 快速测试

配置完成后，可以通过以下方式快速测试：

1. **访问菜单路径**
   - http://localhost:5173/workflow/todo
   - http://localhost:5173/workflow/initiated
   - http://localhost:5173/workflow/template

2. **检查菜单交互**
   - 点击菜单项是否正确跳转
   - 刷新页面后菜单高亮是否保持
   - 浏览器前进/后退按钮是否正常工作

3. **检查权限控制**
   - 以不同角色登录，查看菜单显示是否正确
   - 无权限时访问路径是否被拦截

---

## 📞 常见问题

### Q1: 菜单不显示？
- 检查数据库中菜单记录是否插入成功
- 检查 `visible` 字段是否为 '0'（显示）
- 检查用户是否有对应权限
- 检查菜单组件路径是否正确

### Q2: 点击菜单没有跳转？
- 检查路由配置是否正确
- 检查 `path` 字段是否与路由路径匹配
- 检查组件是否正确导入

### Q3: 待办任务徽标不更新？
- 检查 WebSocket 连接是否正常
- 检查定时刷新逻辑是否执行
- 检查 Zustand store 是否正确更新

### Q4: 权限控制不生效？
- 检查后端接口是否返回正确的权限数据
- 检查前端权限判断逻辑是否正确
- 检查路由守卫是否配置

---

## 📚 相关文档

- [Ant Design Menu 组件文档](https://ant.design/components/menu-cn/)
- [Ant Design Badge 组件文档](https://ant.design/components/badge-cn/)
- [React Router 文档](https://reactrouter.com/)

---

希望这个配置指南能帮助你顺利集成工作流菜单！🎉
