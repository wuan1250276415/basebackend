package com.basebackend.scheduler.camunda.service;

import java.util.List;
import java.util.Map;

/**
 * 任务候选人规则解析器
 *
 * <p>
 * 用于解析节点配置中的候选人规则，动态计算任务的候选用户或候选组。
 * 支持策略模式扩展不同的解析规则。
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
public interface CandidateRuleResolver {

    /**
     * 解析候选人规则
     *
     * @param rule      规则字符串 (e.g., "DEPT_MANAGER", "ROLE:ADMIN")
     * @param variables 流程变量上下文
     * @return 候选人/组 ID 列表
     */
    List<String> resolve(String rule, Map<String, Object> variables);

    /**
     * 是否支持该规则
     *
     * @param rule 规则字符串
     * @return 是否支持
     */
    boolean supports(String rule);
}
