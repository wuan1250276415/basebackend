package com.basebackend.admin.sentinel;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import lombok.extern.slf4j.Slf4j;

/**
 * Sentinel 统一降级处理器
 * 用于处理流控、熔断、降级等 BlockException
 *
 * @author 浮浮酱
 */
@Slf4j
public class SentinelBlockHandler {

    /**
     * 通用的 Block 处理方法（无参数）
     */
    public static Object handleBlock(BlockException ex) {
        log.warn("触发 Sentinel 限流/熔断: {}", ex.getClass().getSimpleName());
        return buildBlockResponse(ex);
    }

    /**
     * 通用的 Block 处理方法（单个参数）
     */
    public static <T> T handleBlock(Object param, BlockException ex) {
        log.warn("触发 Sentinel 限流/熔断: resource={}, param={}", ex.getRuleLimitApp(), param);
        return (T) buildBlockResponse(ex);
    }

    /**
     * 通用的 Block 处理方法（两个参数）
     */
    public static <T> T handleBlock(Object param1, Object param2, BlockException ex) {
        log.warn("触发 Sentinel 限流/熔断: resource={}, param1={}, param2={}", ex.getRuleLimitApp(), param1, param2);
        return (T) buildBlockResponse(ex);
    }

    /**
     * 通用的 Block 处理方法（三个参数）
     */
    public static <T> T handleBlock(Object param1, Object param2, Object param3, BlockException ex) {
        log.warn("触发 Sentinel 限流/熔断: resource={}", ex.getRuleLimitApp());
        return (T) buildBlockResponse(ex);
    }

    /**
     * 通用的 Block 处理方法（四个参数）
     */
    public static <T> T handleBlock(Object param1, Object param2, Object param3, Object param4, BlockException ex) {
        log.warn("触发 Sentinel 限流/熔断: resource={}", ex.getRuleLimitApp());
        return (T) buildBlockResponse(ex);
    }

    /**
     * 认证服务专用 Block 处理器（登录）
     */
    public static Object handleLoginBlock(Object loginRequest, BlockException ex) {
        log.warn("登录接口触发限流: {}", ex.getClass().getSimpleName());
        throw new RuntimeException("登录请求过于频繁，请稍后再试");
    }

    /**
     * 用户查询专用 Block 处理器
     */
    public static Object handleUserQueryBlock(Long userId, BlockException ex) {
        log.warn("用户查询触发限流: userId={}", userId);
        throw new RuntimeException("用户查询请求过于频繁，请稍后再试");
    }

    /**
     * 角色查询专用 Block 处理器
     */
    public static Object handleRoleQueryBlock(Long roleId, BlockException ex) {
        log.warn("角色查询触发限流: roleId={}", roleId);
        throw new RuntimeException("角色查询请求过于频繁，请稍后再试");
    }

    /**
     * 构建限流/熔断响应
     */
    private static Object buildBlockResponse(BlockException ex) {
        String message;

        if (ex instanceof FlowException) {
            message = "请求过于频繁，请稍后再试";
        } else if (ex instanceof DegradeException) {
            message = "服务暂时不可用，请稍后再试";
        } else if (ex instanceof ParamFlowException) {
            message = "热点参数限流，请稍后再试";
        } else if (ex instanceof AuthorityException) {
            message = "没有权限访问";
        } else {
            message = "系统繁忙，请稍后再试";
        }

        throw new RuntimeException(message);
    }
}
