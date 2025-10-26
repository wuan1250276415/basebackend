package com.basebackend.featuretoggle.service;

import com.basebackend.featuretoggle.model.FeatureContext;
import com.basebackend.featuretoggle.model.Variant;

import java.util.Map;

/**
 * 特性开关服务统一接口
 * 支持Unleash和Flagsmith两种实现
 *
 * @author BaseBackend
 */
public interface FeatureToggleService {

    /**
     * 检查特性是否启用（无上下文）
     *
     * @param featureName 特性名称
     * @return true表示启用，false表示禁用
     */
    boolean isEnabled(String featureName);

    /**
     * 检查特性是否启用（带上下文）
     *
     * @param featureName 特性名称
     * @param context     上下文信息
     * @return true表示启用，false表示禁用
     */
    boolean isEnabled(String featureName, FeatureContext context);

    /**
     * 检查特性是否启用（带默认值）
     *
     * @param featureName  特性名称
     * @param defaultValue 默认值
     * @return 特性状态
     */
    boolean isEnabled(String featureName, boolean defaultValue);

    /**
     * 检查特性是否启用（带上下文和默认值）
     *
     * @param featureName  特性名称
     * @param context      上下文信息
     * @param defaultValue 默认值
     * @return 特性状态
     */
    boolean isEnabled(String featureName, FeatureContext context, boolean defaultValue);

    /**
     * 获取变体信息（用于AB测试）
     *
     * @param featureName 特性名称
     * @param context     上下文信息
     * @return 变体信息
     */
    Variant getVariant(String featureName, FeatureContext context);

    /**
     * 获取变体信息（带默认变体）
     *
     * @param featureName    特性名称
     * @param context        上下文信息
     * @param defaultVariant 默认变体
     * @return 变体信息
     */
    Variant getVariant(String featureName, FeatureContext context, Variant defaultVariant);

    /**
     * 获取所有特性开关状态
     *
     * @return 特性名称 -> 是否启用的映射
     */
    Map<String, Boolean> getAllFeatureStates();

    /**
     * 获取所有特性开关状态（带上下文）
     *
     * @param context 上下文信息
     * @return 特性名称 -> 是否启用的映射
     */
    Map<String, Boolean> getAllFeatureStates(FeatureContext context);

    /**
     * 刷新特性开关配置
     */
    void refresh();

    /**
     * 获取提供商名称（Unleash/Flagsmith）
     *
     * @return 提供商名称
     */
    String getProviderName();

    /**
     * 服务是否可用
     *
     * @return true表示可用
     */
    boolean isAvailable();
}
