package com.basebackend.workflow.config;

import com.basebackend.workflow.engine.ProcessEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 工作流引擎自动配置
 * <p>
 * 需要显式启用：{@code basebackend.workflow.enabled=true}
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "basebackend.workflow", name = "enabled", havingValue = "true")
public class WorkflowAutoConfiguration {

    public WorkflowAutoConfiguration() {
        log.info("工作流引擎模块已启用");
    }

    @Bean
    @ConditionalOnMissingBean
    public ProcessEngine processEngine() {
        log.info("注册 ProcessEngine（内存实现）");
        return new ProcessEngine();
    }
}
