package com.basebackend.featuretoggle.context;

import com.basebackend.featuretoggle.model.FeatureContext;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Spring MVC拦截器：在请求进入时解析并绑定特性上下文，响应完成后进行清理。
 * <p>
 * 通过实现 {@link WebMvcConfigurer}，在应用启动时自动注册自身。
 */
@Component
public class RequestContextInterceptor implements HandlerInterceptor, WebMvcConfigurer {

    private final FeatureContextResolver featureContextResolver;

    public RequestContextInterceptor(FeatureContextResolver featureContextResolver) {
        this.featureContextResolver = featureContextResolver;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        FeatureContext context = featureContextResolver.resolve(request);
        FeatureContextHolder.set(context);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                @Nullable Exception ex) {
        FeatureContextHolder.clear();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this).order(Ordered.HIGHEST_PRECEDENCE);
    }
}
