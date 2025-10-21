package com.basebackend.backup.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 备份模块自动配置
 *
 * @author BaseBackend
 */
@Slf4j
@Configuration
@EnableScheduling
@ComponentScan("com.basebackend.backup")
@EnableConfigurationProperties(BackupProperties.class)
@ConditionalOnProperty(prefix = "backup", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BackupAutoConfiguration {

    public BackupAutoConfiguration() {
        log.info("备份模块已启用");
    }
}
