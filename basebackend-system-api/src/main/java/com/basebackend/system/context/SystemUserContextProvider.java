package com.basebackend.system.context;

import com.basebackend.common.context.UserContext;
import com.basebackend.common.starter.interceptor.UserContextProvider;
import com.basebackend.jwt.JwtUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * system-api 的用户上下文提供器
 * <p>
 * 从 SecurityContext 中提取用户信息，支持从 JwtUserDetails 获取完整用户信息，
 * 包括 userId、username、deptId，避免频繁 Feign 调用。
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
        Long deptId = null;

        Object principal = authentication.getPrincipal();

        // 优先从 JwtUserDetails 获取完整用户信息（推荐方式）
        if (principal instanceof JwtUserDetails) {
            JwtUserDetails userDetails = (JwtUserDetails) principal;
            userId = userDetails.getUserId();
            username = userDetails.getUsername();
            deptId = userDetails.getDeptId();
        }
        // 兼容旧的处理方式
        else if (principal instanceof Long) {
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
                .deptId(deptId)
                .ipAddress(request.getRemoteAddr())
                .requestTime(System.currentTimeMillis())
                .build();

        return Optional.of(context);
    }
}
