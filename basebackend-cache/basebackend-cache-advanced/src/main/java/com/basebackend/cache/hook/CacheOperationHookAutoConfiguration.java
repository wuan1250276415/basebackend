package com.basebackend.cache.hook;

import com.basebackend.cache.hotkey.HotKeyDetector;
import com.basebackend.cache.hotkey.HotKeyMitigator;
import com.basebackend.cache.invalidation.CacheInvalidationPublisher;
import com.basebackend.cache.refresh.NearExpiryRefreshManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@Slf4j
@AutoConfiguration
public class CacheOperationHookAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(CacheOperationHook.class)
    public CacheOperationHook cacheOperationHook(
            @Autowired(required = false) HotKeyDetector hotKeyDetector,
            @Autowired(required = false) HotKeyMitigator hotKeyMitigator,
            @Autowired(required = false) NearExpiryRefreshManager nearExpiryRefreshManager,
            @Autowired(required = false) CacheInvalidationPublisher cacheInvalidationPublisher) {
        log.info("Registering AdvancedCacheOperationHook: hotKey={}, refresh={}, invalidation={}",
                hotKeyDetector != null && hotKeyMitigator != null,
                nearExpiryRefreshManager != null,
                cacheInvalidationPublisher != null);
        return new AdvancedCacheOperationHook(
                hotKeyDetector,
                hotKeyMitigator,
                nearExpiryRefreshManager,
                cacheInvalidationPublisher);
    }
}
