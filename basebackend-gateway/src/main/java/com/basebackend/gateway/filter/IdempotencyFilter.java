package com.basebackend.gateway.filter;

import com.basebackend.gateway.enums.GatewayErrorCode;
import com.alibaba.fastjson2.JSON;
import com.basebackend.common.model.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * 请求去重过滤器（幂等性保护）
 * <p>
 * 防止客户端重复提交请求（如按钮双击、网络重试）。
 * 支持基于幂等性键的请求去重。
 * </p>
 *
 * <h3>幂等性键来源：</h3>
 * <ol>
 * <li>请求头 <code>X-Idempotency-Key</code>（客户端提供）</li>
 * <li>自动生成：用户ID + 请求路径 + 请求体 hash</li>
 * </ol>
 *
 * <h3>适用场景：</h3>
 * <ul>
 * <li>POST/PUT/DELETE 等写操作</li>
 * <li>支付、订单创建等敏感操作</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyFilter implements GlobalFilter, Ordered {

    private final ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 幂等性检查是否启用
     */
    @Value("${gateway.idempotency.enabled:false}")
    private boolean idempotencyEnabled;

    /**
     * 幂等性键有效期（秒）
     */
    @Value("${gateway.idempotency.ttl:300}")
    private long idempotencyTtl;

    /**
     * 需要幂等性检查的路径
     */
    @Value("${gateway.idempotency.paths:}")
    private List<String> idempotencyPaths;

    /**
     * 需要幂等性检查的 HTTP 方法
     */
    private static final Set<HttpMethod> IDEMPOTENCY_METHODS = Set.of(
            HttpMethod.POST,
            HttpMethod.PUT,
            HttpMethod.PATCH,
            HttpMethod.DELETE);

    private static final String HEADER_IDEMPOTENCY_KEY = "X-Idempotency-Key";
    private static final String IDEMPOTENCY_KEY_PREFIX = "gateway:idempotency:";
    private static final String PROCESSING = "PROCESSING";
    private static final String COMPLETED = "COMPLETED";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 如果未启用幂等性检查，直接放行
        if (!idempotencyEnabled) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();

        // 只检查写操作
        if (request.getMethod() == null || !IDEMPOTENCY_METHODS.contains(request.getMethod())) {
            return chain.filter(exchange);
        }

        String path = request.getPath().toString();

        // 检查是否需要幂等性检查
        if (!requiresIdempotency(path)) {
            return chain.filter(exchange);
        }

        // 获取幂等性键
        String idempotencyKey = getIdempotencyKey(request);
        if (!StringUtils.hasText(idempotencyKey)) {
            log.debug("未提供幂等性键，跳过检查: path={}", path);
            return chain.filter(exchange);
        }

        String redisKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
        String traceId = request.getHeaders().getFirst(TraceIdFilter.TRACE_ID_HEADER);

        // 尝试设置处理中状态
        return reactiveRedisTemplate.opsForValue()
                .setIfAbsent(redisKey, PROCESSING + ":" + traceId, Duration.ofSeconds(idempotencyTtl))
                .flatMap(success -> {
                    if (Boolean.TRUE.equals(success)) {
                        // 首次请求，继续处理
                        log.debug("[{}] 幂等性检查通过: key={}", traceId, idempotencyKey);

                        return chain.filter(exchange)
                                .doOnSuccess(v -> {
                                    // 请求完成后更新状态
                                    int statusCode = exchange.getResponse().getStatusCode() != null
                                            ? exchange.getResponse().getStatusCode().value()
                                            : 200;
                                    String completedValue = COMPLETED + ":" + statusCode + ":" + traceId;
                                    reactiveRedisTemplate.opsForValue()
                                            .set(redisKey, completedValue, Duration.ofSeconds(idempotencyTtl))
                                            .subscribe();
                                })
                                .doOnError(e -> {
                                    // 请求失败，删除键允许重试
                                    reactiveRedisTemplate.delete(redisKey).subscribe();
                                });
                    } else {
                        // 重复请求
                        return reactiveRedisTemplate.opsForValue().get(redisKey)
                                .flatMap(value -> {
                                    String status = value != null ? value.toString() : PROCESSING;
                                    if (status.startsWith(PROCESSING)) {
                                        // 请求仍在处理中
                                        log.warn("[{}] 幂等性检查：请求正在处理中: key={}",
                                                traceId, idempotencyKey);
                                        return conflictResponse(exchange.getResponse(),
                                                "请求正在处理中，请勿重复提交");
                                    } else if (status.startsWith(COMPLETED)) {
                                        // 请求已完成
                                        log.info("[{}] 幂等性检查：请求已完成: key={}",
                                                traceId, idempotencyKey);
                                        return conflictResponse(exchange.getResponse(),
                                                "请求已处理，请勿重复提交");
                                    }
                                    return chain.filter(exchange);
                                });
                    }
                });
    }

    /**
     * 检查路径是否需要幂等性检查
     */
    private boolean requiresIdempotency(String path) {
        if (idempotencyPaths == null || idempotencyPaths.isEmpty()) {
            return false;
        }

        for (String pattern : idempotencyPaths) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 获取幂等性键
     */
    private String getIdempotencyKey(ServerHttpRequest request) {
        // 优先使用客户端提供的幂等性键
        String clientKey = request.getHeaders().getFirst(HEADER_IDEMPOTENCY_KEY);
        if (StringUtils.hasText(clientKey)) {
            return clientKey;
        }

        // 自动生成：基于用户 ID + 路径
        String userId = request.getHeaders().getFirst("X-User-Id");
        if (!StringUtils.hasText(userId)) {
            return null;
        }

        String path = request.getPath().toString();
        String method = request.getMethod() != null ? request.getMethod().name() : "POST";

        return userId + ":" + method + ":" + path;
    }

    /**
     * 返回冲突响应
     */
    private Mono<Void> conflictResponse(ServerHttpResponse response, String message) {
        if (response.isCommitted()) {
            return Mono.empty();
        }

        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        response.setStatusCode(HttpStatus.CONFLICT);

        Result<?> result = Result.error(GatewayErrorCode.REQUEST_DUPLICATE, message);
        String body = JSON.toJSONString(result);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // 在认证之后执行
        return -50;
    }
}
