# 阶段三：拆分 admin-api 实施总结

## 实施日期
2025-11-17

## 已完成工作

### 1. 创建新的服务模块结构

#### basebackend-user-api（用户服务）
- ✅ 创建模块目录结构
- ✅ 创建 pom.xml 配置文件
- ✅ 创建启动类 UserApiApplication.java
- ✅ 迁移 Controller 层：
  - ProfileController.java
  - RoleController.java
  - UserController.java
- ✅ 迁移 Service 层（已包含实现类）
- ✅ 迁移相关 Entity 和 Mapper

#### basebackend-system-api（系统服务）
- ✅ 创建模块目录结构
- ✅ 创建 pom.xml 配置文件
- ✅ 创建启动类 SystemApiApplication.java
- ✅ 迁移 Controller 层：
  - DeptController.java（部门管理）
  - MenuController.java（菜单管理）
  - DictController.java（字典管理）
- ✅ 创建 application.yml 配置文件
- ⏳ Service、DTO、Mapper 层待完成

#### basebackend-auth-api（认证服务）
- ✅ 创建模块目录结构
- ✅ 创建 pom.xml 配置文件
- ✅ 创建启动类 AuthApiApplication.java
- ✅ 迁移 Controller 层：
  - AuthController.java（认证管理）
- ✅ 创建 application.yml 配置文件（包含JWT配置）
- ⏳ Service、DTO 层待完成

### 2. 更新项目配置
- ✅ 在父 pom.xml 中声明新模块
- ✅ 调整 API 路径：
  - /api/admin/* → /api/user/*（用户服务）
  - /api/admin/* → /api/system/*（系统服务）
  - /api/admin/auth/* → /api/auth/*（认证服务）

## 待完成工作

### 1. 完成Service层迁移
```bash
# system-api需要迁移的Service：
- DeptService 及其实现类
- MenuService 及其实现类
- DictService 及其实现类
- ApplicationResourceService 及其实现类

# auth-api需要迁移的Service：
- AuthService 及其实现类
- TokenService 及其实现类
- LoginService 及其实现类
```

### 2. 完成DTO/Entity层迁移
```bash
# system-api需要的DTO：
- DeptDTO
- MenuDTO
- DictDTO
- DictDataDTO
- ApplicationResourceDTO

# auth-api需要的DTO：
- LoginRequest
- LoginResponse
- PasswordChangeDTO
```

### 3. 完成Mapper层迁移
```bash
# system-api需要的Mapper：
- DeptMapper
- MenuMapper
- DictMapper
- ApplicationResourceMapper

# 对应的MyBatis XML文件也需要迁移
```

### 4. 处理依赖关系
```xml
<!-- system-api的pom.xml需要补充的依赖 -->
<dependency>
    <groupId>com.basebackend</groupId>
    <artifactId>basebackend-jwt</artifactId>
    <version>${project.version}</version>
</dependency>

<!-- auth-api的pom.xml需要补充的依赖 -->
<dependency>
    <groupId>com.basebackend</groupId>
    <artifactId>basebackend-jwt</artifactId>
    <version>${project.version}</version>
</dependency>
<dependency>
    <groupId>com.basebackend</groupId>
    <artifactId>basebackend-security</artifactId>
    <version>${project.version}</version>
</dependency>
```

### 5. 更新网关路由配置
```yaml
# gateway配置需要添加新的路由规则
spring:
  cloud:
    gateway:
      routes:
        - id: user-api
          uri: lb://basebackend-user-api
          predicates:
            - Path=/api/user/**
        
        - id: system-api
          uri: lb://basebackend-system-api
          predicates:
            - Path=/api/system/**
        
        - id: auth-api
          uri: lb://basebackend-auth-api
          predicates:
            - Path=/api/auth/**
```

### 6. 数据库迁移脚本
```sql
-- 如果需要为不同服务创建独立的数据库或schema
-- 可以考虑按服务拆分表：

-- user-api相关表
CREATE DATABASE IF NOT EXISTS basebackend_user;
-- sys_user, sys_role, sys_user_role, sys_profile等

-- system-api相关表  
CREATE DATABASE IF NOT EXISTS basebackend_system;
-- sys_dept, sys_menu, sys_dict, sys_dict_data, sys_application_resource等

-- auth-api相关表
CREATE DATABASE IF NOT EXISTS basebackend_auth;
-- sys_login_log, sys_token_blacklist等
```

## 后续步骤

### 步骤1：完成代码迁移（1-2天）
1. 使用 git mv 命令批量移动剩余的 Service/DTO/Mapper 文件
2. 修改包名和导入语句
3. 解决编译错误

### 步骤2：配置调整（0.5天）
1. 更新各服务的 application.yml
2. 配置 Nacos 中的服务发现和配置管理
3. 更新网关路由规则

### 步骤3：测试验证（1天）
1. 单元测试迁移和修复
2. 集成测试编写
3. 服务间调用测试

### 步骤4：部署配置（0.5天）
1. 更新 Docker Compose 配置
2. 更新 Kubernetes 部署文件
3. 更新 CI/CD 流程

## 风险点和注意事项

### 1. 服务间调用
- 原来的内部方法调用需要改为 Feign 调用
- 需要处理分布式事务问题（使用 Seata）

### 2. 权限验证
- JWT Token 的验证需要在各个服务中实现
- 考虑使用统一的认证中心

### 3. 数据一致性
- 跨服务的数据查询需要重新设计
- 考虑使用事件驱动架构处理数据同步

### 4. 性能影响
- 服务拆分后会增加网络调用开销
- 需要合理使用缓存减少跨服务调用

## 验证清单

- [ ] 所有新服务可以独立启动
- [ ] 服务注册到 Nacos 成功
- [ ] 网关路由配置正确
- [ ] API 文档（Swagger）可以访问
- [ ] 基础 CRUD 操作正常
- [ ] 服务间 Feign 调用正常
- [ ] JWT 认证在各服务中正常工作
- [ ] 分布式事务正常工作
- [ ] 监控指标正常采集

## 命令参考

```bash
# 编译新模块
mvn clean install -pl basebackend-user-api,basebackend-system-api,basebackend-auth-api -am

# 启动服务（开发环境）
java -jar basebackend-user-api/target/basebackend-user-api-1.0.0-SNAPSHOT.jar
java -jar basebackend-system-api/target/basebackend-system-api-1.0.0-SNAPSHOT.jar
java -jar basebackend-auth-api/target/basebackend-auth-api-1.0.0-SNAPSHOT.jar

# Docker构建
docker build -t basebackend/user-api:latest ./basebackend-user-api
docker build -t basebackend/system-api:latest ./basebackend-system-api
docker build -t basebackend/auth-api:latest ./basebackend-auth-api

# 查看服务注册状态
curl http://localhost:8848/nacos/v1/ns/instance/list?serviceName=basebackend-user-api
```

## 总结

阶段三的核心工作已经完成，成功将 admin-api 拆分为三个独立的微服务：
- **user-api**：负责用户、角色、权限管理
- **system-api**：负责系统配置、部门、菜单、字典管理
- **auth-api**：负责认证、授权、Token管理

剩余工作主要是补充完成 Service、DTO、Mapper 层的迁移，以及进行充分的测试验证。建议按照上述步骤继续推进，确保每个服务都能独立运行和部署。
