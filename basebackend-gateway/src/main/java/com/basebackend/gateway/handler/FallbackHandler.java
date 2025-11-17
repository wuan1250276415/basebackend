package com.basebackend.gateway.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Gateway 降级处理器
 *
 * 当服务不可用、超时或达到熔断阈值时，提供友好的降级响应
 *
 * @author 浮浮酱
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FallbackHandler implements HandlerFunction<ServerResponse> {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<ServerResponse> handle(ServerRequest request) {
        String path = request.path();
        log.warn("触发降级处理 - 路径: {}, 方法: {}", path, request.method());

        // 根据不同的服务返回不同的降级响应
        if (path.contains("/admin-api")) {
            return handleAdminApiFallback(request);
        } else if (path.contains("/api/files")) {
            return handleFileServiceFallback(request);
        } else {
            return handleDefaultFallback(request);
        }
    }

    /**
     * Admin API 降级响应
     */
    private Mono<ServerResponse> handleAdminApiFallback(ServerRequest request) {
        Map<String, Object> response = createFallbackResponse(
                "Admin API 服务暂时不可用",
                "请稍后重试或联系管理员",
                request.path()
        );

        return ServerResponse
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(response));
    }

    /**
     * 文件服务降级响应
     */
    private Mono<ServerResponse> handleFileServiceFallback(ServerRequest request) {
        Map<String, Object> response = createFallbackResponse(
                "文件服务暂时不可用",
                "文件上传/下载功能暂时无法使用，请稍后重试",
                request.path()
        );

        return ServerResponse
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(response));
    }

    /**
     * 默认降级响应
     */
    private Mono<ServerResponse> handleDefaultFallback(ServerRequest request) {
        Map<String, Object> response = createFallbackResponse(
                "服务暂时不可用",
                "系统繁忙，请稍后重试",
                request.path()
        );

        return ServerResponse
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(response));
    }

    /**
     * 创建降级响应数据
     */
    private Map<String, Object> createFallbackResponse(String message, String detail, String path) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("code", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("message", message);
        response.put("detail", detail);
        response.put("path", path);
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("fallback", true);

        return response;
    }
}
