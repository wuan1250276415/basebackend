package com.basebackend.observability.alert.repository;

import com.basebackend.observability.alert.AlertRule;
import com.basebackend.observability.alert.mapper.AlertRuleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 基于 MyBatis 的告警规则仓储实现
 * <p>
 * 当数据库 {@link DataSource} 和 {@link AlertRuleMapper} 可用时自动激活，
 * 替代 {@link InMemoryAlertRuleRepository}。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
@ConditionalOnBean({DataSource.class, AlertRuleMapper.class})
public class MybatisAlertRuleRepository implements AlertRuleRepository {

    private final AlertRuleMapper alertRuleMapper;

    @Override
    public AlertRule save(AlertRule rule) {
        if (rule == null) {
            throw new IllegalArgumentException("Rule cannot be null");
        }

        if (rule.getId() == null) {
            alertRuleMapper.insert(rule);
            log.debug("Inserted alert rule: id={}, name={}", rule.getId(), rule.getRuleName());
        } else {
            alertRuleMapper.update(rule);
            log.debug("Updated alert rule: id={}, name={}", rule.getId(), rule.getRuleName());
        }
        return rule;
    }

    @Override
    public Optional<AlertRule> findById(Long id) {
        return Optional.ofNullable(alertRuleMapper.selectById(id));
    }

    @Override
    public List<AlertRule> findAll() {
        return alertRuleMapper.selectAll();
    }

    @Override
    public List<AlertRule> findByEnabledTrue() {
        return alertRuleMapper.selectByEnabled(true);
    }

    @Override
    public void deleteById(Long id) {
        alertRuleMapper.deleteById(id);
        log.debug("Deleted alert rule: id={}", id);
    }

    @Override
    public boolean existsById(Long id) {
        return alertRuleMapper.selectById(id) != null;
    }

    @Override
    public long count() {
        return alertRuleMapper.count();
    }

    @Override
    public Optional<AlertRule> findByRuleName(String ruleName) {
        return Optional.ofNullable(alertRuleMapper.selectByRuleName(ruleName));
    }

    @Override
    public List<AlertRule> findBySeverity(String severity) {
        if (severity == null || severity.isBlank()) {
            return Collections.emptyList();
        }
        return alertRuleMapper.selectBySeverity(severity.toUpperCase());
    }
}
