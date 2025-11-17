# BaseBackend Auth API

认证服务 - 负责登录、认证、授权、会话管理

## 服务信息

- **端口**: 8083
- **服务名**: basebackend-auth-api
- **版本**: 1.0.0-SNAPSHOT

## 功能模块

### 认证授权
- 用户登录
- 用户登出
- Token刷新
- 权限验证

### 会话管理
- 在线用户管理
- 会话超时控制
- 强制下线

### 安全功能
- 双因素认证 (2FA)
- 设备管理
- 登录日志
- 密码策略

## 依赖服务

- Redis 7.2+ (会话存储)
- Nacos 2.2.3+ (配置中心)
- User API (用户信息查询)

## 快速开始

### 本地开发

```bash
# 1. 启动依赖服务
cd docker/compose
docker-compose -f base/docker-compose.base.yml up -d redis

# 2. 启动Nacos
docker-compose -f middleware/docker-compose.middleware.yml up -d nacos

# 3. 启动User API (依赖服务)
cd basebackend-user-api
mvn spring-boot:run &

# 4. 启动Auth API
cd basebackend-auth-api
mvn spring-boot:run
```

### Docker部署

```bash
# 构建镜像
docker build -t basebackend/auth-api:latest -f basebackend-auth-api/Dockerfile .

# 运行容器
docker run -d \
  --name basebackend-auth-api \
  --network basebackend-network \
  -e NACOS_SERVER=nacos:8848 \
  -e NACOS_NAMESPACE=dev \
  -p 8083:8083 \
  basebackend/auth-api:latest
```

## API文档

启动服务后访问: http://localhost:8083/doc.html

## 健康检查

```bash
curl http://localhost:8083/actuator/health
```

## 配置说明

### Nacos配置

需要在Nacos中配置以下文件：
- `basebackend-auth-api.yml` - 服务专属配置
- `common-config.yml` - 公共配置
- `cache-config.yml` - 缓存配置
- `security-config.yml` - 安全配置

### 环境变量

| 变量名 | 说明 | 默认值 |
|-------|------|--------|
| NACOS_SERVER | Nacos地址 | localhost:8848 |
| NACOS_NAMESPACE | Nacos命名空间 | dev |
| SPRING_PROFILES_ACTIVE | 激活的配置文件 | dev |

### JWT配置

```yaml
jwt:
  secret: your-secret-key
  expiration: 86400  # 24小时
  refresh-expiration: 604800  # 7天
```

## 监控指标

访问 http://localhost:8083/actuator/prometheus 查看Prometheus指标

## 安全说明

### Token管理

- Access Token: 短期有效 (默认24小时)
- Refresh Token: 长期有效 (默认7天)
- Token存储在Redis中，支持主动失效

### 密码策略

- 最小长度: 8位
- 必须包含: 大小写字母、数字、特殊字符
- 密码加密: BCrypt
- 密码历史: 记录最近5次密码

### 双因素认证

支持以下2FA方式：
- TOTP (Time-based One-Time Password)
- SMS验证码
- 邮箱验证码

## 服务间调用

### Feign客户端

```java
@FeignClient(name = "basebackend-user-api")
public interface UserClient {
    @GetMapping("/api/users/{id}")
    UserDTO getUserById(@PathVariable Long id);
}
```

### 调用配置

```yaml
feign:
  client:
    config:
      default:
        connectTimeout: 5000
        readTimeout: 10000
  circuitbreaker:
    enabled: true
```

## 故障排查

### 无法调用User API

1. 检查User API是否启动
2. 检查Nacos服务发现是否正常
3. 检查Feign配置是否正确
4. 查看日志: `docker logs basebackend-auth-api`

### Token验证失败

1. 检查JWT密钥配置
2. 检查Token是否过期
3. 检查Redis连接是否正常

## 联系方式

- 项目地址: https://github.com/basebackend/basebackend
- 问题反馈: https://github.com/basebackend/basebackend/issues
