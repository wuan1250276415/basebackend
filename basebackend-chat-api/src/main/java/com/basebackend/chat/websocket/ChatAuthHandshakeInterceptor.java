package com.basebackend.chat.websocket;

import com.basebackend.jwt.JwtUserDetails;
import com.basebackend.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 聊天 WebSocket 握手鉴权拦截器
 * <p>
 * 从 URL query 参数 {@code token} 或 {@code Authorization} 请求头提取 JWT，
 * 校验通过后将 userId / tenantId 存入 WebSocketSession attributes。
 * 鉴权失败直接拒绝握手返回 403。
 */
@Slf4j
@RequiredArgsConstructor
public class ChatAuthHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        URI uri = request.getURI();
        String token = extractQueryParam(uri, "token");

        // 降级: 从 Authorization 头获取
        if (token == null || token.isBlank()) {
            List<String> authHeaders = request.getHeaders().get("Authorization");
            if (authHeaders != null && !authHeaders.isEmpty()) {
                String header = authHeaders.getFirst();
                if (header.startsWith("Bearer ")) {
                    token = header.substring(7);
                }
            }
        }

        if (token == null || token.isBlank()) {
            log.warn("WebSocket 握手拒绝: 缺少 token, uri={}", uri);
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }

        try {
            Claims claims = jwtUtil.parseClaimsStrict(token);
            JwtUserDetails userDetails = JwtUserDetails.fromClaims(claims);

            attributes.put("userId", String.valueOf(userDetails.getUserId()));
            String tenantId = userDetails.getTenantId();
            attributes.put("tenantId", tenantId != null && !tenantId.isBlank() ? tenantId : "0");

            // 可选: 设备ID
            String deviceId = extractQueryParam(uri, "deviceId");
            if (deviceId != null && !deviceId.isBlank()) {
                attributes.put("deviceId", deviceId);
            }

            log.info("WebSocket 握手鉴权成功: userId={}, tenantId={}", userDetails.getUserId(), tenantId);
            return true;
        } catch (Exception e) {
            log.warn("WebSocket 握手鉴权失败: uri={}, error={}", uri, e.getMessage());
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 无需后处理
    }

    /**
     * 从 URI query string 中提取指定参数
     */
    private String extractQueryParam(URI uri, String paramName) {
        String query = uri.getQuery();
        if (query == null || query.isBlank()) {
            return null;
        }
        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                String key = pair.substring(0, idx);
                if (paramName.equals(key) && idx + 1 < pair.length()) {
                    return pair.substring(idx + 1);
                }
            }
        }
        return null;
    }
}
