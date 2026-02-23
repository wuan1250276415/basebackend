package com.basebackend.jwt;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;

/**
 * JWT 模块自动配置
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JwtTokenBlacklist jwtTokenBlacklist(
            @Nullable StringRedisTemplate redisTemplate) {
        return new JwtTokenBlacklist(redisTemplate);
    }
}
