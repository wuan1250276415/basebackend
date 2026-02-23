package com.basebackend.common.datascope.config;

import com.basebackend.common.datascope.aspect.DataScopeAspect;
import com.basebackend.common.datascope.interceptor.DataScopeInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 数据权限自动配置
 * <p>
 * 自动注册 MyBatis 拦截器和 AOP 切面。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(DataScopeProperties.class)
@ConditionalOnProperty(prefix = "basebackend.datascope", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DataScopeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DataScopeInterceptor dataScopeInterceptor() {
        log.info("初始化数据权限 MyBatis 拦截器");
        return new DataScopeInterceptor();
    }

    @Bean
    @ConditionalOnMissingBean
    public DataScopeAspect dataScopeAspect(DataScopeProperties properties) {
        log.info("初始化数据权限 AOP 切面");
        return new DataScopeAspect(properties);
    }
}
