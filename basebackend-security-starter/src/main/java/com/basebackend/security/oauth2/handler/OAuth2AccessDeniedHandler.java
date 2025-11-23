package com.basebackend.security.oauth2.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.basebackend.common.model.Result;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * OAuth2访问拒绝处理器
 *
 * 处理已认证但权限不足的访问请求
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public OAuth2AccessDeniedHandler() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void handle(HttpServletRequest request,
                      HttpServletResponse response,
                      AccessDeniedException accessDeniedException) throws IOException, ServletException {

        // 记录权限拒绝信息
        log.warn("OAuth2访问被拒绝 - URI: {}, User: {}, Error: {}",
            request.getRequestURI(),
            getCurrentUserId(request),
            accessDeniedException.getMessage());

        // 获取权限信息
        String requiredPermission = extractRequiredPermission(accessDeniedException);
        String currentUserPermissions = extractCurrentUserPermissions(request);

        // 构建响应数据
        Result<Object> result = Result.error(HttpServletResponse.SC_FORBIDDEN, "访问被拒绝：权限不足");

        Map<String, Object> additionalInfo = Map.of(
            "timestamp", LocalDateTime.now().toString(),
            "path", request.getRequestURI(),
            "method", request.getMethod(),
            "required_permission", requiredPermission,
            "current_permissions", currentUserPermissions != null ? currentUserPermissions.split(",") : new String[0],
            "suggestion", "请联系管理员获取相应的权限"
        );

        result.setData(additionalInfo);

        // 设置响应头
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("X-Content-Type-Options", "nosniff");

        // 写入响应体
        response.getWriter().write(objectMapper.writeValueAsString(result));
        response.getWriter().flush();

        log.info("已发送OAuth2访问拒绝响应 - Status: {}, Code: ACCESS_DENIED, Required: {}, Current: {}",
            HttpServletResponse.SC_FORBIDDEN, requiredPermission, currentUserPermissions);
    }

    /**
     * 从异常中提取所需权限
     *
     * @param exception 访问拒绝异常
     * @return 所需权限
     */
    private String extractRequiredPermission(AccessDeniedException exception) {
        String message = exception.getMessage();

        if (message == null) {
            return "未知权限";
        }

        // 尝试从异常消息中提取权限信息
        if (message.contains("insufficient")) {
            return extractPermissionFromMessage(message);
        }

        // 返回原始消息（作为最后手段）
        return message;
    }

    /**
     * 从异常消息中提取权限
     *
     * @param message 异常消息
     * @return 权限字符串
     */
    private String extractPermissionFromMessage(String message) {
        // 这里可以添加更复杂的解析逻辑
        // 例如从SpEL表达式中提取权限名称
        if (message.contains("#")) {
            int startIndex = message.indexOf("#") + 1;
            int endIndex = message.indexOf(" ", startIndex);
            if (endIndex == -1) {
                endIndex = message.length();
            }
            return message.substring(startIndex, endIndex).trim();
        }

        return "需要特定权限";
    }

    /**
     * 从请求中提取当前用户的权限信息
     *
     * @param request HTTP请求
     * @return 当前用户权限（逗号分隔）
     */
    private String extractCurrentUserPermissions(HttpServletRequest request) {
        try {
            // 从SecurityContext中提取权限信息
            Object principal = request.getUserPrincipal();
            if (principal != null) {
                // 如果是OAuth2Authentication，可以提取权限信息
                // 这里简化处理，实际应该从认证对象中获取
                log.debug("当前用户: {}", principal);
                return extractPermissionsFromPrincipal(principal);
            }
        } catch (Exception e) {
            log.debug("提取当前用户权限失败", e);
        }
        return null;
    }

    /**
     * 从认证对象中提取权限
     *
     * @param principal 认证主体
     * @return 权限字符串
     */
    private String extractPermissionsFromPrincipal(Object principal) {
        try {
            // 尝试获取权限信息
            if (principal instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken) {
                org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken jwtAuth =
                    (org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken) principal;

                return jwtAuth.getAuthorities().stream()
                    .map(auth -> auth.getAuthority())
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");
            }

            // 其他类型的认证对象处理逻辑...
            return principal.toString();

        } catch (Exception e) {
            log.debug("从认证对象提取权限失败", e);
            return "未知";
        }
    }

    /**
     * 获取当前用户ID
     *
     * @param request HTTP请求
     * @return 用户ID
     */
    private String getCurrentUserId(HttpServletRequest request) {
        try {
            Object principal = request.getUserPrincipal();
            if (principal != null) {
                return principal.toString();
            }
        } catch (Exception e) {
            log.debug("获取当前用户ID失败", e);
        }
        return "匿名用户";
    }
}
