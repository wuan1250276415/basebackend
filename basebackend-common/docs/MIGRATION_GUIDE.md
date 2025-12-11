# BaseBackend Common 模块迁移指南

## 📋 变更概述

从 `1.0.0-SNAPSHOT` 版本开始，`basebackend-common` 模块进行了重大架构重构：

- **之前**：单一 jar 模块（`basebackend-common`）
- **现在**：聚合模块（pom），包含 6 个子模块

## 🏗️ 新模块架构

```
basebackend-common (pom)
├── basebackend-common-core          # 核心模块（常量、枚举、异常、响应模型）
├── basebackend-common-dto           # 通用 DTO（分页查询等）
├── basebackend-common-util          # 工具类（JSON、Bean、日期等）
├── basebackend-common-context       # 上下文管理（用户、租户上下文）
├── basebackend-common-security      # 安全功能（脱敏、验证、密钥管理）
└── basebackend-common-starter       # Spring Boot Starter（推荐使用）
```

## 🔄 迁移方式

### 方式一：使用 Starter（推荐 ⭐）

**适用场景**：大多数应用服务

将原来的依赖：

```xml
<dependency>
    <groupId>com.basebackend</groupId>
    <artifactId>basebackend-common</artifactId>
</dependency>
```

替换为：

```xml
<dependency>
    <groupId>com.basebackend</groupId>
    <artifactId>basebackend-common-starter</artifactId>
</dependency>
```

**优点**：
- ✅ 自动引入所有通用模块
- ✅ 自动配置开箱即用
- ✅ 一次修改，获得所有能力

### 方式二：按需依赖

**适用场景**：纯工具库、不需要 Spring 的模块

根据实际需要引入子模块：

```xml
<!-- 仅需要核心模型和异常 -->
<dependency>
    <groupId>com.basebackend</groupId>
    <artifactId>basebackend-common-core</artifactId>
</dependency>

<!-- 需要工具类 -->
<dependency>
    <groupId>com.basebackend</groupId>
    <artifactId>basebackend-common-util</artifactId>
</dependency>

<!-- 需要安全功能 -->
<dependency>
    <groupId>com.basebackend</groupId>
    <artifactId>basebackend-common-security</artifactId>
</dependency>
```

## 📦 包名兼容性

**重要**：所有 Java 包名保持不变，无需修改 import 语句！

```java
// 以下 import 语句仍然有效，无需修改
import com.basebackend.common.model.Result;
import com.basebackend.common.exception.BusinessException;
import com.basebackend.common.constant.CommonConstants;
import com.basebackend.common.security.SecretManager;
// ... 等等
```

## 🎯 各模块迁移建议

| 原模块 | 推荐依赖 | 说明 |
|--------|---------|------|
| **basebackend-user-api** | `common-starter` | 应用服务，需要完整功能 |
| **basebackend-system-api** | `common-starter` | 应用服务，需要完整功能 |
| **basebackend-gateway** | `common-core` + `common-util` | 网关通常不需要安全和上下文 |
| **basebackend-web** | `common-starter` | Web 模块需要完整支持 |
| **basebackend-database** | `common-core` | 数据库模块只需核心模型 |
| **basebackend-cache** | `common-core` + `common-util` | 缓存模块需要工具类 |
| **basebackend-security** | `common-core` + `common-security` | 安全模块需要安全功能 |

## ⚙️ 自动配置说明

使用 `basebackend-common-starter` 后，以下功能会自动启用：

### 1. 密钥管理

```yaml
# application.yml
security:
  secret-manager:
    cache-ttl: 15m  # 密钥缓存时间（默认 15 分钟）
```

### 2. 输入验证

```java
// @SafeString 注解自动生效
public class UserDTO {
    @SafeString(maxLength = 50)
    private String username;
}
```

### 3. 上下文管理（后续 Phase 添加）

```java
// 用户上下文自动注入
UserContext context = ContextHolder.getUserContext();
```

## 🔍 依赖版本管理

在父 POM 中统一管理版本（推荐）：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.basebackend</groupId>
            <artifactId>basebackend-common-starter</artifactId>
            <version>${basebackend.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

然后在子模块中无需指定版本：

```xml
<dependency>
    <groupId>com.basebackend</groupId>
    <artifactId>basebackend-common-starter</artifactId>
    <!-- 版本由父 POM 管理 -->
</dependency>
```

## ✅ 迁移检查清单

在完成迁移后，请检查以下事项：

- [ ] 修改 POM 依赖（替换为 starter 或具体子模块）
- [ ] 编译通过（`mvn clean compile`）
- [ ] 单元测试通过（`mvn test`）
- [ ] 启动应用无异常
- [ ] 验证自动配置生效（如 SecretManager Bean 存在）
- [ ] 检查日志无警告（关于缺失配置或 Bean）

## 🐛 常见问题

### Q1: 编译报错 "找不到 basebackend-common"

**原因**：`basebackend-common` 现在是 pom 模块，不再提供 jar 包。

**解决**：替换为 `basebackend-common-starter` 或具体子模块。

### Q2: ClassNotFoundException

**原因**：缺少必要的子模块依赖。

**解决**：
- 如果使用 starter，检查是否正确引入
- 如果按需依赖，检查是否遗漏某个子模块

### Q3: 自动配置不生效

**原因**：未使用 starter 或 Spring Boot 版本过低。

**解决**：
- 确保使用 `basebackend-common-starter`
- 确保 Spring Boot 版本 ≥ 3.1.5

## 📚 更多信息

- **模块详细文档**：参见各子模块的 README.md
- **API 文档**：参见 Javadoc
- **示例代码**：参见 `example` 包

## 💬 技术支持

如有问题，请联系：
- 提交 Issue 到内部 Git 仓库
- 联系 BaseBackend Team

---

**最后更新**：2025-11-23
**适用版本**：1.0.0-SNAPSHOT 及以上
