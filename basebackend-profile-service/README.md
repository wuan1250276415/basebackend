# Phase 10.10 - 用户档案服务迁移

## 📋 项目信息

- **Phase**: 10.10 - 用户档案服务独立化（Part 1 - 基础架构搭建）
- **开始时间**: 2025-11-14
- **服务名称**: basebackend-profile-service
- **服务端口**: 8090
- **数据库**: basebackend_profile

---

## 🎯 项目目标

将用户个人资料和偏好设置功能从单体 `basebackend-admin-api` 中独立出来，形成独立的用户档案微服务，实现：

1. ✅ **个人资料管理** - 查询和更新个人资料
2. ✅ **密码管理** - 修改登录密码
3. ✅ **偏好设置管理** - 查询和更新用户偏好设置（主题、语言、通知等）
4. ⏳ **用户中心集成** - 提供统一的用户中心服务

---

## 📦 Part 1 完成内容

### ✅ 已完成

#### 1. 基础架构
- ✅ 创建 Maven 项目结构
- ✅ 配置 pom.xml（完整依赖配置）
- ✅ 创建启动类 ProfileServiceApplication
- ✅ 配置 application.yml（数据库、Redis、Nacos 等）

#### 2. DTO 类（5 个）
- ✅ UserPreferenceDTO.java - 用户偏好设置DTO
- ✅ UpdatePreferenceDTO.java - 更新偏好设置DTO
- ✅ ProfileDetailDTO.java - 个人资料详情DTO
- ✅ UpdateProfileDTO.java - 更新个人资料DTO
- ✅ ChangePasswordDTO.java - 修改密码DTO

#### 3. 实体类和 Mapper
- ✅ UserPreference.java - 用户偏好设置实体（16 个字段）
- ✅ UserPreferenceMapper.java - MyBatis Plus Mapper

### 📊 代码统计

| 类型 | 文件数 | 说明 |
|------|--------|------|
| **配置文件** | 2 个 | pom.xml, application.yml |
| **启动类** | 1 个 | ProfileServiceApplication |
| **DTO 类** | 5 个 | Preference 和 Profile 相关 |
| **实体类** | 1 个 | UserPreference |
| **Mapper** | 1 个 | UserPreferenceMapper |
| **总计** | 10 个文件 | Part 1 基础架构 |

---

## 📂 项目结构

```
basebackend-profile-service/
├── pom.xml
├── README.md
└── src/
    └── main/
        ├── java/com/basebackend/profile/
        │   ├── ProfileServiceApplication.java
        │   ├── controller/          （待 Part 2）
        │   ├── service/             （待 Part 2）
        │   │   └── impl/            （待 Part 2）
        │   ├── dto/
        │   │   ├── preference/
        │   │   │   ├── UserPreferenceDTO.java
        │   │   │   └── UpdatePreferenceDTO.java
        │   │   └── profile/
        │   │       ├── ProfileDetailDTO.java
        │   │       ├── UpdateProfileDTO.java
        │   │       └── ChangePasswordDTO.java
        │   ├── entity/
        │   │   └── UserPreference.java
        │   └── mapper/
        │       └── UserPreferenceMapper.java
        └── resources/
            └── application.yml
```

---

## ⏳ Part 2 待办事项

### 需要迁移的内容

#### 1. Service 层（约 300 行）
- [ ] PreferenceService.java - 接口（28 行）
- [ ] PreferenceServiceImpl.java - 实现（124 行）
- [ ] ProfileService.java - 接口（36 行）
- [ ] ProfileServiceImpl.java - 实现（172 行）

#### 2. Controller 层（约 94 行）
- [ ] ProfileController.java - 合并的控制器
  - 偏好设置端点（2 个）：
    - GET /api/profile/preference - 获取偏好设置
    - PUT /api/profile/preference - 更新偏好设置
  - 个人资料端点（3 个）：
    - GET /api/profile/info - 获取个人资料
    - PUT /api/profile/info - 更新个人资料
    - PUT /api/profile/password - 修改密码

#### 3. 数据库初始化
- [ ] 创建数据库初始化脚本（profile-service-init.sql）
  - user_preference 表结构
  - 默认配置数据

