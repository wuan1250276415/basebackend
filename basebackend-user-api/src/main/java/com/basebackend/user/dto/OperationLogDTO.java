package com.basebackend.user.dto;

import java.time.LocalDateTime;

/**
 * 操作日志DTO
 */
public record OperationLogDTO(
    Long id,
    Long userId,
    String username,
    String operation,
    String method,
    String params,
    Long time,
    String ipAddress,
    String location,
    Integer status,
    String errorMsg,
    LocalDateTime operationTime
) {}
