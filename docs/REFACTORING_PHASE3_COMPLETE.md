# 阶段三：拆分 admin-api - 完成报告

> **完成时间**: 2025-11-17  
> **执行阶段**: Phase 3 - Admin API 拆分  
> **状态**: ✅ 已完成

---

## 一、执行概述

### 1.1 目标

将臃肿的 `basebackend-admin-api` 模块拆分为三个独立的微服务：
- **basebackend-user-api**: 用户、角色、权限管理
- **basebackend-system-api**: 字典、菜单、部门、日志管理
- **basebackend-auth-api**: 认证、授权、会话管理

### 1.2 拆分原则

```
原 admin-api (16个依赖)
    ↓
┌───────────────┬────────────────┬───────────────┐
│   user-api    │  system-api    │   auth-api    │
│   (5个依赖)   │   (5个依赖)    │   (6个依赖)   │
└───────────────┴────────────────┴───────────────┘
```

**优势**:
- 减少单个服务的依赖数量
- 提高服务独立性和可扩展性
- 降低内存占用和启动时间
- 支持独立部署和扩缩容

---

## 二、创建的新模块

### 2.1 basebackend-user-api (用户服务)

**端口**: 8081  
**职责**: 用户、角色、权限管理

#### 依赖模块
```xml
- basebackend-common
- basebackend-database
- basebackend-cache
- basebackend-security
- basebackend-web
```

#### 核心功能
- 用户管理 (UserController)
- 角色管理 (RoleController)
- 权限管理 (PermissionController)
- 用户配置 (ProfileController)

#### 文件结构
```
basebackend-user-api/
├── pom.xml
├── Dockerfile
└── src/main/
    ├── java/com/basebackend/user/
    │   ├── UserApiApplication.java
    │   └── config/
    │       └── SwaggerConfig.java
    └── resources/
        ├── application.yml
        └── bootstrap.yml
```

---

### 2.2 basebackend-system-api (系统服务)

**端口**: 8082  
**职责**: 系统配置和日志管理

#### 依赖模块
```xml
- basebackend-common
- basebackend-database
- basebackend-cache
- basebackend-web
- basebackend-logging
```

#### 核心功能
- 字典管理 (DictController)
- 菜单管理 (MenuController)
- 部门管理 (DeptController)
- 日志管理 (LogController)
- 监控管理 (MonitorController)

#### 文件结构
```
basebackend-system-api/
├── pom.xml
├── Dockerfile
└── src/main/
    ├── java/com/basebackend/system/
    │   ├── SystemApiApplication.java
    │   └── config/
    │       └── SwaggerConfig.java
    └── resources/
        ├── application.yml
        └── bootstrap.yml
```

---

### 2.3 basebackend-auth-api (认证服务)

**端口**: 8083  
**职责**: 认证、授权、会话管理

#### 依赖模块
```xml
- basebackend-common
- basebackend-cache
- basebackend-security
- basebackend-jwt
- basebackend-web
- spring-cloud-starter-openfeign (服务间调用)
```

#### 核心功能
- 登录认证 (AuthController)
- 会话管理 (SecurityController)
- 在线用户管理
- 双因素认证 (2FA)

#### 文件结构
```
basebackend-auth-api/
├── pom.xml
├── Dockerfile
└── src/main/
    ├── java/com/basebackend/auth/
    │   ├── AuthApiApplication.java
    │   └── config/
    │       └── SwaggerConfig.java
    └── resources/
        ├── application.yml
        └── bootstrap.yml
```

---

## 三、配置文件说明

### 3.1 应用配置 (application.yml)

每个服务都有独立的端口和配置：

```yaml
# user-api: 8081
# system-api: 8082
# auth-api: 8083

server:
  port: 808X

spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always

knife4j:
  enable: true
  setting:
    language: zh_cn
```

### 3.2 Nacos配置 (bootstrap.yml)

所有服务统一使用Nacos配置中心：

```yaml
spring:
  application:
    name: basebackend-xxx-api
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER:localhost:8848}
        namespace: ${NACOS_NAMESPACE:dev}
      config:
        server-addr: ${NACOS_SERVER:localhost:8848}
        namespace: ${NACOS_NAMESPACE:dev}
        file-extension: yml
        shared-configs:
          - data-id: common-config.yml
          - data-id: database-config.yml
          - data-id: cache-config.yml
```

---

## 四、Docker支持

### 4.1 Dockerfile

每个服务都有独立的多阶段构建Dockerfile：

**特点**:
- 使用Maven多阶段构建
- 基于Alpine Linux (轻量级)
- 非root用户运行 (安全)
- 内置健康检查
- JVM容器优化参数

**镜像大小预估**:
- user-api: ~200MB
- system-api: ~200MB
- auth-api: ~180MB

### 4.2 Docker Compose编排

创建了 `docker/compose/services/docker-compose.services.yml`:

```yaml
services:
  gateway:
    ports: ["8080:8080"]
    depends_on: [user-api, system-api, auth-api]
  
  user-api:
    healthcheck: http://localhost:8081/actuator/health
    replicas: ${USER_API_REPLICAS:-1}
  
  system-api:
    healthcheck: http://localhost:8082/actuator/health
    replicas: ${SYSTEM_API_REPLICAS:-1}
  
  auth-api:
    healthcheck: http://localhost:8083/actuator/health
    replicas: ${AUTH_API_REPLICAS:-1}
```

---

## 五、API文档配置

### 5.1 Swagger/Knife4j配置

每个服务都配置了独立的API文档：

**user-api**:
- 用户管理 API
- 角色管理 API
- 权限管理 API

**system-api**:
- 字典管理 API
- 菜单管理 API
- 部门管理 API
- 日志管理 API

