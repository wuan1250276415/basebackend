package com.basebackend.database.health.logger;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.health.alert.AlertNotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SlowQueryLogger 安全日志测试")
class SlowQueryLoggerTest {

    @Mock
    private AlertNotificationService alertService;

    private SlowQueryLogger slowQueryLogger;
    private ListAppender<ILoggingEvent> logAppender;
    private Logger targetLogger;

    @BeforeEach
    void setUp() {
        DatabaseEnhancedProperties properties = new DatabaseEnhancedProperties();
        properties.getHealth().setEnabled(true);
        properties.getHealth().setSlowQueryThreshold(1000);

        slowQueryLogger = new SlowQueryLogger(properties, alertService);

        targetLogger = (Logger) LoggerFactory.getLogger(SlowQueryLogger.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        targetLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        targetLogger.detachAppender(logAppender);
    }

    @Test
    @DisplayName("慢查询日志与告警不应泄露敏感值")
    void shouldNotLeakSensitiveValuesInLogsAndAlerts() {
        String sql = "SELECT *   FROM users WHERE email = 'secret@example.com' AND pin = 123456";
        String sensitiveValue = "P@ssw0rd-Value";

        slowQueryLogger.logSlowQuery(sql, 1200, Map.of("password", sensitiveValue));

        String logOutput = logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.joining("\n"));
        assertThat(logOutput)
                .contains("SLOW QUERY DETECTED")
                .contains("Parameter metadata")
                .doesNotContain("secret@example.com")
                .doesNotContain("123456")
                .doesNotContain(sensitiveValue);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(alertService).sendSlowQueryAlert(sqlCaptor.capture(), eq(1200L), eq(1000L));
        assertThat(sqlCaptor.getValue())
                .contains("'?'")
                .doesNotContain("secret@example.com")
                .doesNotContain("123456");
    }

    @Test
    @DisplayName("慢查询SQL应限制输出长度")
    void shouldLimitSanitizedSqlLength() {
        String longLiteral = "A".repeat(300);
        String sql = "SELECT * FROM users WHERE token = '" + longLiteral + "'";

        slowQueryLogger.logSlowQuery(sql, 1200, "secret-token");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(alertService).sendSlowQueryAlert(sqlCaptor.capture(), eq(1200L), eq(1000L));
        assertThat(sqlCaptor.getValue().length()).isLessThanOrEqualTo(203);
    }
}