#### 4. Gateway 配置
- [ ] 更新 nacos-configs/gateway-config.yml
  - 添加 /api/profile/** 路由

#### 5. 父 POM 配置
- [ ] 更新 pom.xml 添加 profile-service 模块

#### 6. 完成报告
- [ ] 创建 PHASE_10.10_COMPLETION_REPORT.md

---

## 🔑 核心功能设计

### 1. 偏好设置管理

**存储结构：**
- 每个用户一条 user_preference 记录
- 首次访问时返回默认值
- 更新时采用 UPSERT 逻辑

**默认偏好：**
```json
{
  "theme": "light",
  "language": "zh-CN",
  "emailNotification": 1,
  "smsNotification": 0,
  "systemNotification": 1,
  "pageSize": 10
}
```

### 2. 个人资料管理

**数据来源：**
- 个人资料数据存储在 `sys_user` 表（user-service）
- 通过 Feign 调用 user-service 获取用户数据
- 部门信息通过 Feign 调用 dept-service 获取

**字段验证：**
- 邮箱唯一性检查
- 手机号唯一性检查
- 头像 URL 格式验证

### 3. 密码管理

**安全机制：**
- 验证旧密码正确性
- 验证新密码与确认密码一致
- 验证新密码不能与旧密码相同
- 密码强度要求：6-20 位，包含大小写字母和数字
- 使用 BCrypt 加密存储

---

## 🗄️ 数据库设计

### user_preference 表结构

```sql
CREATE TABLE `user_preference` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',

    -- 界面设置
    `theme` VARCHAR(20) DEFAULT 'light' COMMENT '主题: light-浅色, dark-深色, auto-自动',
    `primary_color` VARCHAR(20) DEFAULT NULL COMMENT '主题色',
    `layout` VARCHAR(20) DEFAULT 'side' COMMENT '布局: side-侧边, top-顶部',
    `menu_collapse` TINYINT DEFAULT 0 COMMENT '菜单收起状态: 0-展开, 1-收起',

    -- 语言与地区
    `language` VARCHAR(10) DEFAULT 'zh-CN' COMMENT '语言: zh-CN, en-US',
    `timezone` VARCHAR(50) DEFAULT 'Asia/Shanghai' COMMENT '时区',
    `date_format` VARCHAR(20) DEFAULT 'YYYY-MM-DD' COMMENT '日期格式',
    `time_format` VARCHAR(20) DEFAULT 'HH:mm:ss' COMMENT '时间格式',

    -- 通知偏好
    `email_notification` TINYINT DEFAULT 1 COMMENT '邮件通知: 0-关闭, 1-开启',
    `sms_notification` TINYINT DEFAULT 0 COMMENT '短信通知: 0-关闭, 1-开启',
    `system_notification` TINYINT DEFAULT 1 COMMENT '系统通知: 0-关闭, 1-开启',

    -- 其他偏好
    `page_size` INT DEFAULT 10 COMMENT '分页大小',
    `dashboard_layout` TEXT DEFAULT NULL COMMENT '仪表板布局配置（JSON格式）',
    `auto_save` TINYINT DEFAULT 1 COMMENT '自动保存: 0-关闭, 1-开启',

    -- 基础字段
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户偏好设置表';
```

---

## 🔌 API 接口设计

### 1. 偏好设置接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/profile/preference` | 获取当前用户偏好设置 |
| PUT | `/api/profile/preference` | 更新当前用户偏好设置 |

### 2. 个人资料接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/profile/info` | 获取当前用户个人资料 |
| PUT | `/api/profile/info` | 更新当前用户个人资料 |
| PUT | `/api/profile/password` | 修改当前用户密码 |

---

## 🔧 技术栈

- **Spring Boot 3.1.5** - 应用框架
- **Spring Cloud Gateway** - API 网关
- **Spring Cloud Alibaba Nacos** - 服务发现 + 配置中心
- **MyBatis Plus 3.5.5** - ORM 框架
- **Druid 1.2.20** - 数据库连接池
- **Redis** - 缓存（可选，用于偏好设置缓存）
- **Lombok 1.18.38** - 代码简化
- **Swagger/OpenAPI 3** - API 文档
- **Jakarta Validation** - Bean 验证
- **Hutool** - 工具类库

---

## 📝 下一步计划

### Part 2 任务清单

1. **迁移 Service 层**
   - PreferenceService 和 ProfileService
   - 实现获取当前用户 ID 的工具方法
   - 集成 Feign 调用 user-service 和 dept-service

2. **迁移 Controller 层**
   - 合并 PreferenceController 和 ProfileController
   - 统一路由为 /api/profile/**

3. **创建数据库脚本**
   - user_preference 表结构
   - 默认配置数据

4. **配置集成**
   - Gateway 路由配置
   - 父 POM 模块配置

5. **测试验证**
   - 功能测试
   - 集成测试

6. **完成报告**
   - 详细的迁移报告

---

## ⚠️ 注意事项

1. **用户数据来源**
   - 个人资料数据需要通过 Feign 调用 user-service
   - 部门信息需要通过 Feign 调用 dept-service
   - 偏好设置数据存储在本服务的 user_preference 表

2. **事务处理**
   - 更新个人资料时需要远程调用，注意分布式事务处理
   - 密码修改需要验证旧密码，使用 BCrypt 加密

3. **安全性**
   - 所有接口都需要 JWT 认证
   - 只能操作当前登录用户的数据
   - 密码字段不能通过 API 直接查询

4. **性能优化**
   - 偏好设置可以考虑使用 Redis 缓存
   - 个人资料可以缓存部门信息

---

**创建时间**: 2025-11-14
**负责人**: BaseBackend Team
**服务版本**: 1.0.0-SNAPSHOT
**状态**: Part 1 完成，Part 2 待续
