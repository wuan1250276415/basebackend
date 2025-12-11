package com.basebackend.database.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.basebackend.database.audit.interceptor.AuditInterceptor;
import com.basebackend.database.audit.service.AuditLogService;
import com.basebackend.database.health.interceptor.SqlExecutionTimeInterceptor;
import com.basebackend.database.health.logger.SlowQueryLogger;
import com.basebackend.database.interceptor.SqlInjectionPreventionInterceptor;
import com.basebackend.database.security.interceptor.DataScopeInterceptor;
import com.basebackend.database.security.interceptor.DecryptionInterceptor;
import com.basebackend.database.security.interceptor.EncryptionInterceptor;
import com.basebackend.database.security.interceptor.PermissionMaskingInterceptor;
import com.basebackend.database.security.service.DataMaskingService;
import com.basebackend.database.security.service.EncryptionService;
import com.basebackend.database.tenant.interceptor.TenantInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * MyBatis Plus 配置
 */
@Configuration
public class MyBatisPlusConfig {

    /**
     * Configure ObjectMapper with JSR310 support for Java 8 date/time types
     * Only creates this bean if no other ObjectMapper bean exists
     */
    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Register JavaTimeModule for Java 8 date/time support (LocalDateTime,
        // LocalDate, etc.)
        mapper.registerModule(new JavaTimeModule());
        // Disable writing dates as timestamps (use ISO-8601 format instead)
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /**
     * MyBatis Plus 插件配置
     * 使用 ObjectProvider 安全获取可选的拦截器 Bean
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(
            ObjectProvider<TenantInterceptor> tenantInterceptorProvider,
            ObjectProvider<EncryptionInterceptor> encryptionInterceptorProvider,
            ObjectProvider<SqlInjectionPreventionInterceptor> sqlInjectionInterceptorProvider) {

        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 租户拦截器（必须在分页插件之前）
        TenantInterceptor tenantInterceptor = tenantInterceptorProvider.getIfAvailable();
        if (tenantInterceptor != null) {
            interceptor.addInnerInterceptor(tenantInterceptor);
        }

        // 分页插件
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        paginationInnerInterceptor.setMaxLimit(1000L); // 单页最大数量限制
        paginationInnerInterceptor.setOverflow(false); // 溢出总页数后是否进行处理
        interceptor.addInnerInterceptor(paginationInnerInterceptor);

        // 乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        // 防止全表更新与删除插件
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

        // SQL 注入检测（支持配置开关和白名单）
        SqlInjectionPreventionInterceptor sqlInjectionInterceptor = sqlInjectionInterceptorProvider.getIfAvailable();
        if (sqlInjectionInterceptor != null) {
            interceptor.addInnerInterceptor(sqlInjectionInterceptor);
        }

        // 加密拦截器（在保存时加密敏感字段）
        EncryptionInterceptor encryptionInterceptor = encryptionInterceptorProvider.getIfAvailable();
        if (encryptionInterceptor != null) {
            interceptor.addInnerInterceptor(encryptionInterceptor);
        }

        return interceptor;
    }

    /**
     * Register audit interceptor if enabled
     * Note: AuditInterceptor is registered separately as it's a standard MyBatis
     * Interceptor,
     * not an InnerInterceptor. It will be registered to SqlSessionFactory
     * via @PostConstruct.
     */
    @Bean
    @ConditionalOnProperty(prefix = "database.enhanced.audit", name = "enabled", havingValue = "true", matchIfMissing = true)
    public AuditInterceptor auditInterceptor(@Lazy AuditLogService auditLogService,
            DatabaseEnhancedProperties properties,
            ObjectMapper objectMapper) {
        return new AuditInterceptor(auditLogService, properties, objectMapper);
    }

    /**
     * Register decryption interceptor if encryption is enabled
     * Note: DecryptionInterceptor is registered separately as it's a standard
     * MyBatis Interceptor,
     * not an InnerInterceptor. It intercepts result set handling to decrypt
     * sensitive fields.
     */
    @Bean
    @ConditionalOnProperty(prefix = "database.enhanced.security.encryption", name = "enabled", havingValue = "true")
    public DecryptionInterceptor decryptionInterceptor(EncryptionService encryptionService,
            DatabaseEnhancedProperties properties) {
        return new DecryptionInterceptor(encryptionService, properties);
    }

    /**
     * Register permission masking interceptor if masking is enabled
     * Note: PermissionMaskingInterceptor is registered separately as it's a
     * standard MyBatis Interceptor,
     * not an InnerInterceptor. It intercepts result set handling to mask sensitive
     * fields based on user permissions.
     * This interceptor runs after DecryptionInterceptor to ensure data is decrypted
     * before masking decisions are made.
     */
    @Bean
    @ConditionalOnProperty(prefix = "database.enhanced.security.masking", name = "enabled", havingValue = "true")
    public PermissionMaskingInterceptor permissionMaskingInterceptor(DataMaskingService dataMaskingService,
            DatabaseEnhancedProperties properties) {
        return new PermissionMaskingInterceptor(dataMaskingService, properties);
    }

    /**
     * Register SQL execution time interceptor if health monitoring is enabled
     * Note: SqlExecutionTimeInterceptor is registered separately as it's a standard
     * MyBatis Interceptor,
     * not an InnerInterceptor. It intercepts SQL execution to measure execution
     * time and identify slow queries.
     */
    @Bean
    @ConditionalOnProperty(prefix = "database.enhanced.health", name = "enabled", havingValue = "true", matchIfMissing = true)
    public SqlExecutionTimeInterceptor sqlExecutionTimeInterceptor(DatabaseEnhancedProperties properties,
            SlowQueryLogger slowQueryLogger) {
        return new SqlExecutionTimeInterceptor(properties, slowQueryLogger);
    }

    /**
     * Register data scope interceptor if security is enabled
     * Note: DataScopeInterceptor is registered separately as it's a standard
     * MyBatis Interceptor,
     * not an InnerInterceptor. It intercepts SQL execution to apply data permission
     * filtering.
     */
    @Bean
    @ConditionalOnProperty(prefix = "database.enhanced.security.data-scope", name = "enabled", havingValue = "true", matchIfMissing = true)
    public DataScopeInterceptor dataScopeInterceptor() {
        return new DataScopeInterceptor();
    }

    /**
     * Register SQL injection prevention interceptor with configuration support
     * Supports whitelist patterns and mapper methods, configurable strict mode
     */
    @Bean
    @ConditionalOnProperty(prefix = "database.enhanced.sql-injection", name = "enabled", havingValue = "true", matchIfMissing = true)
    public SqlInjectionPreventionInterceptor sqlInjectionPreventionInterceptor(DatabaseEnhancedProperties properties) {
        return new SqlInjectionPreventionInterceptor(properties);
    }

}
