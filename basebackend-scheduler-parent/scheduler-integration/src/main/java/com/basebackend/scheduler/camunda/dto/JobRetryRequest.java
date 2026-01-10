package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.Date;

/**
 * 作业重试请求
 *
 * <p>
 * 用于手动重试失败的作业，可选设置新的重试次数和截止时间。
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@Schema(name = "JobRetryRequest", description = "作业重试请求")
public class JobRetryRequest {

    /**
     * 重试次数
     * 默认为 3 次
     */
    @Schema(description = "重试次数，默认 3 次", example = "3", defaultValue = "3")
    @Min(value = 1, message = "重试次数必须大于等于 1")
    private Integer retries = 3;

    /**
     * 新的截止时间
     * 如果不设置，则立即执行
     */
    @Schema(description = "新的截止时间（不设置则立即执行）", example = "2025-01-01T12:00:00")
    private Date duedate;
}
