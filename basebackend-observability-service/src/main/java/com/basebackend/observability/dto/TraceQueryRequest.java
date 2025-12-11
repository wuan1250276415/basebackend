package com.basebackend.observability.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 追踪查询请求
 * <p>
 * 支持按服务名、操作名、时间范围、持续时间等条件查询追踪数据。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Data
public class TraceQueryRequest {

    /**
     * 服务名称
     */
    @Size(max = 200, message = "服务名称长度不能超过200")
    private String serviceName;

    /**
     * 操作名称
     */
    @Size(max = 500, message = "操作名称长度不能超过500")
    private String operationName;

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
     * 最小持续时间（毫秒）
     */
    @Min(value = 0, message = "最小持续时间不能为负数")
    private Long minDuration;

    /**
     * 最大持续时间（毫秒）
     */
    @Min(value = 0, message = "最大持续时间不能为负数")
    private Long maxDuration;

    /**
     * 标签过滤（格式：key=value,key2=value2）
     */
    @Size(max = 1000, message = "标签长度不能超过1000")
    private String tags;

    /**
     * 限制结果数量
     */
    @Min(value = 1, message = "查询限制至少为1")
    @Max(value = 10000, message = "查询限制不能超过10000")
    private Integer limit = 100;

    /**
     * 验证时间范围
     */
    public boolean isValidTimeRange() {
        if (startTime != null && endTime != null) {
            return endTime >= startTime;
        }
        return true;
    }

    /**
     * 验证持续时间范围
     */
    public boolean isValidDurationRange() {
        if (minDuration != null && maxDuration != null) {
            return maxDuration >= minDuration;
        }
        return true;
    }
}
