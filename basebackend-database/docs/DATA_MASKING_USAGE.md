# 数据脱敏使用指南

## 概述

数据脱敏模块提供了对敏感数据的自动脱敏功能，包括：
- 手机号脱敏
- 身份证号脱敏
- 银行卡号脱敏
- 邮箱脱敏
- 地址脱敏
- 自定义规则脱敏
- 日志自动脱敏

## 配置

### 1. 启用数据脱敏

在 `application.yml` 中配置：

```yaml
database:
  enhanced:
    security:
      masking:
        enabled: true
        # 自定义脱敏规则
        rules:
          phone: "###****####"        # 手机号：保留前3位和后4位
          id-card: "######********####"  # 身份证：保留前6位和后4位
          bank-card: "#### **** **** ####"  # 银行卡：保留前4位和后4位
          custom: "###***"             # 自定义规则
```

### 2. 配置日志脱敏

在 `logback-spring.xml` 中配置：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    
    <!-- 注册日志脱敏转换器 -->
    <conversionRule conversionWord="maskedMsg" 
                    converterClass="com.basebackend.database.security.masking.LogMaskingConverter" />
    
    <!-- 控制台输出 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <!-- 使用 maskedMsg 代替 msg -->
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %maskedMsg%n</pattern>
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE" />
    </root>
    
</configuration>
```

## 使用方式

### 1. 使用 DataMaskingService

```java
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final DataMaskingService maskingService;
    
    public UserDTO getUserInfo(Long userId) {
        User user = userRepository.findById(userId);
        
        UserDTO dto = new UserDTO();
        dto.setName(user.getName());
        // 脱敏手机号
        dto.setPhone(maskingService.maskPhone(user.getPhone()));
        // 脱敏身份证号
        dto.setIdCard(maskingService.maskIdCard(user.getIdCard()));
        // 脱敏银行卡号
        dto.setBankCard(maskingService.maskBankCard(user.getBankCard()));
        
        return dto;
    }
}
```

### 2. 使用 MaskingUtil 工具类

```java
public class UserController {
    
    @GetMapping("/user/{id}")
    public Result<UserDTO> getUser(@PathVariable Long id) {
        User user = userService.getById(id);
        
        UserDTO dto = new UserDTO();
        dto.setName(user.getName());
        // 使用工具类脱敏
        dto.setPhone(MaskingUtil.maskPhone(user.getPhone()));
        dto.setIdCard(MaskingUtil.maskIdCard(user.getIdCard()));
        dto.setBankCard(MaskingUtil.maskBankCard(user.getBankCard()));
        
        return Result.success(dto);
    }
}
```

### 3. 根据敏感类型自动脱敏

```java
@Service
@RequiredArgsConstructor
public class DataService {
    
    private final DataMaskingService maskingService;
    
    public String maskSensitiveData(String data, SensitiveType type) {
        // 根据类型自动选择脱敏方法
        return maskingService.mask(data, type);
    }
    
    public void example() {
        String phone = "13812345678";
        String masked = maskingService.mask(phone, SensitiveType.PHONE);
        // 输出: 138****5678
        
        String idCard = "110101199001011234";
        masked = maskingService.mask(idCard, SensitiveType.ID_CARD);
        // 输出: 110101********1234
    }
}
```

### 4. 使用自定义规则脱敏

```java
@Service
@RequiredArgsConstructor
public class CustomMaskingService {
    
    private final DataMaskingService maskingService;
    
    public String maskWithCustomRule(String data) {
        // 使用自定义规则
        // # 表示保留原字符
        // * 表示用*替换
        // 其他字符直接使用（如分隔符）
        
        String rule = "###-****-####";
        return maskingService.maskWithRule(data, rule);
        // 输入: 12345678901
        // 输出: 123-****-901
    }
}
```

### 5. 日志自动脱敏

配置日志脱敏后，所有日志输出会自动脱敏：

```java
@Slf4j
@Service
public class UserService {
    
    public void processUser(User user) {
        // 日志中的敏感信息会自动脱敏
        log.info("Processing user: phone={}, idCard={}", 
                 user.getPhone(), user.getIdCard());
        // 实际输出: Processing user: phone=138****5678, idCard=110101********1234
        
        log.info("User email: {}", user.getEmail());
        // 实际输出: User email: u***@example.com
    }
}
```

## 脱敏规则说明

### 内置脱敏规则

1. **手机号脱敏**
   - 格式：`138****5678`
   - 保留前3位和后4位，中间用*代替

2. **身份证号脱敏**
   - 格式：`110101********1234`
   - 保留前6位和后4位，中间用*代替

3. **银行卡号脱敏**
   - 格式：`6222 **** **** 1234`
   - 保留前4位和后4位，中间用*代替，每4位加空格

4. **邮箱脱敏**
   - 格式：`u***@example.com`
   - 保留前缀第一个字符和@后的域名

5. **地址脱敏**
   - 格式：`北京市**********`
   - 保留前3个字符（通常是省份）

6. **密码脱敏**
   - 格式：`******`
   - 完全隐藏

### 自定义规则格式

自定义规则使用以下符号：
- `#` - 保留原字符
- `*` - 用*替换
- 其他字符 - 直接使用（如分隔符 `-`、空格等）

