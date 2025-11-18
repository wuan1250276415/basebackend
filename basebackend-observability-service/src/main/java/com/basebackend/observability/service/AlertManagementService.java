package com.basebackend.observability.service;

import java.util.List;
import java.util.Map;

/**
 * 告警管理服务接口
 *
 * @author BaseBackend Team
 * @since 2025-11-18
 */
public interface AlertManagementService {
    
    /**
     * 注册告警规则
     */
    void registerAlertRule(Map<String, Object> rule);
    
    /**
     * 删除告警规则
     */
    void unregisterAlertRule(Long ruleId);
    
    /**
     * 获取所有告警规则
     */
    List<Map<String, Object>> getAllAlertRules();
    
    /**
     * 获取最近的告警事件
     */
    List<Map<String, Object>> getRecentAlerts();
    
    /**
     * 测试告警规则
     */
    Object testAlertRule(Map<String, Object> rule);
    
    /**
     * 获取告警统计
     */
    Map<String, Object> getAlertStats();
}
