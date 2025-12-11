package com.basebackend.featuretoggle.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 配置验证器单元测试
 */
@DisplayName("FeatureTogglePropertiesValidator 单元测试")
class FeatureTogglePropertiesValidatorTest {

    @Test
    @DisplayName("禁用时跳过验证")
    void shouldSkipValidationWhenDisabled() {
        FeatureToggleProperties props = new FeatureToggleProperties();
        props.setEnabled(false);

        var result = FeatureTogglePropertiesValidator.validate(props);
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("Unleash配置缺少URL时验证失败")
    void shouldFailWhenUnleashUrlMissing() {
        FeatureToggleProperties props = new FeatureToggleProperties();
        props.setEnabled(true);
        props.setProvider(FeatureToggleProperties.ProviderType.UNLEASH);
        props.getUnleash().setUrl("");
        props.getUnleash().setApiToken("valid-token");

        var result = FeatureTogglePropertiesValidator.validate(props);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("URL")));
    }

    @Test
    @DisplayName("Unleash配置缺少Token时验证失败")
    void shouldFailWhenUnleashTokenMissing() {
        FeatureToggleProperties props = new FeatureToggleProperties();
        props.setEnabled(true);
        props.setProvider(FeatureToggleProperties.ProviderType.UNLEASH);
        props.getUnleash().setUrl("http://localhost:4242/api");
        props.getUnleash().setApiToken("");

        var result = FeatureTogglePropertiesValidator.validate(props);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("token")));
    }

    @Test
    @DisplayName("Flagsmith配置缺少ApiKey时验证失败")
    void shouldFailWhenFlagsmithApiKeyMissing() {
        FeatureToggleProperties props = new FeatureToggleProperties();
        props.setEnabled(true);
        props.setProvider(FeatureToggleProperties.ProviderType.FLAGSMITH);
        props.getFlagsmith().setUrl("https://api.flagsmith.com");
        props.getFlagsmith().setApiKey("");

        var result = FeatureTogglePropertiesValidator.validate(props);
        assertFalse(result.isValid());
    }

    @Test
    @DisplayName("完整配置验证通过")
    void shouldPassWithValidConfig() {
        FeatureToggleProperties props = new FeatureToggleProperties();
        props.setEnabled(true);
        props.setProvider(FeatureToggleProperties.ProviderType.UNLEASH);
        props.getUnleash().setUrl("http://localhost:4242/api");
        props.getUnleash().setApiToken("valid-api-token-12345");

        var result = FeatureTogglePropertiesValidator.validate(props);
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("无效URL格式验证失败")
    void shouldFailWithInvalidUrl() {
        FeatureToggleProperties props = new FeatureToggleProperties();
        props.setEnabled(true);
        props.setProvider(FeatureToggleProperties.ProviderType.UNLEASH);
        props.getUnleash().setUrl("invalid-url");
        props.getUnleash().setApiToken("valid-token");

        var result = FeatureTogglePropertiesValidator.validate(props);
        assertFalse(result.isValid());
    }

    @Test
    @DisplayName("null配置验证失败")
    void shouldFailWithNullConfig() {
        var result = FeatureTogglePropertiesValidator.validate(null);
        assertFalse(result.isValid());
    }
}
