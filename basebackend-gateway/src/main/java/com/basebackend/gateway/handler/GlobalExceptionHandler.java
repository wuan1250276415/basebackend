package com.basebackend.gateway.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Gateway 全局异常处理器
 *
 * 处理各种异常情况：
 * 1. 限流异常（429 Too Many Requests）
 * 2. 服务不可用（503 Service Unavailable）
 * 3. 网关超时（504 Gateway Timeout）
 * 4. 路由未找到（404 Not Found）
 * 5. 其他异常
 *
 * @author 浮浮酱
 */
@Slf4j
@Order(-1)  // 优先级高于默认异常处理器
@Component
@RequiredArgsConstructor
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        // 如果响应已提交，无法处理
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        // 设置响应类型为 JSON
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // 根据异常类型返回不同的响应
        Map<String, Object> errorResponse;

        if (ex instanceof ResponseStatusException) {
            ResponseStatusException statusException = (ResponseStatusException) ex;
            HttpStatus status = (HttpStatus) statusException.getStatusCode();

            // 限流异常（429）
            if (status == HttpStatus.TOO_MANY_REQUESTS) {
                errorResponse = handleRateLimitException(exchange, ex);
            }
            // 服务不可用（503）
            else if (status == HttpStatus.SERVICE_UNAVAILABLE) {
                errorResponse = handleServiceUnavailableException(exchange, ex);
            }
            // 网关超时（504）
            else if (status == HttpStatus.GATEWAY_TIMEOUT) {
                errorResponse = handleGatewayTimeoutException(exchange, ex);
            }
            // 其他状态码异常
            else {
                errorResponse = handleResponseStatusException(exchange, statusException);
            }

            response.setStatusCode(status);
        }
        // 路由未找到
        else if (ex instanceof NotFoundException) {
            errorResponse = handleNotFoundException(exchange, ex);
            response.setStatusCode(HttpStatus.NOT_FOUND);
        }
        // 其他未知异常
        else {
            errorResponse = handleUnknownException(exchange, ex);
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // 返回 JSON 响应
        return response.writeWith(Mono.fromSupplier(() -> {
            DataBufferFactory bufferFactory = response.bufferFactory();
            try {
                byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
                return bufferFactory.wrap(bytes);
            } catch (JsonProcessingException e) {
                log.error("序列化异常响应失败", e);
                return bufferFactory.wrap(new byte[0]);
            }
        }));
    }

    /**
     * 处理限流异常（429 Too Many Requests）
     */
    private Map<String, Object> handleRateLimitException(ServerWebExchange exchange, Throwable ex) {
        String path = exchange.getRequest().getPath().value();
        String ip = exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";

        log.warn("触发限流 - 路径: {}, IP: {}, 异常: {}", path, ip, ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("code", HttpStatus.TOO_MANY_REQUESTS.value());
        response.put("message", "请求过于频繁，请稍后再试");
        response.put("detail", "您的请求已超过限流阈值，请降低请求频率");
        response.put("path", path);
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("retryAfter", "60s");  // 建议 60 秒后重试

        return response;
    }

    /**
     * 处理服务不可用异常（503 Service Unavailable）
     */
    private Map<String, Object> handleServiceUnavailableException(ServerWebExchange exchange, Throwable ex) {
        String path = exchange.getRequest().getPath().value();

        log.error("服务不可用 - 路径: {}, 异常: {}", path, ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("code", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("message", "服务暂时不可用");
        response.put("detail", "后端服务正在维护或升级，请稍后重试");
        response.put("path", path);
        response.put("timestamp", LocalDateTime.now().toString());

        return response;
    }

    /**
     * 处理网关超时异常（504 Gateway Timeout）
     */
    private Map<String, Object> handleGatewayTimeoutException(ServerWebExchange exchange, Throwable ex) {
        String path = exchange.getRequest().getPath().value();

        log.error("网关超时 - 路径: {}, 异常: {}", path, ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("code", HttpStatus.GATEWAY_TIMEOUT.value());
        response.put("message", "请求超时");
        response.put("detail", "后端服务响应超时，请稍后重试");
        response.put("path", path);
        response.put("timestamp", LocalDateTime.now().toString());

        return response;
    }

    /**
     * 处理路由未找到异常（404 Not Found）
     */
    private Map<String, Object> handleNotFoundException(ServerWebExchange exchange, Throwable ex) {
        String path = exchange.getRequest().getPath().value();

        log.warn("路由未找到 - 路径: {}, 异常: {}", path, ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("code", HttpStatus.NOT_FOUND.value());
        response.put("message", "请求的资源不存在");
        response.put("detail", "未找到匹配的路由或服务");
        response.put("path", path);
        response.put("timestamp", LocalDateTime.now().toString());

        return response;
    }

    /**
     * 处理 ResponseStatusException
     */
    private Map<String, Object> handleResponseStatusException(ServerWebExchange exchange, ResponseStatusException ex) {
        String path = exchange.getRequest().getPath().value();
        HttpStatus status = (HttpStatus) ex.getStatusCode();

        log.warn("响应状态异常 - 路径: {}, 状态码: {}, 异常: {}", path, status.value(), ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("code", status.value());
        response.put("message", ex.getReason() != null ? ex.getReason() : status.getReasonPhrase());
        response.put("detail", ex.getMessage());
        response.put("path", path);
        response.put("timestamp", LocalDateTime.now().toString());

        return response;
    }

    /**
     * 处理未知异常（500 Internal Server Error）
     */
    private Map<String, Object> handleUnknownException(ServerWebExchange exchange, Throwable ex) {
        String path = exchange.getRequest().getPath().value();

        log.error("未知异常 - 路径: {}, 异常类型: {}, 异常信息: {}",
                path, ex.getClass().getSimpleName(), ex.getMessage(), ex);

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("code", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("message", "服务器内部错误");
        response.put("detail", "系统发生未知错误，请联系管理员");
        response.put("path", path);
        response.put("timestamp", LocalDateTime.now().toString());

        return response;
    }
}
