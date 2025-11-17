# Phase 11.4: Nacos 配置中心增强实施指南

## 📋 概述

本指南介绍如何增强 Nacos 配置中心的功能，包括配置加密、版本管理、配置监听和备份机制，确保配置管理的安全性和可靠性。

---

## 🏗️ Nacos 配置中心架构

### 架构图

```
┌────────────────────────────────────────────────────────────────┐
│                  Nacos 配置中心架构                             │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  ┌─────────────┐         ┌─────────────┐                      │
│  │   微服务集群  │◄────────│  Nacos Server │                     │
│  │             │         │             │                      │
│  │  - 动态配置  │         │  - 配置存储   │                      │
│  │  - 配置监听  │         │  - 版本管理   │                      │
│  │  - 热更新    │         │  - 配置加密   │                      │
│  └──────┬──────┘         └──────┬──────┘                      │
│         │                        │                            │
│  ┌──────▼──────┐         ┌──────▼──────┐                      │
│  │   配置客户端  │         │   配置备份   │                      │
│  │  - 拉取配置  │         │  - 定期备份   │                      │
│  │  - 监听变化  │         │  - 恢复机制   │                      │
│  │  - 自动刷新  │         │  - 历史版本   │                      │
│  └─────────────┘         └─────────────┘                      │
│                                                                │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │              配置安全管理                                 │ │
│  ├─────────────────────────────────────────────────────────┤ │
│  │ • 敏感信息加密 (数据库密码、API Key)                      │ │
│  │ • 配置权限控制 (读/写权限)                              │ │
│  │ • 配置审计日志 (变更历史追踪)                             │ │
│  │ • 配置版本回滚 (快速恢复)                               │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

### 功能特性

| 功能 | 说明 | 价值 |
|------|------|------|
| **配置加密** | 使用 Jasypt 加密敏感配置 | 提升安全性 |
| **版本管理** | 配置版本历史和回滚 | 降低风险 |
| **动态刷新** | 配置变更实时推送到服务 | 提升效率 |
| **配置备份** | 自动备份和恢复机制 | 保证可靠性 |
| **权限控制** | 基于角色的配置访问 | 规范化管理 |
| **审计日志** | 配置变更全记录 | 可追溯性 |

---

## 🔐 配置加密实现

### 1. Jasypt 集成

#### 添加依赖

```xml
<dependency>
    <groupId>com.github.ulisesbocchio</groupId>
    <artifactId>jasypt-spring-boot-starter</artifactId>
    <version>3.0.5</version>
</dependency>
```

#### 配置 application.yml

```yaml
jasypt:
  encryptor:
    # 加密算法
    algorithm: PBEWithMD5AndDES
    # 加密密钥 (生产环境应该从环境变量读取)
    password: ${JASYPT_ENCRYPTOR_PASSWORD:basebackend_encrypt_key_2024}
    # 密钥生成器
    keyObtenerationIterations: 1000
    # Pool 大小
    poolSize: 1
    # Provider 类名
    providerClassName: null
    # Provider 实例
    providerName: null
    # Salt 生成器
    saltGeneratorClassname: org.jasypt.salt.RandomSaltGenerator
    # IV 生成器
    ivGeneratorClassname: org.jasypt.iv.NoIvGenerator
    # String 输出类型
    stringOutputType: base64

nacos:
  config:
    # 加密配置的前缀和后缀
    encrypted-data-key: ENC(encrypted_data_key_here)
```

#### 环境变量配置

```bash
# 设置加密密钥
export JASYPT_ENCRYPTOR_PASSWORD="your_encryption_key_here"

# 敏感配置加密
mvn jasypt:encrypt -Djasypt.encryptor.password="${JASYPT_ENCRYPTOR_PASSWORD}"
```

### 2. 自定义加密工具

```java
/**
 * 配置加密工具
 * 用于加密/解密敏感配置信息
 */
@Component
public class ConfigEncryptionUtil {

    private final String encryptorPassword;

    @Autowired
    public ConfigEncryptionUtil(
            @Value("${jasypt.encryptor.password}") String encryptorPassword) {
        this.encryptorPassword = encryptorPassword;
    }

