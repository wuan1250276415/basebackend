# BaseBackend System API

系统服务 - 负责字典、菜单、部门、日志管理

## 服务信息

- **端口**: 8082
- **服务名**: basebackend-system-api
- **版本**: 1.0.0-SNAPSHOT

## 功能模块

### 字典管理
- 字典类型管理
- 字典数据管理
- 字典缓存

### 菜单管理
- 菜单CRUD操作
- 菜单树结构
- 菜单权限关联

### 部门管理
- 部门CRUD操作
- 部门树结构
- 部门用户关联

### 日志管理
- 操作日志记录
- 登录日志记录
- 日志查询和统计

### 监控管理
- 服务器信息
- 缓存监控
- 在线用户统计

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
cd basebackend-system-api
mvn spring-boot:run
```

### Docker部署

```bash
# 构建镜像
docker build -t basebackend/system-api:latest -f basebackend-system-api/Dockerfile .

# 运行容器
docker run -d \
  --name basebackend-system-api \
  --network basebackend-network \
  -e NACOS_SERVER=nacos:8848 \
  -e NACOS_NAMESPACE=dev \
  -p 8082:8082 \
  basebackend/system-api:latest
```

## API文档

启动服务后访问: http://localhost:8082/doc.html

## 健康检查

```bash
curl http://localhost:8082/actuator/health
```

## 配置说明

### Nacos配置

需要在Nacos中配置以下文件：
- `basebackend-system-api.yml` - 服务专属配置
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

访问 http://localhost:8082/actuator/prometheus 查看Prometheus指标

## 联系方式

- 项目地址: https://github.com/basebackend/basebackend
- 问题反馈: https://github.com/basebackend/basebackend/issues
