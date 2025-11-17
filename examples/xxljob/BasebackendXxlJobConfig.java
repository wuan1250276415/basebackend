package com.basebackend.examples.xxljob;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * XXL-Job 配置类
 * 在微服务中集成 XXL-Job 执行器
 */
@Slf4j
@Configuration
public class BasebackendXxlJobConfig {

    @Value("${xxl.job.admin.addresses}")
    private String adminAddresses;

    @Value("${xxl.job.accessToken}")
    private String accessToken;

    @Value("${xxl.job.executor.appname}")
    private String appname;

    @Value("${xxl.job.executor.port}")
    private int port;

    @Value("${xxl.job.executor.logpath}")
    private String logPath;

    @Value("${xxl.job.executor.logretentiondays}")
    private int logRetentionDays;

    @Value("${xxl.job.triggerpool.fast.max:200}")
    private int triggerPoolFastMax;

    @Value("${xxl.job.triggerpool.slow.max:100}")
    private int triggerPoolSlowMax;

    @Value("${xxl.job.job.failover.timeout:30}")
    private int failoverTimeout;

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        log.info("XXL-Job 初始化...");

        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();

        // 调度中心配置
        xxlJobSpringExecutor.setAdminAddresses(adminAddresses);
        xxlJobSpringExecutor.setAccessToken(accessToken);

        // 执行器配置
        xxlJobSpringExecutor.setAppname(appname);
        xxlJobSpringExecutor.setPort(port);
        xxlJobSpringExecutor.setLogPath(logPath);
        xxlJobSpringExecutor.setLogRetentionDays(logRetentionDays);

        // 触发器线程池配置
        xxlJobSpringExecutor.setTriggerPoolFastMax(triggerPoolFastMax);
        xxlJobSpringExecutor.setTriggerPoolSlowMax(triggerPoolSlowMax);

        // 失败转移配置
        xxlJobSpringExecutor.setFailoverTimeout(failoverTimeout);

        log.info("XXL-Job 初始化完成: appname={}, port={}, adminAddresses={}",
            appname, port, adminAddresses);

        return xxlJobSpringExecutor;
    }
}
