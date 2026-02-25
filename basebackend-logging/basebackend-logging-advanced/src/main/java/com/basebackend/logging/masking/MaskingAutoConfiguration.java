package com.basebackend.logging.masking;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * PII脱敏系统自动配置
 *
 * 当ObjectMapper类存在且配置了basebackend.logging.masking.enabled=true时生效。
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@AutoConfiguration
@EnableConfigurationProperties(MaskingProperties.class)
@ConditionalOnClass(ObjectMapper.class)
@ConditionalOnProperty(prefix = "basebackend.logging.masking", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MaskingAutoConfiguration {

    /**
     * 配置脱敏指标
     */
    @Bean
    @ConditionalOnMissingBean
    public MaskingMetrics maskingMetrics() {
        return new MaskingMetrics();
    }

    /**
     * 配置脱敏服务
     */
    @Bean
    @ConditionalOnMissingBean
    public PiiMaskingService piiMaskingService(MaskingProperties properties,
            MaskingMetrics metrics,
            ObjectMapper objectMapper) {
        PiiMaskingService svc = new PiiMaskingService(properties, metrics, objectMapper);
        MaskingServiceHolder.set(svc);
        return svc;
    }

    /**
     * 配置脱敏切面
     */
    @Bean(name = "loggingPiiMaskingAspect")
    @ConditionalOnMissingBean(name = "loggingPiiMaskingAspect")
    public PiiMaskingAspect piiMaskingAspect(PiiMaskingService service, MaskingProperties properties) {
        return new PiiMaskingAspect(service, properties);
    }
}
