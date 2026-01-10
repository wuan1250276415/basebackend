package com.basebackend.scheduler.camunda.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.camunda.bpm.engine.impl.history.handler.CompositeHistoryEventHandler;
import org.camunda.bpm.engine.impl.history.handler.DbHistoryEventHandler;
import org.camunda.bpm.engine.impl.history.handler.HistoryEventHandler;
import org.camunda.bpm.engine.impl.identity.ReadOnlyIdentityProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Camunda 工作流引擎自定义配置
 *
 * <p>
 * 核心功能：
 * <ul>
 * <li>注册自定义的引擎插件（ProcessEnginePlugin）以扩展引擎行为</li>
 * <li>配置历史事件处理器（HistoryEventHandler）用于审计和合规</li>
 * <li>预留自定义身份认证提供者（IdentityProvider）集成点</li>
 * </ul>
 *
 * <p>
 * 设计原则：
 * <ul>
 * <li>保持轻量级，避免影响启动性能</li>
 * <li>遵循 Camunda 最佳实践，避免过度定制</li>
 * <li>预留扩展点，支持后续集成外部系统</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class CamundaConfig {

    private final CamundaProperties camundaProperties;

    /**
     * 自定义流程引擎插件
     *
     * <p>
     * 在引擎初始化时注册自定义的历史事件处理器和其他扩展。
     * 插件在 {@code preInit} 阶段被调用，可以修改引擎配置。
     *
     * @param customHistoryEventHandler 自定义历史事件处理器
     * @return ProcessEnginePlugin 实例
     */
    @Bean
    public ProcessEnginePlugin customProcessEnginePlugin(
            HistoryEventHandler customHistoryEventHandler,
            com.basebackend.scheduler.camunda.listener.GlobalBpmnParseListener globalBpmnParseListener) {
        return new AbstractProcessEnginePlugin() {
            @Override
            public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
                log.info("Initializing custom Camunda process engine plugin");

                // 注册组合历史事件处理器（数据库 + 自定义）
                if (camundaProperties.isAuditEnabled()) {
                    List<HistoryEventHandler> handlers = new ArrayList<>();
                    // 默认的数据库历史记录器
                    handlers.add(new DbHistoryEventHandler());
                    // 自定义的审计日志处理器
                    handlers.add(customHistoryEventHandler);

                    CompositeHistoryEventHandler compositeHandler = new CompositeHistoryEventHandler(handlers);
                    processEngineConfiguration.setHistoryEventHandler(compositeHandler);

                    log.info("Camunda audit logging enabled with composite history handler");
                } else {
                    log.info("Camunda audit logging disabled");
                }

                // 注册全局 BPMN 解析监听器
                List<org.camunda.bpm.engine.impl.bpmn.parser.BpmnParseListener> preListeners = processEngineConfiguration
                        .getCustomPostBPMNParseListeners();
                if (preListeners == null) {
                    preListeners = new ArrayList<>();
                    processEngineConfiguration.setCustomPostBPMNParseListeners(preListeners);
                }
                preListeners.add(globalBpmnParseListener);
                log.info("Registered GlobalBpmnParseListener");

                // 预留：可在此处配置自定义身份认证提供者
                // processEngineConfiguration.setIdentityProviderSessionFactory(customIdentitySessionFactory);

                // 启用作业执行器（Job Executor）以支持异步延续（Async Continuations）和定时器
                processEngineConfiguration.setJobExecutorActivate(true);
                log.info("Job Executor enabled");
            }

            @Override
            public void postInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
                log.info("Custom Camunda process engine plugin post-initialization completed");
            }
        };
    }

    /**
     * 自定义历史事件处理器
     *
     * <p>
     * 用于记录和审计工作流的所有历史事件，包括：
     * <ul>
     * <li>流程实例的启动、完成、终止</li>
     * <li>任务的创建、分配、完成</li>
     * <li>变量的创建和更新</li>
     * <li>流程定义的部署</li>
     * </ul>
     *
     * <p>
     * TODO：生产环境应将事件发送到消息队列或审计系统，避免阻塞流程执行。
     *
     * @return HistoryEventHandler 实例
     */
    @Bean
    public HistoryEventHandler customHistoryEventHandler() {
        // 创建复合历史事件处理器
        CompositeHistoryEventHandler compositeHandler = new CompositeHistoryEventHandler();
        // 添加数据库历史处理器
        compositeHandler.add(new DbHistoryEventHandler());
        return compositeHandler;
    }

    /**
     * 自定义身份认证提供者
     *
     * <p>
     * 用于集成外部身份认证系统（如 LDAP、SSO、IDM 等）。
     * 当前未启用，使用 Camunda 默认的数据库身份认证。
     *
     * <p>
     * TODO：如果需要集成外部身份系统，实现 {@link ReadOnlyIdentityProvider} 接口。
     * 注意：
     * <ul>
     * <li>必须实现为只读模式，避免数据不一致</li>
     * <li>需要考虑性能和缓存策略</li>
     * <li>需要处理认证失败和降级场景</li>
     * </ul>
     *
     * 注意：此处不返回 Bean 以避免 NullBean 问题，需要时可通过 @ConditionalOnProperty 条件注册。
     */
    // @Bean
    // @ConditionalOnProperty(name = "camunda.custom.external-identity-enabled",
    // havingValue = "true")
    // public ReadOnlyIdentityProvider customIdentityProvider() {
    // // TODO: 如需集成外部身份系统，在此实现
    // // return new LdapIdentityProviderPlugin();
    // return null;
    // }

    /**
     * 输出配置信息到日志
     */
    @PostConstruct
    public void logConfiguration() {
        log.info("================================================================");
        log.info("Camunda Custom Configuration:");
        log.info("  - Cache Enabled: {}", camundaProperties.isCacheEnabled());
        log.info("  - Max Cache Size: {}", camundaProperties.getMaxCacheSize());
        log.info("  - Cache Expire Minutes: {}", camundaProperties.getCacheExpireMinutes());
        log.info("  - Monitoring Enabled: {}", camundaProperties.isMonitoringEnabled());
        log.info("  - Audit Enabled: {}", camundaProperties.isAuditEnabled());
        log.info("================================================================");
    }
}
