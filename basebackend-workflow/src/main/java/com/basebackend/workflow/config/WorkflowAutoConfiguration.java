package com.basebackend.workflow.config;

import com.basebackend.workflow.engine.ProcessEngine;
import com.basebackend.workflow.engine.RoleChecker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 工作流引擎自动配置
 * <p>
 * 需要显式启用：{@code basebackend.workflow.enabled=true}
 * <p>
 * 如果 Spring 容器中存在 {@link RoleChecker} Bean，将自动注入到引擎以启用审批权限校验；
 * 否则引擎以无权限校验模式运行。
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
    public ProcessEngine processEngine(ObjectProvider<RoleChecker> roleCheckerProvider) {
        RoleChecker checker = roleCheckerProvider.getIfAvailable();
        if (checker != null) {
            log.info("注册 ProcessEngine（内存实现，已配置 RoleChecker，启用审批权限校验）");
            return new ProcessEngine(checker);
        }
        log.info("注册 ProcessEngine（内存实现，未配置 RoleChecker，跳过审批权限校验）");
        return new ProcessEngine();
    }
}
