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
     */
    public static void setTraceId(String traceId) {
        MDC.put(TRACE_ID, traceId);
    }

    /**
     * 获取 TraceId
     */
    public static String getTraceId() {
        return MDC.get(TRACE_ID);
    }

    /**
     * 设置 RequestId
     */
    public static void setRequestId(String requestId) {
        MDC.put(REQUEST_ID, requestId);
    }

    /**
     * 获取 RequestId
     */
    public static String getRequestId() {
        return MDC.get(REQUEST_ID);
    }

    /**
     * 设置 UserId
     */
    public static void setUserId(String userId) {
        MDC.put(USER_ID, userId);
    }

    /**
     * 获取 UserId
     */
    public static String getUserId() {
        return MDC.get(USER_ID);
    }

    /**
     * 设置 Username
     */
    public static void setUsername(String username) {
        MDC.put(USERNAME, username);
    }

    /**
     * 获取 Username
     */
    public static String getUsername() {
        return MDC.get(USERNAME);
    }

    /**
     * 设置 IP 地址
     */
    public static void setIpAddress(String ipAddress) {
        MDC.put(IP_ADDRESS, ipAddress);
    }

    /**
     * 获取 IP 地址
     */
    public static String getIpAddress() {
        return MDC.get(IP_ADDRESS);
    }

    /**
     * 设置 URI
     */
    public static void setUri(String uri) {
        MDC.put(URI, uri);
    }

    /**
     * 获取 URI
     */
    public static String getUri() {
        return MDC.get(URI);
    }

    /**
     * 设置 HTTP Method
     */
    public static void setMethod(String method) {
        MDC.put(METHOD, method);
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