示例：
```
"###****####"        -> 保留前3位和后4位
"###-****-####"      -> 保留前3位和后4位，添加分隔符
"#### **** **** ####" -> 银行卡格式
```

## 日志脱敏支持的模式

日志脱敏会自动识别并脱敏以下模式：

1. **手机号**: `1[3-9]\d{9}`
2. **身份证号**: `\d{17}[\dXx]`
3. **银行卡号**: `\d{16,19}`
4. **邮箱**: `[\w.-]+@[\w.-]+\.\w+`
5. **密码**: `password=xxx`, `pwd:xxx`, `passwd=xxx`

## 注意事项

1. **性能考虑**
   - 脱敏操作会增加一定的性能开销
   - 建议只在需要展示给用户的场景使用
   - 内部系统间调用可以不脱敏

2. **数据验证**
   - 脱敏前会验证数据格式
   - 格式不正确的数据会返回原值并记录警告日志

3. **配置开关**
   - 可以通过配置关闭脱敏功能
   - 关闭后所有脱敏方法会直接返回原值

4. **日志脱敏**
   - 日志脱敏使用正则匹配，可能会误匹配
   - 建议在生产环境启用，开发环境可以关闭

5. **加密 vs 脱敏**
   - 加密：保护数据存储安全，可以解密还原
   - 脱敏：保护数据展示安全，不可还原
   - 两者可以同时使用

## 完整示例

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class UserManagementService {
    
    private final DataMaskingService maskingService;
    private final UserRepository userRepository;
    
    /**
     * 获取用户信息（脱敏）
     */
    public UserDTO getUserInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        
        // 脱敏敏感信息
        dto.setPhone(maskingService.maskPhone(user.getPhone()));
        dto.setIdCard(maskingService.maskIdCard(user.getIdCard()));
        dto.setBankCard(maskingService.maskBankCard(user.getBankCard()));
        dto.setEmail(maskingService.maskEmail(user.getEmail()));
        dto.setAddress(maskingService.maskAddress(user.getAddress()));
        
        // 日志会自动脱敏
        log.info("Retrieved user info: {}", dto);
        
        return dto;
    }
    
    /**
     * 批量脱敏
     */
    public List<UserDTO> listUsers() {
        List<User> users = userRepository.findAll();
        
        return users.stream()
                .map(user -> {
                    UserDTO dto = new UserDTO();
                    dto.setId(user.getId());
                    dto.setName(user.getName());
                    dto.setPhone(maskingService.maskPhone(user.getPhone()));
                    dto.setIdCard(maskingService.maskIdCard(user.getIdCard()));
                    return dto;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 根据权限决定是否脱敏
     */
    public UserDTO getUserInfoWithPermission(Long userId, boolean hasViewPermission) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        
        // 根据权限决定是否脱敏
        if (hasViewPermission) {
            // 有权限，返回原始数据
            dto.setPhone(user.getPhone());
            dto.setIdCard(user.getIdCard());
        } else {
            // 无权限，返回脱敏数据
            dto.setPhone(maskingService.maskPhone(user.getPhone()));
            dto.setIdCard(maskingService.maskIdCard(user.getIdCard()));
        }
        
        return dto;
    }
}
```

## 测试

```java
@SpringBootTest
class DataMaskingServiceTest {
    
    @Autowired
    private DataMaskingService maskingService;
    
    @Test
    void testMaskPhone() {
        String phone = "13812345678";
        String masked = maskingService.maskPhone(phone);
        assertEquals("138****5678", masked);
    }
    
    @Test
    void testMaskIdCard() {
        String idCard = "110101199001011234";
        String masked = maskingService.maskIdCard(idCard);
        assertEquals("110101********1234", masked);
    }
    
    @Test
    void testMaskBankCard() {
        String bankCard = "6222021234567890";
        String masked = maskingService.maskBankCard(bankCard);
        assertEquals("6222 **** **** 7890", masked);
    }
    
    @Test
    void testMaskEmail() {
        String email = "user@example.com";
        String masked = maskingService.maskEmail(email);
        assertEquals("u***@example.com", masked);
    }
}
```

## 常见问题

### Q: 如何在不同环境使用不同的脱敏策略？

A: 使用 Spring Profile 配置：

```yaml
# application-dev.yml (开发环境 - 不脱敏)
database:
  enhanced:
    security:
      masking:
        enabled: false

# application-prod.yml (生产环境 - 启用脱敏)
database:
  enhanced:
    security:
      masking:
        enabled: true
```

### Q: 如何自定义脱敏规则？

A: 在配置文件中定义自定义规则：

```yaml
database:
  enhanced:
    security:
      masking:
        enabled: true
        rules:
          custom: "##****##"  # 自定义规则
```

然后使用：
```java
String masked = maskingService.maskWithRule(data, "##****##");
```

### Q: 日志脱敏会影响性能吗？

A: 会有一定影响，但通常可以接受。如果对性能要求极高，可以：
1. 只在生产环境启用
2. 使用异步日志
3. 减少日志输出量

### Q: 如何处理特殊格式的数据？

A: 使用自定义规则或扩展 DataMaskingService：

```java
@Service
public class CustomMaskingService extends DataMaskingServiceImpl {
    
    public String maskSpecialFormat(String data) {
        // 自定义脱敏逻辑
        return data;
    }
}
```
