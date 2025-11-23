package com.basebackend.common.starter.config;

import com.basebackend.common.starter.interceptor.UserContextInterceptor;
import com.basebackend.common.starter.interceptor.UserContextProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.MappedInterceptor;

import java.util.List;

/**
 * 用户上下文自动配置，注册拦截器并允许路径配置。
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(UserContextProperties.class)
@ConditionalOnClass(HandlerInterceptor.class)
@ConditionalOnProperty(prefix = "basebackend.common.user-context", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class UserContextAutoConfiguration {

    private final UserContextProperties properties;

    @Bean
    @ConditionalOnMissingBean
    public UserContextInterceptor userContextInterceptor(ObjectProvider<List<UserContextProvider>> providers) {
        return new UserContextInterceptor(providers, properties.getOrder());
    }

    /**
     * 使用 MappedInterceptor 注册，避免与 WebMvcConfigurer 形成启动期循环依赖。
     */
    @Bean
    @ConditionalOnMissingBean(name = "userContextMappedInterceptor")
    public MappedInterceptor userContextMappedInterceptor(UserContextInterceptor interceptor) {
        String[] paths = properties.getPathPatterns().toArray(new String[0]);
        String[] excludes = properties.getExcludePatterns().toArray(new String[0]);
        log.info("用户上下文拦截器已注册，paths={}, excludes={}, order={}",
                properties.getPathPatterns(), properties.getExcludePatterns(), properties.getOrder());
        return new MappedInterceptor(paths, excludes, interceptor);
    }
}
