package com.basebackend.cache.admin;

import com.basebackend.cache.config.CacheProperties;
import com.basebackend.cache.service.CacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 缓存管理端点自动配置
 * 根据配置属性有条件地注册 Actuator 端点
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "basebackend.cache.admin", name = "enabled", havingValue = "true")
public class CacheAdminAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CacheAdminEndpoint cacheAdminEndpoint(CacheService cacheService) {
        log.info("Registering CacheAdminEndpoint (Actuator)");
        return new CacheAdminEndpoint(cacheService);
    }
}