**auth-api**:
- 认证授权 API
- 会话管理 API

### 5.2 访问地址

```
http://localhost:8081/doc.html  # user-api
http://localhost:8082/doc.html  # system-api
http://localhost:8083/doc.html  # auth-api
http://localhost:8080/doc.html  # gateway (聚合)
```

---

## 六、服务依赖关系

### 6.1 服务间调用

```
┌─────────────┐
│   Gateway   │ :8080
└──────┬──────┘
       │
   ┌───┴────────────────┐
   │                    │
┌──▼──────┐  ┌─────────▼┐  ┌──────────┐
│user-api │  │system-api│  │ auth-api │
│  :8081  │  │  :8082   │  │  :8083   │
└─────────┘  └──────────┘  └────┬─────┘
                                 │
                          ┌──────▼──────┐
                          │ Feign调用   │
                          │ user-api    │
                          └─────────────┘
```

### 6.2 数据库访问

- **user-api**: sys_user, sys_role, sys_permission, sys_user_role
- **system-api**: sys_dict, sys_menu, sys_dept, sys_operation_log
- **auth-api**: 通过Feign调用user-api获取用户信息

---

## 七、父POM更新

### 7.1 新增模块声明

```xml
<modules>
    <!-- ... 现有模块 ... -->
    <module>basebackend-admin-api</module>
    
    <!-- 新拆分的微服务模块 -->
    <module>basebackend-user-api</module>
    <module>basebackend-system-api</module>
    <module>basebackend-auth-api</module>
</modules>
```

---

## 八、下一步工作

### 8.1 代码迁移 (待执行)

需要从 `basebackend-admin-api` 迁移以下代码：

#### user-api
```
controller/
  - UserController.java
  - RoleController.java
  - PermissionController.java
  - ProfileController.java

service/
  - UserService.java
  - RoleService.java
  - PermissionService.java
  - ProfileService.java

mapper/
  - SysUserMapper.java
  - SysRoleMapper.java
  - SysPermissionMapper.java
  - SysUserRoleMapper.java

entity/
  - SysUser.java
  - SysRole.java
  - SysPermission.java
  - SysUserRole.java
```

#### system-api
```
controller/
  - DictController.java
  - MenuController.java
  - DeptController.java
  - LogController.java
  - MonitorController.java

service/
  - DictService.java
  - MenuService.java
  - DeptService.java
  - LogService.java
  - MonitorService.java

mapper/
  - SysDictMapper.java
  - SysMenuMapper.java
  - SysDeptMapper.java
  - SysOperationLogMapper.java

entity/
  - SysDict.java
  - SysMenu.java
  - SysDept.java
  - SysOperationLog.java
```

#### auth-api
```
controller/
  - AuthController.java
  - SecurityController.java

service/
  - AuthService.java
  - SecurityService.java

filter/
  - JwtAuthenticationFilter.java

config/
  - AdminSecurityConfig.java
```

### 8.2 配置迁移

需要在Nacos中创建各服务的配置：
- basebackend-user-api.yml
- basebackend-system-api.yml
- basebackend-auth-api.yml

### 8.3 网关路由配置

需要在Gateway中配置新服务的路由：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-api
          uri: lb://basebackend-user-api
          predicates:
            - Path=/api/users/**,/api/roles/**,/api/permissions/**
        
        - id: system-api
          uri: lb://basebackend-system-api
          predicates:
            - Path=/api/dicts/**,/api/menus/**,/api/depts/**,/api/logs/**
        
        - id: auth-api
          uri: lb://basebackend-auth-api
          predicates:
            - Path=/api/auth/**,/api/sessions/**
```

### 8.4 测试验证

- [ ] 编译测试: `mvn clean install -DskipTests`
- [ ] 单元测试: `mvn test`
- [ ] 集成测试: 启动所有服务并测试API
- [ ] 性能测试: 对比拆分前后的性能指标

---

## 九、预期收益

### 9.1 性能提升

| 指标 | admin-api | 拆分后单服务 | 提升 |
|-----|-----------|------------|------|
| 启动时间 | ~60s | ~30s | 50% |
| 内存占用 | ~1.5GB | ~500MB | 67% |
| 依赖数量 | 16个 | 5-6个 | 65% |

### 9.2 架构优势

✅ **独立部署**: 每个服务可独立发布，互不影响  
✅ **弹性扩展**: 根据负载独立扩缩容  
✅ **故障隔离**: 单个服务故障不影响其他服务  
✅ **技术演进**: 可独立升级技术栈  
✅ **团队协作**: 不同团队可并行开发

---

## 十、总结

### 10.1 已完成工作

✅ 创建了3个新的微服务模块  
✅ 配置了独立的POM依赖  
✅ 创建了应用启动类和配置文件  
✅ 配置了Swagger API文档  
✅ 创建了Docker镜像构建文件  
✅ 更新了Docker Compose编排  
✅ 更新了父POM模块声明

### 10.2 待完成工作

⏳ 代码迁移 (Controller, Service, Mapper, Entity)  
⏳ Nacos配置创建  
⏳ Gateway路由配置  
⏳ 数据库访问权限配置  
⏳ 集成测试和验证

### 10.3 风险提示

⚠️ **数据一致性**: 跨服务事务需要使用Seata分布式事务  
⚠️ **服务调用**: auth-api调用user-api需要配置Feign超时和重试  
⚠️ **配置管理**: 确保Nacos配置正确，避免服务启动失败  
⚠️ **网络延迟**: 服务间调用会增加网络开销，需要优化

---

**文档版本**: v1.0  
**创建时间**: 2025-11-17  
**负责人**: Architecture Team  
**下一阶段**: 代码迁移和集成测试
