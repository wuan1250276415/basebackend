package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 作业详情数据传输对象
 *
 * <p>继承 {@link JobDTO}，在摘要信息基础上增加异常堆栈、活动名称等详情字段。</p>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(name = "JobDetailDTO", description = "作业详情信息")
public class JobDetailDTO extends JobDTO {

    /**
     * 作业处理器配置
     */
    @Schema(description = "作业处理器配置", example = "{...}")
    private String jobHandlerConfiguration;

    /**
     * 异常堆栈
     */
    @Schema(description = "异常堆栈（完整错误信息）")
    private String exceptionStacktrace;

    /**
     * 活动名称
     */
    @Schema(description = "活动名称", example = "发送邮件")
    private String activityName;

    /**
     * 是否可立即执行
     */
    @Schema(description = "是否可立即执行（截止时间已到且有重试次数）", example = "true")
    private Boolean executable;

    /**
     * 失败活动 ID（关联）
     */
    @Schema(description = "失败活动 ID", example = "failed_activity_123")
    private String failedActivityId;
}
