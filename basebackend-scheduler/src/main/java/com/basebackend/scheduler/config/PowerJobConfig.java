package com.basebackend.scheduler.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.powerjob.client.PowerJobClient;

/**
 * PowerJob配置
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(SchedulerProperties.class)
public class PowerJobConfig {

    private final SchedulerProperties schedulerProperties;

    /**
     * 创建PowerJob客户端
     * 用于任务管理API调用
     */
    @Bean
    public PowerJobClient powerJobClient() {
        SchedulerProperties.PowerJob config = schedulerProperties.getPowerjob();

        // PowerJobClient构造函数: (server-address, app-name, password)
        // password可以为空，使用默认密码或无密码
        PowerJobClient client = new PowerJobClient(
                config.getServerAddress(),
                config.getAppName(),
                ""  // 密码，默认为空
        );

        log.info("PowerJob Client初始化成功，Server地址: {}, AppName: {}",
                config.getServerAddress(), config.getAppName());

        return client;
    }
}
