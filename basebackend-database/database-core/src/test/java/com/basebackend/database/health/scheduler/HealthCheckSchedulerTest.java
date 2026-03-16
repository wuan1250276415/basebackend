package com.basebackend.database.health.scheduler;

import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.health.alert.AlertNotificationService;
import com.basebackend.database.health.indicator.DataSourceHealthIndicator;
import com.basebackend.database.health.model.DataSourceHealth;
import com.basebackend.database.health.monitor.ConnectionPoolMonitor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HealthCheckScheduler 测试")
class HealthCheckSchedulerTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private DataSourceHealthIndicator healthIndicator;

    @Mock
    private ConnectionPoolMonitor poolMonitor;

    @Mock
    private AlertNotificationService alertService;

    @Test
    @DisplayName("健康状态变化应触发变化告警")
    void shouldSendStatusChangeAlertWhenHealthChanged() {
        DatabaseEnhancedProperties properties = new DatabaseEnhancedProperties();
        properties.getHealth().setEnabled(true);
        properties.getFailover().setEnabled(false);

        HealthCheckScheduler scheduler = new HealthCheckScheduler(
                dataSource, properties, healthIndicator, poolMonitor, alertService);

        DataSourceHealth upHealth = DataSourceHealth.builder()
                .name("primary")
                .status(DataSourceHealth.HealthStatus.UP)
                .build();
        DataSourceHealth downHealth = DataSourceHealth.builder()
                .name("primary")
                .status(DataSourceHealth.HealthStatus.DOWN)
                .errorMessage("connection failed")
                .build();

        when(healthIndicator.checkDataSource(dataSource, "primary"))
                .thenReturn(upHealth)
                .thenReturn(downHealth);

        scheduler.performHealthCheck();
        scheduler.performHealthCheck();

        verify(alertService).sendHealthStatusChangeAlert("primary", "UP", "DOWN");
        verify(alertService).sendDataSourceFailureAlert("primary", "connection failed");
    }

    @Test
    @DisplayName("健康状态未变化时不应触发变化告警")
    void shouldNotSendStatusChangeAlertWhenHealthUnchanged() {
        DatabaseEnhancedProperties properties = new DatabaseEnhancedProperties();
        properties.getHealth().setEnabled(true);
        properties.getFailover().setEnabled(false);

        HealthCheckScheduler scheduler = new HealthCheckScheduler(
                dataSource, properties, healthIndicator, poolMonitor, alertService);

        DataSourceHealth upHealth1 = DataSourceHealth.builder()
                .name("primary")
                .status(DataSourceHealth.HealthStatus.UP)
                .build();
        DataSourceHealth upHealth2 = DataSourceHealth.builder()
                .name("primary")
                .status(DataSourceHealth.HealthStatus.UP)
                .build();

        when(healthIndicator.checkDataSource(dataSource, "primary"))
                .thenReturn(upHealth1)
                .thenReturn(upHealth2);

        scheduler.performHealthCheck();
        scheduler.performHealthCheck();

        verify(alertService, never()).sendHealthStatusChangeAlert(anyString(), anyString(), anyString());
        verify(alertService, never()).sendDataSourceFailureAlert(anyString(), anyString());
    }
}
