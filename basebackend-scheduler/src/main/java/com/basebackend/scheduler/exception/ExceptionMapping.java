package com.basebackend.scheduler.exception;

import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 异常映射配置
 *
 * <p>统一定义错误码与HTTP状态码之间的映射关系，确保异常处理的一致性。
 * 这个类提供了灵活的映射规则，支持精确匹配和前缀匹配。
 *
 * <p>使用示例：
 * <pre>
 * // 获取错误码对应的HTTP状态码
 * HttpStatus status = ExceptionMapping.getHttpStatus("VALIDATION_FAILED");
 * </pre>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
public final class ExceptionMapping {

    // ========== HTTP状态码常量 ==========

    /**
     * 400 Bad Request - 客户端请求错误
     */
    public static final String BAD_REQUEST = "400";

    /**
     * 401 Unauthorized - 未授权
     */
    public static final String UNAUTHORIZED = "401";

    /**
     * 403 Forbidden - 禁止访问
     */
    public static final String FORBIDDEN = "403";

    /**
     * 404 Not Found - 资源不存在
     */
    public static final String NOT_FOUND = "404";

    /**
     * 409 Conflict - 资源冲突
     */
    public static final String CONFLICT = "409";

    /**
     * 422 Unprocessable Entity - 请求格式正确但语义错误
     */
    public static final String UNPROCESSABLE_ENTITY = "422";

    /**
     * 429 Too Many Requests - 请求过多
     */
    public static final String TOO_MANY_REQUESTS = "429";

    /**
     * 500 Internal Server Error - 服务器内部错误
     */
    public static final String INTERNAL_SERVER_ERROR = "500";

    /**
     * 502 Bad Gateway - 网关错误
     */
    public static final String BAD_GATEWAY = "502";

    /**
     * 503 Service Unavailable - 服务不可用
     */
    public static final String SERVICE_UNAVAILABLE = "503";

    /**
     * 504 Gateway Timeout - 网关超时
     */
    public static final String GATEWAY_TIMEOUT = "504";

    // ========== 精确匹配映射 ==========

    /**
     * 精确匹配的错误码映射
     *
     * <p>这些错误码会精确匹配对应的HTTP状态码。
     */
    private static final Map<String, String> EXACT_MATCH_MAPPING;

    static {
        Map<String, String> mapping = new HashMap<>();

        // 4xx 客户端错误
        mapping.put("VALIDATION_FAILED", BAD_REQUEST);
        mapping.put("BIND_FAILED", BAD_REQUEST);
        mapping.put("TYPE_MISMATCH", BAD_REQUEST);
        mapping.put("MISSING_PARAMETER", BAD_REQUEST);
        mapping.put("JSON_PARSE_ERROR", BAD_REQUEST);
        mapping.put("INVALID_ARGUMENT", BAD_REQUEST);
        mapping.put("PARAM_REQUIRED", BAD_REQUEST);
        mapping.put("PARAM_INVALID", BAD_REQUEST);

        mapping.put("UNAUTHORIZED", UNAUTHORIZED);
        mapping.put("AUTHENTICATION_FAILED", UNAUTHORIZED);

        mapping.put("FORBIDDEN", FORBIDDEN);
        mapping.put("ACCESS_DENIED", FORBIDDEN);

        mapping.put("NOT_FOUND", NOT_FOUND);
        mapping.put("RESOURCE_NOT_FOUND", NOT_FOUND);
        mapping.put("DATA_NOT_FOUND", NOT_FOUND);

        mapping.put("CONFLICT", CONFLICT);
        mapping.put("STATE_CONFLICT", CONFLICT);
        mapping.put("OPTIMISTIC_LOCK_CONFLICT", CONFLICT);
        mapping.put("DUPLICATE_RESOURCE", CONFLICT);

        mapping.put("UNPROCESSABLE_ENTITY", UNPROCESSABLE_ENTITY);

        // 5xx 服务器错误
        mapping.put("INTERNAL_SERVER_ERROR", INTERNAL_SERVER_ERROR);
        mapping.put("SYSTEM_ERROR", INTERNAL_SERVER_ERROR);
        mapping.put("ENGINE_ERROR", INTERNAL_SERVER_ERROR);
        mapping.put("DATABASE_ERROR", INTERNAL_SERVER_ERROR);

        mapping.put("BAD_GATEWAY", BAD_GATEWAY);

        mapping.put("SERVICE_UNAVAILABLE", SERVICE_UNAVAILABLE);
        mapping.put("MAINTENANCE_MODE", SERVICE_UNAVAILABLE);

        mapping.put("GATEWAY_TIMEOUT", GATEWAY_TIMEOUT);

        EXACT_MATCH_MAPPING = Collections.unmodifiableMap(mapping);
    }

    // ========== 前缀匹配映射 ==========

