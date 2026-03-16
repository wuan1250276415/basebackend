package com.basebackend.database.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.basebackend.database.audit.interceptor.AuditInterceptor;
import com.basebackend.database.audit.service.AuditLogService;
import com.basebackend.database.health.interceptor.SqlExecutionTimeInterceptor;
import com.basebackend.database.health.logger.SlowQueryLogger;
import com.basebackend.database.interceptor.SqlInjectionPreventionInterceptor;
import com.basebackend.database.config.DatabaseVendor;
import com.basebackend.database.config.DatabaseVendorDetector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * MyBatis Plus 配置
 * <p>
 * 仅注册 core 模块自身的拦截器。
 * security / multitenant 拦截器由各自子模块的 AutoConfiguration 注册，
 * 通过 ObjectProvider + @Qualifier 以 InnerInterceptor 基类型引用，消除编译期耦合。
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
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /**
     * MyBatis Plus 插件配置
     * <p>
     * tenantInterceptor / encryptionInterceptor 由 database-multitenant / database-security
     * 子模块提供，通过 @Qualifier + InnerInterceptor 基类型消除对子模块类型的编译依赖。
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(
            DatabaseVendorDetector vendorDetector,
            @Qualifier("tenantInterceptor") ObjectProvider<InnerInterceptor> tenantInterceptorProvider,
            @Qualifier("encryptionInterceptor") ObjectProvider<InnerInterceptor> encryptionInterceptorProvider,
            ObjectProvider<SqlInjectionPreventionInterceptor> sqlInjectionInterceptorProvider) {

        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 租户拦截器（必须在分页插件之前）
        InnerInterceptor tenantInterceptor = tenantInterceptorProvider.getIfAvailable();
        if (tenantInterceptor != null) {
            interceptor.addInnerInterceptor(tenantInterceptor);
        }

        // B4: derive DbType from auto-detected database vendor instead of hardcoding MYSQL
        DbType dbType = toMybatisPlusDbType(vendorDetector.detect());
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor(dbType);
        paginationInnerInterceptor.setMaxLimit(1000L);
        paginationInnerInterceptor.setOverflow(false);
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
        InnerInterceptor encryptionInterceptor = encryptionInterceptorProvider.getIfAvailable();
        if (encryptionInterceptor != null) {
            interceptor.addInnerInterceptor(encryptionInterceptor);
        }

        return interceptor;
    }

    /**
     * Register audit interceptor if enabled
     */
    @Bean
    @ConditionalOnProperty(prefix = "database.enhanced.audit", name = "enabled", havingValue = "true", matchIfMissing = true)
    public AuditInterceptor auditInterceptor(@Lazy AuditLogService auditLogService,
            DatabaseEnhancedProperties properties,
            ObjectMapper objectMapper) {
        return new AuditInterceptor(auditLogService, properties, objectMapper);
    }

    /**
     * Register SQL execution time interceptor if health monitoring is enabled
     */
    @Bean
    @ConditionalOnProperty(prefix = "database.enhanced.health", name = "enabled", havingValue = "true", matchIfMissing = true)
    public SqlExecutionTimeInterceptor sqlExecutionTimeInterceptor(DatabaseEnhancedProperties properties,
            SlowQueryLogger slowQueryLogger) {
        return new SqlExecutionTimeInterceptor(properties, slowQueryLogger);
    }

    /**
     * Register SQL injection prevention interceptor with configuration support
     */
    @Bean
    @ConditionalOnProperty(prefix = "database.enhanced.sql-injection", name = "enabled", havingValue = "true", matchIfMissing = true)
    public SqlInjectionPreventionInterceptor sqlInjectionPreventionInterceptor(DatabaseEnhancedProperties properties) {
        return new SqlInjectionPreventionInterceptor(properties);
    }

    private DbType toMybatisPlusDbType(DatabaseVendor vendor) {
        return switch (vendor) {
            case POSTGRESQL -> DbType.POSTGRE_SQL;
            default -> DbType.MYSQL;
        };
    }

}
