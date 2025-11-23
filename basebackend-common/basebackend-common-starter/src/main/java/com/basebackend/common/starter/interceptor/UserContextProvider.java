package com.basebackend.common.starter.interceptor;

import com.basebackend.common.context.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;

import java.util.Optional;

/**
 * 用户上下文加载器，按需为拦截器提供用户上下文数据。
 */
public interface UserContextProvider {

    /**
     * 根据当前认证信息与请求构建用户上下文。
     *
     * @param authentication 当前认证信息
     * @param request        当前请求
     * @return 可选的用户上下文（为空则跳过）
     */
    Optional<UserContext> loadUserContext(Authentication authentication, HttpServletRequest request);
}
