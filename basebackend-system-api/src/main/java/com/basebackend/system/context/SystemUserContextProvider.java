package com.basebackend.system.context;

import com.basebackend.common.context.UserContext;
import com.basebackend.common.starter.interceptor.UserContextProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * system-api 的用户上下文提供器：从 SecurityContext 中提取基础用户信息。
 * 避免依赖 Feign/数据库，防止启动时循环依赖。
 */
@Component
public class SystemUserContextProvider implements UserContextProvider {

    @Override
    public Optional<UserContext> loadUserContext(Authentication authentication, HttpServletRequest request) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty();
        }

        Long userId = null;
        String username = null;

        Object principal = authentication.getPrincipal();
        if (principal instanceof Long) {
            userId = (Long) principal;
            username = authentication.getName();
        } else if (principal instanceof String) {
            username = (String) principal;
            try {
                userId = Long.parseLong(username);
            } catch (NumberFormatException ignored) {
                // 如果 principal 不是数字，则仅设置用户名
            }
        }

        if (userId == null && username == null) {
            return Optional.empty();
        }

        UserContext context = UserContext.builder()
                .userId(userId)
                .username(username)
                .ipAddress(request.getRemoteAddr())
                .requestTime(System.currentTimeMillis())
                .build();

        return Optional.of(context);
    }
}
