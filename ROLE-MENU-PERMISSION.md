# 🔐 角色菜单权限功能说明

## 📋 功能概述

实现了基于角色的动态菜单权限控制，不同角色登录后只能看到被分配的菜单，未分配的菜单不予显示。

## 🎯 核心功能

### 1. **后端实现** ✅

#### 菜单权限查询
```java
// MenuController.java
@GetMapping("/current-user")
public Result<List<MenuDTO>> getCurrentUserMenuTree() {
    // 获取当前登录用户ID
    Long currentUserId = getCurrentUserId();
    // 查询用户的菜单权限
    List<MenuDTO> menuTree = menuService.getMenuTreeByUserId(currentUserId);
    return Result.success("查询成功", menuTree);
}
```

#### 查询逻辑
```sql
-- 通过用户角色关联查询菜单
SELECT DISTINCT m.* FROM sys_menu m
INNER JOIN sys_role_menu rm ON m.id = rm.menu_id
INNER JOIN sys_user_role ur ON rm.role_id = ur.role_id
WHERE ur.user_id = #{userId}
  AND m.status = 1
  AND m.visible = 1
  AND m.deleted = 0
ORDER BY m.order_num
```

### 2. **前端实现** ✅

#### 动态菜单加载
```typescript
// BasicLayout/index.tsx
useEffect(() => {
  loadUserMenu()
}, [])

const loadUserMenu = async () => {
  const response = await getCurrentUserMenuTree()
  const menus = response.data || []
  setMenuList(menus) // 保存到状态管理
}
```

#### 菜单构建
```typescript
const buildMenuItems = (menus: MenuType[]): MenuProps['items'] => {
  return menus
    .filter(menu => menu.visible === 1 && menu.status === 1)
    .sort((a, b) => (a.orderNum || 0) - (b.orderNum || 0))
    .map(menu => ({
      key: menu.path || menu.id,
      icon: <IconComponent />,
      label: menu.menuName,
      children: menu.children ? buildMenuItems(menu.children) : undefined,
      onClick: () => navigate(menu.path)
    }))
}
```

#### 菜单状态管理
```typescript
// stores/menu.ts
export const useMenuStore = create<MenuState>()(
  persist(
    (set) => ({
      menuList: [],
      setMenuList: (menuList) => set({ menuList }),
      clearMenu: () => set({ menuList: [] }),
    }),
    { name: 'menu-storage' }
  )
)
```

## 🔧 使用步骤

### 步骤1: 创建角色
```bash
# 访问角色管理
http://localhost:3000/system/role

# 操作
1. 点击"新增角色"
2. 填写角色信息
   - 角色名称: 如"普通员工"
   - 角色标识: 如"employee"
   - 数据范围: 选择权限范围
3. 保存角色
```

### 步骤2: 分配菜单权限
```bash
# 在角色列表中
1. 找到目标角色
2. 点击"分配菜单"按钮
3. 在树形菜单中勾选权限
   - 勾选父节点会自动勾选子节点
   - 可以只勾选部分子节点
4. 点击"确定"保存
```

### 步骤3: 分配角色给用户
```bash
# 访问用户管理
http://localhost:3000/system/user

# 操作
1. 找到目标用户
2. 点击"编辑"
3. 在"角色"选择框中选择角色（可多选）
4. 保存用户信息
```

### 步骤4: 验证权限
```bash
# 使用该用户登录
1. 退出当前登录
2. 使用测试用户登录
3. 观察左侧菜单
   - 只显示被分配的菜单
   - 未分配的菜单不显示
```

## 📊 权限示例

### 示例1: 超级管理员（admin）
```
✅ 系统管理
   ✅ 用户管理
   ✅ 角色管理
   ✅ 菜单管理
   ✅ 部门管理
   ✅ 字典管理
✅ 系统监控
   ✅ 在线用户
   ✅ 服务器监控
   ✅ 登录日志
   ✅ 操作日志
```

### 示例2: 普通员工（employee）
```
✅ 个人中心
   ✅ 个人信息
   ✅ 修改密码
❌ 系统管理 (未分配)
❌ 系统监控 (未分配)
```

### 示例3: 部门经理（manager）
```
✅ 系统管理
   ✅ 用户管理 (仅查看)
   ❌ 角色管理 (未分配)
   ❌ 菜单管理 (未分配)
   ✅ 部门管理
✅ 个人中心
```

## 🎨 菜单配置说明

### 菜单类型
- **M - 目录**: 仅作为导航，不对应具体页面
- **C - 菜单**: 对应具体页面，可点击跳转
- **F - 按钮**: 页面内的操作按钮，不在侧边栏显示

### 菜单字段说明
```typescript
{
  menuName: "用户管理",      // 菜单名称
  parentId: "1",            // 父菜单ID (0表示根菜单)
  orderNum: 1,              // 排序号（越小越靠前）
  path: "/system/user",     // 路由路径
  component: "User",        // 组件名称
  menuType: "C",            // 菜单类型
  visible: 1,               // 是否显示 (1:显示 0:隐藏)
  status: 1,                // 状态 (1:启用 0:禁用)
  icon: "UserOutlined",     // 图标名称
  perms: "system:user:list" // 权限标识
}
```

### 图标配置
前端支持的图标（可在菜单管理中配置）:
```typescript
const iconMap = {
  DashboardOutlined,  // 仪表盘
  UserOutlined,       // 用户
  TeamOutlined,       // 团队/角色
  ApartmentOutlined,  // 组织/部门
  SafetyOutlined,     // 安全/权限
  BookOutlined,       // 书/字典
  MonitorOutlined,    // 监控
  SettingOutlined,    // 设置
}
```

