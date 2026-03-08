package com.basebackend.cache.invalidation;

import com.basebackend.common.util.JsonUtils;
import com.basebackend.cache.config.CacheProperties;
import com.basebackend.cache.manager.MultiLevelCacheManager;
import com.basebackend.cache.service.CacheService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;

/**
 * 跨服务缓存失效监听器
 * 订阅 Redis Pub/Sub 通道，接收其他服务发布的失效事件并应用到本地
 *
 * 行为：
 * 1. 反序列化 CacheInvalidationEvent
 * 2. 跳过自身发布的事件（防止自失效循环）
 * 3. 执行对应的缓存失效操作
 * 4. 如果启用了多级缓存，同时失效本地 Caffeine
 */
@Slf4j
public class CacheInvalidationListener implements MessageListener {

    private static final String SIGNATURE_ALGORITHM = "HmacSHA256";
    private static final long DEFAULT_TIME_WINDOW_MILLIS = Duration.ofMinutes(5).toMillis();

    private final CacheService cacheService;
    private final CacheProperties cacheProperties;
    private final MultiLevelCacheManager multiLevelCacheManager;
    private final MeterRegistry meterRegistry;

    public CacheInvalidationListener(
            CacheService cacheService,
            CacheProperties cacheProperties,
            @Autowired(required = false) MultiLevelCacheManager multiLevelCacheManager,
            @Autowired(required = false) MeterRegistry meterRegistry) {
        this.cacheService = cacheService;
        this.cacheProperties = cacheProperties;
        this.multiLevelCacheManager = multiLevelCacheManager;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body;
        try {
            body = new String(message.getBody(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Failed to read invalidation message body", e);
            return;
        }

        CacheInvalidationEvent event;
        try {
            event = JsonUtils.parseObject(body, CacheInvalidationEvent.class);
        } catch (Exception e) {
            log.warn("Failed to deserialize invalidation event: {}", body, e);
            return;
        }

        if (event == null || event.getType() == null) {
            log.warn("Received invalid invalidation event: {}", body);
            return;
        }

        // 安全校验：先做时间窗与签名校验，再执行业务失效
        if (!authenticate(event)) {
            return;
        }

        // 跳过自身发布的事件
        String selfName = cacheProperties.getInvalidation().getServiceName();
        if (selfName.equals(event.getSource())) {
            recordSkippedSelf();
            log.debug("Skipping self-origin invalidation event: correlationId={}", event.getCorrelationId());
            return;
        }

        log.info("Received invalidation event: source={}, type={}, cacheName={}, key={}",
                event.getSource(), event.getType(), event.getCacheName(), event.getKeyPattern());

        applyInvalidation(event);
        recordReceived(event);
    }

    private boolean authenticate(CacheInvalidationEvent event) {
        CacheProperties.Invalidation config = cacheProperties.getInvalidation();
        if (!isTimestampWithinWindow(event, config)) {
            log.warn("Rejected invalidation event due to timestamp window validation failure: source={}, correlationId={}, timestamp={}",
                    event.getSource(), event.getCorrelationId(), event.getTimestamp());
            return false;
        }

        if (!config.isSignatureEnabled()) {
            return true;
        }

        String secret = config.getSignatureSecret();
        if (secret == null || secret.isBlank()) {
            log.error("Rejected invalidation event because signature is enabled but secret is empty: source={}, correlationId={}",
                    event.getSource(), event.getCorrelationId());
            return false;
        }

        String incomingSignature = event.getSignature();
        if (incomingSignature == null || incomingSignature.isBlank()) {
            if (config.isAllowUnsignedLegacy()) {
                log.warn("Accepting unsigned legacy invalidation event because allowUnsignedLegacy=true: source={}, correlationId={}",
                        event.getSource(), event.getCorrelationId());
                return true;
            }
            log.warn("Rejected invalidation event because signature is missing: source={}, correlationId={}",
                    event.getSource(), event.getCorrelationId());
            return false;
        }

        String expectedSignature = signEvent(event, secret);
        if (expectedSignature == null) {
            log.error("Rejected invalidation event because signature verification failed internally: source={}, correlationId={}",
                    event.getSource(), event.getCorrelationId());
            return false;
        }

        boolean verified = MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                incomingSignature.getBytes(StandardCharsets.UTF_8));
        if (!verified) {
            log.warn("Rejected invalidation event due to signature mismatch: source={}, correlationId={}, cacheName={}, key={}",
                    event.getSource(), event.getCorrelationId(), event.getCacheName(), event.getKeyPattern());
            return false;
        }

        return true;
    }

    private boolean isTimestampWithinWindow(CacheInvalidationEvent event, CacheProperties.Invalidation config) {
        long timestamp = event.getTimestamp();
        if (timestamp <= 0) {
            return false;
        }

        Duration timeWindow = config.getSignatureTimeWindow();
        long windowMillis = timeWindow == null ? DEFAULT_TIME_WINDOW_MILLIS : timeWindow.toMillis();
        if (windowMillis <= 0) {
            log.warn("Configured signatureTimeWindow is invalid (<=0), fallback to {} ms", DEFAULT_TIME_WINDOW_MILLIS);
            windowMillis = DEFAULT_TIME_WINDOW_MILLIS;
        }

        long drift = Math.abs(System.currentTimeMillis() - timestamp);
        if (drift > windowMillis) {
            log.warn("Invalidation event timestamp drift exceeded window: drift={}ms, window={}ms, source={}, correlationId={}",
                    drift, windowMillis, event.getSource(), event.getCorrelationId());
            return false;
        }

        return true;
    }

    private String signEvent(CacheInvalidationEvent event, String secret) {
        try {
            Mac mac = Mac.getInstance(SIGNATURE_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), SIGNATURE_ALGORITHM);
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(event.buildSignaturePayload().getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("Failed to build expected invalidation signature: source={}, correlationId={}",
                    event.getSource(), event.getCorrelationId(), e);
            return null;
        }
    }

    private void applyInvalidation(CacheInvalidationEvent event) {
        try {
            switch (event.getType()) {
                case EVICT -> {
                    cacheService.delete(event.getKeyPattern());
                    evictFromLocal(event.getKeyPattern());
                }
                case CLEAR -> {
                    cacheService.clearCache(event.getCacheName());
                    evictAllFromLocal();
                }
                case CLEAR_ALL -> {
                    // 该路径是受控的跨服务失效事件，已通过签名与时间窗校验
                    cacheService.clearAllCaches(true);
                    evictAllFromLocal();
                }
            }
        } catch (Exception e) {
            log.error("Failed to apply invalidation event: type={}, cacheName={}, key={}",
                    event.getType(), event.getCacheName(), event.getKeyPattern(), e);
        }
    }

    private void evictFromLocal(String key) {
        if (multiLevelCacheManager != null) {
            multiLevelCacheManager.evict(key);
        }
    }

    private void evictAllFromLocal() {
        if (multiLevelCacheManager != null) {
            multiLevelCacheManager.clear();
        }
    }

    private void recordReceived(CacheInvalidationEvent event) {
        if (meterRegistry == null) {
            return;
        }
        Counter.builder("cache.invalidation.received")
                .tag("cacheName", event.getCacheName())
                .tag("type", event.getType().name())
                .tag("source", event.getSource())
                .register(meterRegistry)
                .increment();
    }

    private void recordSkippedSelf() {
        if (meterRegistry == null) {
            return;
        }
        Counter.builder("cache.invalidation.skipped.self")
                .register(meterRegistry)
                .increment();
    }
}
