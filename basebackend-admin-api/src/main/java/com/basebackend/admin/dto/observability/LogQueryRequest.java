package com.basebackend.admin.dto.observability;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 日志查询请求
 */
@Data
@Schema(description = "日志查询请求")
public class LogQueryRequest {

    @Schema(description = "关键词搜索", example = "error")
    private String keyword;

    @Schema(description = "日志级别：INFO, WARN, ERROR, DEBUG", example = "ERROR")
    private String level;

    @Schema(description = "TraceId", example = "abc123")
    private String traceId;

    @Schema(description = "应用名称", example = "basebackend-admin-api")
    private String application;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "分页大小", example = "100")
    private Integer limit = 100;
}
