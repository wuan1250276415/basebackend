package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程实例分页查询请求
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "ProcessInstancePageQuery", description = "流程实例分页查询请求")
public class ProcessInstancePageQuery extends BasePageQuery {

    /**
     * 租户 ID
     */
    @Schema(
        description = "租户 ID，支持多租户过滤",
        example = "tenant_001"
    )
    private String tenantId;

    /**
     * 业务键（精确匹配）
     */
    @Schema(
        description = "业务键，精确匹配",
        example = "ORDER_20250101_001"
    )
    private String businessKey;

    /**
     * 流程定义 Key（精确匹配）
     */
    @Schema(
        description = "流程定义 Key",
        example = "order_approval"
    )
    private String processDefinitionKey;

    /**
     * 流程定义 ID（精确匹配）
     */
    @Schema(
        description = "流程定义 ID",
        example = "order_approval:1:12345"
    )
    private String processDefinitionId;

    /**
     * 启动人（精确匹配）
     */
    @Schema(
        description = "启动人",
        example = "alice"
    )
    private String startedBy;

    /**
     * 挂起状态过滤
     */
    @Schema(
        description = "挂起状态过滤：true=已挂起，false=激活中，null=不限制",
        example = "false"
    )
    private Boolean suspended;
}