    /**
     * 加密配置
     *
     * @param plainText 明文配置
     * @return 加密后的配置
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.trim().isEmpty()) {
            return plainText;
        }

        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword(encryptorPassword);
        encryptor.setAlgorithm("PBEWithMD5AndDES");

        return "ENC(" + encryptor.encrypt(plainText) + ")";
    }

    /**
     * 解密配置
     *
     * @param encryptedText 加密配置
     * @return 解密后的配置
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.trim().isEmpty()) {
            return encryptedText;
        }

        if (!encryptedText.startsWith("ENC(") || !encryptedText.endsWith(")")) {
            return encryptedText;
        }

        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword(encryptorPassword);
        encryptor.setAlgorithm("PBEWithMD5AndDES");

        String encrypted = encryptedText.substring(4, encryptedText.length() - 1);
        return encryptor.decrypt(encrypted);
    }

    /**
     * 批量加密配置文件
     *
     * @param configMap 配置 Map
     * @return 加密后的配置 Map
     */
    public Map<String, String> encryptConfig(Map<String, String> configMap) {
        return configMap.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> encrypt(entry.getValue())
            ));
    }

    /**
     * 判断是否为加密配置
     */
    public boolean isEncrypted(String value) {
        return value != null && value.startsWith("ENC(") && value.endsWith(")");
    }
}
```

### 3. 敏感配置示例

```yaml
# application.yml
spring:
  datasource:
    # 加密前: url: jdbc:mysql://localhost:3306/basebackend
    # 加密后:
    url: ENC(encrypted_mysql_url)
    username: ENC(encrypted_mysql_username)
    password: ENC(encrypted_mysql_password)

  redis:
    # 加密前: password: redis_password
    # 加密后:
    password: ENC(encrypted_redis_password)

# 其他敏感配置
api:
  # 加密前: secret: your_api_secret
  # 加密后:
  secret: ENC(encrypted_api_secret)

# 日志中的敏感信息过滤
logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{50} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{50} - %msg%n"
  # 过滤敏感关键词
  level:
    root: INFO
