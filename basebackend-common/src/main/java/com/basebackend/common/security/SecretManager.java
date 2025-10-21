package com.basebackend.common.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 统一的密钥、证书和令牌管理器，支持环境变量读取与缓存
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecretManager {

    private final ConfigurableEnvironment environment;
    private final SecurityBaselineProperties properties;
    private final Map<String, SecretHolder> cache = new ConcurrentHashMap<>();

    public Optional<String> getSecret(String key) {
        return getSecret(key, null);
    }

    public Optional<String> getSecret(String key, Supplier<String> fallback) {
        SecretHolder holder = cache.get(key);
        if (holder != null && !holder.isExpired()) {
            return Optional.of(holder.value());
        }

        String value = resolveFromEnvironment(key);
        if (StringUtils.isBlank(value) && fallback != null) {
            value = StringUtils.trimToNull(fallback.get());
        }

        if (StringUtils.isBlank(value)) {
            return Optional.empty();
        }

        cache.put(key, new SecretHolder(value, Instant.now().plus(properties.getSecretCacheTtl())));
        return Optional.of(value);
    }

    public String getRequiredSecret(String key, Supplier<String> fallback) {
        return getSecret(key, fallback)
                .orElseThrow(() -> new IllegalStateException("Missing required secret: " + key));
    }

    public String getRequiredSecret(String key) {
        return getRequiredSecret(key, null);
    }

    public void refreshSecret(String key) {
        cache.remove(key);
    }

    private String resolveFromEnvironment(String key) {
        String value = environment.getProperty(key);
        if (StringUtils.isNotBlank(value)) {
            return value;
        }

        String kebabKey = key.replace('.', '-');
        value = environment.getProperty(kebabKey);
        if (StringUtils.isNotBlank(value)) {
            return value;
        }

        String envKey = key.toUpperCase(Locale.ROOT).replace('.', '_');
        value = System.getenv(envKey);
        if (StringUtils.isNotBlank(value)) {
            return value;
        }

        String fileProperty = Optional.ofNullable(environment.getProperty(key + ".file"))
                .orElse(environment.getProperty(key + ".path"));
        if (StringUtils.isBlank(fileProperty)) {
            fileProperty = System.getenv(envKey + "_FILE");
        }
        if (StringUtils.isNotBlank(fileProperty)) {
            try {
                return Files.readString(Path.of(fileProperty), StandardCharsets.UTF_8).trim();
            } catch (IOException e) {
                log.error("Failed to read secret file for key {}", key, e);
            }
        }

        log.debug("Secret {} not found in environment", key);
        return null;
    }

    private record SecretHolder(String value, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
