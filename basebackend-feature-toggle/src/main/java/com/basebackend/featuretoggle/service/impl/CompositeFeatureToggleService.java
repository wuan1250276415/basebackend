package com.basebackend.featuretoggle.service.impl;

import com.basebackend.featuretoggle.model.FeatureContext;
import com.basebackend.featuretoggle.model.Variant;
import com.basebackend.featuretoggle.service.FeatureToggleService;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.function.Supplier;

/**
 * 复合特性开关服务
 * <p>
 * 组合多个特性开关提供商，实现Failover机制。
 * 优先使用主服务，当主服务不可用时自动降级到备服务。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
public class CompositeFeatureToggleService implements FeatureToggleService {

    private final FeatureToggleService primaryService;
    private final FeatureToggleService secondaryService;

    public CompositeFeatureToggleService(FeatureToggleService primaryService, FeatureToggleService secondaryService) {
        this.primaryService = primaryService;
        this.secondaryService = secondaryService;
        log.info("Created CompositeFeatureToggleService with primary={}, secondary={}",
                primaryService.getClass().getSimpleName(),
                secondaryService.getClass().getSimpleName());
    }

    @Override
    public boolean isEnabled(String featureName) {
        return executeBooleanWithFailover(() -> primaryService.isEnabled(featureName),
                                          () -> secondaryService.isEnabled(featureName),
                                          featureName, "isEnabled");
    }

    @Override
    public boolean isEnabled(String featureName, FeatureContext context) {
        return executeBooleanWithFailover(() -> primaryService.isEnabled(featureName, context),
                                  () -> secondaryService.isEnabled(featureName, context),
                                  featureName, "isEnabled");
    }

    @Override
    public boolean isEnabled(String featureName, boolean defaultValue) {
        return executeBooleanWithFailover(() -> primaryService.isEnabled(featureName, defaultValue),
                                  () -> secondaryService.isEnabled(featureName, defaultValue),
                                  featureName, "isEnabled");
    }

    @Override
    public boolean isEnabled(String featureName, FeatureContext context, boolean defaultValue) {
        return executeBooleanWithFailover(() -> primaryService.isEnabled(featureName, context, defaultValue),
                                  () -> secondaryService.isEnabled(featureName, context, defaultValue),
                                  featureName, "isEnabled");
    }

    @Override
    public Variant getVariant(String featureName, FeatureContext context) {
        return executeVariantWithFailover(() -> primaryService.getVariant(featureName, context),
                                          () -> secondaryService.getVariant(featureName, context),
                                          featureName, "getVariant");
    }

    @Override
    public Variant getVariant(String featureName, FeatureContext context, Variant defaultVariant) {
        return executeVariantWithFailover(() -> primaryService.getVariant(featureName, context, defaultVariant),
                                          () -> secondaryService.getVariant(featureName, context, defaultVariant),
                                          featureName, "getVariant");
    }

    @Override
    public Map<String, Boolean> getAllFeatureStates() {
        return executeMapWithFailover(() -> primaryService.getAllFeatureStates(),
                                      () -> secondaryService.getAllFeatureStates(),
                                      "all", "getAllFeatureStates");
    }

    @Override
    public Map<String, Boolean> getAllFeatureStates(FeatureContext context) {
        return executeMapWithFailover(() -> primaryService.getAllFeatureStates(context),
                                      () -> secondaryService.getAllFeatureStates(context),
                                      "all", "getAllFeatureStates");
    }

    @Override
    public void refresh() {
        try {
            primaryService.refresh();
            log.info("Primary service refreshed successfully");
        } catch (Exception e) {
            log.warn("Failed to refresh primary service", e);
        }

        try {
            secondaryService.refresh();
            log.info("Secondary service refreshed successfully");
        } catch (Exception e) {
            log.warn("Failed to refresh secondary service", e);
        }
    }

    @Override
    public String getProviderName() {
        return String.format("Composite[%s + %s]",
                primaryService.getProviderName(),
                secondaryService.getProviderName());
    }

    @Override
    public boolean isAvailable() {
        return primaryService.isAvailable() || secondaryService.isAvailable();
    }

    /**
     * 执行带Failover的逻辑
     */
    private boolean executeBooleanWithFailover(Supplier<Boolean> primary,
                                               Supplier<Boolean> secondary,
                                               String featureName, String operation) {
        try {
            if (primaryService.isAvailable()) {
                boolean result = primary.get();
                log.debug("Operation '{}' succeeded on primary service for feature '{}'",
                        operation, featureName);
                return result;
            } else {
                log.warn("Primary service not available, trying secondary service");
            }
        } catch (Exception e) {
            log.error("Primary service failed for operation '{}' on feature '{}': {}",
                    operation, featureName, e.getMessage(), e);
        }

        // 降级到备服务
        try {
            if (secondaryService.isAvailable()) {
                boolean result = secondary.get();
                log.warn("Operation '{}' executed on secondary service for feature '{}' (primary failed)",
                        operation, featureName);
                return result;
            } else {
                log.error("Both primary and secondary services are not available for feature '{}'", featureName);
            }
        } catch (Exception e) {
            log.error("Secondary service also failed for operation '{}' on feature '{}': {}",
                    operation, featureName, e.getMessage(), e);
        }

        // 所有服务都不可用，返回默认值
        log.error("All services failed for operation '{}' on feature '{}', returning default false",
                operation, featureName);
        return false;
    }

    /**
     * 执行带Failover的逻辑（Map返回类型）
     */
    private Map<String, Boolean> executeMapWithFailover(Supplier<Map<String, Boolean>> primary,
                                                        Supplier<Map<String, Boolean>> secondary,
                                                        String featureName, String operation) {
        try {
            if (primaryService.isAvailable()) {
                Map<String, Boolean> result = primary.get();
                log.debug("Operation '{}' succeeded on primary service", operation);
                return result;
            } else {
                log.warn("Primary service not available, trying secondary service");
            }
        } catch (Exception e) {
            log.error("Primary service failed for operation '{}': {}", operation, e.getMessage(), e);
        }

        // 降级到备服务
        try {
            if (secondaryService.isAvailable()) {
                Map<String, Boolean> result = secondary.get();
                log.warn("Operation '{}' executed on secondary service (primary failed)", operation);
                return result;
            } else {
                log.error("Both primary and secondary services are not available");
            }
        } catch (Exception e) {
            log.error("Secondary service also failed for operation '{}': {}", operation, e.getMessage(), e);
        }

        // 所有服务都不可用，返回空Map
        log.error("All services failed for operation '{}', returning empty map", operation);
        return Map.of();
    }

    /**
     * 执行带Failover的逻辑（Variant返回类型）
     */
    private Variant executeVariantWithFailover(Supplier<Variant> primary, Supplier<Variant> secondary,
                                               String featureName, String operation) {
        try {
            if (primaryService.isAvailable()) {
                Variant result = primary.get();
                log.debug("Operation '{}' succeeded on primary service for feature '{}'",
                        operation, featureName);
                return result;
            } else {
                log.warn("Primary service not available, trying secondary service");
            }
        } catch (Exception e) {
            log.error("Primary service failed for operation '{}' on feature '{}': {}",
                    operation, featureName, e.getMessage(), e);
        }

        // 降级到备服务
        try {
            if (secondaryService.isAvailable()) {
                Variant result = secondary.get();
                log.warn("Operation '{}' executed on secondary service for feature '{}' (primary failed)",
                        operation, featureName);
                return result;
            } else {
                log.error("Both primary and secondary services are not available for feature '{}'", featureName);
            }
        } catch (Exception e) {
            log.error("Secondary service also failed for operation '{}' on feature '{}': {}",
                    operation, featureName, e.getMessage(), e);
        }

        // 所有服务都不可用，返回默认Variant
        log.error("All services failed for operation '{}' on feature '{}', returning default variant",
                operation, featureName);
        return Variant.defaultVariant();
    }
}
