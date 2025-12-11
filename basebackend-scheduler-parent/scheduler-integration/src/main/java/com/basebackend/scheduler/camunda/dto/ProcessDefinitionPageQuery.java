package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程定义分页查询请求
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "ProcessDefinitionPageQuery", description = "流程定义分页查询请求")
public class ProcessDefinitionPageQuery extends BasePageQuery {

    /**
     * 关键词（名称或 Key 模糊搜索）
     */
    @Schema(
        description = "关键词，支持名称或 Key 模糊搜索",
        example = "审批"
    )
    private String keyword;

    /**
     * 精确的流程定义 Key
     */
    @Schema(
        description = "精确的流程定义 Key",
        example = "order_approval"
    )
    private String key;

    /**
     * 租户 ID
     */
    @Schema(
        description = "租户 ID，支持多租户过滤",
        example = "tenant_001"
    )
    private String tenantId;

    /**
     * 是否只查询最新版本
     */
    @Schema(
        description = "是否只查询最新版本，默认 true",
        defaultValue = "true"
    )
    private Boolean latestVersion = Boolean.TRUE;

    /**
     * 挂起状态过滤
     */
    @Schema(
        description = "挂起状态过滤：true=已挂起，false=激活中，null=不限制",
        example = "false"
    )
    private Boolean suspended;

    /**
     * 流程定义名称
     */
    @Schema(
        description = "流程定义名称",
        example = "订单审批流程"
    )
    private String name;

    /**
     * 是否只查询最新版本（兼容方法）
     */
    public boolean isLatestOnly() {
        return Boolean.TRUE.equals(latestVersion);
    }

    /**
     * 是否挂起（兼容方法）
     */
    public Boolean isSuspended() {
        return suspended;
    }
}
