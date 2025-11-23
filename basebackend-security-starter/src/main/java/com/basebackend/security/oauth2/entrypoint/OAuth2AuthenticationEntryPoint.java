package com.basebackend.security.oauth2.entrypoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.basebackend.common.model.Result;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * OAuth2认证入口点
 *
 * 处理未认证的访问请求，返回标准化的错误响应
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public OAuth2AuthenticationEntryPoint() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void commence(HttpServletRequest request,
                        HttpServletResponse response,
                        AuthenticationException authException) throws IOException, ServletException {

        // 记录认证失败信息
        log.warn("OAuth2认证失败 - URI: {}, Error: {}, Message: {}",
            request.getRequestURI(),
            authException.getClass().getSimpleName(),
            authException.getMessage());

        // 处理OAuth2特定异常
        if (authException instanceof OAuth2AuthenticationException) {
            OAuth2AuthenticationException oauth2Exception = (OAuth2AuthenticationException) authException;
            OAuth2Error error = oauth2Exception.getError();

            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                "UNAUTHORIZED",
                error.getDescription(),
                Map.of(
                    "error", error.getErrorCode(),
                    "error_description", error.getDescription(),
                    "error_uri", error.getUri() != null ? error.getUri().toString() : null,
                    "timestamp", LocalDateTime.now().toString(),
                    "path", request.getRequestURI()
                ));
            return;
        }

        // 处理通用认证异常
        String errorCode = determineErrorCode(authException);
        String errorDescription = authException.getMessage();

        // 对于常见错误，返回更友好的错误描述
        if (authException.getMessage() != null) {
            if (authException.getMessage().contains("JWT") ||
                authException.getMessage().contains("Token")) {
                errorDescription = "无效或过期的Token";
            } else if (authException.getMessage().contains("expired")) {
                errorDescription = "Token已过期，请重新登录";
            } else if (authException.getMessage().contains("signature")) {
                errorDescription = "Token签名无效";
            }
        }

        sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
            errorCode,
            errorDescription,
            Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "path", request.getRequestURI(),
                "method", request.getMethod()
            ));
    }

    /**
     * 确定错误代码
     *
     * @param authException 认证异常
     * @return 错误代码
     */
    private String determineErrorCode(AuthenticationException authException) {
        String message = authException.getMessage();

        if (message == null) {
            return "AUTHENTICATION_FAILED";
        }

        if (message.contains("expired") || message.contains("过期")) {
            return "TOKEN_EXPIRED";
        } else if (message.contains("signature") || message.contains("签名")) {
            return "INVALID_SIGNATURE";
        } else if (message.contains("malformed") || message.contains("格式错误")) {
            return "MALFORMED_TOKEN";
        } else if (message.contains("unsupported") || message.contains("不支持")) {
            return "UNSUPPORTED_TOKEN";
        } else if (message.contains("required") || message.contains("必需")) {
            return "TOKEN_REQUIRED";
        } else if (message.contains("audience") || message.contains("受众")) {
            return "INVALID_AUDIENCE";
        } else if (message.contains("issuer") || message.contains("签发者")) {
            return "INVALID_ISSUER";
        } else {
            return "AUTHENTICATION_REQUIRED";
        }
    }

    /**
     * 发送错误响应
     *
     * @param response HTTP响应
     * @param status HTTP状态码
     * @param errorCode 错误代码
     * @param errorMessage 错误消息
     * @param additionalInfo 附加信息
     * @throws IOException 写入响应异常
     */
    private void sendErrorResponse(HttpServletResponse response,
                                   int status,
                                   String errorCode,
                                   String errorMessage,
                                   Map<String, Object> additionalInfo) throws IOException {

        Result<Object> result = Result.error(status, errorMessage);

        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("errorCode", errorCode);
        if (additionalInfo != null) {
            payload.putAll(additionalInfo);
        }
        if (!payload.isEmpty()) {
            result.setData(payload);
        }

        // 设置响应头
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("X-Content-Type-Options", "nosniff");

        // 写入响应体
        response.getWriter().write(objectMapper.writeValueAsString(result));
        response.getWriter().flush();

        log.info("已发送OAuth2认证错误响应 - Status: {}, Code: {}, Message: {}",
            status, errorCode, errorMessage);
    }
}
