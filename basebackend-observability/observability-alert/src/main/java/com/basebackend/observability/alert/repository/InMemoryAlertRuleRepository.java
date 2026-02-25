package com.basebackend.observability.alert.repository;

import com.basebackend.observability.alert.AlertRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 内存告警规则仓储实现
 * <p>
 * 用于开发测试环境。生产环境应使用数据库实现。
 * 支持基本的CRUD操作和查询功能。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Repository
@ConditionalOnMissingBean(AlertRuleRepository.class)
public class InMemoryAlertRuleRepository implements AlertRuleRepository {

    private final Map<Long, AlertRule> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public AlertRule save(AlertRule rule) {
        if (rule == null) {
            throw new IllegalArgumentException("Rule cannot be null");
        }

        // 如果没有ID，生成一个新ID
        if (rule.getId() == null) {
            rule.setId(idGenerator.getAndIncrement());
        }

        storage.put(rule.getId(), rule);
        log.debug("Saved alert rule: id={}, name={}", rule.getId(), rule.getRuleName());
        return rule;
    }

    @Override
    public Optional<AlertRule> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<AlertRule> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public List<AlertRule> findByEnabledTrue() {
        return storage.values().stream()
                .filter(rule -> Boolean.TRUE.equals(rule.getEnabled()))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        AlertRule removed = storage.remove(id);
        if (removed != null) {
            log.debug("Deleted alert rule: id={}, name={}", id, removed.getRuleName());
        }
    }

    @Override
    public boolean existsById(Long id) {
        return storage.containsKey(id);
    }

    @Override
    public long count() {
        return storage.size();
    }

    @Override
    public Optional<AlertRule> findByRuleName(String ruleName) {
        return storage.values().stream()
                .filter(rule -> ruleName.equals(rule.getRuleName()))
                .findFirst();
    }

    @Override
    public List<AlertRule> findBySeverity(String severity) {
        return storage.values().stream()
                .filter(rule -> severity.equals(rule.getSeverity()))
                .collect(Collectors.toList());
    }

    /**
     * 清空所有规则（用于测试）
     */
    public void clear() {
        storage.clear();
        log.debug("Cleared all alert rules");
    }
}
