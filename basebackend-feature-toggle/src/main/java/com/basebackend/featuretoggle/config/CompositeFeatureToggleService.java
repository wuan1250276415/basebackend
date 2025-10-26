package com.basebackend.featuretoggle.config;

import com.basebackend.featuretoggle.model.FeatureContext;
import com.basebackend.featuretoggle.model.Variant;
import com.basebackend.featuretoggle.service.FeatureToggleService;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 组合特性开关服务
 * 同时支持Unleash和Flagsmith，优先使用主提供商，失败时回退到备用提供商
 *
 * @author BaseBackend
 */
@Slf4j
public class CompositeFeatureToggleService implements FeatureToggleService {

    private final FeatureToggleService primaryService;
    private final FeatureToggleService secondaryService;

    public CompositeFeatureToggleService(FeatureToggleService primaryService, FeatureToggleService secondaryService) {
        this.primaryService = primaryService;
        this.secondaryService = secondaryService;
        log.info("CompositeFeatureToggleService initialized: primary={}, secondary={}",
                primaryService.getProviderName(), secondaryService.getProviderName());
    }

    @Override
    public boolean isEnabled(String featureName) {
        try {
            return primaryService.isEnabled(featureName);
        } catch (Exception e) {
            log.warn("Primary service ({}) failed for feature '{}', falling back to secondary ({})",
                    primaryService.getProviderName(), featureName, secondaryService.getProviderName());
            return secondaryService.isEnabled(featureName);
        }
    }

    @Override
    public boolean isEnabled(String featureName, FeatureContext context) {
        try {
            return primaryService.isEnabled(featureName, context);
        } catch (Exception e) {
            log.warn("Primary service ({}) failed for feature '{}' with context, falling back to secondary ({})",
                    primaryService.getProviderName(), featureName, secondaryService.getProviderName());
            return secondaryService.isEnabled(featureName, context);
        }
    }

    @Override
    public boolean isEnabled(String featureName, boolean defaultValue) {
        try {
            return primaryService.isEnabled(featureName, defaultValue);
        } catch (Exception e) {
            log.warn("Primary service ({}) failed for feature '{}', falling back to secondary ({})",
                    primaryService.getProviderName(), featureName, secondaryService.getProviderName());
            return secondaryService.isEnabled(featureName, defaultValue);
        }
    }

    @Override
    public boolean isEnabled(String featureName, FeatureContext context, boolean defaultValue) {
        try {
            return primaryService.isEnabled(featureName, context, defaultValue);
        } catch (Exception e) {
            log.warn("Primary service ({}) failed for feature '{}' with context, falling back to secondary ({})",
                    primaryService.getProviderName(), featureName, secondaryService.getProviderName());
            return secondaryService.isEnabled(featureName, context, defaultValue);
        }
    }

    @Override
    public Variant getVariant(String featureName, FeatureContext context) {
        try {
            return primaryService.getVariant(featureName, context);
        } catch (Exception e) {
            log.warn("Primary service ({}) failed to get variant for feature '{}', falling back to secondary ({})",
                    primaryService.getProviderName(), featureName, secondaryService.getProviderName());
            return secondaryService.getVariant(featureName, context);
        }
    }

    @Override
    public Variant getVariant(String featureName, FeatureContext context, Variant defaultVariant) {
        try {
            return primaryService.getVariant(featureName, context, defaultVariant);
        } catch (Exception e) {
            log.warn("Primary service ({}) failed to get variant for feature '{}', falling back to secondary ({})",
                    primaryService.getProviderName(), featureName, secondaryService.getProviderName());
            return secondaryService.getVariant(featureName, context, defaultVariant);
        }
    }

    @Override
    public Map<String, Boolean> getAllFeatureStates() {
        Map<String, Boolean> primaryStates = new HashMap<>();
        Map<String, Boolean> secondaryStates = new HashMap<>();

        try {
            primaryStates = primaryService.getAllFeatureStates();
        } catch (Exception e) {
            log.warn("Primary service ({}) failed to get all feature states",
                    primaryService.getProviderName());
        }

        try {
            secondaryStates = secondaryService.getAllFeatureStates();
        } catch (Exception e) {
            log.warn("Secondary service ({}) failed to get all feature states",
                    secondaryService.getProviderName());
        }

        // 合并结果，主服务优先
        Map<String, Boolean> combined = new HashMap<>(secondaryStates);
        combined.putAll(primaryStates);
        return combined;
    }

    @Override
    public Map<String, Boolean> getAllFeatureStates(FeatureContext context) {
        Map<String, Boolean> primaryStates = new HashMap<>();
        Map<String, Boolean> secondaryStates = new HashMap<>();

        try {
            primaryStates = primaryService.getAllFeatureStates(context);
        } catch (Exception e) {
            log.warn("Primary service ({}) failed to get all feature states with context",
                    primaryService.getProviderName());
        }

        try {
            secondaryStates = secondaryService.getAllFeatureStates(context);
        } catch (Exception e) {
            log.warn("Secondary service ({}) failed to get all feature states with context",
                    secondaryService.getProviderName());
        }

        // 合并结果，主服务优先
        Map<String, Boolean> combined = new HashMap<>(secondaryStates);
        combined.putAll(primaryStates);
        return combined;
    }

    @Override
    public void refresh() {
        try {
            primaryService.refresh();
        } catch (Exception e) {
            log.warn("Failed to refresh primary service ({})", primaryService.getProviderName());
        }

        try {
            secondaryService.refresh();
        } catch (Exception e) {
            log.warn("Failed to refresh secondary service ({})", secondaryService.getProviderName());
        }
    }

    @Override
    public String getProviderName() {
        return String.format("Composite(%s+%s)",
                primaryService.getProviderName(),
                secondaryService.getProviderName());
    }

    @Override
    public boolean isAvailable() {
        return primaryService.isAvailable() || secondaryService.isAvailable();
    }

    /**
     * 获取主服务
     */
    public FeatureToggleService getPrimaryService() {
        return primaryService;
    }

    /**
     * 获取备用服务
     */
    public FeatureToggleService getSecondaryService() {
        return secondaryService;
    }
}
