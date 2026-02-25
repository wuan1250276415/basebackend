package com.basebackend.database.migration.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.api.callback.BaseCallback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flyway 配置类
 *
 * @author basebackend
 */
@Slf4j
@Configuration
@ConditionalOnClass(name = "org.flywaydb.core.Flyway")
@ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", havingValue = "true")
public class FlywayConfiguration {

    /**
     * Flyway 回调：在迁移前后记录日志
     * Spring Boot 4 移除了 FlywayMigrationStrategy，改用 Flyway 原生 Callback
     */
    @Bean
    public BaseCallback flywayLoggingCallback() {
        return new BaseCallback() {
            @Override
            public boolean supports(Event event, Context context) {
                return event == Event.BEFORE_MIGRATE || event == Event.AFTER_MIGRATE
                        || event == Event.BEFORE_REPAIR || event == Event.AFTER_REPAIR;
            }

            @Override
            public boolean canHandleInTransaction(Event event, Context context) {
                return true;
            }

            @Override
            public void handle(Event event, Context context) {
                switch (event) {
                    case BEFORE_MIGRATE -> log.info("准备执行 Flyway 数据库迁移...");
                    case AFTER_MIGRATE -> log.info("Flyway 数据库迁移完成");
                    case BEFORE_REPAIR -> log.info("准备修复 Flyway 迁移历史...");
                    case AFTER_REPAIR -> log.info("Flyway 迁移历史修复完成");
                    default -> { }
                }
            }
        };
    }
}
