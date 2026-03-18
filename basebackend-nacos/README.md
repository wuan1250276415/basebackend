# basebackend-nacos

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7+-green.svg)](https://spring.io/projects/spring-boot)
[![Nacos](https://img.shields.io/badge/Nacos-1.x%2F2.x-blue.svg)](https://nacos.io/)

basebackend-nacos 是基于 Spring Boot 的 Nacos 配置中心和服务发现模块，提供了完善的自动配置、动态刷新、多环境隔离和灰度发布等功能。

## 📋 目录

- [功能特性](#功能特性)
- [快速开始](#快速开始)
- [配置说明](#配置说明)
- [使用示例](#使用示例)
- [注解驱动](#注解驱动)
- [动态配置刷新](#动态配置刷新)
- [灰度发布](#灰度发布)
- [最佳实践](#最佳实践)
- [FAQ](#faq)

## ✨ 功能特性

### 核心功能
- 🎯 **自动配置** - 基于 Spring Boot Starter，无需复杂配置
- 🔄 **动态刷新** - 配置变更自动推送到客户端，无需重启
- 🌍 **多环境隔离** - 支持 dev/test/prod 环境配置隔离
- 👥 **多租户支持** - 支持租户级别的配置隔离
- 📊 **配置中心** - 统一管理所有服务的配置
- 🔍 **服务发现** - 基于 Nacos 的服务注册与发现

### 企业级特性
- 🎭 **灰度发布** - 支持 IP、百分比、标签三种灰度策略
- 🔐 **配置验证** - 启动时自动验证关键配置
- 📝 **审计日志** - 记录配置变更历史
- 🛡️ **安全加固** - 支持配置加密和环境变量
- 📈 **监控指标** - 提供配置变更和服务发现监控

### 技术特性
- ⚡ **高性能** - 异步刷新，批量监听器注册
- 🔧 **低耦合** - 分层架构，职责清晰
- 🔌 **易扩展** - 支持自定义监听器和处理器
- 📚 **完整文档** - 详细的配置说明和使用示例

## 🚀 快速开始

### 1. 添加依赖

在 `pom.xml` 中添加依赖：

```xml
<dependency>
    <groupId>com.basebackend</groupId>
    <artifactId>basebackend-nacos</artifactId>
    <version>${basebackend.version}</version>
</dependency>
```

### 2. 配置 application.yml

```yaml
nacos:
  enabled: true
  environment: dev
  tenant-id: public
  app-id: 10001

  config:
    enabled: true
    server-addr: 127.0.0.1:8848
    namespace: dev
    group: DEFAULT_GROUP
    refresh-enabled: true

    # 共享配置
    shared-configs:
      - data-id: common-config.yml
        refresh: true
      - data-id: mysql-config.yml
        refresh: true

  discovery:
    enabled: true
    server-addr: 127.0.0.1:8848
    namespace: dev
    group: DEFAULT_GROUP
    metadata:
      version: 1.0.0
      region: beijing
```

### 3. 使用自动配置

```java
@SpringBootApplication
@EnableNacosSupport
public class MyApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }

    @Autowired
    private ConfigService configService;

    @Autowired
    private NamingService namingService;
}
```

## 📖 配置说明

### 核心配置项

| 配置项 | 说明 | 默认值 | 示例 |
|--------|------|--------|------|
| `nacos.enabled` | 模块启用开关 | `true` | `true` |
| `nacos.environment` | 环境名称 | `dev` | `dev/test/prod` |
| `nacos.tenant-id` | 租户ID | `public` | `public/tenantA` |
| `nacos.app-id` | 应用ID | 无 | `10001` |

### 配置中心配置

| 配置项 | 说明 | 默认值 | 示例 |
|--------|------|--------|------|
| `nacos.config.enabled` | 是否启用配置中心 | `true` | `true` |
| `nacos.config.server-addr` | Nacos服务器地址 | `127.0.0.1:8848` | `203.0.113.10:8848` |
| `nacos.config.namespace` | 命名空间 | `public` | `dev/test/prod` |
| `nacos.config.group` | 分组 | `DEFAULT_GROUP` | `DEFAULT_GROUP` |
| `nacos.config.refresh-enabled` | 动态刷新开关 | `自动检测` | `true` |
| `nacos.config.shared-configs` | 共享配置列表 | `[]` | `见示例` |
| `nacos.config.extension-configs` | 扩展配置列表 | `[]` | `见示例` |

### 服务发现配置

| 配置项 | 说明 | 默认值 | 示例 |
|--------|------|--------|------|
| `nacos.discovery.enabled` | 是否启用服务发现 | `true` | `true` |
| `nacos.discovery.server-addr` | Nacos服务器地址 | `127.0.0.1:8848` | `203.0.113.10:8848` |
| `nacos.discovery.namespace` | 命名空间 | `public` | `dev/test/prod` |
| `nacos.discovery.group` | 分组 | `DEFAULT_GROUP` | `DEFAULT_GROUP` |
| `nacos.discovery.weight` | 实例权重 | `1.0` | `0.5` |
| `nacos.discovery.metadata` | 实例元数据 | `{}` | `{version: "1.0.0"}` |

## 💡 使用示例

### 方式一：自动配置（推荐）

最简单的方式，只需添加依赖和基本配置：

```yaml
# application.yml
nacos:
  config:
    server-addr: 127.0.0.1:8848
    shared-configs:
      - data-id: common-config.yml
        refresh: true
```

```java
// 自动注入使用
@Service
public class MyService {

    @Autowired
    private ConfigService configService;

    public void myMethod() {
        String config = configService.getConfig("my-config.yml", "DEFAULT_GROUP", 5000);
        System.out.println(config);
    }
}
```

### 方式二：注解驱动

使用 `@EnableNacosSupport` 注解启用：

```java
@Configuration
@EnableNacosSupport(config = true, discovery = true)
public class NacosConfig {

    @Autowired
    private ConfigService configService;

    @Autowired
    private NamingService namingService;

    @PostConstruct
    public void init() {
        // 注册配置监听器
        configService.addListener("my-config.yml", "DEFAULT_GROUP",
            new Listener() {
                @Override
                public void receiveConfigInfo(String configInfo) {
                    log.info("配置变更：{}", configInfo);
                    // 处理配置变更...
                }

                @Override
                public Executor getExecutor() {
                    return Executors.newFixedThreadPool(2);
                }
            }
        );
    }
}
```

说明：
- `@EnableNacosSupport(config = ..., discovery = ...)` 会在当前 Spring `Environment` 中覆盖
  `nacos.config.enabled` / `nacos.discovery.enabled`，仅对当前应用上下文生效。
- 不会写入 JVM 全局系统属性（不使用 `System.setProperty`）。

### 方式三：配置刷新

使用 `@NacosRefreshScope` 注解实现 Bean 自动刷新：

```java
@Component
@NacosRefreshScope
public class MyConfigBean {

    @Value("${my.config.key:default}")
    private String configKey;

    public void printConfig() {
        System.out.println("当前配置：" + configKey);
    }
}
```

## 🎭 灰度发布

灰度发布支持三种策略：IP、百分比、标签。

### IP灰度

```java
@Service
public class GrayReleaseDemo {

    @Autowired
    private GrayReleaseService grayReleaseService;

    public void startGrayReleaseByIp() {
        GrayReleaseConfig grayConfig = new GrayReleaseConfig();
        grayConfig.setDataId("my-config.yml");
        grayConfig.setStrategyType("ip");
        grayConfig.setTargetInstances("203.0.113.10,203.0.113.11");

        GrayReleaseResult result = grayReleaseService.startGrayRelease(configInfo, grayConfig);
        if (result.isSuccess()) {
            log.info("灰度发布启动成功：{}", result.getMessage());
        }
    }
}
```

### 百分比灰度

```java
public void startGrayReleaseByPercentage() {
    GrayReleaseConfig grayConfig = new GrayReleaseConfig();
    grayConfig.setDataId("my-config.yml");
    grayConfig.setStrategyType("percentage");
    grayConfig.setPercentage(20); // 20%实例灰度

    GrayReleaseResult result = grayReleaseService.startGrayRelease(configInfo, grayConfig);
}
```

### 标签灰度

```java
public void startGrayReleaseByLabel() {
    GrayReleaseConfig grayConfig = new GrayReleaseConfig();
    grayConfig.setDataId("my-config.yml");
    grayConfig.setStrategyType("label");
    grayConfig.setLabels("{\"version\":\"1.0\",\"region\":\"beijing\"}");

    GrayReleaseResult result = grayReleaseService.startGrayRelease(configInfo, grayConfig);
}
```

### 灰度全量发布

```java
public void promoteToFull() {
    GrayReleaseResult result = grayReleaseService.promoteToFull(configInfo, grayConfig);
    if (result.isSuccess()) {
        log.info("灰度全量发布成功");
    }
}
```

### 回滚灰度发布

```java
public void rollbackGrayRelease() {
    GrayReleaseResult result = grayReleaseService.rollbackGrayRelease(originalConfig, grayConfig);
    if (result.isSuccess()) {
        log.info("灰度回滚成功");
    }
}
```

## 🔄 动态配置刷新

### 配置变更监听

```java
@Component
public class CommonConfigListener implements SharedConfigListener {

    @Override
    public String getDataIdPattern() {
        return "common-config.yml";
    }

    @Override
    public String getGroup() {
        return "DEFAULT_GROUP";
    }

    @Override
    public void onChange(String dataId, String group, String content) {
        log.info("common-config.yml 配置变更");
        // 处理配置变更...
    }
}
```

### 事件驱动

```java
@Component
public class ConfigChangeHandler {

    @EventListener
    public void handleConfigChange(ConfigChangeEvent event) {
        log.info("配置变更事件：{}", event.getDataId());
        // 根据配置ID执行相应逻辑...
    }
}
```

## 🌍 多环境配置

### 环境隔离策略

通过 `nacos.environment` 配置实现环境隔离：

```yaml
# 开发环境
nacos:
  environment: dev
  config:
    namespace: dev

# 测试环境
nacos:
  environment: test
  config:
    namespace: test

# 生产环境
nacos:
  environment: prod
  config:
    namespace: prod
```

### 租户隔离

通过 `nacos.tenant-id` 配置实现租户隔离：

```yaml
nacos:
  tenant-id: tenantA
  config:
    group: tenantA_DEFAULT_GROUP
```

### 应用隔离

通过 `nacos.app-id` 配置实现应用隔离：

```yaml
nacos:
  app-id: 10001
  config:
    group: public_app_10001
```

## 📝 最佳实践

### 1. 配置管理

- 使用环境变量覆盖默认值：`${NACOS_SERVER:127.0.0.1:8848}`
- 为关键配置设置默认值和验证
- 敏感配置使用加密存储

### 2. 命名规范

- Data ID：`{serviceName}-{env}.yml`
- Group：`{tenantId}_{appId}`
- Namespace：`{env}`

### 3. 动态刷新

- 仅对需要刷新的配置启用 `refresh: true`
- 避免过多的监听器影响性能
- 在监听器中处理异常，防止影响其他监听器

### 4. 灰度发布

- 灰度百分比建议从 5% 开始，逐步扩大
- 观察灰度实例的运行指标
- 设置灰度超时时间，自动回滚

### 5. 监控告警

- 监控 Nacos 连接状态
- 告警配置刷新失败
- 追踪配置变更频率

## ❓ 常见问题

### Q1: 如何修改 Nacos 服务器地址？

A1: 在 `application.yml` 中配置：
```yaml
nacos:
  config:
    server-addr: 203.0.113.10:8848
  discovery:
    server-addr: 203.0.113.10:8848
```

### Q2: 配置不生效怎么办？

A2: 检查以下几点：
1. 确认 Nacos 服务器地址正确
2. 确认命名空间和分组配置正确
3. 检查 Data ID 是否存在
4. 查看日志是否有报错信息

### Q3: 如何禁用配置中心？

A3: 在 `application.yml` 中设置：
```yaml
nacos:
  config:
    enabled: false
```

### Q4: 灰度发布不生效？

A4: 检查以下几点：
1. 确认实例 IP 或标签配置正确
2. 确认灰度策略参数有效
3. 查看灰度发布日志
4. 确认 Nacos 实例元数据支持

### Q5: 如何实现配置加密？

A5: 使用 Jasypt 加密：
```yaml
jasypt:
  encryptor:
    password: ${JASYPT_ENCRYPTOR_PASSWORD}
    algorithm: PBEWITHHMACSHA512ANDAES_256

spring:
  datasource:
    password: ENC(密文)
```

## 📚 文档链接

- [Nacos 官方文档](https://nacos.io/)
- [Spring Cloud Alibaba](https://spring-cloud-alibaba-group.github.io/github-pages/2021/en-us/Spring%20Cloud%20Alibaba%20reference%20documentation.pdf)
- [Spring Boot 配置](https://docs.spring.io/spring-boot/docs/current/reference/html/)

## 📄 许可证

本项目采用 Apache 2.0 许可证。详情见 [LICENSE](https://www.apache.org/licenses/LICENSE-2.0) 文件。

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

---

**版本**: 1.0.0
**更新日期**: 2025-11-25
