# Nacos配置中心配置文件

## 目录结构

```
config/nacos/
├── README.md                      # 本文件
├── application-common.yml         # 公共配置
├── application-datasource.yml     # 数据源配置
├── application-redis.yml          # Redis配置
├── user-api.yml                   # 用户服务配置
├── system-api.yml                 # 系统服务配置
├── auth-api.yml                   # 认证服务配置
├── notification-service.yml       # 通知服务配置
└── observability-service.yml      # 可观测性服务配置
```

## 配置上传

### 方式1：使用脚本上传（推荐）

```bash
# Linux/Mac
./bin/maintenance/upload-nacos-configs.sh

# Windows
bin\maintenance\upload-nacos-configs.bat
```

### 方式2：手动上传

1. 访问Nacos控制台：http://localhost:8848/nacos
2. 登录（默认用户名/密码：nacos/nacos）
3. 进入"配置管理" -> "配置列表"
4. 点击"+"创建配置
5. 填写配置信息：
   - Data ID：配置文件名（如 application-common.yml）
   - Group：DEFAULT_GROUP
   - 配置格式：YAML
   - 配置内容：复制对应文件内容

### 方式3：使用Nacos Open API

```bash
# 上传公共配置
curl -X POST "http://localhost:8848/nacos/v1/cs/configs" \
  -d "dataId=application-common.yml" \
  -d "group=DEFAULT_GROUP" \
  -d "content=$(cat config/nacos/application-common.yml)"
```

## 配置说明

### 公共配置（application-common.yml）
- Jackson序列化配置
- 文件上传配置
- Actuator监控配置
- Feign客户端配置
- 熔断限流配置
- 日志配置

### 数据源配置（application-datasource.yml）
- MySQL连接配置
- HikariCP连接池配置
- MyBatis Plus配置

### Redis配置（application-redis.yml）
- Redis连接配置
- Lettuce连接池配置
- 缓存配置

### 服务专属配置
每个服务有自己的配置文件，包含：
- 服务端口
- 服务名称
- 特定功能配置

## 配置优先级

配置加载优先级（从高到低）：
1. 本地配置文件（application.yml）
2. Nacos服务专属配置（如 user-api.yml）
3. Nacos公共配置（application-common.yml）

## 配置刷新

Nacos支持配置动态刷新，修改配置后无需重启服务。

在需要刷新的配置类上添加 `@RefreshScope` 注解：

```java
@RefreshScope
@ConfigurationProperties(prefix = "custom")
public class CustomProperties {
    // 配置属性
}
```

## 环境变量

配置文件中使用环境变量格式：`${ENV_VAR:default_value}`

示例：
```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:basebackend}
```

环境变量可以通过以下方式设置：
1. 系统环境变量
2. .env文件（需要配合工具如dotenv）
3. Docker环境变量
4. Kubernetes ConfigMap/Secret

## 安全建议

1. **不要在配置文件中存储明文密码**
   - 使用环境变量
   - 使用Nacos配置加密
   - 使用密钥管理服务（如Vault）

2. **限制Nacos访问权限**
   - 修改默认用户名密码
   - 配置访问控制
   - 启用鉴权

3. **配置备份**
   - 定期导出配置
   - 版本控制
   - 灾难恢复计划

## 故障排查

### 配置未生效
1. 检查Data ID和Group是否正确
2. 检查配置格式是否正确
3. 检查服务是否连接到Nacos
4. 查看服务日志

### 连接Nacos失败
1. 检查Nacos是否启动
2. 检查网络连接
3. 检查配置的Nacos地址
4. 检查防火墙设置

## 参考文档

- [Nacos官方文档](https://nacos.io/zh-cn/docs/what-is-nacos.html)
- [Spring Cloud Alibaba文档](https://spring-cloud-alibaba-group.github.io/github-pages/hoxton/zh-cn/index.html)
