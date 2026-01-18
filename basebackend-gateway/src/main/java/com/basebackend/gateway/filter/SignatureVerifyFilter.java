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
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

/**
 * 请求签名验证过滤器
 * <p>
 * 提供 API 请求签名验证和防重放攻击保护。
 * </p>
 *
 * <h3>签名规则：</h3>
 * <ol>
 * <li>按照固定顺序拼接：appId + timestamp + nonce + method + path</li>
 * <li>使用 HMAC-SHA256 算法计算签名</li>
 * <li>将签名进行 Base64 编码</li>
 * </ol>
 *
 * <h3>防重放机制：</h3>
 * <ul>
 * <li>时间戳有效期：默认 5 分钟内</li>
 * <li>Nonce 唯一性：Redis 存储已使用的 nonce（TTL=时间戳有效期）</li>
 * </ul>
 *
 * <h3>请求头说明：</h3>
 * <ul>
 * <li><b>X-App-Id</b>: 应用 ID</li>
 * <li><b>X-Timestamp</b>: 请求时间戳（毫秒）</li>
 * <li><b>X-Nonce</b>: 随机字符串（防重放）</li>
 * <li><b>X-Signature</b>: 请求签名</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SignatureVerifyFilter implements GlobalFilter, Ordered {

    private final ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 签名验证是否启用
     */
    @Value("${gateway.security.signature.enabled:false}")
    private boolean signatureEnabled;

    /**
     * 签名密钥（生产环境应从密钥管理服务获取）
     */
    @Value("${gateway.security.signature.secret-key:defaultSecretKey123456}")
    private String secretKey;

    /**
     * 时间戳有效期（毫秒）
     */
    @Value("${gateway.security.signature.timestamp-validity:300000}")
    private long timestampValidity;

    /**
     * 需要签名验证的路径
     */
    @Value("${gateway.security.signature.paths:}")
    private List<String> signaturePaths;

    /**
     * 签名验证白名单路径
     */
    @Value("${gateway.security.signature.whitelist:}")
    private List<String> signatureWhitelist;

    private static final String HEADER_APP_ID = "X-App-Id";
    private static final String HEADER_TIMESTAMP = "X-Timestamp";
    private static final String HEADER_NONCE = "X-Nonce";
    private static final String HEADER_SIGNATURE = "X-Signature";

    private static final String NONCE_KEY_PREFIX = "gateway:nonce:";
    private static final String ALGORITHM = "HmacSHA256";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 如果签名验证未启用，直接放行
        if (!signatureEnabled) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().toString();

        // 检查是否需要签名验证
        if (!requiresSignature(path)) {
            return chain.filter(exchange);
        }

        // 获取签名相关头信息
        String appId = request.getHeaders().getFirst(HEADER_APP_ID);
        String timestamp = request.getHeaders().getFirst(HEADER_TIMESTAMP);
        String nonce = request.getHeaders().getFirst(HEADER_NONCE);
        String signature = request.getHeaders().getFirst(HEADER_SIGNATURE);

        // 验证必要参数
        if (!StringUtils.hasText(appId) || !StringUtils.hasText(timestamp) ||
                !StringUtils.hasText(nonce) || !StringUtils.hasText(signature)) {
            log.warn("签名验证失败 - 缺少必要参数: path={}", path);
            return unauthorized(exchange.getResponse(), "签名验证失败，缺少必要参数");
        }

        // 验证时间戳
        long requestTime;
        try {
            requestTime = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            log.warn("签名验证失败 - 时间戳格式错误: timestamp={}", timestamp);
            return unauthorized(exchange.getResponse(), "签名验证失败，时间戳格式错误");
        }

        long currentTime = System.currentTimeMillis();
        if (Math.abs(currentTime - requestTime) > timestampValidity) {
            log.warn("签名验证失败 - 时间戳过期: timestamp={}, current={}", timestamp, currentTime);
            return unauthorized(exchange.getResponse(), "签名验证失败，请求已过期");
        }

        // 验证 nonce 防重放
        String nonceKey = NONCE_KEY_PREFIX + nonce;
        return reactiveRedisTemplate.hasKey(nonceKey)
                .flatMap(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        log.warn("签名验证失败 - nonce 已使用: nonce={}", nonce);
                        return unauthorized(exchange.getResponse(), "签名验证失败，请求已处理");
                    }

                    // 验证签名
                    String method = request.getMethod() != null ? request.getMethod().name() : "GET";
                    String expectedSignature = calculateSignature(appId, timestamp, nonce, method, path);

                    if (!signature.equals(expectedSignature)) {
                        log.warn("签名验证失败 - 签名不匹配: path={}, appId={}", path, appId);
                        return unauthorized(exchange.getResponse(), "签名验证失败，签名不正确");
                    }

                    // 存储 nonce，防止重放
                    Duration nonceTtl = Duration.ofMillis(timestampValidity * 2);
                    return reactiveRedisTemplate.opsForValue()
                            .set(nonceKey, "1", nonceTtl)
                            .then(chain.filter(exchange));
                });
    }

    /**
     * 检查路径是否需要签名验证
     */
    private boolean requiresSignature(String path) {
        // 检查白名单
        if (signatureWhitelist != null) {
            for (String pattern : signatureWhitelist) {
                if (pathMatcher.match(pattern, path)) {
                    return false;
                }
            }
        }

        // 检查是否在签名路径列表中
        if (signaturePaths == null || signaturePaths.isEmpty()) {
            return false; // 默认不需要签名
        }

        for (String pattern : signaturePaths) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 计算请求签名
     */
    private String calculateSignature(String appId, String timestamp, String nonce,
            String method, String path) {
        String data = String.join("|", appId, timestamp, nonce, method, path);
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            mac.init(keySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("签名计算失败", e);
            return "";
        }
    }

    /**
     * 返回未授权响应
     */
    private Mono<Void> unauthorized(ServerHttpResponse response, String message) {
        if (response.isCommitted()) {
            return Mono.empty();
        }

        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        response.setStatusCode(HttpStatus.UNAUTHORIZED);

        Result<?> result = Result.error(GatewayErrorCode.TOKEN_INVALID, message);
        String body = JSON.toJSONString(result);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // 在认证过滤器之前执行
        return -150;
    }
}
