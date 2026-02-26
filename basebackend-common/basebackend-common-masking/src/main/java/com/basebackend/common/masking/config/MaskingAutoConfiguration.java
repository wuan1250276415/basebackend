package com.basebackend.common.masking.config;

import com.basebackend.common.masking.MaskType;
import com.basebackend.common.masking.MaskingStrategyRegistry;
import com.basebackend.common.masking.impl.*;
import com.basebackend.common.masking.jackson.MaskingStrategyRegistryHolder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(MaskingProperties.class)
@ConditionalOnProperty(prefix = "basebackend.common.masking", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MaskingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MaskingStrategyRegistry maskingStrategyRegistry() {
        MaskingStrategyRegistry registry = new MaskingStrategyRegistry();
        registry.register(MaskType.PHONE, new PhoneMaskingStrategy());
        registry.register(MaskType.EMAIL, new EmailMaskingStrategy());
        registry.register(MaskType.ID_CARD, new IdCardMaskingStrategy());
        registry.register(MaskType.BANK_CARD, new BankCardMaskingStrategy());
        registry.register(MaskType.ADDRESS, new AddressMaskingStrategy());
        MaskingStrategyRegistryHolder.setRegistry(registry);
        return registry;
    }
}
