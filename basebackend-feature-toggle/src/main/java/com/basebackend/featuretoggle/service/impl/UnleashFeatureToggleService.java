package com.basebackend.featuretoggle.service.impl;

import com.basebackend.featuretoggle.model.FeatureContext;
import com.basebackend.featuretoggle.model.Variant;
import com.basebackend.featuretoggle.service.FeatureToggleService;
import io.getunleash.Unleash;
import io.getunleash.UnleashContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Unleash特性开关服务实现
 *
 * @author BaseBackend
 */
@Slf4j
public class UnleashFeatureToggleService implements FeatureToggleService {

    private final Unleash unleash;

    public UnleashFeatureToggleService(Unleash unleash) {
        this.unleash = unleash;
        log.info("UnleashFeatureToggleService initialized");
    }

    @Override
    public boolean isEnabled(String featureName) {
        try {
            return unleash.isEnabled(featureName);
        } catch (Exception e) {
            log.error("Failed to check feature '{}' in Unleash: {}", featureName, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isEnabled(String featureName, FeatureContext context) {
        try {
            UnleashContext unleashContext = convertToUnleashContext(context);
            return unleash.isEnabled(featureName, unleashContext);
        } catch (Exception e) {
            log.error("Failed to check feature '{}' with context in Unleash: {}", featureName, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isEnabled(String featureName, boolean defaultValue) {
        try {
            return unleash.isEnabled(featureName, defaultValue);
        } catch (Exception e) {
            log.error("Failed to check feature '{}' in Unleash, returning default: {}", featureName, defaultValue);
            return defaultValue;
        }
    }

    @Override
    public boolean isEnabled(String featureName, FeatureContext context, boolean defaultValue) {
        try {
            UnleashContext unleashContext = convertToUnleashContext(context);
            return unleash.isEnabled(featureName, unleashContext, defaultValue);
        } catch (Exception e) {
            log.error("Failed to check feature '{}' with context in Unleash, returning default: {}", featureName, defaultValue);
            return defaultValue;
        }
    }

    @Override
    public Variant getVariant(String featureName, FeatureContext context) {
        return getVariant(featureName, context, Variant.defaultVariant());
    }

    @Override
    public Variant getVariant(String featureName, FeatureContext context, Variant defaultVariant) {
        try {
            UnleashContext unleashContext = convertToUnleashContext(context);
            io.getunleash.Variant unleashVariant = unleash.getVariant(featureName, unleashContext);

            if (!unleashVariant.isEnabled()) {
                return defaultVariant;
            }

            unleashVariant.getPayload();
            return Variant.builder()
                    .name(unleashVariant.getName())
                    .enabled(unleashVariant.isEnabled())
                    .payload(unleashVariant.getPayload().toString())
                    .build();
        } catch (Exception e) {
            log.error("Failed to get variant for feature '{}' in Unleash: {}", featureName, e.getMessage());
            return defaultVariant;
        }
    }

    @Override
    public Map<String, Boolean> getAllFeatureStates() {
        try {
            Map<String, Boolean> states = new HashMap<>();
            unleash.more().getFeatureToggleNames().forEach(name ->
                    states.put(name, unleash.isEnabled(name))
            );
            return states;
        } catch (Exception e) {
            log.error("Failed to get all feature states from Unleash: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    @Override
    public Map<String, Boolean> getAllFeatureStates(FeatureContext context) {
        try {
            UnleashContext unleashContext = convertToUnleashContext(context);
            Map<String, Boolean> states = new HashMap<>();
            unleash.more().getFeatureToggleNames().forEach(name ->
                    states.put(name, unleash.isEnabled(name, unleashContext))
            );
            return states;
        } catch (Exception e) {
            log.error("Failed to get all feature states with context from Unleash: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    @Override
    public void refresh() {
        log.info("Unleash client refreshes automatically, no manual refresh needed");
    }

    @Override
    public String getProviderName() {
        return "Unleash";
    }

    @Override
    public boolean isAvailable() {
        try {
            // 尝试获取特性开关列表来验证服务可用性
            unleash.more().getFeatureToggleNames();
            return true;
        } catch (Exception e) {
            log.warn("Unleash service is not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 转换为Unleash上下文
     */
    private UnleashContext convertToUnleashContext(FeatureContext context) {
        if (context == null) {
            return UnleashContext.builder().build();
        }

        UnleashContext.Builder builder = UnleashContext.builder();

        if (StringUtils.hasText(context.getUserId())) {
            builder.userId(context.getUserId());
        }
        if (StringUtils.hasText(context.getSessionId())) {
            builder.sessionId(context.getSessionId());
        }
        if (StringUtils.hasText(context.getIpAddress())) {
            builder.remoteAddress(context.getIpAddress());
        }
        if (StringUtils.hasText(context.getEnvironment())) {
            builder.environment(context.getEnvironment());
        }

        // 添加自定义属性
        if (context.getProperties() != null && !context.getProperties().isEmpty()) {
            context.getProperties().forEach(builder::addProperty);
        }

        return builder.build();
    }
}
