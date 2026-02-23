package com.basebackend.security.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 安全事件发布器，封装 Spring ApplicationEventPublisher
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 发布安全事件
     */
    public void publish(SecurityEvent event) {
        try {
            applicationEventPublisher.publishEvent(event);
            log.debug("安全事件已发布: type={}, source={}", event.getEventType(), event.getSource());
        } catch (Exception e) {
            // 事件发布失败不应影响业务主流程
            log.warn("安全事件发布失败: type={}, error={}", event.getEventType(), e.getMessage());
        }
    }
}
