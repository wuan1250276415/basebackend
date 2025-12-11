package com.basebackend.featuretoggle.context;

import com.basebackend.featuretoggle.model.FeatureContext;

import jakarta.servlet.http.HttpServletRequest;

/**
 * SPI接口：用于解析特性开关所需的上下文信息。
 * <p>
 * 不同入口类型（HTTP、消息、定时任务等）可以提供各自的实现以获取合适的上下文。
 */
public interface FeatureContextResolver {

    /**
     * 解析特性开关上下文。
     *
     * @param request 当前HTTP请求；非HTTP场景可为 {@code null}
     * @return 解析到的上下文信息，若未能解析则返回 {@link FeatureContext#empty()}
     */
    FeatureContext resolve(HttpServletRequest request);

    /**
     * 解析当前线程的上下文，不依赖外部入参。
     *
     * @return 解析到的上下文信息，若未能解析则返回 {@link FeatureContext#empty()}
     */
    default FeatureContext resolve() {
        return resolve(null);
    }
}
