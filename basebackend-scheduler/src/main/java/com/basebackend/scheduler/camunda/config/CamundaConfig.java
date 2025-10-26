package com.basebackend.scheduler.camunda.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.camunda.bpm.spring.boot.starter.configuration.Ordering;
import org.camunda.bpm.spring.boot.starter.configuration.impl.AbstractCamundaConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Camunda BPM配置
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "camunda.bpm.enabled", havingValue = "true", matchIfMissing = true)
public class CamundaConfig extends AbstractCamundaConfiguration {

    private final CamundaProperties camundaProperties;

    @Override
    @Order(Ordering.DEFAULT_ORDER + 1)
    public void preInit(SpringProcessEngineConfiguration configuration) {
        log.info("Camunda BPM 配置初始化开始");

        // 历史级别配置
        configuration.setHistory(camundaProperties.getHistoryLevel());

        // 作业执行配置
        CamundaProperties.JobExecution jobExecution = camundaProperties.getJobExecution();
        configuration.setJobExecutorActivate(jobExecution.getEnabled());
        configuration.setJobExecutorDeploymentAware(true);

        // 数据库配置
        configuration.setDatabaseSchemaUpdate("true");
        configuration.setDatabaseTablePrefix("");

        // 启用指标收集
        configuration.setMetricsEnabled(true);
        configuration.setDbMetricsReporterActivate(true);

        // 启用任务指标
        configuration.setTaskMetricsEnabled(true);

        // BPMN解析监听器
        configuration.setFailedJobRetryTimeCycle("R3/PT5M");

        log.info("Camunda BPM 配置初始化完成: historyLevel={}, jobExecutor={}",
                camundaProperties.getHistoryLevel(), jobExecution.getEnabled());
    }

    /**
     * 创建管理员用户
     */
    @Bean
    @Order(Ordering.DEFAULT_ORDER + 2)
    public CamundaAdminInitializer camundaAdminInitializer(ProcessEngine processEngine) {
        return new CamundaAdminInitializer(processEngine, camundaProperties);
    }
}
