package com.basebackend.admin.sentinel;

import lombok.extern.slf4j.Slf4j;

/**
 * Sentinel 统一异常降级处理器
 * 用于处理业务异常的降级逻辑
 *
 * @author 浮浮酱
 */
@Slf4j
public class SentinelFallbackHandler {

    /**
     * 通用的 Fallback 处理方法（无参数）
     */
    public static Object handleFallback(Throwable ex) {
        log.error("业务执行异常: {}", ex.getMessage(), ex);
        return buildFallbackResponse(ex);
    }

    /**
     * 通用的 Fallback 处理方法（单个参数）
     */
    public static <T> T handleFallback(Object param, Throwable ex) {
        log.error("业务执行异常: param={}, error={}", param, ex.getMessage(), ex);
        return (T) buildFallbackResponse(ex);
    }

    /**
     * 通用的 Fallback 处理方法（两个参数）
     */
    public static <T> T handleFallback(Object param1, Object param2, Throwable ex) {
        log.error("业务执行异常: param1={}, param2={}, error={}", param1, param2, ex.getMessage(), ex);
        return (T) buildFallbackResponse(ex);
    }

    /**
     * 通用的 Fallback 处理方法（三个参数）
     */
    public static <T> T handleFallback(Object param1, Object param2, Object param3, Throwable ex) {
        log.error("业务执行异常: error={}", ex.getMessage(), ex);
        return (T) buildFallbackResponse(ex);
    }

    /**
     * 通用的 Fallback 处理方法（四个参数）
     */
    public static <T> T handleFallback(Object param1, Object param2, Object param3, Object param4, Throwable ex) {
        log.error("业务执行异常: error={}", ex.getMessage(), ex);
        return (T) buildFallbackResponse(ex);
    }

    /**
     * 认证服务专用 Fallback 处理器（登录）
     */
    public static Object handleLoginFallback(Object loginRequest, Throwable ex) {
        log.error("登录服务异常: {}", ex.getMessage(), ex);
        throw new RuntimeException("登录服务暂时不可用，请稍后再试");
    }

    /**
     * 用户查询专用 Fallback 处理器
     */
    public static Object handleUserQueryFallback(Long userId, Throwable ex) {
        log.error("用户查询服务异常: userId={}, error={}", userId, ex.getMessage(), ex);
        throw new RuntimeException("用户查询服务暂时不可用，请稍后再试");
    }

    /**
     * 角色查询专用 Fallback 处理器
     */
    public static Object handleRoleQueryFallback(Long roleId, Throwable ex) {
        log.error("角色查询服务异常: roleId={}, error={}", roleId, ex.getMessage(), ex);
        throw new RuntimeException("角色查询服务暂时不可用，请稍后再试");
    }

    /**
     * 构建降级响应
     */
    private static Object buildFallbackResponse(Throwable ex) {
        String message = "服务暂时不可用，请稍后再试";

        // 根据异常类型自定义消息
        if (ex instanceof NullPointerException) {
            message = "数据异常，请联系管理员";
        } else if (ex instanceof IllegalArgumentException) {
            message = "参数异常: " + ex.getMessage();
        } else if (ex instanceof RuntimeException) {
            message = ex.getMessage();
        }

        throw new RuntimeException(message);
    }
}
