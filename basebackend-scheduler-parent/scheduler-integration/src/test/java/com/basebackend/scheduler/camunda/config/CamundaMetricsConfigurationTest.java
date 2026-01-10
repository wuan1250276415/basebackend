package com.basebackend.scheduler.camunda.config;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.JobQuery;
import org.camunda.bpm.engine.task.TaskQuery;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * CamundaMetricsConfiguration 测试
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
class CamundaMetricsConfigurationTest {

    @Test
    void registersBasicGauges() {
        // Given
        ManagementService managementService = mock(ManagementService.class);
        TaskService taskService = mock(TaskService.class);
        JobQuery jobQuery = mock(JobQuery.class);
        when(managementService.createJobQuery()).thenReturn(jobQuery);
        when(jobQuery.active()).thenReturn(jobQuery);
        when(jobQuery.count()).thenReturn(5L);
        when(jobQuery.withException()).thenReturn(jobQuery);
        when(jobQuery.count()).thenReturn(2L);

        TaskQuery taskQuery = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.taskUnassigned()).thenReturn(taskQuery);
        when(taskQuery.count()).thenReturn(10L);

        // When
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        new CamundaMetricsConfiguration()
                .camundaMeterBinder(managementService, taskService)
                .bindTo(registry);

        // Then
        assertThat(registry.find("camunda.jobs.running").gauge()).isNotNull();
        assertThat(registry.find("camunda.jobs.failed").gauge()).isNotNull();
        assertThat(registry.find("camunda.tasks.pending").gauge()).isNotNull();
    }

    @Test
    void handlesExceptionsGracefully() {
        // Given
        ManagementService managementService = mock(ManagementService.class);
        TaskService taskService = mock(TaskService.class);
        when(managementService.createJobQuery()).thenThrow(new RuntimeException("DB connection error"));

        // When
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        new CamundaMetricsConfiguration()
                .camundaMeterBinder(managementService, taskService)
                .bindTo(registry);

        // Then - 指标注册不应该抛出异常，即使服务异常
        assertThat(registry.getMeters()).isEmpty();
    }
}
