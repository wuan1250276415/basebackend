package com.basebackend.database.tenant.config;

import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.tenant.annotation.IgnoreTenantAspect;
import com.basebackend.database.tenant.filter.TenantContextFilter;
import com.basebackend.database.tenant.handler.TenantMetaObjectHandler;
import com.basebackend.database.tenant.interceptor.TenantInterceptor;
import com.basebackend.database.tenant.propagation.TenantPropagationInterceptor;
import com.basebackend.database.tenant.quota.TenantQuotaManager;
import com.basebackend.database.tenant.resolver.DomainTenantResolver;
import com.basebackend.database.tenant.resolver.HeaderTenantResolver;
import com.basebackend.database.tenant.resolver.TenantResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.util.List;

/**
 * 多租户自动配置类
 * <p>
 * 根据配置启用多租户功能，包括：
 * <ul>
 *   <li>SQL 自动注入 tenant_id（TenantInterceptor）</li>
 *   <li>INSERT 自动填充 tenant_id（TenantMetaObjectHandler）</li>
 *   <li>HTTP 请求自动解析租户（TenantContextFilter）</li>
 *   <li>微服务间租户传播（TenantPropagationInterceptor）</li>
 *   <li>@IgnoreTenant 声明式跳过过滤</li>
 *   <li>租户配额管理（TenantQuotaManager）</li>
 * </ul>
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "database.enhanced.multi-tenancy", name = "enabled", havingValue = "true")
public class MultiTenancyAutoConfiguration {

    private final DatabaseEnhancedProperties properties;

    public MultiTenancyAutoConfiguration(DatabaseEnhancedProperties properties) {
        this.properties = properties;
        log.info("多租户模块已启用");
        log.info("  - 隔离模式: {}", properties.getMultiTenancy().getIsolationMode());
        log.info("  - 租户字段: {}", properties.getMultiTenancy().getTenantColumn());
        log.info("  - 排除表: {}", properties.getMultiTenancy().getExcludedTables());
    }

    // ==================== 数据层（原有） ====================

    @Bean
    @ConditionalOnMissingBean
    public TenantInterceptor tenantInterceptor() {
        return new TenantInterceptor(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public TenantMetaObjectHandler tenantMetaObjectHandler() {
        return new TenantMetaObjectHandler(properties);
    }

    // ==================== Web 层（新增） ====================

    /**
     * Web 层多租户配置：Filter + Resolver
     */
    @Configuration
    @ConditionalOnClass(name = "jakarta.servlet.Filter")
    static class TenantWebConfig {

        @Bean
        @ConditionalOnMissingBean(name = "headerTenantResolver")
        public HeaderTenantResolver headerTenantResolver() {
            return new HeaderTenantResolver();
        }

        @Bean
        @ConditionalOnMissingBean(name = "domainTenantResolver")
        public DomainTenantResolver domainTenantResolver() {
            return new DomainTenantResolver();
        }

        @Bean
        @ConditionalOnMissingBean(name = "tenantContextFilterRegistration")
        public FilterRegistrationBean<TenantContextFilter> tenantContextFilterRegistration(
                List<TenantResolver> resolvers) {
            TenantContextFilter filter = new TenantContextFilter(
                    resolvers,
                    List.of("/actuator", "/health", "/login", "/auth", "/public"),
                    false
            );
            FilterRegistrationBean<TenantContextFilter> registration = new FilterRegistrationBean<>(filter);
            registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
            registration.addUrlPatterns("/*");
            log.info("租户 Web Filter 已注册, 解析器数量: {}", resolvers.size());
            return registration;
        }
    }

    // ==================== 传播层（新增） ====================

    @Bean
    @ConditionalOnMissingBean
    public TenantPropagationInterceptor tenantPropagationInterceptor() {
        log.info("租户传播拦截器已注册 (RestClient/RestTemplate)");
        return new TenantPropagationInterceptor();
    }

    // ==================== 注解层（新增） ====================

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.aspectj.lang.ProceedingJoinPoint")
    public IgnoreTenantAspect ignoreTenantAspect() {
        log.info("@IgnoreTenant 注解切面已注册");
        return new IgnoreTenantAspect();
    }

    // ==================== 配额管理（新增） ====================

    @Bean
    @ConditionalOnMissingBean
    public TenantQuotaManager tenantQuotaManager() {
        log.info("租户配额管理器已注册");
        return new TenantQuotaManager();
    }
}