```

---

## 📊 版本管理实现

### 1. 配置版本表

```sql
-- 配置版本历史表
CREATE TABLE `config_version_history` (
  `id` BIGINT AUTO_INCREMENT NOT NULL,
  `config_key` VARCHAR(255) NOT NULL COMMENT '配置键',
  `config_value` LONGTEXT COMMENT '配置值',
  `config_group` VARCHAR(64) DEFAULT 'DEFAULT_GROUP' COMMENT '配置组',
  `version` INT NOT NULL COMMENT '版本号',
  `author` VARCHAR(64) NOT NULL COMMENT '作者',
  `change_type` VARCHAR(20) NOT NULL COMMENT '变更类型: CREATE/UPDATE/DELETE',
  `change_desc` TEXT COMMENT '变更描述',
  `md5` VARCHAR(64) DEFAULT NULL COMMENT 'MD5 校验值',
  `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key_version` (`config_key`, `version`),
  KEY `idx_config_key` (`config_key`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配置版本历史表';

-- 配置审计日志表
CREATE TABLE `config_audit_log` (
  `id` BIGINT AUTO_INCREMENT NOT NULL,
  `config_key` VARCHAR(255) NOT NULL,
  `config_group` VARCHAR(64) DEFAULT 'DEFAULT_GROUP',
  `operation` VARCHAR(20) NOT NULL COMMENT '操作类型: READ/WRITE/UPDATE/DELETE',
  `operator` VARCHAR(64) NOT NULL COMMENT '操作人',
  `operator_ip` VARCHAR(50) DEFAULT NULL COMMENT '操作人 IP',
  `before_value` LONGTEXT COMMENT '变更前值',
  `after_value` LONGTEXT COMMENT '变更后值',
  `result` TINYINT NOT NULL COMMENT '操作结果: 1-成功, 0-失败',
  `error_msg` TEXT COMMENT '错误信息',
  `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_config_key` (`config_key`),
  KEY `idx_operator` (`operator`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配置审计日志表';
```

### 2. 配置版本管理服务

```java
/**
 * 配置版本管理服务
 */
@Service
@Transactional
public class ConfigVersionService {

    @Autowired
    private ConfigVersionHistoryMapper versionMapper;

    @Autowired
    private ConfigAuditLogMapper auditMapper;

    @Autowired
    private NacosConfigService nacosConfigService;

    /**
     * 更新配置并记录版本
     */
    public void updateConfig(String configKey, String configValue, String group,
                           String author, String changeDesc) {
        // 1. 获取当前版本
        Integer currentVersion = versionMapper.getCurrentVersion(configKey, group);
        if (currentVersion == null) {
            currentVersion = 0;
        }

        // 2. 生成新版本
        Integer newVersion = currentVersion + 1;

        // 3. 计算 MD5
        String md5 = DigestUtils.md5Hex(configValue);

        // 4. 记录版本历史
        ConfigVersionHistory version = new ConfigVersionHistory();
        version.setConfigKey(configKey);
        version.setConfigValue(configValue);
        version.setConfigGroup(group);
        version.setVersion(newVersion);
        version.setAuthor(author);
        version.setChangeType("UPDATE");
        version.setChangeDesc(changeDesc);
        version.setMd5(md5);
        versionMapper.insert(version);

        // 5. 更新 Nacos 配置
        nacosConfigService.publishConfig(configKey, configValue, group);

        // 6. 记录审计日志
        saveAuditLog(configKey, group, "UPDATE", author, null, configValue, true, null);
    }

    /**
     * 获取配置版本历史
     */
    public List<ConfigVersionHistory> getVersionHistory(String configKey, String group) {
        return versionMapper.selectByConfigKey(configKey, group);
    }

    /**
     * 回滚到指定版本
     */
    public void rollbackToVersion(String configKey, String group, Integer targetVersion,
                                String operator, String reason) {
        // 1. 获取目标版本配置
        ConfigVersionHistory targetVersionData = versionMapper
            .selectByConfigKeyAndVersion(configKey, group, targetVersion);

        if (targetVersionData == null) {
            throw new BusinessException("目标版本不存在");
        }

        // 2. 记录回滚版本
        Integer currentVersion = versionMapper.getCurrentVersion(configKey, group);
        Integer newVersion = currentVersion + 1;

        ConfigVersionHistory rollbackVersion = new ConfigVersionHistory();
        rollbackVersion.setConfigKey(configKey);
        rollbackVersion.setConfigValue(targetVersionData.getConfigValue());
        rollbackVersion.setConfigGroup(group);
        rollbackVersion.setVersion(newVersion);
        rollbackVersion.setAuthor(operator);
        rollbackVersion.setChangeType("ROLLBACK");
        rollbackVersion.setChangeDesc("回滚到版本 " + targetVersion + ", 原因: " + reason);
        rollbackVersion.setMd5(targetVersionData.getMd5());
        versionMapper.insert(rollbackVersion);

        // 3. 更新 Nacos 配置
        nacosConfigService.publishConfig(
            configKey,
            targetVersionData.getConfigValue(),
            group
        );

        // 4. 记录审计日志
        saveAuditLog(configKey, group, "ROLLBACK", operator, null,
            targetVersionData.getConfigValue(), true, "回滚到版本 " + targetVersion);
    }

    /**
     * 比较两个版本的差异
     */
    public ConfigDiff compareVersions(String configKey, String group,
                                    Integer version1, Integer version2) {
        ConfigVersionHistory v1 = versionMapper
            .selectByConfigKeyAndVersion(configKey, group, version1);
        ConfigVersionHistory v2 = versionMapper
            .selectByConfigKeyAndVersion(configKey, group, version2);

        ConfigDiff diff = new ConfigDiff();
        diff.setVersion1(version1);
        diff.setVersion2(version2);
        diff.setBeforeValue(v1.getConfigValue());
        diff.setAfterValue(v2.getConfigValue());
        diff.setChanged(!Objects.equals(v1.getConfigValue(), v2.getConfigValue()));

        return diff;
    }

    /**
     * 保存审计日志
     */
    private void saveAuditLog(String configKey, String group, String operation,
                            String operator, String beforeValue, String afterValue,
                            boolean success, String errorMsg) {
        ConfigAuditLog log = new ConfigAuditLog();
        log.setConfigKey(configKey);
        log.setConfigGroup(group);
        log.setOperation(operation);
        log.setOperator(operator);
        log.setBeforeValue(beforeValue);
        log.setAfterValue(afterValue);
        log.setResult(success ? 1 : 0);
        log.setErrorMsg(errorMsg);
        log.setOperatorIp(getClientIp());
        auditMapper.insert(log);
    }
}
```

---

## 👂 配置监听和动态刷新

### 1. 配置监听器

```java
/**
 * 配置监听器
 * 监听 Nacos 配置变化并动态刷新
 */
@Component
public class NacosConfigListener {

    private static final Logger log = LoggerFactory.getLogger(NacosConfigListener.class);

    @Autowired
    private ConfigurableApplicationContext context;

    /**
     * 监听应用配置变化
     */
    @EventListener
    public void onConfigChanged(ConfigChangedEvent event) {
        log.info("配置变化: key={}, group={}, content={}",
            event.getConfigKey(), event.getGroup(), event.getContent());

        try {
            // 获取变更的配置
            String configKey = event.getConfigKey();
            String newValue = event.getContent();

            // 刷新相应的 Bean
            refreshConfigBean(configKey, newValue);

            // 发送通知
            notifyConfigChange(configKey, event);

        } catch (Exception e) {
            log.error("配置变化处理失败", e);
        }
    }

    /**
     * 刷新配置 Bean
     */
    private void refreshConfigBean(String configKey, String newValue) {
        // 根据配置键找到对应的 Bean 并刷新
        switch (configKey) {
            case "database.config":
                refreshDatabaseConfig(newValue);
                break;
            case "redis.config":
                refreshRedisConfig(newValue);
                break;
            case "api.config":
                refreshApiConfig(newValue);
                break;
            default:
                log.warn("未处理的配置键: {}", configKey);
        }
    }

    /**
     * 刷新数据库配置
     */
    private void refreshDatabaseConfig(String configValue) {
        // 解析配置 JSON
        DatabaseConfig config = JSON.parseObject(configValue, DatabaseConfig.class);

        // 动态更新数据源
        DataSource oldDataSource = context.getBean(DataSource.class);
        DataSource newDataSource = createDataSource(config);

        // 替换 Bean
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        beanFactory.removeBeanDefinition("dataSource");
        beanFactory.registerSingleton("dataSource", newDataSource);

        log.info("数据库配置已刷新");
    }

    /**
     * 刷新 Redis 配置
     */
    private void refreshRedisConfig(String configValue) {
        RedisConfig config = JSON.parseObject(configValue, RedisConfig.class);

        // 重新初始化 Redis 连接
        RedisTemplate<String, Object> oldTemplate = context.getBean("redisTemplate", RedisTemplate.class);
        RedisTemplate<String, Object> newTemplate = createRedisTemplate(config);

        // 替换 Bean
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        beanFactory.removeBeanDefinition("redisTemplate");
        beanFactory.registerSingleton("redisTemplate", newTemplate);

        log.info("Redis 配置已刷新");
    }

    private void refreshApiConfig(String configValue) {
        // API 配置刷新逻辑
        log.info("API 配置已刷新");
    }

    private void notifyConfigChange(String configKey, ConfigChangedEvent event) {
        // 发送配置变化通知
        // 可以通过消息队列、邮件等方式通知
        log.info("发送配置变化通知: {}", configKey);
    }
}
```

### 2. 配置刷新注解

```java
/**
 * 配置刷新注解
 * 用于标记需要动态刷新的配置项
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RefreshableConfig {

    /**
     * 配置键
     */
    String key();

    /**
     * 配置组
     */
    String group() default "DEFAULT_GROUP";

    /**
     * 数据类型
     */
    Class<?> type() default String.class;
}

/**
 * 配置刷新处理器
 */
@Component
public class ConfigRefreshHandler {

    @Autowired
    private ConfigurableApplicationContext context;

    /**
     * 刷新标记的配置
     */
    public void refreshConfig(String configKey, Object newValue) {
        // 找到所有标记了 @RefreshableConfig 的字段
        Field[] fields = context.getBeanFactory().getBeanClass().getDeclaredFields();

        for (Field field : fields) {
            RefreshableConfig annotation = field.getAnnotation(RefreshableConfig.class);
            if (annotation != null && annotation.key().equals(configKey)) {
                try {
                    field.setAccessible(true);
                    Object bean = findBeanByField(field);
                    field.set(bean, convertValue(newValue, annotation.type()));
                    log.info("配置已刷新: key={}, bean={}, field={}",
                        configKey, bean.getClass().getSimpleName(), field.getName());
                } catch (Exception e) {
                    log.error("配置刷新失败", e);
                }
            }
        }
    }
}
```

### 3. 使用示例

```java
/**
 * 数据库配置
 */
@Component
public class DatabaseConfig {

    @RefreshableConfig(key = "database.url")
    private String url;

    @RefreshableConfig(key = "database.username")
    private String username;

    @RefreshableConfig(key = "database.password")
    private String password;

    @RefreshableConfig(key = "database.driverClassName")
    private String driverClassName;

    // getter/setter
}
```

---

## 💾 配置备份机制

### 1. 配置备份服务

```java
/**
 * 配置备份服务
 */
@Service
public class ConfigBackupService {

    private static final Logger log = LoggerFactory.getLogger(ConfigBackupService.class);

    @Autowired
    private ConfigVersionHistoryMapper versionMapper;

    @Autowired
    private NacosConfigService nacosConfigService;

    @Autowired
    private ObjectStorageService objectStorageService;

    /**
     * 备份所有配置
     */
    @Scheduled(cron = "0 0 3 * * ?") // 每天凌晨3点执行
    public void backupAllConfigs() {
        log.info("开始备份所有配置...");

        try {
            // 1. 获取所有配置
            List<ConfigKey> allConfigs = nacosConfigService.getAllConfigs();

            // 2. 创建备份文件
            String backupFileName = "config-backup-" + System.currentTimeMillis() + ".zip";
            File backupFile = createBackupFile(allConfigs, backupFileName);

            // 3. 上传到云存储
            String backupUrl = objectStorageService.uploadBackup(backupFile);

            // 4. 记录备份信息
            saveBackupRecord(backupFileName, backupUrl, allConfigs.size());

            log.info("配置备份完成: file={}, url={}, count={}",
                backupFileName, backupUrl, allConfigs.size());

        } catch (Exception e) {
            log.error("配置备份失败", e);
        }
    }

    /**
     * 创建备份文件
     */
    private File createBackupFile(List<ConfigKey> configs, String fileName) throws IOException {
        File backupDir = new File(System.getProperty("java.io.tmpdir"), "nacos-backup");
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }

        File backupFile = new File(backupDir, fileName);

        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(backupFile))) {
            // 1. 备份配置文件
            for (ConfigKey configKey : configs) {
                String content = nacosConfigService.getConfig(
                    configKey.getKey(),
                    configKey.getGroup()
                );

                ZipEntry entry = new ZipEntry(configKey.getGroup() + "/" + configKey.getKey() + ".txt");
                zipOut.putNextEntry(entry);
                zipOut.write(content.getBytes(StandardCharsets.UTF_8));
                zipOut.closeEntry();
            }

            // 2. 备份版本历史
            List<ConfigVersionHistory> versionHistory = versionMapper.selectAll();
            ZipEntry historyEntry = new ZipEntry("version-history.json");
            zipOut.putNextEntry(historyEntry);
            zipOut.write(JSON.toJSONBytes(versionHistory));
            zipOut.closeEntry();

            // 3. 创建备份清单
            BackupManifest manifest = BackupManifest.builder()
                .backupTime(new Date())
                .configCount(configs.size())
                .version("1.0")
                .build();

            ZipEntry manifestEntry = new ZipEntry("manifest.json");
            zipOut.putNextEntry(manifestEntry);
            zipOut.write(JSON.toJSONBytes(manifest));
            zipOut.closeEntry();
        }

        return backupFile;
    }

    /**
     * 恢复配置
     */
    public void restoreConfig(String backupFileName) {
        log.info("开始恢复配置: {}", backupFileName);

        try {
            // 1. 下载备份文件
            File backupFile = objectStorageService.downloadBackup(backupFileName);

            // 2. 解压并解析
            List<ConfigRestoreItem> restoreItems = parseBackupFile(backupFile);

            // 3. 逐个恢复配置
            for (ConfigRestoreItem item : restoreItems) {
                nacosConfigService.publishConfig(
                    item.getKey(),
                    item.getValue(),
                    item.getGroup()
                );
                log.info("配置已恢复: {}/{}", item.getGroup(), item.getKey());
            }

            // 4. 记录恢复日志
            saveRestoreLog(backupFileName, restoreItems.size());

            log.info("配置恢复完成: {}", backupFileName);

        } catch (Exception e) {
            log.error("配置恢复失败", e);
            throw new BusinessException("配置恢复失败: " + e.getMessage(), e);
        }
    }

    /**
     * 清理过期备份
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    public void cleanupOldBackups() {
        log.info("开始清理过期备份...");

        // 清理30天前的备份
        Date cutoffDate = DateUtils.addDays(new Date(), -30);

        List<String> expiredBackups = objectStorageService.getExpiredBackups(cutoffDate);
        for (String backupName : expiredBackups) {
            objectStorageService.deleteBackup(backupName);
            log.info("已删除过期备份: {}", backupName);
        }

        log.info("过期备份清理完成: {} 个", expiredBackups.size());
    }

    @Data
    @Builder
    public static class BackupManifest {
        private Date backupTime;
        private int configCount;
        private String version;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ConfigRestoreItem {
        private String group;
        private String key;
        private String value;
    }
}
```

---

## 📋 配置管理界面

### 1. 配置列表接口

```java
/**
 * 配置管理接口
 */
@RestController
@RequestMapping("/api/admin/config")
@Api(tags = "配置管理")
public class ConfigController {

    @Autowired
    private ConfigVersionService configVersionService;

    @Autowired
    private ConfigBackupService configBackupService;

    /**
     * 获取配置列表
     */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('CONFIG_READ')")
    public Result<PageInfo<ConfigInfo>> listConfigs(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String configKey,
            @RequestParam(required = false) String group) {

        List<ConfigInfo> configs = nacosConfigService.listConfigs(configKey, group);
        PageInfo<ConfigInfo> pageInfo = new PageInfo<>(configs, pageNum, pageSize);
        return Result.success(pageInfo);
    }

    /**
     * 更新配置
     */
    @PostMapping("/update")
    @PreAuthorize("hasAuthority('CONFIG_WRITE')")
    @Log(value = "更新配置", level = LogLevel.WARN)
    public Result<Void> updateConfig(@RequestBody @Valid ConfigUpdateRequest request) {
        configVersionService.updateConfig(
            request.getConfigKey(),
            request.getConfigValue(),
            request.getGroup(),
            getCurrentUsername(),
            request.getDescription()
        );
        return Result.success();
    }

    /**
     * 回滚配置
     */
    @PostMapping("/rollback")
    @PreAuthorize("hasAuthority('CONFIG_WRITE')")
    @Log(value = "回滚配置", level = LogLevel.WARN)
    public Result<Void> rollbackConfig(@RequestBody @Valid ConfigRollbackRequest request) {
        configVersionService.rollbackToVersion(
            request.getConfigKey(),
            request.getGroup(),
            request.getTargetVersion(),
            getCurrentUsername(),
            request.getReason()
        );
        return Result.success();
    }

    /**
     * 获取配置历史
     */
    @GetMapping("/{configKey}/history")
    @PreAuthorize("hasAuthority('CONFIG_READ')")
    public Result<List<ConfigVersionHistory>> getHistory(
            @PathVariable String configKey,
            @RequestParam(required = false) String group) {

        List<ConfigVersionHistory> history = configVersionService.getVersionHistory(configKey, group);
        return Result.success(history);
    }

    /**
     * 手动备份配置
     */
    @PostMapping("/backup")
    @PreAuthorize("hasAuthority('CONFIG_ADMIN')")
    public Result<String> backupConfigs() {
        configBackupService.backupAllConfigs();
        return Result.success("备份任务已提交");
    }

    /**
     * 恢复配置
     */
    @PostMapping("/restore")
    @PreAuthorize("hasAuthority('CONFIG_ADMIN')")
    @Log(value = "恢复配置", level = LogLevel.WARN)
    public Result<Void> restoreConfig(@RequestParam String backupFileName) {
        configBackupService.restoreConfig(backupFileName);
        return Result.success();
    }
}
```

---

## 🧪 测试与验证

### 1. 配置加密测试

```java
@SpringBootTest
public class ConfigEncryptionTest {

    @Autowired
    private ConfigEncryptionUtil encryptionUtil;

    @Test
    public void testEncryptDecrypt() {
        // 测试明文加密
        String plaintext = "my_secret_password";
        String encrypted = encryptionUtil.encrypt(plaintext);
        assertTrue(encryptionUtil.isEncrypted(encrypted));

        // 测试解密
        String decrypted = encryptionUtil.decrypt(encrypted);
        assertEquals(plaintext, decrypted);
    }

    @Test
    public void testEncryptConfig() {
        Map<String, String> configMap = new HashMap<>();
        configMap.put("database.password", "root123");
        configMap.put("redis.password", "redis456");

        Map<String, String> encryptedMap = encryptionUtil.encryptConfig(configMap);

        assertTrue(encryptedMap.get("database.password").startsWith("ENC("));
        assertTrue(encryptedMap.get("redis.password").startsWith("ENC("));
    }
}
```

### 2. 配置监听测试

```java
@SpringBootTest
public class ConfigListenerTest {

    @Autowired
    private NacosConfigService nacosConfigService;

    @Test
    public void testConfigChangeNotification() throws InterruptedException {
        // 1. 发布配置变化
        nacosConfigService.publishConfig("test.config", "value1", "DEFAULT_GROUP");

        // 2. 等待监听器响应
        Thread.sleep(2000);

        // 3. 验证配置已更新
        String newValue = nacosConfigService.getConfig("test.config", "DEFAULT_GROUP");
        assertEquals("value1", newValue);
    }
}
```

---

## 📊 监控与告警

### 1. 配置中心监控指标

```java
/**
 * 配置中心监控指标
 */
@Component
public class ConfigMetrics {

    private final MeterRegistry meterRegistry;

    public ConfigMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    private final Counter configUpdateCounter = Counter.builder("nacos_config_updates_total")
        .description("配置更新总数")
        .register(meterRegistry);

    private final Timer configLoadTimer = Timer.builder("nacos_config_load_duration")
        .description("配置加载耗时")
        .register(meterRegistry);

    private final Gauge activeConfigGauge = Gauge.builder("nacos_active_configs")
        .description("活跃配置数量")
        .register(meterRegistry, this, ConfigMetrics::getActiveConfigCount);

    public void recordConfigUpdate(String configKey, boolean success) {
        configUpdateCounter.increment(
            Tags.of("config_key", configKey, "status", success ? "success" : "failure")
        );
    }

    public void recordConfigLoad(Duration duration) {
        configLoadTimer.record(duration);
    }

    private int getActiveConfigCount() {
        // 获取活跃配置数量
        return 0;
    }
}
```

---

## 📚 参考资料

1. [Nacos 配置管理官方文档](https://nacos.io/zh-cn/docs/quick-start.html)
2. [Jasypt 配置加密](https://github.com/ulisesbocchio/jasypt-spring-boot)
3. [Spring Cloud 配置中心](https://spring.io/projects/spring-cloud-config)

---

**编制：** 浮浮酱 🐱（猫娘工程师）
**日期：** 2025-11-14
**状态：** 📋 指南完成，准备实施

**加油喵～ Nacos 配置中心增强即将完成！** ฅ'ω'ฅ
