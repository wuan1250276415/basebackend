package com.basebackend.web.config;

import com.basebackend.web.filter.SecurityHeaderFilter;
import com.basebackend.web.filter.XssFilter;
import com.basebackend.web.interceptor.LoggingInterceptor;
import com.basebackend.web.interceptor.PerformanceInterceptor;
import com.basebackend.web.version.ApiVersionManager;
import com.basebackend.web.version.VersionInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.config.annotation.*;

import jakarta.servlet.DispatcherType;

/**
 * Web MVC 配置类
 * 注册拦截器、过滤器等组件
 *
 * @author basebackend
 * @since 2025-11-23
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final CorsConfig corsConfig;
    private final SecurityHeaderConfig securityHeaderConfig;
    private final LoggingInterceptor loggingInterceptor;
    private final PerformanceInterceptor performanceInterceptor;
    private final VersionInterceptor versionInterceptor;

    /**
     * 注册拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loggingInterceptor)
                .order(1)
                .addPathPatterns("/**");

        registry.addInterceptor(performanceInterceptor)
                .order(2)
                .addPathPatterns("/**");

        registry.addInterceptor(versionInterceptor)
                .order(3)
                .addPathPatterns("/**");
    }

    /**
     * 配置跨域
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (corsConfig.isEnabled()) {
            registry.addMapping("/**")
                    .allowedOrigins(corsConfig.getAllowedOrigins().toArray(new String[0]))
                    .allowedMethods(corsConfig.getAllowedMethods().toArray(new String[0]))
                    .allowedHeaders(corsConfig.getAllowedHeaders().toArray(new String[0]))
                    .exposedHeaders(corsConfig.getExposedHeaders().toArray(new String[0]))
                    .allowCredentials(corsConfig.isAllowCredentials())
                    .maxAge(corsConfig.getMaxAge());
        }
    }

    /**
     * 静态资源映射
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600);

        registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/static/favicon.ico");
    }

    /**
     * 注册 XSS 过滤器
     */
    @Bean
    @Order(1)
    public FilterRegistrationBean<XssFilter> xssFilter() {
        FilterRegistrationBean<XssFilter> filterBean = new FilterRegistrationBean<>(new XssFilter());
        filterBean.setEnabled(true);
        filterBean.addUrlPatterns("/*");
        filterBean.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.FORWARD);
        return filterBean;
    }

    /**
     * 注册安全头过滤器
     */
    @Bean
    @Order(2)
    public FilterRegistrationBean<SecurityHeaderFilter> securityHeaderFilter() {
        FilterRegistrationBean<SecurityHeaderFilter> filterBean = new FilterRegistrationBean<>(new SecurityHeaderFilter(securityHeaderConfig));
        filterBean.setEnabled(securityHeaderConfig.isEnabled());
        filterBean.addUrlPatterns("/*");
        filterBean.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.FORWARD);
        return filterBean;
    }

    /**
     * 配置默认 Servlet 处理
     */
    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
    }
}
