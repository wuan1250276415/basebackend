package com.basebackend.featuretoggle.config;

import com.basebackend.featuretoggle.service.FeatureToggleService;
import com.basebackend.featuretoggle.service.impl.FlagsmithFeatureToggleService;
import com.basebackend.featuretoggle.service.impl.UnleashFeatureToggleService;
import com.flagsmith.FlagsmithClient;
import io.getunleash.DefaultUnleash;
import io.getunleash.Unleash;
import io.getunleash.util.UnleashConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * 特性开关自动配置类
 *
 * @author BaseBackend
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(FeatureToggleProperties.class)
@ConditionalOnProperty(prefix = "feature-toggle", name = "enabled", havingValue = "true")
public class FeatureToggleAutoConfiguration {

    private final FeatureToggleProperties properties;

    public FeatureToggleAutoConfiguration(FeatureToggleProperties properties) {
        this.properties = properties;
        log.info("FeatureToggle enabled with provider: {}", properties.getProvider());
    }

    /**
     * 配置Unleash客户端
     */
    @Bean
    @ConditionalOnProperty(prefix = "feature-toggle", name = "provider", havingValue = "UNLEASH", matchIfMissing = true)
    @ConditionalOnMissingBean(Unleash.class)
    public Unleash unleashClient() {
        FeatureToggleProperties.UnleashConfig config = properties.getUnleash();

        if (!StringUtils.hasText(config.getApiToken())) {
            log.warn("Unleash API token is not configured, using mock mode");
            // 返回一个基本的Unleash客户端（所有特性默认禁用）
        }

        UnleashConfig.Builder builder = UnleashConfig.builder()
                .appName(config.getAppName())
                .unleashAPI(config.getUrl())
                .apiKey(config.getApiToken())
                .environment(config.getEnvironment())
                .fetchTogglesInterval(config.getFetchTogglesInterval())
                .sendMetricsInterval(config.getSendMetricsInterval())
                .synchronousFetchOnInitialisation(config.isSynchronousFetchOnInitialisation());

        if (StringUtils.hasText(config.getInstanceId())) {
            builder.instanceId(config.getInstanceId());
        }

        log.info("Initializing Unleash client: url={}, appName={}, environment={}",
                config.getUrl(), config.getAppName(), config.getEnvironment());

        return new DefaultUnleash(builder.build());
    }

    /**
     * 配置Flagsmith客户端
     */
    @Bean
    @ConditionalOnProperty(prefix = "feature-toggle", name = "provider", havingValue = "FLAGSMITH")
    @ConditionalOnMissingBean(FlagsmithClient.class)
    public FlagsmithClient flagsmithClient() {
        FeatureToggleProperties.FlagsmithConfig config = properties.getFlagsmith();

        if (!StringUtils.hasText(config.getApiKey())) {
            throw new IllegalStateException("Flagsmith API key is required when provider is FLAGSMITH");
        }

        log.info("Initializing Flagsmith client: url={}", config.getUrl());

        return FlagsmithClient.newBuilder()
                .setApiKey(config.getApiKey())
                .withApiUrl(config.getUrl())
                .build();
    }

    /**
     * 配置Unleash服务
     */
    @Bean
    @ConditionalOnProperty(prefix = "feature-toggle", name = "provider", havingValue = "UNLEASH", matchIfMissing = true)
    @ConditionalOnMissingBean(name = "unleashFeatureToggleService")
    public FeatureToggleService unleashFeatureToggleService(Unleash unleash) {
        log.info("Creating UnleashFeatureToggleService");
        return new UnleashFeatureToggleService(unleash);
    }

    /**
     * 配置Flagsmith服务
     */
    @Bean
    @ConditionalOnProperty(prefix = "feature-toggle", name = "provider", havingValue = "FLAGSMITH")
    @ConditionalOnMissingBean(name = "flagsmithFeatureToggleService")
    public FeatureToggleService flagsmithFeatureToggleService(FlagsmithClient flagsmithClient) {
        log.info("Creating FlagsmithFeatureToggleService");
        return new FlagsmithFeatureToggleService(flagsmithClient);
    }

    /**
     * 同时配置两个提供商时，创建组合服务
     */
    @Bean
    @ConditionalOnProperty(prefix = "feature-toggle", name = "provider", havingValue = "BOTH")
    @ConditionalOnMissingBean(FeatureToggleService.class)
    public FeatureToggleService compositeFeatureToggleService() {
        log.info("Creating Composite FeatureToggleService with both Unleash and Flagsmith");

        // 创建两个客户端
        Unleash unleash = unleashClient();
        FlagsmithClient flagsmith = flagsmithClient();

        // 根据primaryProvider决定使用哪个作为主服务
        FeatureToggleService primary = properties.getPrimaryProvider() == FeatureToggleProperties.ProviderType.UNLEASH
                ? new UnleashFeatureToggleService(unleash)
                : new FlagsmithFeatureToggleService(flagsmith);

        FeatureToggleService secondary = properties.getPrimaryProvider() == FeatureToggleProperties.ProviderType.UNLEASH
                ? new FlagsmithFeatureToggleService(flagsmith)
                : new UnleashFeatureToggleService(unleash);

        return new CompositeFeatureToggleService(primary, secondary);
    }
}
