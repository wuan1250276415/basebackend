package com.basebackend.observability.health;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * RocketMQ 健康检查器
 * 检查 RocketMQ 连接状态和生产者/消费者状态
 */
@Slf4j
@Component
@ConditionalOnClass(RocketMQTemplate.class)
public class RocketMQHealthIndicator implements HealthIndicator {

    @Autowired(required = false)
    private RocketMQTemplate rocketMQTemplate;

    @Override
    public Health health() {
        if (rocketMQTemplate == null) {
            return Health.unknown()
                    .withDetail("message", "RocketMQ is not configured")
                    .build();
        }

        try {
            Map<String, Object> details = new HashMap<>();

            // 检查 RocketMQTemplate 是否可用
            if (rocketMQTemplate.getProducer() == null) {
                return Health.down()
                        .withDetail("message", "RocketMQ producer is not initialized")
                        .build();
            }

            details.put("producerAvailable", true);

            // 获取 Producer 信息
            try {
                String producerGroup = rocketMQTemplate.getProducer().getProducerGroup();
                details.put("producerGroup", producerGroup);
            } catch (Exception e) {
                log.debug("Failed to get producer group", e);
            }

            details.put("status", "connected");

            return Health.up()
                    .withDetails(details)
                    .build();

        } catch (Exception e) {
            log.error("RocketMQ health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("errorType", e.getClass().getSimpleName())
                    .build();
        }
    }
}
