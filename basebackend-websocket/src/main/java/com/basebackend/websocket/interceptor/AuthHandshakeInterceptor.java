package com.basebackend.websocket.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.security.Principal;
import java.util.Map;

/**
 * WebSocket 握手拦截器
 * <p>
 * 在 WebSocket 连接建立前拦截，只接受已经由上层安全链路认证过的请求。
 * <p>
 * 历史实现会直接信任客户端传入的 {@code X-User-Id} 或 {@code userId} 查询参数，
 * 这会导致任意用户冒充。当前实现改为只读取服务端已建立的 {@link Principal}，
 * 并可选从请求角色中推导管理员标识。
 */
@Slf4j
public class AuthHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            log.warn("WebSocket 握手拒绝: 不支持的请求类型 {}", request.getClass().getName());
            response.setStatusCode(org.springframework.http.HttpStatus.FORBIDDEN);
            return false;
        }

        HttpServletRequest httpRequest = servletRequest.getServletRequest();
        Principal principal = httpRequest.getUserPrincipal();
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            log.warn("WebSocket 握手拒绝: 缺少已认证主体, uri={}", request.getURI());
            response.setStatusCode(org.springframework.http.HttpStatus.FORBIDDEN);
            return false;
        }

        String userId = principal.getName().trim();
        if (userId.isEmpty()) {
            log.warn("WebSocket 握手拒绝: 主体名称为空, uri={}", request.getURI());
            response.setStatusCode(org.springframework.http.HttpStatus.FORBIDDEN);
            return false;
        }

        attributes.put("userId", userId);

        String tenantId = request.getHeaders().getFirst("X-Tenant-Id");
        if (tenantId != null && !tenantId.isBlank()) {
            attributes.put("tenantId", tenantId.trim());
        }

        boolean isAdmin = httpRequest.isUserInRole("ADMIN") || httpRequest.isUserInRole("SYSTEM_ADMIN");
        if (isAdmin) {
            attributes.put("isAdmin", Boolean.TRUE);
        }

        log.debug("WebSocket 握手成功: userId={}, isAdmin={}", userId, isAdmin);
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
