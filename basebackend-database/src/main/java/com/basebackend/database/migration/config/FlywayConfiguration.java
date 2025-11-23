package com.basebackend.database.migration.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flyway 配置类
 * 
 * @author basebackend
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", havingValue = "true")
public class FlywayConfiguration {

    /**
     * 自定义 Flyway 迁移策略
     * 可以在这里添加迁移前后的钩子逻辑
     */
    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            // 迁移前的逻辑
            log.info("准备执行 Flyway 数据库迁移...");
            
            // 验证迁移脚本
            try {
                flyway.validate();
                log.info("迁移脚本验证通过");
            } catch (Exception e) {
                log.warn("迁移脚本验证失败，将尝试修复: {}", e.getMessage());
                flyway.repair();
            }
            
            // 执行迁移
            flyway.migrate();
            
            // 迁移后的逻辑
            log.info("Flyway 数据库迁移完成");
        };
    }
}
