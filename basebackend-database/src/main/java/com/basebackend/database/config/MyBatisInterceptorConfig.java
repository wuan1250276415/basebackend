package com.basebackend.database.config;

import com.basebackend.database.interceptor.SlowSqlInterceptor;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis 拦截器配置
 *
 * 配置慢查询监控拦截器
 *
 * @author 浮浮酱
 */
@Slf4j
@Configuration
public class MyBatisInterceptorConfig {

    /**
     * 注册慢查询监控拦截器
     *
     * 条件：配置 mybatis.slow-sql-monitor.enabled=true 时启用（默认启用）
     */
    @Bean
    @ConditionalOnProperty(name = "mybatis.slow-sql-monitor.enabled", havingValue = "true", matchIfMissing = true)
    public SlowSqlInterceptor slowSqlInterceptor(MeterRegistry meterRegistry) {
        log.info("初始化慢查询监控拦截器");
        return new SlowSqlInterceptor(meterRegistry);
    }
}
