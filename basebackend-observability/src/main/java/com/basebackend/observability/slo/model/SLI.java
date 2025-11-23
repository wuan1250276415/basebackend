package com.basebackend.observability.slo.model;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * 服务级别指标 (SLI) 接口
 * <p>
 * SLI 是用于衡量服务质量的可量化指标，常见类型包括：
 * <ul>
 *     <li>可用性 - 成功请求占比</li>
 *     <li>延迟 - 响应时间百分位数</li>
 *     <li>错误率 - 错误请求占比</li>
 *     <li>吞吐量 - 每秒请求数</li>
 * </ul>
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public interface SLI {

    /**
     * 基于 Micrometer 指标计算当前 SLI 值
     *
     * @param registry Micrometer 注册表
     * @param service  服务名称
     * @param method   方法名称
     * @param sloName  SLO 标识符
     * @return 计算的 SLI 值
     */
    double calculate(MeterRegistry registry, String service, String method, String sloName);
}
