package com.basebackend.cache.eviction;

import com.basebackend.cache.config.CacheProperties;
import com.basebackend.cache.manager.CacheEvictionManager;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.util.List;

/**
 * 定时缓存淘汰执行器
 * 根据配置的 cron 规则，定期调用 CacheEvictionManager.evictByPattern() 清理缓存
 */
@Slf4j
public class ScheduledEvictionExecutor {

    private final CacheEvictionManager cacheEvictionManager;
    private final TaskScheduler taskScheduler;
    private final List<CacheProperties.Eviction.ScheduledRule> rules;
    private final MeterRegistry meterRegistry;

    public ScheduledEvictionExecutor(
            CacheEvictionManager cacheEvictionManager,
            TaskScheduler taskScheduler,
            List<CacheProperties.Eviction.ScheduledRule> rules,
            @Autowired(required = false) MeterRegistry meterRegistry) {
        this.cacheEvictionManager = cacheEvictionManager;
        this.taskScheduler = taskScheduler;
        this.rules = rules;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void registerRules() {
        if (rules == null || rules.isEmpty()) {
            log.info("No scheduled eviction rules configured");
            return;
        }

        for (CacheProperties.Eviction.ScheduledRule rule : rules) {
            if (!rule.isEnabled()) {
                log.info("Skipping disabled eviction rule: {}", rule.getName());
                continue;
            }

            log.info("Registering scheduled eviction rule: name={}, pattern={}, cron={}",
                    rule.getName(), rule.getPattern(), rule.getCron());

            taskScheduler.schedule(
                    () -> executeRule(rule),
                    new CronTrigger(rule.getCron())
            );
        }

        log.info("Registered {} scheduled eviction rules",
                rules.stream().filter(CacheProperties.Eviction.ScheduledRule::isEnabled).count());
    }

    private void executeRule(CacheProperties.Eviction.ScheduledRule rule) {
        log.info("Executing scheduled eviction: name={}, pattern={}", rule.getName(), rule.getPattern());
        try {
            long evicted = cacheEvictionManager.evictByPattern(rule.getPattern());
            log.info("Scheduled eviction completed: name={}, evicted={}", rule.getName(), evicted);
            recordEviction(rule.getName(), evicted);
        } catch (Exception e) {
            log.error("Scheduled eviction failed: name={}, pattern={}", rule.getName(), rule.getPattern(), e);
        }
    }

    private void recordEviction(String ruleName, long evicted) {
        if (meterRegistry == null) {
            return;
        }
        Counter.builder("cache.scheduled.eviction")
                .tag("rule", ruleName)
                .register(meterRegistry)
                .increment(evicted);
    }
}
