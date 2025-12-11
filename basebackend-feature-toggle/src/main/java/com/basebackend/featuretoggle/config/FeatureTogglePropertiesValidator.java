package com.basebackend.featuretoggle.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.bind.validation.ValidationErrors;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 特性开关配置验证器
 * <p>
 * 验证配置的完整性和正确性，在应用启动时进行检查。
 * </p>
 *
 * @author BaseBackend
 */
@Slf4j
public class FeatureTogglePropertiesValidator {

    /**
     * 验证配置
     *
     * @param properties 配置属性
     * @return 验证结果
     */
    public static ValidationResult validate(FeatureToggleProperties properties) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (properties == null) {
            errors.add("FeatureToggleProperties is null");
            return new ValidationResult(false, errors, warnings);
        }

        // 如果未启用，跳过验证
        if (!properties.isEnabled()) {
            log.info("Feature toggle is disabled, skipping validation");
            return new ValidationResult(true, errors, warnings);
        }

        // 验证提供商配置
        FeatureToggleProperties.ProviderType provider = properties.getProvider();
        if (provider == null) {
            errors.add("Provider type is not specified");
        } else {
            switch (provider) {
                case UNLEASH:
                    validateUnleashConfig(properties.getUnleash(), errors, warnings);
                    break;
                case FLAGSMITH:
                    validateFlagsmithConfig(properties.getFlagsmith(), errors, warnings);
                    break;
                case BOTH:
                    validateUnleashConfig(properties.getUnleash(), errors, warnings);
                    validateFlagsmithConfig(properties.getFlagsmith(), errors, warnings);
                    break;
            }
        }

        // 验证缓存配置
        validateCacheConfig(properties.getCache(), warnings);

        boolean isValid = errors.isEmpty();
        if (!isValid) {
            log.error("Feature toggle configuration validation failed: {}", errors);
        }
        if (!warnings.isEmpty()) {
            log.warn("Feature toggle configuration warnings: {}", warnings);
        }

        return new ValidationResult(isValid, errors, warnings);
    }

    /**
     * 验证 Unleash 配置
     */
    private static void validateUnleashConfig(FeatureToggleProperties.UnleashConfig config,
                                              List<String> errors, List<String> warnings) {
        if (config == null) {
            errors.add("Unleash configuration is missing");
            return;
        }

        // 验证 URL
        if (!StringUtils.hasText(config.getUrl())) {
            errors.add("Unleash URL is required");
        } else if (!isValidUrl(config.getUrl())) {
            errors.add("Unleash URL is invalid: " + config.getUrl());
        }

        // 验证 API Token
        if (!StringUtils.hasText(config.getApiToken())) {
            errors.add("Unleash API token is required");
        } else if (config.getApiToken().length() < 10) {
            warnings.add("Unleash API token seems too short");
        }

        // 验证应用名称
        if (!StringUtils.hasText(config.getAppName())) {
            warnings.add("Unleash app name is not set, using default");
        }

        // 验证同步间隔
        if (config.getFetchTogglesInterval() < 1) {
            warnings.add("Unleash fetch interval is too small, recommend >= 5 seconds");
        }
    }

    /**
     * 验证 Flagsmith 配置
     */
    private static void validateFlagsmithConfig(FeatureToggleProperties.FlagsmithConfig config,
                                                List<String> errors, List<String> warnings) {
        if (config == null) {
            errors.add("Flagsmith configuration is missing");
            return;
        }

        // 验证 URL
        if (!StringUtils.hasText(config.getUrl())) {
            errors.add("Flagsmith URL is required");
        } else if (!isValidUrl(config.getUrl())) {
            errors.add("Flagsmith URL is invalid: " + config.getUrl());
        }

        // 验证 API Key
        if (!StringUtils.hasText(config.getApiKey())) {
            errors.add("Flagsmith API key is required");
        }

        // 验证超时配置
        if (config.getConnectTimeout() < 100) {
            warnings.add("Flagsmith connect timeout is very small: " + config.getConnectTimeout() + "ms");
        }
        if (config.getReadTimeout() < 100) {
            warnings.add("Flagsmith read timeout is very small: " + config.getReadTimeout() + "ms");
        }
    }

    /**
     * 验证缓存配置
     */
    private static void validateCacheConfig(FeatureToggleProperties.CacheConfig config, List<String> warnings) {
        if (config == null) {
            return;
        }

        if (config.getMaxSize() < 100) {
            warnings.add("Cache max size is very small: " + config.getMaxSize());
        }

        if (config.getExpireAfterWrite() < 10) {
            warnings.add("Cache expire after write is very short: " + config.getExpireAfterWrite() + "s");
        }
    }

    /**
     * 简单的 URL 验证
     */
    private static boolean isValidUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        return url.startsWith("http://") || url.startsWith("https://");
    }

    /**
     * 验证结果
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;

        public ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
            this.valid = valid;
            this.errors = errors != null ? errors : new ArrayList<>();
            this.warnings = warnings != null ? warnings : new ArrayList<>();
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        @Override
        public String toString() {
            return "ValidationResult{valid=" + valid + ", errors=" + errors + ", warnings=" + warnings + "}";
        }
    }
}
