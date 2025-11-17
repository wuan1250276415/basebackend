# Nacos配置与服务发现 - 完整实现指南

## 项目概述

本项目在BaseBackend现有的Nacos基础上，实现了企业级的配置中心与服务发现增强功能。

### 技术栈

- **Nacos**: 2.2.3 (服务注册与配置中心)
- **Spring Cloud Alibaba**: 2022.0.0.0
- **MySQL**: 8.0 (Nacos配置持久化)
- **Spring Boot**: 3.2.x
- **MyBatis-Plus**: 3.5.x

### 核心功能

1. **多维度配置隔离**
   - 环境隔离（dev/test/prod）
   - 租户隔离（支持多租户场景）
   - 应用隔离（基于现有应用管理体系）
   - 灰度隔离（灰度发布）

2. **配置生命周期管理**
   - 配置创建、编辑、删除
   - 配置版本管理
   - 配置历史记录
   - 配置回滚

3. **混合推送模式**
   - 关键配置：手动审核发布
   - 普通配置：自动推送
   - 支持强制发布

4. **灰度发布**
   - 按IP灰度：指定实例列表
   - 按百分比灰度：随机选择N%实例
   - 按标签灰度：根据实例metadata筛选
   - 灰度全量发布
   - 灰度回滚

5. **服务发现管理**
   - 服务列表查询
   - 服务实例管理
   - 实例上下线控制
   - 实例权重调整
   - 健康检查

## 架构设计

### 模块结构

```
basebackend
├── basebackend-nacos          # Nacos增强模块
│   ├── model                  # 数据模型
│   ├── enums                  # 枚举类型
│   ├── isolation              # 配置隔离
│   ├── service                # 核心服务
│   └── listener               # 配置监听
├── basebackend-admin-api      # 管理API
│   ├── entity/nacos           # 数据库实体
│   ├── mapper/nacos           # 数据访问层
│   ├── dto/nacos              # 数据传输对象
│   ├── service/nacos          # 业务服务层
│   └── controller/nacos       # 控制器层
└── docker/nacos               # Docker部署配置
```

### 数据库设计

#### 1. sys_nacos_config - 配置表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| data_id | VARCHAR(255) | 配置Data ID |
| group_name | VARCHAR(128) | 配置分组 |
| namespace | VARCHAR(128) | 命名空间 |
| content | LONGTEXT | 配置内容 |
| type | VARCHAR(32) | 配置类型 |
| environment | VARCHAR(32) | 环境 |
| tenant_id | VARCHAR(64) | 租户ID |
| app_id | BIGINT | 应用ID |
| version | INT | 版本号 |
| status | VARCHAR(32) | 状态 |
| is_critical | TINYINT | 是否关键配置 |
| publish_type | VARCHAR(32) | 发布类型 |
| md5 | VARCHAR(64) | MD5值 |

#### 2. sys_nacos_config_history - 配置历史表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| config_id | BIGINT | 配置ID |
| content | LONGTEXT | 配置内容 |
| version | INT | 版本号 |
| operation_type | VARCHAR(32) | 操作类型 |
| operator | BIGINT | 操作人 |
| rollback_from | INT | 回滚来源版本 |

#### 3. sys_nacos_gray_config - 灰度发布配置表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| config_id | BIGINT | 配置ID |
| strategy_type | VARCHAR(32) | 策略类型 |
| target_instances | TEXT | 目标实例 |
| percentage | INT | 灰度百分比 |
| labels | VARCHAR(500) | 实例标签 |
| status | VARCHAR(32) | 灰度状态 |
| gray_content | LONGTEXT | 灰度内容 |

#### 4. sys_nacos_service - 服务注册表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| service_name | VARCHAR(255) | 服务名 |
| group_name | VARCHAR(128) | 分组名 |
| namespace | VARCHAR(128) | 命名空间 |
| instance_count | INT | 实例总数 |
| healthy_count | INT | 健康实例数 |
| status | VARCHAR(32) | 服务状态 |

#### 5. sys_nacos_publish_task - 配置发布任务表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| config_id | BIGINT | 配置ID |
| publish_type | VARCHAR(32) | 发布类型 |
| status | VARCHAR(32) | 任务状态 |
| executor | BIGINT | 执行人 |
| result | TEXT | 执行结果 |

## 核心实现

### 1. 配置隔离实现

#### ConfigIsolationContext