    /**
     * 前缀匹配规则
     *
     * <p>如果错误码以这些前缀开头，则使用对应的HTTP状态码。
     */
    private static final Map<String, String> PREFIX_MATCH_RULES;

    static {
        Map<String, String> rules = new HashMap<>();

        // 客户端错误前缀
        rules.put("VALIDATION_", BAD_REQUEST);
        rules.put("PARAM_", BAD_REQUEST);
        rules.put("INVALID_", BAD_REQUEST);
        rules.put("MISSING_", BAD_REQUEST);
        rules.put("BAD_REQUEST", BAD_REQUEST);

        rules.put("UNAUTHORIZED", UNAUTHORIZED);
        rules.put("AUTH_", UNAUTHORIZED);

        rules.put("FORBIDDEN", FORBIDDEN);

        rules.put("NOT_FOUND", NOT_FOUND);
        rules.put("RESOURCE_", NOT_FOUND);
        rules.put("DATA_", NOT_FOUND);

        rules.put("CONFLICT", CONFLICT);
        rules.put("DUPLICATE_", CONFLICT);
        rules.put("ALREADY_", CONFLICT);

        // 服务器错误前缀
        rules.put("INTERNAL_", INTERNAL_SERVER_ERROR);
        rules.put("SYSTEM_", INTERNAL_SERVER_ERROR);
        rules.put("ENGINE_", INTERNAL_SERVER_ERROR);
        rules.put("DATABASE_", INTERNAL_SERVER_ERROR);
        rules.put("SERVER_", INTERNAL_SERVER_ERROR);

        rules.put("SERVICE_", SERVICE_UNAVAILABLE);
        rules.put("MAINTENANCE", SERVICE_UNAVAILABLE);

        PREFIX_MATCH_RULES = Collections.unmodifiableMap(rules);
    }

    /**
     * 私有构造函数，防止实例化工具类
     */
    private ExceptionMapping() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 根据错误码获取对应的HTTP状态码
     *
     * <p>匹配规则：
     * <ol>
     *   <li>精确匹配优先</li>
     *   <li>其次是前缀匹配</li>
     *   <li>最后返回500（服务器内部错误）</li>
     * </ol>
     *
     * @param errorCode 错误码
     * @return HTTP状态码
     */
    public static String getHttpStatus(String errorCode) {
        if (errorCode == null || errorCode.trim().isEmpty()) {
            return INTERNAL_SERVER_ERROR;
        }

        // 1. 精确匹配
        if (EXACT_MATCH_MAPPING.containsKey(errorCode)) {
            return EXACT_MATCH_MAPPING.get(errorCode);
        }

        // 2. 前缀匹配
        for (Map.Entry<String, String> entry : PREFIX_MATCH_RULES.entrySet()) {
            if (errorCode.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }

        // 3. 默认返回500
        return INTERNAL_SERVER_ERROR;
    }

    /**
     * 根据错误码获取对应的HTTP状态枚举
     *
     * @param errorCode 错误码
     * @return HTTP状态枚举
     */
    public static HttpStatus getHttpStatusEnum(String errorCode) {
        String statusCode = getHttpStatus(errorCode);
        try {
            int code = Integer.parseInt(statusCode);
            return HttpStatus.valueOf(code);
        } catch (Exception e) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

    /**
     * 判断错误码是否为客户端错误（4xx）
     *
     * @param errorCode 错误码
     * @return true 如果是客户端错误
     */
    public static boolean isClientError(String errorCode) {
        String status = getHttpStatus(errorCode);
        return status.startsWith("4");
    }

    /**
     * 判断错误码是否为服务器错误（5xx）
     *
     * @param errorCode 错误码
     * @return true 如果是服务器错误
     */
    public static boolean isServerError(String errorCode) {
        String status = getHttpStatus(errorCode);
        return status.startsWith("5");
    }

    /**
     * 获取错误码的严重程度描述
     *
     * @param errorCode 错误码
     * @return 严重程度描述
     */
    public static String getSeverity(String errorCode) {
        if (isServerError(errorCode)) {
            return "HIGH"; // 服务器错误，优先级高
        } else if (errorCode != null &&
                (errorCode.contains("CONFLICT") || errorCode.contains("FORBIDDEN"))) {
            return "MEDIUM"; // 冲突或权限问题，优先级中等
        } else {
            return "LOW"; // 其他错误，优先级低
        }
    }

    /**
     * 获取所有精确匹配的映射（用于调试和文档）
     *
     * @return 精确匹配映射的只读副本
     */
    public static Map<String, String> getExactMatchMapping() {
        return EXACT_MATCH_MAPPING;
    }

    /**
     * 获取所有前缀匹配规则（用于调试和文档）
     *
     * @return 前缀匹配规则的只读副本
     */
    public static Map<String, String> getPrefixMatchRules() {
        return PREFIX_MATCH_RULES;
    }
}
