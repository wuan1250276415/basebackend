# Nacos 服务发现集成指南

## 概述

本项目已集成 Nacos 3.1.0 作为服务发现和配置管理中心，替代了传统的 Eureka 方案。

## 架构优势

### Nacos vs Eureka
- **功能更丰富**: 支持服务发现、配置管理、服务健康检查
- **性能更好**: 基于 Raft 协议，支持 AP 和 CP 模式
- **运维更简单**: 提供 Web 控制台，支持动态配置
- **生态更完善**: 与 Spring Cloud Alibaba 深度集成

## 快速开始

### 1. 启动 Nacos 服务

#### 方式一：Docker Compose（推荐）
```bash
# 启动 Nacos 和 MySQL
docker-compose up -d

# 查看服务状态
docker-compose ps
```

#### 方式二：本地安装
```bash
# 下载 Nacos 3.1.0
wget https://github.com/alibaba/nacos/releases/download/3.1.0/nacos-server-3.1.0.tar.gz

# 解压并启动
tar -xzf nacos-server-3.1.0.tar.gz
cd nacos/bin
./startup.sh -m standalone
```

### 2. 启动微服务

```bash
# 启动所有服务
./start-services.sh

# 或分别启动
cd basebackend-demo-api && mvn spring-boot:run &
cd basebackend-gateway && mvn spring-boot:run &
```

### 3. 验证服务注册

访问 Nacos 控制台：http://localhost:8848/nacos
- 用户名/密码：nacos/nacos
- 查看服务列表，确认服务已注册

## 配置说明

### 服务发现配置

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
        namespace: public
        group: DEFAULT_GROUP
        metadata:
          version: 1.0.0
          region: beijing
```

### 配置管理

```yaml
spring:
  cloud:
    nacos:
      config:
        server-addr: localhost:8848
        namespace: public
        group: DEFAULT_GROUP
        file-extension: yml
```

## 服务架构

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Gateway       │    │   Demo-API      │    │   Nacos         │
│   Port: 8180    │◄──►│   Port: 8081    │◄──►│   Port: 8848    │
│                 │    │                 │    │                 │
│ - 路由转发      │    │ - 业务逻辑      │    │ - 服务发现      │
│ - 负载均衡      │    │ - 数据访问      │    │ - 配置管理      │
│ - 认证授权      │    │ - 缓存处理      │    │ - 健康检查      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 服务注册信息

### Gateway 服务
- **服务名**: basebackend-gateway
- **端口**: 8180
- **健康检查**: /actuator/health
- **元数据**: version=1.0.0, region=beijing

### Demo-API 服务
- **服务名**: basebackend-demo-api
- **端口**: 8081
- **健康检查**: /actuator/health
- **元数据**: version=1.0.0, region=beijing

## 路由配置

Gateway 通过 Nacos 服务发现自动路由到后端服务：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: basebackend-demo-api
          uri: lb://basebackend-demo-api  # 通过 Nacos 解析服务地址
          predicates:
            - Path=/api/**
```

## 配置管理

### 共享配置
- `common-config.yml`: 通用配置
- `gateway-config.yml`: Gateway 专用配置
- `demo-api-config.yml`: Demo-API 专用配置

### 动态配置更新
服务会自动监听 Nacos 配置变化并热更新，无需重启服务。

## 监控和运维

### 健康检查
- 服务自动向 Nacos 发送心跳
- Nacos 监控服务健康状态
- 不健康的服务会被自动摘除

### 服务治理
- 服务列表查看
- 服务详情监控
- 配置管理
- 服务权重调整

## 故障排查

### 常见问题

1. **服务注册失败**
   - 检查 Nacos 服务是否启动
   - 检查网络连接
   - 查看应用日志

2. **服务发现失败**
   - 确认服务已注册到 Nacos
   - 检查服务名是否正确
   - 查看 Gateway 日志

3. **配置不生效**
   - 检查 bootstrap.yml 配置
   - 确认配置已推送到 Nacos
   - 查看配置刷新日志

### 日志级别
```yaml
logging:
  level:
    com.alibaba.cloud.nacos: DEBUG
    org.springframework.cloud.gateway: INFO
```

## 扩展功能

### 1. 多环境配置
```yaml
spring:
  cloud:
    nacos:
      discovery:
        namespace: ${spring.profiles.active}
```

### 2. 服务分组
```yaml
spring:
  cloud:
    nacos:
      discovery:
        group: ${spring.profiles.active}
```

### 3. 配置加密
支持敏感配置加密存储和传输。

## 最佳实践

1. **服务命名规范**: 使用统一的命名规范
2. **配置管理**: 合理使用命名空间和分组
3. **监控告警**: 配置服务健康监控
4. **版本管理**: 使用元数据管理服务版本
5. **安全配置**: 生产环境启用认证和加密
