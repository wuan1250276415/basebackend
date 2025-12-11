# 数据加密功能使用指南

## 概述

数据加密功能提供了字段级别的透明加密，使用 `@Sensitive` 注解标记需要加密的字段，系统会自动在保存时加密、查询时解密。

## 功能特性

- **透明加密**：使用注解标记字段，无需修改业务代码
- **AES 加密**：采用 AES 算法进行加密，安全可靠
- **自动处理**：通过 MyBatis 拦截器自动加密和解密
- **可配置**：支持通过配置文件启用/禁用加密功能

## 配置

### 1. 启用加密功能

在 `application.yml` 中配置：

```yaml
database:
  enhanced:
    security:
      encryption:
        enabled: true
        algorithm: AES
        secret-key: ${ENCRYPTION_KEY:your-secret-key-here}
```

**重要提示**：
- 生产环境中，密钥应该从环境变量或密钥管理服务（如 Vault）获取
- 不要在配置文件中硬编码密钥
- 密钥一旦设置，不应随意更改，否则已加密的数据将无法解密

### 2. 环境变量配置

推荐使用环境变量设置密钥：

```bash
export ENCRYPTION_KEY=your-production-secret-key
```

## 使用方法

### 1. 标记敏感字段

在实体类中使用 `@Sensitive` 注解标记需要加密的字段：

```java
import com.basebackend.database.security.annotation.Sensitive;
import com.basebackend.database.security.annotation.SensitiveType;
import lombok.Data;

@Data
public class User {
    private Long id;
    private String username;
    
    // 加密手机号
    @Sensitive(type = SensitiveType.PHONE)
    private String phone;
    
    // 加密身份证号
    @Sensitive(type = SensitiveType.ID_CARD)
    private String idCard;
    
    // 加密银行卡号
    @Sensitive(type = SensitiveType.BANK_CARD)
    private String bankCard;
    
    // 加密邮箱
    @Sensitive(type = SensitiveType.EMAIL)
    private String email;
    
    // 自定义敏感字段
    @Sensitive
    private String secretData;
}
```

### 2. 正常使用 MyBatis Plus

加密和解密过程是透明的，业务代码无需修改：

```java
@Service
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    // 保存用户 - 敏感字段会自动加密
    public void saveUser(User user) {
        userMapper.insert(user);
        // 数据库中存储的是加密后的数据
    }
    
    // 查询用户 - 敏感字段会自动解密
    public User getUser(Long id) {
        User user = userMapper.selectById(id);
        // 返回的是解密后的数据
        return user;
    }
    
    // 更新用户 - 敏感字段会自动加密
    public void updateUser(User user) {
        userMapper.updateById(user);
    }
}
```

## 注解说明

### @Sensitive 注解属性

```java
@Sensitive(
    type = SensitiveType.CUSTOM,  // 敏感类型
    encrypt = true,                // 是否加密存储（默认 true）
    mask = true                    // 是否脱敏显示（默认 true，用于未来的脱敏功能）
)
```

### SensitiveType 枚举

- `CUSTOM`：自定义类型
- `PHONE`：手机号
- `ID_CARD`：身份证号
- `BANK_CARD`：银行卡号
- `EMAIL`：邮箱
- `PASSWORD`：密码
- `ADDRESS`：地址

## 工作原理

### 加密流程

1. 用户调用 `insert` 或 `update` 方法
2. `EncryptionInterceptor` 拦截 SQL 执行
3. 扫描参数对象，查找带有 `@Sensitive` 注解的字段
4. 使用 `EncryptionService` 加密字段值
5. 执行 SQL，将加密后的数据存入数据库

### 解密流程

1. 用户调用查询方法
2. MyBatis 执行查询，获取结果集
3. `DecryptionInterceptor` 拦截结果集处理
4. 扫描结果对象，查找带有 `@Sensitive` 注解的字段
5. 使用 `EncryptionService` 解密字段值
6. 返回解密后的数据给用户

### 加密标识

加密后的数据会添加 `ENC:` 前缀，用于标识该数据已加密：

```
原始数据：13800138000
加密后：ENC:aGVsbG8gd29ybGQ=...
```

系统会自动检测数据是否已加密，避免重复加密。

## 注意事项

### 1. 字段类型限制

- 只支持 `String` 类型的字段
- 其他类型的字段即使标记了 `@Sensitive` 注解也会被忽略

### 2. 数据库字段长度

加密后的数据长度会增加，建议：
- 原始数据长度 N
- 数据库字段长度至少为 `N * 2 + 50`

例如：
- 手机号（11位）：建议字段长度 100
- 身份证号（18位）：建议字段长度 100
- 银行卡号（16-19位）：建议字段长度 100

### 3. 性能考虑

- 加密解密会增加一定的性能开销
- 建议只对真正敏感的字段使用加密
- 对于高并发场景，可以考虑使用缓存减少数据库查询

### 4. 密钥管理

- **不要在代码中硬编码密钥**
- 使用环境变量或密钥管理服务
- 定期轮换密钥（需要重新加密所有数据）
- 做好密钥的备份和恢复

### 5. 数据迁移

如果需要启用加密功能，对于已有数据：

1. 备份数据库
2. 编写数据迁移脚本
3. 读取原始数据
4. 使用加密服务加密
5. 更新数据库

示例迁移代码：

```java
@Service
public class DataMigrationService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private EncryptionService encryptionService;
    
    public void migrateUserData() {
        List<User> users = userMapper.selectList(null);
        
        for (User user : users) {
            // 只加密未加密的数据
            if (user.getPhone() != null && !encryptionService.isEncrypted(user.getPhone())) {
                user.setPhone(encryptionService.encrypt(user.getPhone()));
            }
            if (user.getIdCard() != null && !encryptionService.isEncrypted(user.getIdCard())) {
                user.setIdCard(encryptionService.encrypt(user.getIdCard()));
            }
            
            userMapper.updateById(user);
        }
    }
}
```

## 故障排查

### 1. 加密未生效

检查配置：
```yaml
database:
  enhanced:
    security:
      encryption:
        enabled: true  # 确保为 true
```

### 2. 解密失败

可能原因：
- 密钥不正确
- 数据已损坏
- 数据格式不正确

查看日志：
```
ERROR Failed to decrypt data
```

### 3. 性能问题

- 检查是否对过多字段使用了加密
- 考虑使用缓存
- 优化查询，减少不必要的字段查询

## 最佳实践

1. **最小化加密范围**：只对真正敏感的数据加密
2. **合理设计字段长度**：预留足够的空间存储加密数据
3. **使用环境变量**：不要在配置文件中硬编码密钥
4. **定期审计**：检查哪些数据被加密，是否符合安全要求
5. **备份策略**：加密数据的备份和恢复需要特别注意
6. **监控告警**：监控加密解密的失败率，及时发现问题

## 示例项目

完整的示例代码请参考：`basebackend-database/src/test/java/com/basebackend/database/security/`

## 相关文档

- [数据库增强功能总览](DATABASE_ENHANCEMENT_README.md)
- [多租户使用指南](MULTI_TENANCY_USAGE.md)
- [审计日志使用指南](AUDIT_ARCHIVE_IMPLEMENTATION.md)
