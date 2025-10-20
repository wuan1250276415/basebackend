# 应用管理系统实现说明

## 概述

本次实现了完整的应用管理系统，支持多应用架构，包括应用信息管理、应用资源（菜单）管理，以及角色、字典的应用隔离功能。

## 数据库变更

### 新增表

#### 1. sys_application - 应用信息表
存储应用的基本信息，包括：
- `app_name`: 应用名称
- `app_code`: 应用编码（唯一标识）
- `app_type`: 应用类型（web/mobile/api等）
- `app_icon`: 应用图标
- `app_url`: 应用地址
- `status`: 是否启用
- `order_num`: 显示顺序

#### 2. sys_application_resource - 应用资源表
存储应用的菜单和资源信息，包括：
- `app_id`: 所属应用ID
- `resource_name`: 资源名称
- `parent_id`: 父资源ID
- `resource_type`: 资源类型（M-目录，C-菜单，F-按钮）
- `path`: 路由地址
- `component`: 组件路径
- `perms`: 权限标识
- `icon`: 菜单图标
- `visible`: 是否显示
- `open_type`: 打开方式（current-当前页，blank-新窗口）
- `order_num`: 显示顺序
- `status`: 状态

#### 3. sys_role_resource - 角色资源关联表
存储角色与应用资源的关联关系，替代原来的 sys_role_menu 表

### 修改表

#### 1. sys_role - 角色表
添加字段：
- `app_id`: 所属应用ID（NULL表示系统角色）

#### 2. sys_dict - 字典表
添加字段：
- `app_id`: 所属应用ID（NULL表示系统字典）

#### 3. sys_dict_data - 字典数据表
添加字段：
- `app_id`: 所属应用ID（NULL表示系统字典）

## 后端实现

### 实体类 (Entity)

1. **SysApplication** - 应用信息实体
2. **SysApplicationResource** - 应用资源实体
3. **SysRoleResource** - 角色资源关联实体
4. **SysRole** - 角色实体（新增 appId 字段）
5. **SysDict** - 字典实体（新增 appId 字段）
6. **SysDictData** - 字典数据实体（新增 appId 字段）

### DTO类

1. **ApplicationDTO** - 应用信息传输对象
2. **ApplicationResourceDTO** - 应用资源传输对象
3. **RoleDTO** - 角色传输对象（新增 appId 字段）
4. **DictDTO** - 字典传输对象（新增 appId 字段）
5. **DictDataDTO** - 字典数据传输对象（新增 appId 字段）

### Mapper层

1. **SysApplicationMapper** - 应用信息Mapper
   - 查询所有启用的应用
   - 根据应用编码查询应用

2. **SysApplicationResourceMapper** - 应用资源Mapper
   - 根据应用ID查询资源列表
   - 根据应用ID和用户ID查询用户有权限的资源
   - 查询资源树
   - 根据角色ID查询资源ID列表

3. **SysRoleResourceMapper** - 角色资源关联Mapper
   - 批量插入角色资源关联
   - 删除角色的所有资源关联
   - 根据资源ID删除关联

### Service层

1. **ApplicationService** - 应用管理服务
   - 查询应用列表
   - 查询启用的应用列表
   - 创建/更新/删除应用
   - 启用/禁用应用

2. **ApplicationResourceService** - 应用资源管理服务
   - 查询应用的资源树
   - 查询用户的资源树（根据权限过滤）
   - 创建/更新/删除资源
   - 查询角色的资源ID列表
   - 分配角色资源

### Controller层

1. **ApplicationController** - 应用管理控制器
   ```
   GET    /api/admin/application/list          - 查询应用列表
   GET    /api/admin/application/enabled       - 查询启用的应用列表
   GET    /api/admin/application/{id}          - 根据ID查询应用
   GET    /api/admin/application/code/{code}   - 根据编码查询应用
   POST   /api/admin/application               - 创建应用
   PUT    /api/admin/application               - 更新应用
   DELETE /api/admin/application/{id}          - 删除应用
   PUT    /api/admin/application/{id}/status/{status} - 修改应用状态
   ```

2. **ApplicationResourceController** - 应用资源管理控制器
   ```
   GET    /api/admin/application/resource/tree/{appId}      - 查询应用的资源树
   GET    /api/admin/application/resource/user/tree/{appId} - 查询用户的资源树
   GET    /api/admin/application/resource/{id}              - 根据ID查询资源
   POST   /api/admin/application/resource                   - 创建资源
   PUT    /api/admin/application/resource                   - 更新资源
   DELETE /api/admin/application/resource/{id}              - 删除资源
   GET    /api/admin/application/resource/role/{roleId}     - 查询角色的资源ID列表
   POST   /api/admin/application/resource/role/{roleId}/assign - 分配角色资源
   ```

## 使用说明

### 1. 初始化数据库

执行根目录下的 SQL 脚本：
```bash
mysql -u root -p basebackend < init-application-management.sql
```

该脚本会：
- 创建应用信息表和应用资源表
- 修改角色表和字典表，添加应用ID字段
- 创建角色资源关联表
- 插入默认应用数据（系统管理平台、用户门户）
- 迁移现有菜单数据到应用资源表

### 2. 编译和启动

```bash
# 编译项目
mvn clean package -DskipTests

# 启动服务
./start-admin-api.sh
```

### 3. API测试