```java
public class ConfigIsolationContext {
    private String environment;  // 环境
    private String tenantId;     // 租户ID
    private Long appId;          // 应用ID
    private String namespace;    // 命名空间
    private String group;        // 分组

    // 构建完整的Data ID
    public String buildDataId(String originalDataId) {
        // 格式：{environment}/{tenantId}/{appId}/{originalDataId}
        StringBuilder dataId = new StringBuilder();
        if (environment != null) dataId.append(environment).append("/");
        if (tenantId != null) dataId.append(tenantId).append("/");
        if (appId != null) dataId.append("app_").append(appId).append("/");
        dataId.append(originalDataId);
        return dataId.toString();
    }

    // 构建命名空间（环境隔离）
    public String buildNamespace() {
        return namespace != null ? namespace : environment;
    }

    // 构建分组（租户/应用隔离）
    public String buildGroup() {
        if (group != null) return group;
        if (tenantId != null && appId != null) {
            return tenantId + "_" + appId;
        }
        return "DEFAULT_GROUP";
    }
}
```

### 2. 混合推送模式实现

#### ConfigPublisher

```java
public PublishResult publishConfig(ConfigInfo configInfo, boolean force) {
    // 检查是否为关键配置
    if (!force && configInfo.getIsCritical()) {
        return PublishResult.pending("需要手动审核发布");
    }

    // 发布到Nacos
    boolean success = nacosConfigService.publishConfig(configInfo);

    return success ? PublishResult.success() : PublishResult.failed();
}
```

### 3. 灰度发布实现

#### 策略选择

```java
private List<String> selectTargetInstances(
    List<Instance> allInstances,
    GrayReleaseConfig grayConfig
) {
    switch (grayConfig.getStrategyType()) {
        case IP:
            return selectByIp(allInstances, grayConfig.getTargetInstances());
        case PERCENTAGE:
            return selectByPercentage(allInstances, grayConfig.getPercentage());
        case LABEL:
            return selectByLabel(allInstances, grayConfig.getLabels());
    }
}
```

#### 按百分比灰度

```java
private List<String> selectByPercentage(List<Instance> instances, Integer percentage) {
    int count = (int) Math.ceil(instances.size() * percentage / 100.0);
    Collections.shuffle(instances);  // 随机选择
    return instances.stream()
        .limit(count)
        .map(i -> i.getIp() + ":" + i.getPort())
        .collect(Collectors.toList());
}
```

#### 按标签灰度

```java
private List<String> selectByLabel(List<Instance> instances, String labelsJson) {
    Map<String, String> requiredLabels = parseLabels(labelsJson);
    return instances.stream()
        .filter(i -> matchesLabels(i.getMetadata(), requiredLabels))
        .map(i -> i.getIp() + ":" + i.getPort())
        .collect(Collectors.toList());
}
```

### 4. 配置版本管理

#### 自动记录历史

```java
private void recordHistory(SysNacosConfig config, String operationType, Integer rollbackFrom) {
    SysNacosConfigHistory history = new SysNacosConfigHistory();
    history.setConfigId(config.getId());
    history.setContent(config.getContent());
    history.setVersion(config.getVersion());
    history.setOperationType(operationType);
    history.setRollbackFrom(rollbackFrom);
    history.setMd5(config.getMd5());

    configHistoryMapper.insert(history);
}
```

#### 配置回滚

```java
public void rollbackConfig(ConfigRollbackDTO rollbackDTO) {
    SysNacosConfig config = nacosConfigMapper.selectById(rollbackDTO.getConfigId());
    SysNacosConfigHistory history = configHistoryMapper.selectById(rollbackDTO.getHistoryId());

    // 回滚配置内容
    config.setContent(history.getContent());
    config.setVersion(config.getVersion() + 1);
    config.setMd5(history.getMd5());
    nacosConfigMapper.updateById(config);

    // 发布到Nacos
    configPublisher.manualPublish(convertToConfigInfo(config));

    // 记录回滚历史
    recordHistory(config, "rollback", history.getVersion());
}
```

### 5. 服务发现管理

#### 实例上下线

```java
public boolean enableInstance(String serviceName, String groupName, String ip, int port) {
    // 获取所有实例
    List<Instance> instances = namingService.getAllInstances(serviceName, groupName);

    // 找到目标实例
    Instance targetInstance = instances.stream()
        .filter(i -> i.getIp().equals(ip) && i.getPort() == port)
        .findFirst()
        .orElse(null);

    if (targetInstance == null) return false;

    // 更新状态
    targetInstance.setEnabled(true);
    namingService.registerInstance(serviceName, groupName, targetInstance);

    return true;
}
```

## API接口说明

### 配置管理

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 查询配置 | POST | /api/nacos/config/page | 分页查询配置 |
| 获取详情 | GET | /api/nacos/config/{id} | 获取配置详情 |
| 创建配置 | POST | /api/nacos/config | 创建新配置 |
| 更新配置 | PUT | /api/nacos/config/{id} | 更新配置 |
| 删除配置 | DELETE | /api/nacos/config/{id} | 删除配置 |
| 发布配置 | POST | /api/nacos/config/publish | 发布配置 |
| 回滚配置 | POST | /api/nacos/config/rollback | 回滚配置 |

