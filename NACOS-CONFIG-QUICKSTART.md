# Nacos配置与服务发现 - 快速开始指南

## 概述

本指南将帮助您在5分钟内快速上手BaseBackend的Nacos配置与服务发现功能。

## 功能特性

✅ **多维度配置隔离**
- 环境隔离（dev/test/prod）
- 租户隔离（多租户）
- 应用隔离
- 灰度发布

✅ **配置管理**
- 配置CRUD操作
- 配置历史版本管理
- 一键回滚
- 混合推送模式（关键配置手动发布，普通配置自动推送）

✅ **服务发现**
- 服务实例管理
- 实例上下线控制
- 健康检查
- 权重调整

✅ **灰度发布**
- 按IP灰度
- 按百分比灰度
- 按标签灰度

## 快速开始

### 第一步：启动Nacos集群

```bash
cd docker/nacos
./start.sh
```

访问Nacos控制台：http://localhost:8848/nacos
- 用户名：`nacos`
- 密码：`nacos`

### 第二步：初始化数据库

执行SQL脚本：

```bash
mysql -u root -p your_database < basebackend-admin-api/src/main/resources/db/migration/V1.4__create_nacos_config_tables.sql
```

### 第三步：启动应用

```bash
cd basebackend-admin-api
mvn spring-boot:run
```

应用会自动连接到Nacos集群并注册服务。

### 第四步：使用配置管理API

#### 1. 创建配置

```bash
curl -X POST http://localhost:8082/api/nacos/config \
  -H "Content-Type: application/json" \
  -d '{
    "dataId": "application-dev.yml",
    "groupName": "DEFAULT_GROUP",
    "namespace": "public",
    "content": "server:\n  port: 8082",
    "type": "yaml",
    "environment": "dev",
    "isCritical": false,
    "description": "开发环境配置"
  }'
```

#### 2. 查询配置

```bash
curl -X POST http://localhost:8082/api/nacos/config/page \
  -H "Content-Type: application/json" \
  -d '{
    "pageNum": 1,
    "pageSize": 10,
    "environment": "dev"
  }'
```

#### 3. 发布配置

```bash
curl -X POST http://localhost:8082/api/nacos/config/publish \
  -H "Content-Type: application/json" \
  -d '{
    "configId": 1,
    "force": false
  }'
```

#### 4. 回滚配置

```bash
curl -X POST http://localhost:8082/api/nacos/config/rollback \
  -H "Content-Type: application/json" \
  -d '{
    "configId": 1,
    "historyId": 5
  }'
```

### 第五步：灰度发布示例

#### 1. 创建灰度发布（按百分比）

```bash
curl -X POST http://localhost:8082/api/nacos/gray-release \
  -H "Content-Type: application/json" \
  -d '{
    "configId": 1,
    "strategyType": "percentage",
    "percentage": 20,
    "grayContent": "server:\n  port: 8083"
  }'
```

#### 2. 灰度全量发布

```bash
curl -X POST http://localhost:8082/api/nacos/gray-release/promote/1
```

#### 3. 灰度回滚

```bash
curl -X POST http://localhost:8082/api/nacos/gray-release/rollback/1
```

### 第六步：服务发现管理

#### 1. 获取所有服务

```bash
curl http://localhost:8082/api/nacos/service/list?pageNo=1&pageSize=100
```

#### 2. 获取服务实例

```bash
curl http://localhost:8082/api/nacos/service/admin-api/instances
```

#### 3. 下线实例

```bash
curl -X POST "http://localhost:8082/api/nacos/service/instance/disable?serviceName=admin-api&groupName=DEFAULT_GROUP&ip=192.168.1.100&port=8082"
```

#### 4. 上线实例

```bash
curl -X POST "http://localhost:8082/api/nacos/service/instance/enable?serviceName=admin-api&groupName=DEFAULT_GROUP&ip=192.168.1.100&port=8082"
```

## 配置隔离示例

### 环境隔离

为不同环境创建独立的命名空间：

```bash
# 创建dev环境配置
curl -X POST http://localhost:8082/api/nacos/config \
  -H "Content-Type: application/json" \
  -d '{
    "dataId": "redis.yml",
    "namespace": "dev",
    "environment": "dev",
    "content": "spring:\n  redis:\n    host: dev-redis\n    port: 6379"
  }'

# 创建prod环境配置
curl -X POST http://localhost:8082/api/nacos/config \
  -H "Content-Type: application/json" \
  -d '{
    "dataId": "redis.yml",
    "namespace": "prod",
    "environment": "prod",
    "content": "spring:\n  redis:\n    host: prod-redis\n    port: 6379"
  }'
```

### 租户隔离

为不同租户创建独立配置：

```bash
curl -X POST http://localhost:8082/api/nacos/config \
  -H "Content-Type: application/json" \
  -d '{
    "dataId": "app-config.yml",
    "tenantId": "tenant_001",
    "content": "company:\n  name: 租户A公司"
  }'
```

### 应用隔离

为不同应用创建独立配置：

```bash
curl -X POST http://localhost:8082/api/nacos/config \
  -H "Content-Type: application/json" \
  -d '{
    "dataId": "app-config.yml",
    "appId": 1,
    "content": "app:\n  name: 应用1"
  }'
```

## 最佳实践

### 1. 配置命名规范

```
{environment}/{tenantId}/{appId}/{dataId}
```

例如：
- `dev/tenant_001/app_1/application.yml`
- `prod/public/common/redis.yml`

### 2. 关键配置与普通配置

**关键配置**（需手动审核发布）：
- 数据库连接信息
- 支付配置
- 第三方API密钥

**普通配置**（自动发布）：
- 功能开关
- 日志级别
- 缓存配置

设置`isCritical: true`来标记关键配置。

### 3. 灰度发布策略

**按IP灰度**：适用于指定服务器测试
```json
{
  "strategyType": "ip",
  "targetInstances": "192.168.1.100,192.168.1.101"
}
```

**按百分比灰度**：适用于逐步推广
```json
{
  "strategyType": "percentage",
  "percentage": 20
}
```

**按标签灰度**：适用于特定环境
```json
{
  "strategyType": "label",
  "labels": "{\"environment\":\"test\",\"version\":\"v2\"}"
}
```

### 4. 配置版本管理

- 每次配置修改都会自动创建历史版本
- 可随时回滚到任意历史版本
- 建议在重大变更前手动备份

## 常见问题

### Q1: 如何切换环境？

设置环境变量：
```bash
export NACOS_NAMESPACE=prod
export SPRING_PROFILES_ACTIVE=prod
```

### Q2: 如何确认配置已生效？

1. 查看应用日志确认配置刷新
2. 访问`/actuator/nacos-config`查看当前配置
3. 在Nacos控制台查看监听查询

### Q3: 灰度发布失败怎么办？

1. 检查灰度策略配置是否正确
2. 确认目标实例存在且健康
3. 查看应用日志和Nacos日志

### Q4: 如何批量导入配置？

可以使用Nacos Open API批量导入，或通过控制台的克隆功能。

## API参考

完整API文档请参考：`NACOS-CONFIG-IMPLEMENTATION.md`

## 下一步

- 阅读完整实现文档了解架构设计
- 查看Docker部署指南了解集群配置
- 探索前端管理界面（待实现）

## 技术支持

如有问题，请查看：
- [Nacos官方文档](https://nacos.io/zh-cn/docs/what-is-nacos.html)
- `docker/nacos/README.md` - Docker部署指南
- `NACOS-CONFIG-IMPLEMENTATION.md` - 完整实现文档
