package com.basebackend.common.starter.interceptor;

import com.basebackend.common.context.UserContext;
import com.basebackend.common.context.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 通用用户上下文拦截器：通过外部提供的 UserContextProvider 构建并注入 UserContext。
 */
@Slf4j
public class UserContextInterceptor implements HandlerInterceptor, Ordered {

    private final ObjectProvider<List<UserContextProvider>> providerProvider;
    private final int order;

    public UserContextInterceptor(ObjectProvider<List<UserContextProvider>> providers, int order) {
        this.providerProvider = providers;
        this.order = order;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        List<UserContextProvider> providers = providerProvider.getIfAvailable(Collections::emptyList);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return true;
        }

        try {
            for (UserContextProvider provider : providers) {
                Optional<UserContext> maybeContext = provider.loadUserContext(authentication, request);
                if (maybeContext != null && maybeContext.isPresent()) {
                    UserContext context = maybeContext.get();
                    UserContextHolder.set(context);
                    log.debug("用户上下文已设置: userId={}, username={}", context.getUserId(), context.getUsername());
                    break;
                }
            }
        } catch (Exception e) {
            log.error("设置用户上下文失败", e);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContextHolder.clear();
    }

    @Override
    public int getOrder() {
        return order;
    }
}
