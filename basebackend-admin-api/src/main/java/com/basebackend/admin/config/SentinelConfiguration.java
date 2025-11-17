package com.basebackend.admin.config;

import com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sentinel 配置类
 * 启用 @SentinelResource 注解支持
 *
 * @author 浮浮酱
 */
@Configuration
public class SentinelConfiguration {

    /**
     * 配置 Sentinel 切面
     * 用于支持 @SentinelResource 注解
     */
    @Bean
    public SentinelResourceAspect sentinelResourceAspect() {
        return new SentinelResourceAspect();
    }
}
