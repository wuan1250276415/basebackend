package com.basebackend.observability.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 指标查询请求
 * <p>
 * 支持按指标名、时间范围、标签等条件查询指标数据。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Data
public class MetricsQueryRequest {

    /**
     * 指标名称
     */
    @NotBlank(message = "指标名称不能为空")
    @Size(max = 500, message = "指标名称长度不能超过500")
    private String metricName;

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
     * 标签过滤（格式：key=value,key2=value2）
     */
    @Size(max = 1000, message = "标签长度不能超过1000")
    private String tags;

    /**
     * 聚合方式：avg, sum, max, min, count
     */
    @Pattern(regexp = "^(avg|sum|max|min|count)?$", message = "聚合方式必须是 avg, sum, max, min 或 count")
    private String aggregation;

    /**
     * 查询步长（秒）
     */
    @Min(value = 1, message = "查询步长至少为1秒")
    private Integer step = 60;

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
