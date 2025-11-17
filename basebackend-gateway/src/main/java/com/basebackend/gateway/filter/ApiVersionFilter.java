package com.basebackend.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * API 版本管理过滤器
 *
 * 支持多种版本控制方式：
 * 1. URL 路径版本：/v1/api/users、/v2/api/users
 * 2. 请求头版本：X-API-Version: v1
 * 3. 查询参数版本：/api/users?version=v1
 *
 * 版本路由规则：
 * - 默认版本：v1
 * - 支持版本：v1, v2
 * - 不支持的版本返回 400 Bad Request
 *
 * @author 浮浮酱
 */
@Slf4j
@Component
public class ApiVersionFilter extends AbstractGatewayFilterFactory<ApiVersionFilter.Config> {

    private static final String DEFAULT_VERSION = "v1";
    private static final List<String> SUPPORTED_VERSIONS = Arrays.asList("v1", "v2", "v3");

    public ApiVersionFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // 1. 从 URL 路径中提取版本
            String version = extractVersionFromPath(request.getURI().getPath());

            // 2. 如果路径中没有版本，从请求头中获取
            if (version == null) {
                version = extractVersionFromHeader(request);
            }

            // 3. 如果请求头中没有版本，从查询参数中获取
            if (version == null) {
                version = extractVersionFromQuery(request);
            }

            // 4. 如果都没有，使用默认版本
            if (version == null) {
                version = config.getDefaultVersion() != null ? config.getDefaultVersion() : DEFAULT_VERSION;
                log.debug("未指定 API 版本，使用默认版本: {}", version);
            }

            // 5. 验证版本是否支持
            if (!isVersionSupported(version)) {
                log.warn("不支持的 API 版本: {}, 请求路径: {}", version, request.getURI().getPath());
                return handleUnsupportedVersion(exchange, version);
            }

            // 6. 添加版本信息到请求头（供后端服务使用）
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-API-Version", version)
                    .header("X-Gateway-Version-Resolved", "true")
                    .build();

            // 7. 添加版本信息到响应头
            ServerHttpResponse response = exchange.getResponse();
            response.getHeaders().add("X-API-Version", version);

            log.debug("API 版本解析成功 - 版本: {}, 路径: {}", version, request.getURI().getPath());

            // 8. 如果需要路径重写，去除版本号
            if (config.isStripVersionFromPath() && version != null) {
                String path = request.getURI().getPath();
                String newPath = path.replaceFirst("/" + version, "");

                modifiedRequest = modifiedRequest.mutate()
                        .path(newPath)
                        .build();

                log.debug("路径重写 - 原路径: {}, 新路径: {}", path, newPath);
            }

            // 9. 继续过滤器链
            ServerWebExchange modifiedExchange = exchange.mutate().request(modifiedRequest).build();
            return chain.filter(modifiedExchange);
        };
    }

    /**
     * 从 URL 路径中提取版本
     * 例如：/v1/api/users -> v1
     */
    private String extractVersionFromPath(String path) {
        if (path == null) {
            return null;
        }

        // 匹配 /v1、/v2、/v3 等
        for (String version : SUPPORTED_VERSIONS) {
            if (path.startsWith("/" + version + "/") || path.equals("/" + version)) {
                return version;
            }
        }

        return null;
    }

    /**
     * 从请求头中提取版本
     * 请求头：X-API-Version: v1
     */
    private String extractVersionFromHeader(ServerHttpRequest request) {
        String version = request.getHeaders().getFirst("X-API-Version");
        if (version != null && !version.isEmpty()) {
            // 标准化版本号（支持 v1、V1、1 等格式）
            version = normalizeVersion(version);
        }
        return version;
    }

    /**
     * 从查询参数中提取版本
     * 例如：/api/users?version=v1
     */
    private String extractVersionFromQuery(ServerHttpRequest request) {
        List<String> versionParams = request.getQueryParams().get("version");
        if (versionParams != null && !versionParams.isEmpty()) {
            String version = versionParams.get(0);
            return normalizeVersion(version);
        }
        return null;
    }

    /**
     * 标准化版本号
     * 例如：1 -> v1, V1 -> v1, v1 -> v1
     */
    private String normalizeVersion(String version) {
        if (version == null || version.isEmpty()) {
            return null;
        }

        version = version.trim().toLowerCase();

        // 如果是纯数字，添加 v 前缀
        if (version.matches("\\d+")) {
            version = "v" + version;
        }

        return version;
    }

    /**
     * 检查版本是否支持
     */
    private boolean isVersionSupported(String version) {
        return SUPPORTED_VERSIONS.contains(version);
    }

    /**
     * 处理不支持的版本
     */
    private Mono<Void> handleUnsupportedVersion(ServerWebExchange exchange, String version) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.BAD_REQUEST);
        response.getHeaders().add("Content-Type", "application/json");
        response.getHeaders().add("X-Error-Type", "UNSUPPORTED_VERSION");

        String errorMessage = String.format(
                "{\"success\":false,\"code\":400,\"message\":\"不支持的 API 版本: %s\",\"supportedVersions\":%s}",
                version,
                SUPPORTED_VERSIONS
        );

        return response.writeWith(
                Mono.just(response.bufferFactory().wrap(errorMessage.getBytes()))
        );
    }

    /**
     * 配置类
     */
    public static class Config {
        /**
         * 默认版本
         */
        private String defaultVersion = DEFAULT_VERSION;

        /**
         * 是否从路径中去除版本号
         * 例如：/v1/api/users -> /api/users
         */
        private boolean stripVersionFromPath = true;

        public String getDefaultVersion() {
            return defaultVersion;
        }

        public void setDefaultVersion(String defaultVersion) {
            this.defaultVersion = defaultVersion;
        }

        public boolean isStripVersionFromPath() {
            return stripVersionFromPath;
        }

        public void setStripVersionFromPath(boolean stripVersionFromPath) {
            this.stripVersionFromPath = stripVersionFromPath;
        }
    }
}
