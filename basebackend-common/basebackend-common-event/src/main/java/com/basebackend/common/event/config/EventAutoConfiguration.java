package com.basebackend.common.event.config;

import com.basebackend.common.event.DomainEventPublisher;
import com.basebackend.common.event.impl.ReliableDomainEventPublisher;
import com.basebackend.common.event.impl.SpringDomainEventPublisher;
import com.basebackend.common.event.retry.EventCleanupScheduler;
import com.basebackend.common.event.retry.EventRetryScheduler;
import com.basebackend.common.event.store.EventStore;
import com.basebackend.common.event.store.InMemoryEventStore;
import com.basebackend.common.event.store.JdbcEventStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

@AutoConfiguration
@EnableConfigurationProperties(EventProperties.class)
@ConditionalOnProperty(prefix = "basebackend.common.event", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EventAutoConfiguration {

    /**
     * 内存事件存储（默认）
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "basebackend.common.event.store", name = "type", havingValue = "memory", matchIfMissing = true)
    static class InMemoryEventStoreConfiguration {
        @Bean
        @ConditionalOnMissingBean(EventStore.class)
        public EventStore eventStore() {
            return new InMemoryEventStore();
        }
    }

    /**
     * JDBC 事件存储
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "basebackend.common.event.store", name = "type", havingValue = "jdbc")
    @ConditionalOnBean(JdbcTemplate.class)
    static class JdbcEventStoreConfiguration {
        @Bean
        @ConditionalOnMissingBean(EventStore.class)
        public EventStore eventStore(JdbcTemplate jdbcTemplate) {
            return new JdbcEventStore(jdbcTemplate);
        }
    }

    /**
     * 可靠事件发布器（当 EventStore 可用时）
     */
    @Bean
    @ConditionalOnMissingBean(DomainEventPublisher.class)
    @ConditionalOnBean(EventStore.class)
    public DomainEventPublisher reliableDomainEventPublisher(
            ApplicationEventPublisher applicationEventPublisher,
            EventStore eventStore) {
        return new ReliableDomainEventPublisher(applicationEventPublisher, eventStore);
    }

    /**
     * 简单事件发布器（EventStore 不可用时的降级）
     */
    @Bean
    @ConditionalOnMissingBean({DomainEventPublisher.class, EventStore.class})
    public DomainEventPublisher simpleDomainEventPublisher(
            ApplicationEventPublisher applicationEventPublisher) {
        return new SpringDomainEventPublisher(applicationEventPublisher);
    }

    /**
     * 事件重试调度器
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "basebackend.common.event.retry", name = "enabled", havingValue = "true", matchIfMissing = true)
    @EnableScheduling
    static class EventRetryConfiguration {
        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnBean(EventStore.class)
        public EventRetryScheduler eventRetryScheduler(
                EventStore eventStore,
                ApplicationEventPublisher applicationEventPublisher,
                EventProperties properties) {
            return new EventRetryScheduler(eventStore, applicationEventPublisher,
                    properties.getRetry().getBatchSize());
        }
    }

    /**
     * 过期事件清理调度器
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "basebackend.common.event.cleanup", name = "enabled", havingValue = "true", matchIfMissing = true)
    @EnableScheduling
    static class EventCleanupConfiguration {
        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnBean(EventStore.class)
        public EventCleanupScheduler eventCleanupScheduler(
                EventStore eventStore,
                EventProperties properties) {
            return new EventCleanupScheduler(eventStore, properties.getCleanup().getExpiredDays());
        }
    }
}
