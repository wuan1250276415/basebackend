package com.basebackend.logging.audit.storage.database;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 数据库审计存储自动配置
 *
 * 仅在 MyBatis Plus 可用且配置启用时激活。
 *
 * @author basebackend team
 * @since 2025-12-10
 */
@Slf4j
@Configuration
@ConditionalOnClass(BaseMapper.class)
@ConditionalOnProperty(value = "basebackend.logging.audit.database.enabled", havingValue = "true")
@MapperScan(basePackages = "com.basebackend.logging.audit.storage.database")
public class DatabaseAuditStorageAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DatabaseAuditStorage databaseAuditStorage(SysAuditLogMapper mapper) {
        log.info("初始化数据库审计存储");
        return new DatabaseAuditStorage(mapper);
    }
}
