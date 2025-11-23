# BaseBackend Common

企业级 Java 后端通用能力库，提供核心模型、工具类、上下文管理、安全功能等开箱即用的基础设施。

## 📦 模块架构

```
basebackend-common/
├── basebackend-common-core       # 核心模块：常量、枚举、异常、响应模型
├── basebackend-common-dto        # DTO 模块：分页查询、通用请求响应
├── basebackend-common-util       # 工具模块：JSON、Bean、日期、ID 生成
├── basebackend-common-context    # 上下文模块：用户/租户/请求上下文
├── basebackend-common-security   # 安全模块：脱敏、验证、密钥管理
└── basebackend-common-starter    # Starter：自动配置，一站式集成 ⭐
```

## 🚀 快速开始

### Maven 依赖（推荐）

```xml
<dependency>
    <groupId>com.basebackend</groupId>
    <artifactId>basebackend-common-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 核心功能

#### 1. 统一响应模型

```java
@RestController
public class UserController {

    @GetMapping("/user/{id}")
    public Result<User> getUser(@PathVariable Long id) {
        User user = userService.findById(id);
        return Result.success(user);
    }

    @GetMapping("/users")
    public PageResult<User> listUsers(PageQuery query) {
        Page<User> page = userService.list(query.toPage());
        return new PageResult<>(page);
    }
}
```

#### 2. 业务异常处理

```java
public void validateUser(User user) {
    if (user.getAge() < 18) {
        throw new BusinessException(400, "用户年龄不能小于 18 岁");
    }
}
```

#### 3. 输入验证

```java
public class UserDTO {
    @SafeString(maxLength = 50)
    private String username;

    @SafeString(required = true)
    private String email;
}
```

#### 4. 密钥管理

```java
@Service
public class DataService {

    @Autowired
    private SecretManager secretManager;

    public void connect() {
        String apiKey = secretManager.getSecret("api.key");
        // 自动支持环境变量、文件、缓存等多种来源
    }
}
```

## 📖 详细文档

- **[迁移指南](./MIGRATION_GUIDE.md)** - 从旧版本迁移到新架构
- **[子模块文档](./basebackend-common-core/README.md)** - 各子模块详细说明
- **[最佳实践](./BEST_PRACTICES.md)** - 使用建议和代码规范

## 🔧 开发构建

```bash
# 编译所有模块
mvn clean install

# 编译特定模块
mvn clean install -pl basebackend-common-core -am

# 跳过测试
mvn clean install -DskipTests
```

## 📋 版本要求

- **JDK**: 17+
- **Spring Boot**: 3.1.5+
- **Maven**: 3.6+

## 🎯 设计原则

本模块遵循以下原则：

1. **KISS（简单至上）** - 追求代码和设计的极致简洁
2. **YAGNI（精益求精）** - 仅实现当前明确所需的功能
3. **DRY（杜绝重复）** - 避免代码和逻辑的重复
4. **SOLID** - 遵循面向对象设计的五大原则

## 🤝 贡献

欢迎贡献代码和建议！请遵循以下步骤：

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 📄 许可证

本项目采用内部许可证，仅供 BaseBackend 团队内部使用。

## 📞 联系我们

- **团队**: BaseBackend Team
- **邮箱**: basebackend@example.com
- **文档**: https://docs.basebackend.internal

---

**最后更新**: 2025-11-23 | **版本**: 1.0.0-SNAPSHOT
