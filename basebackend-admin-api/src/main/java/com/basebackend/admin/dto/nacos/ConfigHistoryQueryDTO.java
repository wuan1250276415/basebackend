package com.basebackend.admin.dto.nacos;

import java.time.LocalDateTime;

/**
 * 配置历史查询DTO
 */
public record ConfigHistoryQueryDTO(
    /** 配置ID */
    Long configId,
    /** 配置Data ID */
    String dataId,
    /** 操作类型 */
    String operationType,
    /** 操作人 */
    Long operator,
    /** 开始时间 */
    LocalDateTime startTime,
    /** 结束时间 */
    LocalDateTime endTime,
    /** 页码 */
    Integer pageNum,
    /** 每页大小 */
    Integer pageSize
) {}