#### 查询应用列表
```bash
curl -X GET "http://localhost:8081/api/admin/application/list" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### 创建应用
```bash
curl -X POST "http://localhost:8081/api/admin/application" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "appName": "财务管理系统",
    "appCode": "finance",
    "appType": "web",
    "appIcon": "el-icon-money",
    "appUrl": "/finance",
    "status": 1,
    "orderNum": 3
  }'
```

#### 查询应用资源树
```bash
curl -X GET "http://localhost:8081/api/admin/application/resource/tree/1" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### 创建应用资源
```bash
curl -X POST "http://localhost:8081/api/admin/application/resource" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "appId": 1,
    "resourceName": "用户管理",
    "parentId": 0,
    "resourceType": "M",
    "path": "/user",
    "icon": "el-icon-user",
    "visible": 1,
    "openType": "current",
    "orderNum": 1,
    "status": 1
  }'
```

#### 分配角色资源
```bash
curl -X POST "http://localhost:8081/api/admin/application/resource/role/1/assign" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '[1, 2, 3, 4, 5]'
```

## 核心功能

### 1. 应用管理
- 创建和管理多个应用（如：后台管理系统、用户门户、财务系统等）
- 每个应用有独立的编码、图标、地址等信息
- 支持启用/禁用应用

### 2. 应用资源管理
- 为每个应用维护独立的菜单资源
- 支持树形结构的资源管理
- 资源类型包括：目录(M)、菜单(C)、按钮(F)
- 支持设置打开方式（当前页/新窗口）

### 3. 角色应用隔离
- 角色可以归属到特定应用
- 不同应用的角色互相独立
- 通过 app_id 字段实现隔离

### 4. 字典应用隔离
- 字典可以归属到特定应用
- 不同应用的字典数据互相独立
- 支持系统级字典（app_id 为 NULL）

### 5. 权限控制
- 用户通过角色获得应用资源的访问权限
- 支持按应用查询用户可访问的资源树
- 细粒度的资源权限控制

## 架构优势

1. **多租户支持** - 通过应用隔离，可以轻松实现多租户系统
2. **灵活扩展** - 可以随时添加新应用，无需修改现有代码
3. **权限细化** - 资源级别的权限控制，更加灵活
4. **数据隔离** - 角色和字典按应用隔离，避免数据混乱
5. **兼容性好** - 向后兼容现有的菜单系统

## 注意事项

1. 应用编码（app_code）必须唯一，建议使用小写字母和下划线
2. 删除应用时，会同时软删除该应用下的所有资源
3. 删除资源前，需要先删除其所有子资源
4. 角色的 app_id 为 NULL 时表示系统角色，可以访问所有应用
5. 字典的 app_id 为 NULL 时表示系统字典，所有应用都可以使用

## 后续扩展建议

1. **前端实现**
   - 应用选择器组件
   - 应用资源树管理界面
   - 角色资源分配界面

2. **功能增强**
   - 应用级别的数据权限控制
   - 应用访问日志
   - 应用使用统计

3. **性能优化**
   - 资源树缓存
   - 用户权限缓存
   - 应用配置缓存

## 文件清单

### 实体类
- `basebackend-admin-api/src/main/java/com/basebackend/admin/entity/SysApplication.java`
- `basebackend-admin-api/src/main/java/com/basebackend/admin/entity/SysApplicationResource.java`
- `basebackend-admin-api/src/main/java/com/basebackend/admin/entity/SysRoleResource.java`

### DTO类
- `basebackend-admin-api/src/main/java/com/basebackend/admin/dto/ApplicationDTO.java`
- `basebackend-admin-api/src/main/java/com/basebackend/admin/dto/ApplicationResourceDTO.java`

### Mapper
- `basebackend-admin-api/src/main/java/com/basebackend/admin/mapper/SysApplicationMapper.java`
- `basebackend-admin-api/src/main/java/com/basebackend/admin/mapper/SysApplicationResourceMapper.java`
- `basebackend-admin-api/src/main/java/com/basebackend/admin/mapper/SysRoleResourceMapper.java`

### Mapper XML
- `basebackend-admin-api/src/main/resources/mapper/SysApplicationMapper.xml`
- `basebackend-admin-api/src/main/resources/mapper/SysApplicationResourceMapper.xml`
- `basebackend-admin-api/src/main/resources/mapper/SysRoleResourceMapper.xml`

### Service
- `basebackend-admin-api/src/main/java/com/basebackend/admin/service/ApplicationService.java`
- `basebackend-admin-api/src/main/java/com/basebackend/admin/service/ApplicationResourceService.java`
- `basebackend-admin-api/src/main/java/com/basebackend/admin/service/impl/ApplicationServiceImpl.java`
- `basebackend-admin-api/src/main/java/com/basebackend/admin/service/impl/ApplicationResourceServiceImpl.java`

### Controller
- `basebackend-admin-api/src/main/java/com/basebackend/admin/controller/ApplicationController.java`
- `basebackend-admin-api/src/main/java/com/basebackend/admin/controller/ApplicationResourceController.java`

### SQL脚本
- `init-application-management.sql`

## 总结

本次实现完成了一个完整的应用管理系统，支持：
- ✅ 应用信息的增删改查
- ✅ 应用资源（菜单）的树形管理
- ✅ 角色和资源的关联
- ✅ 角色的应用隔离
- ✅ 字典的应用隔离
- ✅ 用户权限的应用级控制
- ✅ 数据库迁移脚本

系统已经可以正常使用，后续可以根据实际需求进行前端界面的开发和功能的进一步优化。
