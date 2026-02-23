package com.basebackend.scheduler.config;

import com.basebackend.common.starter.properties.CommonProperties;
import com.basebackend.observability.metrics.BusinessMetrics;
import com.basebackend.scheduler.monitoring.config.MonitoringInterceptor;
import com.basebackend.scheduler.monitoring.metrics.WorkflowMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.mockito.Mockito;

/**
 * Controller测试专用配置
 * 解决Controller测试中的依赖注入问题
 */
@TestConfiguration
@Profile("test")
public class ControllerTestConfig {

    @Bean
    @Primary
    public RepositoryService repositoryService() {
        return Mockito.mock(RepositoryService.class);
    }

    @Bean
    @Primary
    public RuntimeService runtimeService() {
        return Mockito.mock(RuntimeService.class);
    }

    @Bean
    @Primary
    public TaskService taskService() {
        return Mockito.mock(TaskService.class);
    }

    @Bean
    @Primary
    public MeterRegistry meterRegistry() {
        return Mockito.mock(MeterRegistry.class);
    }

    @Bean
    @Primary
    public BusinessMetrics businessMetrics() {
        return Mockito.mock(BusinessMetrics.class);
    }

    @Bean
    @Primary
    public MonitoringInterceptor monitoringInterceptor() {
        return Mockito.mock(MonitoringInterceptor.class);
    }

    @Bean
    @Primary
    public CommonProperties commonProperties() {
        return Mockito.mock(CommonProperties.class);
    }

    @Bean
    @Primary
    public WorkflowMetrics workflowMetrics() {
        return Mockito.mock(WorkflowMetrics.class);
    }

    @Bean
    @Primary
    public com.basebackend.jwt.JwtUtil jwtUtil() {
        return Mockito.mock(com.basebackend.jwt.JwtUtil.class);
    }
}
