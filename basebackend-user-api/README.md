# BaseBackend User API

用户服务 - 负责用户、角色、权限管理

## 服务信息

- **端口**: 8081
- **服务名**: basebackend-user-api
- **版本**: 1.0.0-SNAPSHOT

## 功能模块

### 用户管理
- 用户CRUD操作
- 用户查询和搜索
- 用户状态管理
- 用户密码管理

### 角色管理
- 角色CRUD操作
- 角色权限分配
- 角色用户关联

### 权限管理
- 权限CRUD操作
- 权限树结构
- 权限验证

### 用户配置
- 个人信息管理
- 用户偏好设置
- 通知设置

## 依赖服务

- MySQL 8.0+
- Redis 7.2+
- Nacos 2.2.3+

## 快速开始

### 本地开发

```bash
# 1. 启动依赖服务
cd docker/compose
docker-compose -f base/docker-compose.base.yml up -d

# 2. 启动Nacos
docker-compose -f middleware/docker-compose.middleware.yml up -d nacos

# 3. 启动服务
cd basebackend-user-api
mvn spring-boot:run
```

### Docker部署

```bash
# 构建镜像
docker build -t basebackend/user-api:latest -f basebackend-user-api/Dockerfile .

# 运行容器
docker run -d \
  --name basebackend-user-api \
  --network basebackend-network \
  -e NACOS_SERVER=nacos:8848 \
  -e NACOS_NAMESPACE=dev \
  -p 8081:8081 \
  basebackend/user-api:latest
```

## API文档

启动服务后访问: http://localhost:8081/doc.html

## 健康检查

```bash
curl http://localhost:8081/actuator/health
```

## 配置说明

### Nacos配置

需要在Nacos中配置以下文件：
- `basebackend-user-api.yml` - 服务专属配置
- `common-config.yml` - 公共配置
- `database-config.yml` - 数据库配置
- `cache-config.yml` - 缓存配置

### 环境变量

| 变量名 | 说明 | 默认值 |
|-------|------|--------|
| NACOS_SERVER | Nacos地址 | localhost:8848 |
| NACOS_NAMESPACE | Nacos命名空间 | dev |
| SPRING_PROFILES_ACTIVE | 激活的配置文件 | dev |

## 监控指标

访问 http://localhost:8081/actuator/prometheus 查看Prometheus指标

## 开发指南

### 添加新接口

1. 在 `controller` 包下创建Controller
2. 在 `service` 包下创建Service接口和实现
3. 在 `mapper` 包下创建Mapper接口
4. 在 `entity` 包下创建实体类
5. 添加Swagger注解

### 代码规范

- 遵循阿里巴巴Java开发手册
- 使用Lombok简化代码
- 统一异常处理
- 统一返回格式

## 故障排查

### 服务无法启动

1. 检查Nacos是否正常运行
2. 检查数据库连接配置
3. 检查端口是否被占用
4. 查看日志: `docker logs basebackend-user-api`

### 无法注册到Nacos

1. 检查网络连通性: `ping nacos`
2. 检查Nacos配置是否正确
3. 检查命名空间是否存在

## 联系方式

- 项目地址: https://github.com/basebackend/basebackend
- 问题反馈: https://github.com/basebackend/basebackend/issues