### 配置历史

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 查询历史 | POST | /api/nacos/config-history/page | 分页查询历史 |
| 历史列表 | GET | /api/nacos/config-history/list/{configId} | 获取配置的所有历史版本 |
| 历史详情 | GET | /api/nacos/config-history/{historyId} | 获取历史详情 |

### 灰度发布

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 创建灰度 | POST | /api/nacos/gray-release | 创建灰度发布 |
| 全量发布 | POST | /api/nacos/gray-release/promote/{grayId} | 灰度全量发布 |
| 灰度回滚 | POST | /api/nacos/gray-release/rollback/{grayId} | 灰度回滚 |

### 服务发现

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 服务列表 | GET | /api/nacos/service/list | 获取所有服务 |
| 服务实例 | GET | /api/nacos/service/{serviceName}/instances | 获取服务实例 |
| 健康实例 | GET | /api/nacos/service/{serviceName}/healthy-instances | 获取健康实例 |
| 上线实例 | POST | /api/nacos/service/instance/enable | 上线实例 |
| 下线实例 | POST | /api/nacos/service/instance/disable | 下线实例 |
| 更新权重 | POST | /api/nacos/service/instance/weight | 更新实例权重 |

## 部署指南

### 1. 启动Nacos集群

```bash
cd docker/nacos
./start.sh
```

### 2. 初始化数据库

```bash
mysql -u root -p < basebackend-admin-api/src/main/resources/db/migration/V1.4__create_nacos_config_tables.sql
```

### 3. 配置应用

在`application.yml`中启用nacos profile：

```yaml
spring:
  profiles:
    active: dev,nacos
```

### 4. 启动应用

```bash
mvn spring-boot:run
```

## 监控与运维

### 1. Nacos控制台

访问：http://localhost:8848/nacos

- 查看服务注册情况
- 查看配置列表
- 查看监听查询
- 查看集群节点状态

### 2. 应用监控端点

- `/actuator/nacos-config` - 查看当前配置
- `/actuator/nacos-discovery` - 查看服务发现信息
- `/actuator/health` - 健康检查

### 3. 日志监控

查看Nacos日志：

```bash
tail -f docker/nacos/nacos1/logs/nacos.log
```

查看应用日志：

```bash
tail -f logs/application.log | grep -i nacos
```

## 性能优化

### 1. Nacos集群优化

- 增加JVM内存：修改docker-compose.yml中的JVM_XMS和JVM_XMX
- 使用SSD存储：将MySQL数据目录挂载到SSD
- 调整MySQL配置：优化innodb_buffer_pool_size

### 2. 应用配置优化

```yaml
spring:
  cloud:
    nacos:
      discovery:
        # 心跳间隔（毫秒）
        heart-beat-interval: 5000
        # 心跳超时（毫秒）
        heart-beat-timeout: 15000
      config:
        # 配置长轮询超时（毫秒）
        long-poll-timeout: 46000
        # 配置重试次数
        max-retry: 10
```

### 3. 连接池优化

建议使用连接池复用Nacos连接，减少TCP连接开销。

## 故障排查

### 问题1：配置未生效

**可能原因**：
1. 配置未发布到Nacos
2. 应用未订阅该配置
3. namespace或group配置错误

**排查步骤**：
1. 在Nacos控制台确认配置存在
2. 查看监听查询确认应用已订阅
3. 检查应用日志确认配置刷新

### 问题2：服务注册失败

**可能原因**：
1. Nacos服务不可达
2. 网络问题
3. 认证失败

**排查步骤**：
1. 检查Nacos集群状态
2. 测试网络连通性
3. 确认用户名密码正确

### 问题3：灰度发布失败

**可能原因**：
1. 目标实例不存在
2. 灰度策略配置错误
3. 权限不足

**排查步骤**：
1. 确认目标实例存在且健康
2. 检查灰度策略配置
3. 查看应用和Nacos日志

## 最佳实践

### 1. 配置规范

- 使用有意义的Data ID命名
- 合理使用命名空间隔离环境
- 关键配置设置为手动发布
- 定期清理无用配置

### 2. 版本管理

- 重大变更前先备份
- 使用版本号标识配置
- 保留足够的历史版本
- 定期归档旧版本

### 3. 灰度发布

- 小范围验证后再全量
- 监控灰度期间的指标
- 准备回滚方案
- 记录灰度过程

### 4. 安全建议

- 修改默认密码
- 使用HTTPS通信
- 限制网络访问
- 定期审计配置变更

## 技术支持

- Nacos官方文档：https://nacos.io
- 项目Issue：请提交到项目仓库
- 快速开始指南：NACOS-CONFIG-QUICKSTART.md
- Docker部署指南：docker/nacos/README.md
