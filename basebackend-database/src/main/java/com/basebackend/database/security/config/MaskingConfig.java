package com.basebackend.database.security.config;

import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.security.service.DataMaskingService;
import com.basebackend.database.security.service.impl.DataMaskingServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 数据脱敏配置类
 * 根据配置启用数据脱敏功能
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "database.enhanced.security.masking", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class MaskingConfig {
    
    private final DatabaseEnhancedProperties properties;
    
    @Bean("dataMaskingService")
    public DataMaskingService dataMaskingService() {
        log.info("Initializing Data Masking Service");
        log.info("Masking enabled: {}", properties.getSecurity().getMasking().isEnabled());
        log.info("Custom masking rules: {}", properties.getSecurity().getMasking().getRules());
        
        return new DataMaskingServiceImpl(properties);
    }
}
