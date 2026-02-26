package com.basebackend.notification.constants;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * NotificationConstants 单元测试
 */
class NotificationConstantsTest {

    @Test
    @DisplayName("常量值正确")
    void shouldHaveCorrectValues() {
        assertThat(NotificationConstants.NOTIFICATION_TOPIC).isEqualTo("notification-topic");
        assertThat(NotificationConstants.TAG_SYSTEM).isEqualTo("SYSTEM");
        assertThat(NotificationConstants.TAG_ANNOUNCEMENT).isEqualTo("ANNOUNCEMENT");
        assertThat(NotificationConstants.TAG_REMINDER).isEqualTo("REMINDER");
        assertThat(NotificationConstants.NOTIFICATION_CONSUMER_GROUP).isEqualTo("notification-consumer-group");
    }

    @Test
    @DisplayName("SSE 超时和心跳值合理")
    void shouldHaveReasonableSseValues() {
        assertThat(NotificationConstants.SSE_TIMEOUT).isEqualTo(5 * 60 * 1000L);
        assertThat(NotificationConstants.SSE_HEARTBEAT_INTERVAL).isEqualTo(30 * 1000L);
        assertThat(NotificationConstants.SSE_HEARTBEAT_INTERVAL).isLessThan(NotificationConstants.SSE_TIMEOUT);
    }
}
