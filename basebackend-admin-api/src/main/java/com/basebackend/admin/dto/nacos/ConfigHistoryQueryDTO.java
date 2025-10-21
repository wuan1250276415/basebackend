package com.basebackend.admin.dto.nacos;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 配置历史查询DTO
 */
@Data
public class ConfigHistoryQueryDTO {

    /**
     * 配置ID
     */
    private Long configId;

    /**
     * 配置Data ID
     */
    private String dataId;

    /**
     * 操作类型
     */
    private String operationType;

    /**
     * 操作人
     */
    private Long operator;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 页码
     */
    private Integer pageNum;

    /**
     * 每页大小
     */
    private Integer pageSize;
}
