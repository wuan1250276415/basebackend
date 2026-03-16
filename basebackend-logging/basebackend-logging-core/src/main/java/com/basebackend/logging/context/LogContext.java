package com.basebackend.logging.context;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * 日志上下文管理
 * 用于在整个请求链路中传递 TraceId、UserId、RequestId 等信息
 */
public class LogContext {

    private static final String TRACE_ID = "traceId";
    private static final String REQUEST_ID = "requestId";
    private static final String USER_ID = "userId";
    private static final String USERNAME = "username";
    private static final String IP_ADDRESS = "ipAddress";
    private static final String URI = "uri";
    private static final String METHOD = "method";

    /**
     * 初始化日志上下文
     */
    public static void init() {
        setTraceId(generateTraceId());
        setRequestId(generateRequestId());
    }

    /**
     * 生成 TraceId
     */
    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成 RequestId
     */
    public static String generateRequestId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 设置 TraceId
     *
     * <p>Logback 的 {@code MDC.put(key, null)} 会抛出 {@link IllegalArgumentException}，
     * 所有 setter 均忽略 null 值，避免上层调用者需要自行判空。
     */
    public static void setTraceId(String traceId) {
        if (traceId != null) MDC.put(TRACE_ID, traceId);
    }

    /**
     * 获取 TraceId
     */
    public static String getTraceId() {
        return MDC.get(TRACE_ID);
    }

    /** 设置 RequestId（null 安全） */
    public static void setRequestId(String requestId) {
        if (requestId != null) MDC.put(REQUEST_ID, requestId);
    }

    /**
     * 获取 RequestId
     */
    public static String getRequestId() {
        return MDC.get(REQUEST_ID);
    }

    /** 设置 UserId（null 安全） */
    public static void setUserId(String userId) {
        if (userId != null) MDC.put(USER_ID, userId);
    }

    /**
     * 获取 UserId
     */
    public static String getUserId() {
        return MDC.get(USER_ID);
    }

    /** 设置 Username（null 安全） */
    public static void setUsername(String username) {
        if (username != null) MDC.put(USERNAME, username);
    }

    /**
     * 获取 Username
     */
    public static String getUsername() {
        return MDC.get(USERNAME);
    }

    /** 设置 IP 地址（null 安全） */
    public static void setIpAddress(String ipAddress) {
        if (ipAddress != null) MDC.put(IP_ADDRESS, ipAddress);
    }

    /**
     * 获取 IP 地址
     */
    public static String getIpAddress() {
        return MDC.get(IP_ADDRESS);
    }

    /** 设置 URI（null 安全） */
    public static void setUri(String uri) {
        if (uri != null) MDC.put(URI, uri);
    }

    /**
     * 获取 URI
     */
    public static String getUri() {
        return MDC.get(URI);
    }

    /** 设置 HTTP Method（null 安全） */
    public static void setMethod(String method) {
        if (method != null) MDC.put(METHOD, method);
    }

    /**
     * 获取 HTTP Method
     */
    public static String getMethod() {
        return MDC.get(METHOD);
    }

    /**
     * 清除日志上下文
     */
    public static void clear() {
        MDC.clear();
    }

    /**
     * 设置自定义属性
     */
    public static void put(String key, String value) {
        MDC.put(key, value);
    }

    /**
     * 获取自定义属性
     */
    public static String get(String key) {
        return MDC.get(key);
    }
}
