package com.basebackend.observability.alert.repository;

import com.basebackend.observability.alert.AlertRule;

import java.util.List;
import java.util.Optional;

/**
 * 告警规则仓储接口
 * <p>
 * 提供告警规则的持久化操作抽象，支持多种存储实现（内存、数据库、Redis等）。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public interface AlertRuleRepository {

    /**
     * 保存或更新告警规则
     *
     * @param rule 告警规则
     * @return 保存后的规则
     */
    AlertRule save(AlertRule rule);

    /**
     * 根据ID查找规则
     *
     * @param id 规则ID
     * @return 规则（可能为空）
     */
    Optional<AlertRule> findById(Long id);

    /**
     * 查找所有规则
     *
     * @return 规则列表
     */
    List<AlertRule> findAll();

    /**
     * 查找所有启用的规则
     *
     * @return 启用的规则列表
     */
    List<AlertRule> findByEnabledTrue();

    /**
     * 根据ID删除规则
     *
     * @param id 规则ID
     */
    void deleteById(Long id);

    /**
     * 检查规则是否存在
     *
     * @param id 规则ID
     * @return 是否存在
     */
    boolean existsById(Long id);

    /**
     * 统计规则数量
     *
     * @return 规则总数
     */
    long count();

    /**
     * 根据规则名查找
     *
     * @param ruleName 规则名
     * @return 规则（可能为空）
     */
    Optional<AlertRule> findByRuleName(String ruleName);

    /**
     * 根据严重级别查找
     *
     * @param severity 严重级别
     * @return 规则列表
     */
    List<AlertRule> findBySeverity(String severity);
}
