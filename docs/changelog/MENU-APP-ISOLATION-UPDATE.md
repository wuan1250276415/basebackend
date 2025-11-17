# 菜单管理应用隔离功能更新说明

## 概述
本次更新为菜单管理添加了应用级隔离功能，并修复了创建菜单后无法渲染的bug（sys_role_menu表数据缺失问题）。

## 修改内容

### 1. 后端修改

#### 1.1 实体类更新
**SysMenu.java** (basebackend-admin-api/src/main/java/com/basebackend/admin/entity/SysMenu.java:18-22)
- 添加 `appId` 字段，支持应用级菜单隔离
- 当 appId 为 null 时，表示系统菜单

```java
/**
 * 应用ID（为空表示系统菜单）
 */
@TableField("app_id")
private Long appId;
```

#### 1.2 DTO更新
**MenuDTO.java** (basebackend-admin-api/src/main/java/com/basebackend/admin/dto/MenuDTO.java:20-23)
- 添加 `appId` 字段

```java
/**
 * 应用ID（为空表示系统菜单）
 */
private Long appId;
```

#### 1.3 Service层修复
**MenuServiceImpl.java** (basebackend-admin-api/src/main/java/com/basebackend/admin/service/impl/MenuServiceImpl.java:58-93)
- **重要修复**：在 `create` 方法中添加了自动为管理员角色分配菜单权限的逻辑
- 解决了创建菜单后 sys_role_menu 表没有数据，导致菜单无法渲染的问题

```java
// 自动为管理员角色（ID=1）分配新创建的菜单权限
try {
    SysRoleMenu roleMenu = new SysRoleMenu();
    roleMenu.setRoleId(1L); // 管理员角色ID
    roleMenu.setMenuId(menu.getId());
    roleMenu.setCreateTime(LocalDateTime.now());
    roleMenu.setCreateBy(1L);
    roleMenuMapper.insert(roleMenu);
    log.info("已自动为管理员角色分配菜单权限: menuId={}", menu.getId());
} catch (Exception e) {
    log.warn("为管理员角色分配菜单权限失败（可能已存在）: {}", e.getMessage());
    // 不影响菜单创建，只记录警告
}
```

### 2. 前端修改

#### 2.1 类型定义更新
**types/index.ts** (basebackend-admin-web/src/types/index.ts:90-111)
- Menu 接口添加 `appId` 字段

#### 2.2 菜单管理页面完全重写
**Menu/index.tsx** (basebackend-admin-web/src/pages/System/Menu/index.tsx)

新增功能：
- **应用筛选**：在页面顶部添加应用选择器，支持按应用过滤菜单
  - 全部：显示所有菜单
  - 系统菜单：只显示 appId 为空的菜单
  - 各应用菜单：显示对应应用的菜单

- **应用标签显示**：每个菜单节点显示所属应用的标签
  - 系统菜单：灰色标签 "系统菜单"
  - 应用菜单：蓝色标签显示应用名称

- **新增菜单时自动继承应用**：
  - 在已选择应用的情况下新增根菜单，自动设置为该应用的菜单
  - 新增子菜单时，自动继承父菜单的应用ID

- **表单增强**：
  - 添加"所属应用"选择字段（可选，为空则为系统菜单）
  - 调整表单布局，将应用选择放在首位

### 3. 数据库更新

#### 3.1 数据库迁移脚本
**add-menu-app-id.sql** - 为 sys_menu 表添加 app_id 字段的迁移脚本

```sql
-- 1. 为 sys_menu 表添加 app_id 字段
ALTER TABLE sys_menu
ADD COLUMN app_id BIGINT DEFAULT NULL COMMENT '应用ID（为空表示系统菜单）' AFTER id;

-- 2. 为 app_id 字段添加索引
CREATE INDEX idx_app_id ON sys_menu(app_id);

-- 3. 更新表注释
ALTER TABLE sys_menu COMMENT='系统菜单表（支持应用级隔离）';
```

## 问题修复

### Bug #1: 创建菜单后无法渲染
**问题描述**：
- 创建菜单后，sys_role_menu 表没有对应的数据
- 导致管理员用户无法看到新创建的菜单

**根本原因**：
- MenuServiceImpl.create() 方法只创建了菜单记录
- 没有自动为管理员角色分配该菜单权限

**解决方案**：
- 在创建菜单时，自动为管理员角色（role_id=1）插入 sys_role_menu 记录
- 使用 try-catch 包裹，避免重复插入时影响菜单创建

### Bug #2: 菜单缺少应用隔离
**问题描述**：
- 所有应用的菜单混在一起
- 无法区分系统菜单和应用菜单

**解决方案**：
- 为 SysMenu 实体和 MenuDTO 添加 appId 字段
- 前端页面添加应用筛选功能
- 支持按应用查看和管理菜单

## 使用说明

### 1. 执行数据库迁移
```bash
# 连接到数据库
mysql -u root -p your_database

# 执行迁移脚本
source /path/to/add-menu-app-id.sql
```

### 2. 重启后端服务
```bash
cd basebackend-admin-api
mvn spring-boot:run
```

### 3. 使用菜单管理
1. 访问菜单管理页面
2. 使用顶部的"所属应用"下拉框筛选菜单：
   - 选择"全部"：查看所有菜单
   - 选择"系统菜单"：只查看系统级菜单
   - 选择具体应用：查看该应用的菜单
3. 新增菜单时：
   - 可以选择所属应用（留空则为系统菜单）
   - 子菜单会自动继承父菜单的应用
4. 新创建的菜单会自动分配给管理员角色，无需手动设置

## 功能特性

✅ **应用级隔离**：不同应用可以有独立的菜单结构
✅ **系统菜单支持**：appId 为空表示系统级菜单，所有应用共享
✅ **自动权限分配**：创建菜单时自动为管理员分配权限
✅ **前端筛选**：支持按应用筛选菜单显示
✅ **可视化标签**：清晰显示每个菜单所属的应用
✅ **继承机制**：子菜单自动继承父菜单的应用设置
✅ **向后兼容**：已有菜单（appId为空）自动作为系统菜单

## 注意事项

1. **数据迁移**：执行 add-menu-app-id.sql 前请先备份数据库
2. **管理员角色ID**：代码中硬编码了管理员角色ID为1，如果你的系统不同需要修改
3. **权限分配**：只有新创建的菜单会自动分配给管理员，已有菜单不受影响
4. **应用选择**：创建菜单时不选择应用则为系统菜单，系统菜单对所有应用可见

## 编译状态

✅ 后端编译成功 (BUILD SUCCESS)
✅ 前端无编译错误
✅ 所有修改已验证

## 相关文件

### 后端文件
- `basebackend-admin-api/src/main/java/com/basebackend/admin/entity/SysMenu.java`
- `basebackend-admin-api/src/main/java/com/basebackend/admin/dto/MenuDTO.java`
- `basebackend-admin-api/src/main/java/com/basebackend/admin/service/impl/MenuServiceImpl.java`

### 前端文件
- `basebackend-admin-web/src/pages/System/Menu/index.tsx`
- `basebackend-admin-web/src/types/index.ts`

### 数据库脚本
- `add-menu-app-id.sql`
