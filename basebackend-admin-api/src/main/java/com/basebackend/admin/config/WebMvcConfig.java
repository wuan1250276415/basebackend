package com.basebackend.admin.config;

import com.basebackend.admin.interceptor.UserContextInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-09
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final UserContextInterceptor userContextInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册用户上下文拦截器
        registry.addInterceptor(userContextInterceptor)
                .addPathPatterns("/**")  // 拦截所有请求
                .excludePathPatterns(
                        "/api/admin/auth/login",     // 登录接口
                        "/api/admin/auth/register",  // 注册接口
                        "/error",                    // 错误页面
                        "/swagger-ui/**",            // Swagger UI
                        "/v3/api-docs/**",           // Swagger API文档
                        "/swagger-resources/**",     // Swagger资源
                        "/webjars/**",               // Web资源
                        "/favicon.ico",              // 图标
                        "/actuator/**"               // Actuator监控端点
                )
                .order(1);  // 设置拦截器顺序
    }
}
