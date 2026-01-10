package com.basebackend.scheduler.camunda.service.impl;

import com.basebackend.scheduler.camunda.service.CandidateRuleResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 候选人规则解析器默认实现
 *
 * <p>
 * 支持以下格式：
 * <ul>
 * <li>USER:userId1,userId2 - 指定具体用户</li>
 * <li>GROUP:groupId1,groupId2 - 指定具体组/角色</li>
 * <li>VAR:variableName - 从流程变量中获取</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Slf4j
@Service
public class CandidateRuleResolverImpl implements CandidateRuleResolver {

    private static final String PREFIX_USER = "USER:";
    private static final String PREFIX_GROUP = "GROUP:";
    private static final String PREFIX_VAR = "VAR:";

    @Override
    public List<String> resolve(String rule, Map<String, Object> variables) {
        if (!StringUtils.hasText(rule)) {
            return Collections.emptyList();
        }

        try {
            if (rule.startsWith(PREFIX_USER)) {
                String users = rule.substring(PREFIX_USER.length());
                return List.of(users.split(","));
            } else if (rule.startsWith(PREFIX_GROUP)) {
                String groups = rule.substring(PREFIX_GROUP.length());
                return List.of(groups.split(","));
            } else if (rule.startsWith(PREFIX_VAR)) {
                String varName = rule.substring(PREFIX_VAR.length());
                Object val = variables.get(varName);
                if (val instanceof String) {
                    return List.of(((String) val).split(","));
                } else if (val instanceof List) {
                    return (List<String>) val;
                }
            } else {
                // 默认策略或自定义策略 TODO: 扩展更多策略
                log.warn("Unknown candidate rule format: {}", rule);
            }
        } catch (Exception e) {
            log.error("Failed to resolve candidate rule: {}", rule, e);
        }

        return Collections.emptyList();
    }

    @Override
    public boolean supports(String rule) {
        return StringUtils.hasText(rule);
    }
}
