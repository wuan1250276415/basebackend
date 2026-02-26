package com.basebackend.websocket.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手拦截器
 * <p>
 * 在 WebSocket 连接建立前拦截，提取用户信息并存入 session attributes。
 * 支持从 Header、Query Parameter、Cookie 等方式提取用户标识。
 */
@Slf4j
public class AuthHandshakeInterceptor implements HandshakeInterceptor {

    private final String userIdHeader;

    public AuthHandshakeInterceptor() {
        this("X-User-Id");
    }

    public AuthHandshakeInterceptor(String userIdHeader) {
        this.userIdHeader = userIdHeader;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String userId = null;

        // 1. 从 Header 提取
        String headerValue = request.getHeaders().getFirst(userIdHeader);
        if (headerValue != null && !headerValue.isBlank()) {
            userId = headerValue;
        }

        // 2. 从 Query Parameter 提取
        if (userId == null && request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpRequest = servletRequest.getServletRequest();
            userId = httpRequest.getParameter("userId");

            // 也可以从 token 参数中解析
            String token = httpRequest.getParameter("token");
            if (userId == null && token != null) {
                // TODO: 从 JWT token 中解析 userId（需要注入 JwtService）
                log.debug("WebSocket 握手: 收到 token，需业务方实现解析逻辑");
            }
        }

        if (userId != null && !userId.isBlank()) {
            attributes.put("userId", userId.trim());
            log.debug("WebSocket 握手成功: userId={}", userId);
            return true;
        }

        // 允许匿名连接（由 Handler 分配临时 ID）
        log.debug("WebSocket 握手: 未提供 userId，允许匿名连接");
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.warn("WebSocket 握手后异常: {}", exception.getMessage());
        }
    }
}
