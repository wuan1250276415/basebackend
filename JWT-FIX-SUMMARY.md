# JWT Token互通问题修复总结

## 问题根源

经过分析，发现JWT Token无法互通的**根本原因**是：

### 1. JWT Secret配置不一致 ❌

**之前的配置：**
- **Gateway**: 没有配置 `jwt.secret`，使用默认值
  ```yaml
  # 缺少JWT配置
  ```

- **demo-api**: 配置了自定义secret
  ```yaml
  jwt:
    secret: basebackend-demo-secret-key-for-jwt-token-generation-minimum-256-bits-required
    expiration: 86400000
  ```

**问题**: 即使Gateway和demo-api使用同一个`JwtUtil`类（basebackend-jwt模块），但由于secret不同，生成和验证Token的签名不一致，导致无法互通。

## 解决方案

### 修复1: 统一JWT Secret配置

在Gateway的`application.yml`中添加JWT配置，**与demo-api完全一致**：

```yaml
# basebackend-gateway/src/main/resources/application.yml

# JWT配置 - 必须与demo-api保持一致
jwt:
  secret: basebackend-demo-secret-key-for-jwt-token-generation-minimum-256-bits-required
  expiration: 86400000  # 24小时
```

### 修复2: 增强日志

添加JWT模块的DEBUG日志，方便排查问题：

```yaml
logging:
  level:
    com.basebackend.jwt: DEBUG  # 新增
```

## 验证步骤

### 1. 确认配置一致性

```bash
# 检查Gateway的JWT配置
grep -A2 "^jwt:" basebackend-gateway/src/main/resources/application.yml

# 检查demo-api的JWT配置
grep -A2 "^jwt:" basebackend-demo-api/src/main/resources/application.yml

# 两者应该完全一致！
```

### 2. 重新编译

```bash
cd /home/wuan/IdeaProjects/basebackend
mvn clean package -pl basebackend-gateway,basebackend-demo-api -am -DskipTests
```

### 3. 启动服务

```bash
# Terminal 1: 启动Gateway
java -jar basebackend-gateway/target/basebackend-gateway-1.0.0-SNAPSHOT.jar

# Terminal 2: 启动demo-api
java -jar basebackend-demo-api/target/basebackend-demo-api-1.0.0-SNAPSHOT.jar
```

### 4. 运行诊断测试

```bash
cd /home/wuan/IdeaProjects/basebackend
./test-jwt-interop.sh
```

## 测试脚本说明

`test-jwt-interop.sh` 脚本会进行以下测试：

### 测试1: 直接访问demo-api登录
验证demo-api能否正常生成Token

### 测试2: 通过Gateway登录
验证Gateway白名单是否正常工作

### 测试3: Token互通性验证
- 3a: 使用demo-api生成的Token通过Gateway访问
- 3b: 使用Gateway获取的Token直接访问demo-api

### 测试4: 配置一致性检查
自动检查两个服务的JWT secret是否一致

### 测试5: Token内容解码
解码Token的payload，查看实际内容

## 预期结果

如果配置正确，应该看到：

```
====================================
诊断总结
====================================
🎉 成功: JWT Token完全互通！
```

## 常见问题排查

### 问题1: 仍然返回401 Unauthorized

**检查清单:**

1. **JWT Secret是否完全一致？**
   ```bash
   # 应该输出完全相同的值
   grep "secret:" basebackend-gateway/src/main/resources/application.yml
   grep "secret:" basebackend-demo-api/src/main/resources/application.yml
   ```

2. **是否重新编译了？**
   配置文件修改后必须重新编译才能生效

3. **Gateway日志是否有错误？**
   查看Gateway控制台输出，看是否有Token验证失败的日志

4. **demo-api日志是否有错误？**
   查看demo-api控制台输出

### 问题2: Gateway白名单不生效

**检查AuthenticationFilter配置:**

```java
// basebackend-gateway/src/main/java/com/basebackend/gateway/filter/AuthenticationFilter.java

private static final List<String> WHITELIST = Arrays.asList(
    "/basebackend-demo-api/api/auth/**",  // 必须匹配实际请求路径
    "/api/public/**",
    "/actuator/**"
);
```

**注意**: 路径必须包含 `/basebackend-demo-api` 前缀，因为demo-api的context-path是 `/basebackend-demo-api`

### 问题3: Nacos服务发现问题

确保两个服务都正确注册到Nacos：

```bash
# 访问Nacos控制台
http://localhost:8848/nacos

# 用户名: nacos
# 密码: nacos

# 检查服务列表，应该看到：
# - basebackend-gateway
# - basebackend-demo-api
```

## 架构回顾

现在的架构已经完全统一：

```
┌─────────────────────────────────────────┐
│         basebackend-jwt                 │
│  ┌───────────────────────────────────┐  │
│  │   JwtUtil (统一实现)               │  │
│  │   - secret: 从配置读取             │  │
│  │   - generateToken()               │  │
│  │   - validateToken()               │  │
│  └───────────────────────────────────┘  │
└──────────────┬────────────┬─────────────┘
               │            │
       ┌───────┴────┐  ┌───┴────────┐
       │  Gateway   │  │  demo-api  │
       │            │  │            │
       │ JWT配置:   │  │ JWT配置:   │
       │ secret: X  │  │ secret: X  │
       │ (相同✅)   │  │ (相同✅)   │
       └────────────┘  └────────────┘
```

## 配置文件对比

### Gateway配置 (`basebackend-gateway/src/main/resources/application.yml`)

```yaml
jwt:
  secret: basebackend-demo-secret-key-for-jwt-token-generation-minimum-256-bits-required
  expiration: 86400000
```

### demo-api配置 (`basebackend-demo-api/src/main/resources/application.yml`)

```yaml
jwt:
  secret: basebackend-demo-secret-key-for-jwt-token-generation-minimum-256-bits-required
  expiration: 86400000
```

✅ **完全一致！**

## 总结

修复JWT Token互通问题的关键是：

1. ✅ **创建统一的JWT模块** (`basebackend-jwt`) - 确保使用同一份代码
2. ✅ **统一JWT Secret配置** - 确保Gateway和demo-api的secret完全一致
3. ✅ **添加诊断工具** - 使用测试脚本快速定位问题

现在JWT Token应该可以在Gateway和demo-api之间完全互通了！
