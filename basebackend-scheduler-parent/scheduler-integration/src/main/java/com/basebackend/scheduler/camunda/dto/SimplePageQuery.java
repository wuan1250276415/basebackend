package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 精简分页查询参数
 *
 * <p>仅包含分页字段（current/size），用于子资源端点的分页查询。
 * 相比完整的查询 DTO（如 ProcessInstanceHistoryQuery），此类不包含过滤条件，
 * 使 Swagger 文档更清晰，避免暴露未使用的字段。
 *
 * <p>适用场景：
 * <ul>
 *   <li>历史流程实例的活动历史查询</li>
 *   <li>历史流程实例的审计日志查询</li>
 *   <li>其他只需分页、不需过滤的子资源端点</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "SimplePageQuery", description = "精简分页查询参数（仅分页字段）")
public class SimplePageQuery extends BasePageQuery {
    // 无额外字段，仅继承基类的分页功能
    // 保留此类是为了在 API 中明确表达"仅分页查询"的语义
}
