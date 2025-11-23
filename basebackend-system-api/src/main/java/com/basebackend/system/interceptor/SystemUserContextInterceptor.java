package com.basebackend.system.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 系统服务用户上下文拦截器
 *
 * 为 system-api 提供简化的用户上下文支持
 * 实际上 system-api 主要关注权限控制，用户信息通过 user-api 管理
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemUserContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 获取当前认证信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {

            try {
                // 记录用户访问日志
                String username = authentication.getName();
                log.debug("用户访问 system-api: username={}, uri={}", username, request.getRequestURI());

                // system-api 暂不维护用户上下文，主要依赖 user-api
                // 如果需要用户详细信息，可以通过 Feign 调用 user-api 获取

            } catch (Exception e) {
                log.error("处理用户上下文失败", e);
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                 Object handler, Exception ex) {
        // system-api 暂无需要清理的上下文
        // 如果后续实现了 ThreadLocal 缓存，在这里清理
    }
}
