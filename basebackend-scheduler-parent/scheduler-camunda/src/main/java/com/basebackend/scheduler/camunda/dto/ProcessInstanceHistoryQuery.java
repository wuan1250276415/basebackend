package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 历史流程实例分页查询请求
 *
 * <p>继承 {@link ProcessInstancePageQuery}，在运行时查询基础上增加历史查询条件。</p>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "ProcessInstanceHistoryQuery", description = "历史流程实例分页查询请求")
public class ProcessInstanceHistoryQuery extends ProcessInstancePageQuery {

    /**
     * 是否只查询已完成实例
     */
    @Schema(
        description = "是否只查询已完成实例：true=已完成，false=未完成，null=不限制",
        example = "true"
    )
    private Boolean finished;

    /**
     * 开始时间（过滤用 - 之后）
     */
    @Schema(
        description = "开始时间过滤（之后）",
        example = "2025-01-01T00:00:00Z"
    )
    private java.time.Instant startedAfter;

    /**
     * 开始时间（过滤用 - 之前）
     */
    @Schema(
        description = "开始时间过滤（之前）",
        example = "2025-01-31T23:59:59Z"
    )
    private java.time.Instant startedBefore;

    /**
     * 是否已完成
     */
    public Boolean isFinished() {
        return finished;
    }
}
