package com.basebackend.scheduler.config;

import com.basebackend.scheduler.camunda.service.HistoricProcessInstanceService;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.mockito.Mockito;

@TestConfiguration
@Profile("test")
public class TestConfig {

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
    public HistoricProcessInstanceService historicProcessInstanceService() {
        return Mockito.mock(HistoricProcessInstanceService.class);
    }

    @Bean
    @Primary
    public HistoricTaskInstance historicTaskInstanceService() {
        return Mockito.mock(HistoricTaskInstance.class);
    }
}
