package com.basebackend.cache.hotkey;

import com.basebackend.cache.config.CacheProperties;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 热点 Key 检测与缓解自动配置
 * 创建 HotKeyDetector 和 HotKeyMitigator Bean
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "basebackend.cache.hot-key", name = "enabled", havingValue = "true")
public class HotKeyAutoConfiguration {

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "hotKeyScheduler")
    public ScheduledExecutorService hotKeyScheduler() {
        return Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "hotkey-window-rotator");
            t.setDaemon(true);
            return t;
        });
    }

    @Bean
    @ConditionalOnMissingBean
    public HotKeyDetector hotKeyDetector(
            CacheProperties cacheProperties,
            ScheduledExecutorService hotKeyScheduler,
            @Autowired(required = false) MeterRegistry meterRegistry) {
        log.info("Registering HotKeyDetector: windowSize={}, threshold={}, topK={}",
                cacheProperties.getHotKey().getWindowSize(),
                cacheProperties.getHotKey().getThreshold(),
                cacheProperties.getHotKey().getTopK());
        return new HotKeyDetector(cacheProperties, hotKeyScheduler, meterRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public HotKeyMitigator hotKeyMitigator(
            HotKeyDetector hotKeyDetector,
            CacheProperties cacheProperties,
            @Autowired(required = false) MeterRegistry meterRegistry) {
        log.info("Registering HotKeyMitigator: localCacheMaxSize={}, localCacheTtl={}",
                cacheProperties.getHotKey().getLocalCacheMaxSize(),
                cacheProperties.getHotKey().getLocalCacheTtl());
        return new HotKeyMitigator(hotKeyDetector, cacheProperties, meterRegistry);
    }
}
