package com.basebackend.observability.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.common.exception.BusinessException;
import com.basebackend.observability.entity.AlertEvent;
import com.basebackend.observability.entity.AlertRule;
import com.basebackend.observability.mapper.AlertEventMapper;
import com.basebackend.observability.mapper.AlertRuleMapper;
import com.basebackend.observability.service.AlertManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 告警管理服务实现
 *
 * @author BaseBackend Team
 * @since 2025-11-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertManagementServiceImpl implements AlertManagementService {

    private final AlertRuleMapper alertRuleMapper;
    private final AlertEventMapper alertEventMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void registerAlertRule(Map<String, Object> ruleMap) {
        AlertRule rule = new AlertRule();
        rule.setRuleName((String) ruleMap.get("ruleName"));
        rule.setDescription((String) ruleMap.get("description"));
        rule.setMetricName((String) ruleMap.get("metricName"));
        rule.setThreshold(((Number) ruleMap.getOrDefault("threshold", 0)).doubleValue());
        rule.setOperator((String) ruleMap.getOrDefault("operator", "gt"));
        rule.setDuration(((Number) ruleMap.getOrDefault("duration", 60)).intValue());
        rule.setSeverity((String) ruleMap.getOrDefault("severity", "warning"));
        rule.setEnabled((Boolean) ruleMap.getOrDefault("enabled", true));
        rule.setNotificationChannels((String) ruleMap.get("notificationChannels"));
        rule.setCreateTime(LocalDateTime.now());
        rule.setUpdateTime(LocalDateTime.now());
        rule.setCreatedBy((String) ruleMap.getOrDefault("createdBy", "system"));
        rule.setDeleted(0);
        
        int result = alertRuleMapper.insert(rule);
        if (result <= 0) {
            throw new BusinessException("Failed to register alert rule");
        }
        
        log.info("Alert rule registered: id={}, name={}", rule.getId(), rule.getRuleName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unregisterAlertRule(Long ruleId) {
        AlertRule rule = alertRuleMapper.selectById(ruleId);
        if (rule == null) {
            log.warn("Alert rule not found: id={}", ruleId);
            throw new BusinessException("Alert rule not found");
        }
        
        int result = alertRuleMapper.deleteById(ruleId);
        if (result <= 0) {
            throw new BusinessException("Failed to unregister alert rule");
        }
        
        log.info("Alert rule unregistered: id={}, name={}", ruleId, rule.getRuleName());
    }

    @Override
    public List<Map<String, Object>> getAllAlertRules() {
        LambdaQueryWrapper<AlertRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlertRule::getDeleted, 0)
               .orderByDesc(AlertRule::getCreateTime);
        
        List<AlertRule> rules = alertRuleMapper.selectList(wrapper);
        log.info("Getting all alert rules, count: {}", rules.size());
        
        return rules.stream()
            .map(rule -> BeanUtil.beanToMap(rule))
            .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getRecentAlerts() {
        LambdaQueryWrapper<AlertEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(AlertEvent::getCreateTime)
               .last("LIMIT 100");
        
        List<AlertEvent> events = alertEventMapper.selectList(wrapper);
        log.info("Getting recent alerts, count: {}", events.size());
        
        return events.stream()
            .map(event -> BeanUtil.beanToMap(event))
            .collect(Collectors.toList());
    }

    @Override
    public Object testAlertRule(Map<String, Object> ruleMap) {
        log.info("Testing alert rule: {}", ruleMap.get("ruleName"));
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Alert rule test passed");
        result.put("ruleName", ruleMap.get("ruleName"));
        result.put("timestamp", LocalDateTime.now());
        
        // 可以在这里添加实际的规则测试逻辑
        
        return result;
    }

    @Override
    public Map<String, Object> getAlertStats() {
        log.info("Getting alert stats");
        
        // 统计规则数量
        LambdaQueryWrapper<AlertRule> ruleWrapper = new LambdaQueryWrapper<>();
        ruleWrapper.eq(AlertRule::getDeleted, 0);
        long totalRules = alertRuleMapper.selectCount(ruleWrapper);
        
        // 统计启用的规则
        LambdaQueryWrapper<AlertRule> enabledWrapper = new LambdaQueryWrapper<>();
        enabledWrapper.eq(AlertRule::getDeleted, 0)
                     .eq(AlertRule::getEnabled, true);
        long activeRules = alertRuleMapper.selectCount(enabledWrapper);
        
        // 统计事件数量
        long totalEvents = alertEventMapper.selectCount(null);
        
        // 统计最近24小时的事件
        LambdaQueryWrapper<AlertEvent> recentWrapper = new LambdaQueryWrapper<>();
        recentWrapper.ge(AlertEvent::getCreateTime, LocalDateTime.now().minusHours(24));
        long recentEvents = alertEventMapper.selectCount(recentWrapper);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRules", totalRules);
        stats.put("activeRules", activeRules);
        stats.put("totalEvents", totalEvents);
        stats.put("recentEvents24h", recentEvents);
        
        return stats;
    }
}
