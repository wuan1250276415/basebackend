package com.basebackend.featuretoggle.service.impl;

import com.basebackend.featuretoggle.model.FeatureContext;
import com.basebackend.featuretoggle.model.Variant;
import com.basebackend.featuretoggle.service.FeatureToggleService;
import com.flagsmith.FlagsmithClient;
import com.flagsmith.exceptions.FlagsmithClientError;
import com.flagsmith.models.Flags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Flagsmith特性开关服务实现
 *
 * @author BaseBackend
 */
@Slf4j
public class FlagsmithFeatureToggleService implements FeatureToggleService {

    private final FlagsmithClient flagsmithClient;

    public FlagsmithFeatureToggleService(FlagsmithClient flagsmithClient) {
        this.flagsmithClient = flagsmithClient;
        log.info("FlagsmithFeatureToggleService initialized");
    }

    @Override
    public boolean isEnabled(String featureName) {
        try {
            Flags flags = flagsmithClient.getEnvironmentFlags();
            return flags.isFeatureEnabled(featureName);
        } catch (FlagsmithClientError e) {
            log.error("Failed to check feature '{}' in Flagsmith: {}", featureName, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isEnabled(String featureName, FeatureContext context) {
        try {
            if (context == null || !StringUtils.hasText(context.getUserId())) {
                return isEnabled(featureName);
            }

            Flags flags = flagsmithClient.getIdentityFlags(context.getUserId(), buildTraits(context));
            return flags.isFeatureEnabled(featureName);
        } catch (FlagsmithClientError e) {
            log.error("Failed to check feature '{}' with context in Flagsmith: {}", featureName, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isEnabled(String featureName, boolean defaultValue) {
        try {
            return isEnabled(featureName);
        } catch (Exception e) {
            log.error("Failed to check feature '{}' in Flagsmith, returning default: {}", featureName, defaultValue);
            return defaultValue;
        }
    }

    @Override
    public boolean isEnabled(String featureName, FeatureContext context, boolean defaultValue) {
        try {
            return isEnabled(featureName, context);
        } catch (Exception e) {
            log.error("Failed to check feature '{}' with context in Flagsmith, returning default: {}", featureName, defaultValue);
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
            Flags flags;
            if (context != null && StringUtils.hasText(context.getUserId())) {
                flags = flagsmithClient.getIdentityFlags(context.getUserId(), buildTraits(context));
            } else {
                flags = flagsmithClient.getEnvironmentFlags();
            }

            boolean isEnabled = flags.isFeatureEnabled(featureName);
            String featureValue = flags.getFeatureValue(featureName);

            if (!isEnabled) {
                return defaultVariant;
            }

            // Flagsmith的变体信息包含在feature value中
            return Variant.builder()
                    .name(StringUtils.hasText(featureValue) ? featureValue : "default")
                    .enabled(true)
                    .payload(featureValue)
                    .build();
        } catch (FlagsmithClientError e) {
            log.error("Failed to get variant for feature '{}' in Flagsmith: {}", featureName, e.getMessage());
            return defaultVariant;
        }
    }

    @Override
    public Map<String, Boolean> getAllFeatureStates() {
        try {
            Flags flags = flagsmithClient.getEnvironmentFlags();
            Map<String, Boolean> states = new HashMap<>();
            flags.getAllFlags().forEach(flag ->
                    states.put(flag.getFeature().getName(), flag.isEnabled())
            );
            return states;
        } catch (FlagsmithClientError e) {
            log.error("Failed to get all feature states from Flagsmith: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    @Override
    public Map<String, Boolean> getAllFeatureStates(FeatureContext context) {
        try {
            Flags flags;
            if (context != null && StringUtils.hasText(context.getUserId())) {
                flags = flagsmithClient.getIdentityFlags(context.getUserId(), buildTraits(context));
            } else {
                flags = flagsmithClient.getEnvironmentFlags();
            }

            Map<String, Boolean> states = new HashMap<>();
            flags.getAllFlags().forEach(flag ->
                    states.put(flag.getFeature().getName(), flag.isEnabled())
            );
            return states;
        } catch (FlagsmithClientError e) {
            log.error("Failed to get all feature states with context from Flagsmith: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    @Override
    public void refresh() {
        log.info("Flagsmith client refreshes automatically, no manual refresh needed");
    }

    @Override
    public String getProviderName() {
        return "Flagsmith";
    }

    @Override
    public boolean isAvailable() {
        try {
            flagsmithClient.getEnvironmentFlags();
            return true;
        } catch (FlagsmithClientError e) {
            log.warn("Flagsmith service is not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 构建用户特征（Traits）
     */
    private Map<String, Object> buildTraits(FeatureContext context) {
        Map<String, Object> traits = new HashMap<>();

        if (StringUtils.hasText(context.getUsername())) {
            traits.put("username", context.getUsername());
        }
        if (StringUtils.hasText(context.getEmail())) {
            traits.put("email", context.getEmail());
        }
        if (StringUtils.hasText(context.getIpAddress())) {
            traits.put("ip_address", context.getIpAddress());
        }
        if (StringUtils.hasText(context.getEnvironment())) {
            traits.put("environment", context.getEnvironment());
        }

        // 添加自定义属性
        if (context.getProperties() != null) {
            traits.putAll(context.getProperties());
        }

        return traits;
    }
}
