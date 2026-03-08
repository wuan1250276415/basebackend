package com.basebackend.database.security.config;

import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.security.interceptor.DataScopeInterceptor;
import com.basebackend.database.security.interceptor.PermissionMaskingInterceptor;
import com.basebackend.database.security.service.DataMaskingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * 数据库安全自动配置
 * 统一注册加密/脱敏/数据权限相关的拦截器 Bean。
 * <p>
 * 原先这些 Bean 定义在 core 的 MyBatisPlusConfig 中，
 * 拆分后迁移到本模块，消除 core 对 security 包的编译依赖。
 */
@Slf4j
@AutoConfiguration
@Import({EncryptionConfig.class, MaskingConfig.class})
public class DatabaseSecurityAutoConfiguration {

    public DatabaseSecurityAutoConfiguration() {
        log.info("Database Security module initialized");
    }

    /**
     * Register permission masking interceptor if masking is enabled
     */
    @Bean
    @ConditionalOnProperty(prefix = "database.enhanced.security.masking", name = "enabled", havingValue = "true")
    public PermissionMaskingInterceptor permissionMaskingInterceptor(DataMaskingService dataMaskingService,
            DatabaseEnhancedProperties properties) {
        return new PermissionMaskingInterceptor(dataMaskingService, properties);
    }

    /**
     * Register data scope interceptor if security is enabled
     */
    @Bean
    @ConditionalOnProperty(prefix = "database.enhanced.security.data-scope", name = "enabled", havingValue = "true", matchIfMissing = true)
    public DataScopeInterceptor dataScopeInterceptor() {
        return new DataScopeInterceptor();
    }
}
