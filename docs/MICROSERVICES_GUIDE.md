# 微服务架构指南

> 本文档介绍 Base Backend 项目拆分后的微服务架构

## 📋 目录

- [架构概览](#架构概览)
- [服务列表](#服务列表)
- [快速开始](#快速开始)
- [服务详情](#服务详情)
- [开发指南](#开发指南)
- [部署指南](#部署指南)
- [监控运维](#监控运维)
- [常见问题](#常见问题)

---

## 架构概览

### 服务拓扑

```
                    ┌─────────────┐
                    │   Gateway   │
                    │   :8080     │
                    └──────┬──────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
   ┌────▼─────┐      ┌────▼─────┐      ┌────▼─────┐
   │user-api  │      │system-api│      │auth-api  │
   │  :8081   │      │  :8082   │      │  :8083   │
   └────┬─────┘      └────┬─────┘      └────┬─────┘
        │                  │                  │
        └──────────────────┼──────────────────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
   ┌────▼─────┐      ┌────▼─────┐      ┌────▼─────┐
   │  MySQL   │      │  Redis   │      │  Nacos   │
   │  :3306   │      │  :6379   │      │  :8848   │
   └──────────┘      └──────────┘      └──────────┘
```

### 分层架构

```
┌─────────────────────────────────────────────────┐
│              API Gateway Layer                   │
│                  (Gateway)                       │
└─────────────────────────────────────────────────┘
                       ↓
┌─────────────────────────────────────────────────┐
│            Business Service Layer                │
│   user-api  │  system-api  │  auth-api          │
└─────────────────────────────────────────────────┘
                       ↓
┌─────────────────────────────────────────────────┐
│           Infrastructure Layer                   │
│   security  │  cache  │  database  │  web       │
└─────────────────────────────────────────────────┘
                       ↓
┌─────────────────────────────────────────────────┐
│              Foundation Layer                    │
│                  (common)                        │
└─────────────────────────────────────────────────┘
```

---

## 服务列表

| 服务名 | 端口 | 职责 | 依赖 | 状态 |
|-------|------|------|------|------|
| **user-api** | 8081 | 用户、角色、权限管理 | MySQL, Redis, Nacos | ✅ 已创建 |
| **system-api** | 8082 | 字典、菜单、部门、日志 | MySQL, Redis, Nacos | ✅ 已创建 |
| **auth-api** | 8083 | 认证、授权、会话管理 | Redis, Nacos, user-api | ✅ 已创建 |
| **gateway** | 8080 | API网关、路由、限流 | Nacos | ✅ 已存在 |

### 服务依赖关系

```yaml
user-api:
  depends_on:
    - common
    - database
    - cache
    - security
    - web

system-api:
  depends_on:
    - common
    - database
    - cache
    - web
    - logging

auth-api:
  depends_on:
    - common
    - cache
    - security
    - jwt
    - web
  calls:
    - user-api  # Feign调用
```

---

## 快速开始

### 前置条件

- JDK 17+
- Maven 3.8+
- Docker & Docker Compose
- 至少 8GB 可用内存

### 一键启动

```bash
# 1. 启动基础设施和中间件
cd docker/compose
./start-all.sh

# 2. 启动微服务
cd ../..
bash bin/start/start-microservices.sh start

# 3. 验证服务状态
bash bin/start/start-microservices.sh status
```

### 分步启动

#### Step 1: 启动基础设施

```bash
cd docker/compose
docker-compose -f base/docker-compose.base.yml up -d
```

等待 MySQL 和 Redis 启动完成（约 30 秒）

#### Step 2: 启动中间件

```bash
docker-compose -f middleware/docker-compose.middleware.yml up -d nacos
```

等待 Nacos 启动完成（约 60 秒）

#### Step 3: 启动微服务

```bash
# 启动 user-api
cd basebackend-user-api
mvn spring-boot:run &

# 启动 system-api
cd ../basebackend-system-api
mvn spring-boot:run &

# 启动 auth-api
cd ../basebackend-auth-api
mvn spring-boot:run &
```

#### Step 4: 验证部署

```bash
# 检查服务健康状态
curl http://localhost:8081/actuator/health  # user-api
curl http://localhost:8082/actuator/health  # system-api
curl http://localhost:8083/actuator/health  # auth-api

# 访问 API 文档
open http://localhost:8081/doc.html  # user-api
open http://localhost:8082/doc.html  # system-api
open http://localhost:8083/doc.html  # auth-api
```

---

## 服务详情

### User API (用户服务)

**端口**: 8081  
**文档**: [basebackend-user-api/README.md](../basebackend-user-api/README.md)

**核心功能**:
- 用户管理 (CRUD)
- 角色管理
- 权限管理
- 用户配置

**API 端点**:
```
GET    /api/users          # 用户列表
POST   /api/users          # 创建用户
GET    /api/users/{id}     # 用户详情
PUT    /api/users/{id}     # 更新用户
DELETE /api/users/{id}     # 删除用户

GET    /api/roles          # 角色列表
POST   /api/roles          # 创建角色
GET    /api/permissions    # 权限列表
```

**数据库表**:
- sys_user
- sys_role
- sys_permission
- sys_user_role
- sys_role_permission

---

### System API (系统服务)

**端口**: 8082  
**文档**: [basebackend-system-api/README.md](../basebackend-system-api/README.md)

**核心功能**:
- 字典管理
- 菜单管理
- 部门管理
- 日志管理
- 监控管理

**API 端点**:
```
GET    /api/dicts          # 字典列表
POST   /api/dicts          # 创建字典
GET    /api/menus          # 菜单列表
GET    /api/depts          # 部门列表
GET    /api/logs           # 日志列表
GET    /api/monitor/server # 服务器信息
```

**数据库表**:
- sys_dict
- sys_dict_data
- sys_menu
- sys_dept
- sys_operation_log
- sys_login_log

---

### Auth API (认证服务)

**端口**: 8083  
**文档**: [basebackend-auth-api/README.md](../basebackend-auth-api/README.md)

**核心功能**:
- 用户登录/登出
- Token 管理
- 会话管理
- 双因素认证 (2FA)
- 设备管理

**API 端点**:
```
POST   /api/auth/login     # 用户登录
POST   /api/auth/logout    # 用户登出
POST   /api/auth/refresh   # 刷新Token
GET    /api/auth/info      # 当前用户信息
GET    /api/sessions       # 在线用户列表
DELETE /api/sessions/{id}  # 强制下线
```

**依赖服务**:
- Redis (会话存储)
- User API (用户信息查询)

---

## 开发指南

### 本地开发环境

#### 1. IDE 配置

**IntelliJ IDEA**:
1. 导入项目: File -> Open -> 选择项目根目录
2. 等待 Maven 依赖下载完成
3. 配置 JDK 17: File -> Project Structure -> Project SDK

#### 2. 启动单个服务

```bash
# 方式一: Maven
cd basebackend-user-api
mvn spring-boot:run

# 方式二: IDE
# 右键 UserApiApplication.java -> Run
```

#### 3. 调试配置

在 IDE 中创建 Spring Boot 运行配置:
- Main class: `com.basebackend.user.UserApiApplication`
- VM options: `-Dspring.profiles.active=dev`
- Environment variables: `NACOS_SERVER=localhost:8848`

### 添加新接口

#### Step 1: 创建 Controller

```java
@RestController
@RequestMapping("/api/users")
@Tag(name = "用户管理")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping
    @Operation(summary = "获取用户列表")
    public Result<List<UserDTO>> list() {
        return Result.success(userService.list());
    }
}
```

#### Step 2: 创建 Service

```java
@Service
public class UserServiceImpl implements UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Override
    public List<UserDTO> list() {
        return userMapper.selectList(null)
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
}
```

#### Step 3: 创建 Mapper

```java
@Mapper
public interface UserMapper extends BaseMapper<SysUser> {
    // MyBatis-Plus 自动提供 CRUD 方法
}
```

### 服务间调用

#### 配置 Feign 客户端

```java
@FeignClient(name = "basebackend-user-api")
public interface UserClient {
    
    @GetMapping("/api/users/{id}")
    Result<UserDTO> getUserById(@PathVariable Long id);
}
```

#### 使用 Feign 客户端

```java
@Service
public class AuthServiceImpl implements AuthService {
    
    @Autowired
    private UserClient userClient;
    
    public UserDTO getUserInfo(Long userId) {
        Result<UserDTO> result = userClient.getUserById(userId);
        return result.getData();
    }
}
```

### 配置管理

#### Nacos 配置

在 Nacos 中创建配置文件:

**basebackend-user-api.yml**:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/basebackend
    username: root
    password: root123456

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

**common-config.yml** (共享配置):
```yaml
spring:
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: GMT+8

logging:
  level:
    com.basebackend: debug
```

---

## 部署指南

### Docker 部署

#### 构建镜像

```bash
# 构建所有服务
docker-compose -f docker/compose/services/docker-compose.services.yml build

# 构建单个服务
docker build -t basebackend/user-api:latest -f basebackend-user-api/Dockerfile .
```

#### 启动服务

```bash
# 启动所有服务
cd docker/compose
docker-compose -f base/docker-compose.base.yml up -d
docker-compose -f middleware/docker-compose.middleware.yml up -d
docker-compose -f services/docker-compose.services.yml up -d

# 查看日志
docker-compose -f services/docker-compose.services.yml logs -f user-api
```

### Kubernetes 部署

#### 创建 Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-api
  namespace: basebackend
spec:
  replicas: 2
  selector:
    matchLabels:
      app: user-api
  template:
    metadata:
      labels:
        app: user-api
    spec:
      containers:
      - name: user-api
        image: basebackend/user-api:latest
        ports:
        - containerPort: 8081
        env:
        - name: NACOS_SERVER
          value: "nacos:8848"
        - name: NACOS_NAMESPACE
          value: "prod"
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8081
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8081
          initialDelaySeconds: 30
          periodSeconds: 5
```

#### 创建 Service

```yaml
apiVersion: v1
kind: Service
metadata:
  name: user-api
  namespace: basebackend
spec:
  selector:
    app: user-api
  ports:
  - port: 8081
    targetPort: 8081
  type: ClusterIP
```

#### 部署

```bash
kubectl apply -f k8s/user-api-deployment.yaml
kubectl apply -f k8s/user-api-service.yaml
```

---

## 监控运维

### 健康检查

```bash
# 检查所有服务
bash bin/start/start-microservices.sh status

# 检查单个服务
curl http://localhost:8081/actuator/health
```

### 查看日志

```bash
# Docker 日志
docker logs -f basebackend-user-api

# 本地日志
tail -f basebackend-user-api/logs/user-api.log

# Kubernetes 日志
kubectl logs -f deployment/user-api -n basebackend
```

### Prometheus 指标

访问 `/actuator/prometheus` 端点查看指标:

```bash
curl http://localhost:8081/actuator/prometheus
```

**关键指标**:
- `http_server_requests_seconds` - HTTP 请求耗时
- `jvm_memory_used_bytes` - JVM 内存使用
- `system_cpu_usage` - CPU 使用率
- `jdbc_connections_active` - 数据库连接数

### 性能调优

#### JVM 参数

```bash
java -jar \
  -Xms512m \
  -Xmx1g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/logs/heapdump.hprof \
  app.jar
```

#### 数据库连接池

```yaml
spring:
  datasource:
    druid:
      initial-size: 5
      min-idle: 5
      max-active: 20
      max-wait: 60000
      test-while-idle: true
      validation-query: SELECT 1
```

---

## 常见问题

### Q1: 服务无法启动

**症状**: 服务启动失败或启动后立即退出

**排查步骤**:
1. 检查依赖服务是否启动
   ```bash
   docker ps | grep -E "mysql|redis|nacos"
   ```

2. 检查端口是否被占用
   ```bash
   netstat -ano | findstr :8081
   ```

3. 查看日志
   ```bash
   tail -f basebackend-user-api/logs/user-api.log
   ```

**常见原因**:
- Nacos 未启动或连接失败
- 数据库连接配置错误
- 端口被占用
- JDK 版本不匹配

---

### Q2: 服务间调用失败

**症状**: Feign 调用返回 404 或超时

**排查步骤**:
1. 检查服务是否注册到 Nacos
   ```bash
   curl http://localhost:8848/nacos/v1/ns/instance/list?serviceName=basebackend-user-api
   ```

2. 检查网络连通性
   ```bash
   curl http://localhost:8081/actuator/health
   ```

3. 检查 Feign 配置
   ```yaml
   feign:
     client:
       config:
         default:
           connectTimeout: 5000
           readTimeout: 10000
   ```

**常见原因**:
- 服务未注册到 Nacos
- Feign 超时配置过短
- 网络不通
- 接口路径错误

---

### Q3: 内存占用过高

**症状**: 服务运行一段时间后内存持续增长

**排查步骤**:
1. 生成堆转储
   ```bash
   jmap -dump:format=b,file=heapdump.hprof <pid>
   ```

2. 使用 MAT 分析堆转储

3. 检查是否有内存泄漏

**常见原因**:
- 缓存未设置过期时间
- 数据库连接未关闭
- 线程池未正确配置
- 大对象未及时释放

---

### Q4: 数据库连接池耗尽

**症状**: 出现 "Could not get JDBC Connection" 错误

**解决方案**:
1. 增加连接池大小
   ```yaml
   spring:
     datasource:
       druid:
         max-active: 50
   ```

2. 检查是否有慢查询
   ```sql
   SHOW PROCESSLIST;
   ```

3. 优化查询性能
   - 添加索引
   - 优化 SQL
   - 使用缓存

---

## 参考资料

### 官方文档
- [Spring Boot 文档](https://spring.io/projects/spring-boot)
- [Spring Cloud 文档](https://spring.io/projects/spring-cloud)
- [Nacos 文档](https://nacos.io/zh-cn/docs/what-is-nacos.html)
- [MyBatis-Plus 文档](https://baomidou.com/)

### 项目文档
- [部署指南](deployment/README.md)
- [开发指南](development/getting-started.md)
- [重构总结](REFACTORING_SUMMARY.md)
- [阶段三完成报告](REFACTORING_PHASE3_COMPLETE.md)

### 联系方式
- 项目地址: https://github.com/basebackend/basebackend
- 问题反馈: https://github.com/basebackend/basebackend/issues
- 技术支持: support@basebackend.com

---

**文档版本**: v1.0  
**最后更新**: 2025-11-17  
**维护团队**: Architecture Team