## 🔐 权限控制流程

```
┌─────────────┐
│  用户登录   │
└──────┬──────┘
       │
       ▼
┌─────────────┐
│ 查询用户角色 │
└──────┬──────┘
       │
       ▼
┌─────────────┐
│ 查询角色菜单 │
└──────┬──────┘
       │
       ▼
┌─────────────┐
│ 构建菜单树  │
└──────┬──────┘
       │
       ▼
┌─────────────┐
│ 渲染侧边栏  │
└─────────────┘
```

## 📝 数据库表关系

```
sys_user (用户表)
    ↓ (1:N)
sys_user_role (用户角色关联表)
    ↓ (N:1)
sys_role (角色表)
    ↓ (1:N)
sys_role_menu (角色菜单关联表)
    ↓ (N:1)
sys_menu (菜单表)
```

## 🔄 缓存机制

### 菜单缓存
```typescript
// 使用 Zustand + localStorage 持久化
{
  name: 'menu-storage',
  storage: localStorage,
  partialize: (state) => ({
    menuList: state.menuList
  })
}
```

### 缓存刷新
- **登录时**: 自动加载菜单
- **角色变更**: 需要重新登录
- **登出时**: 清除菜单缓存

## 🎯 实现细节

### 1. 菜单过滤
```typescript
menus
  .filter(menu => menu.visible === 1)  // 只显示可见菜单
  .filter(menu => menu.status === 1)   // 只显示启用菜单
  .filter(menu => menu.menuType !== 'F') // 不显示按钮类型
  .sort((a, b) => a.orderNum - b.orderNum) // 按序号排序
```

### 2. 递归构建
```typescript
const buildMenuItems = (menus: MenuType[]): MenuProps['items'] => {
  return menus.map(menu => ({
    key: menu.id,
    label: menu.menuName,
    icon: <Icon />,
    children: menu.children ? buildMenuItems(menu.children) : undefined
  }))
}
```

### 3. 路由跳转
```typescript
menuItem.onClick = () => {
  if (menu.path) {
    navigate(menu.path)
  }
}
```

## 🧪 测试步骤

### 1. 创建测试角色
```bash
# 1. 登录管理员账号
用户名: admin
密码: admin123

# 2. 创建角色
角色名称: 测试角色
角色标识: test_role

# 3. 分配菜单权限
勾选: 
  - 仪表盘
  - 系统管理 > 用户管理 (仅查看权限)
```

### 2. 创建测试用户
```bash
# 1. 创建用户
用户名: testuser
密码: test123
昵称: 测试用户

# 2. 分配角色
选择: 测试角色
```

### 3. 验证权限
```bash
# 1. 退出管理员账号
# 2. 使用 testuser 登录
# 3. 观察左侧菜单
预期结果:
  ✅ 显示: 仪表盘
  ✅ 显示: 系统管理 > 用户管理
  ❌ 不显示: 角色管理、菜单管理等其他菜单
```

## 🚀 高级功能

### 1. 按钮权限控制
```typescript
// 在页面组件中判断按钮权限
const { permissions } = useAuthStore()

const canCreate = permissions.includes('system:user:create')
const canEdit = permissions.includes('system:user:edit')
const canDelete = permissions.includes('system:user:delete')

<Button disabled={!canCreate}>新增</Button>
<Button disabled={!canEdit}>编辑</Button>
<Button disabled={!canDelete}>删除</Button>
```

### 2. 数据权限控制
```java
// 在Service层根据角色的dataScope过滤数据
public List<User> getUserList(UserQuery query) {
    // 获取当前用户角色
    List<Role> roles = getCurrentUserRoles();
    
    // 根据数据范围过滤
    if (hasDataScope(roles, DataScope.DEPT)) {
        // 只查询本部门数据
        query.setDeptId(getCurrentUserDeptId());
    }
    
    return userMapper.selectList(query);
}
```

### 3. 菜单国际化
```typescript
// 支持多语言菜单
const menuName = t(menu.menuName) || menu.menuName
```

## 📞 常见问题

### Q1: 修改角色权限后不生效？
**A**: 需要重新登录，因为菜单在登录时加载并缓存。

### Q2: 如何添加新菜单图标？
**A**: 在 `BasicLayout/index.tsx` 的 `iconMap` 中添加新图标。

### Q3: 菜单顺序如何调整？
**A**: 修改菜单的 `orderNum` 字段，数字越小越靠前。

### Q4: 如何隐藏某个菜单？
**A**: 将菜单的 `visible` 字段设为 0，或 `status` 设为 0。

### Q5: 支持多级菜单吗？
**A**: 支持无限层级菜单，通过递归构建实现。

## 🎉 功能特点

✅ **动态加载** - 根据用户角色动态加载菜单  
✅ **权限控制** - 细粒度的菜单权限控制  
✅ **树形结构** - 支持多级菜单  
✅ **图标支持** - 可配置菜单图标  
✅ **排序功能** - 自定义菜单顺序  
✅ **缓存优化** - 菜单数据本地缓存  
✅ **易于扩展** - 便于添加新菜单和权限  

---

## 📚 相关文档

- [完整功能总结](COMPLETE-FEATURES-SUMMARY.md)
- [快速启动指南](FULLSTACK-QUICKSTART.md)
- [前端开发指南](ADMIN-WEB-GUIDE.md)

**角色菜单权限功能已完成！** 🎊
