package com.basebackend.system.dto;

import java.time.LocalDateTime;

/**
 * 操作日志DTO
 */
public record OperationLogDTO(
        /** 日志ID */
        String id,
        /** 用户ID */
        Long userId,
        /** 用户名 */
        String username,
        /** 操作 */
        String operation,
        /** 请求方法 */
        String method,
        /** 请求参数 */
        String params,
        /** 执行时长(毫秒) */
        Long time,
        /** IP地址 */
        String ipAddress,
        /** 操作地点 */
        String location,
        /** 操作状态：0-失败，1-成功 */
        Integer status,
        /** 错误消息 */
        String errorMsg,
        /** 操作时间 */
        LocalDateTime operationTime
) {
}
