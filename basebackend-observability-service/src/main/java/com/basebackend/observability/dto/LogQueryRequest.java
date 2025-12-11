package com.basebackend.observability.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 日志查询请求
 * <p>
 * 支持按服务名、日志级别、关键词、时间范围等条件查询日志。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Data
public class LogQueryRequest {

    /**
     * 服务名称
     */
    @Size(max = 200, message = "服务名称长度不能超过200")
    private String serviceName;

    /**
     * 日志级别：DEBUG, INFO, WARN, ERROR
     */
    @Pattern(regexp = "^(DEBUG|INFO|WARN|ERROR)?$", message = "日志级别必须是 DEBUG, INFO, WARN 或 ERROR")
    private String level;

    /**
     * 关键词搜索
     */
    @Size(max = 500, message = "关键词长度不能超过500")
    private String keyword;

    /**
     * 开始时间（时间戳，毫秒）
     */
    @Min(value = 0, message = "开始时间不能为负数")
    private Long startTime;

    /**
     * 结束时间（时间戳，毫秒）
     */
    @Min(value = 0, message = "结束时间不能为负数")
    private Long endTime;

    /**
     * 限制结果数量
     */
    @Min(value = 1, message = "查询限制至少为1")
    @Max(value = 10000, message = "查询限制不能超过10000")
    private Integer limit = 100;

    /**
     * 排序方式：asc, desc
     */
    @Pattern(regexp = "^(asc|desc)?$", message = "排序方式必须是 asc 或 desc")
    private String sort = "desc";

    /**
     * 验证时间范围
     */
    public boolean isValidTimeRange() {
        if (startTime != null && endTime != null) {
            return endTime >= startTime;
        }
        return true;
    }
}
