package com.basebackend.scheduler.camunda.controller;

import com.basebackend.common.dto.PageResult;
import com.basebackend.common.model.Result;
import com.basebackend.scheduler.camunda.config.PaginationConstants;
import com.basebackend.scheduler.camunda.dto.HistoricActivityInstanceDTO;
import com.basebackend.scheduler.camunda.dto.HistoricProcessInstanceDTO;
import com.basebackend.scheduler.camunda.dto.HistoricProcessInstanceDetailDTO;
import com.basebackend.scheduler.camunda.dto.HistoricProcessInstanceStatusDTO;
import com.basebackend.scheduler.camunda.dto.ProcessInstanceHistoryQuery;
import com.basebackend.scheduler.camunda.dto.ProcessTrackingDTO;
import com.basebackend.scheduler.camunda.dto.SimplePageQuery;
import com.basebackend.scheduler.camunda.dto.UserOperationLogDTO;
import com.basebackend.scheduler.camunda.service.HistoricProcessInstanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Camunda 历史流程实例控制器
 *
 * <p>
 * 提供历史流程实例的查询与分析功能，包括：
 * <ul>
 * <li>历史流程实例分页查询（租户、业务键、流程定义、启动人、完成状态过滤）</li>
 * <li>历史流程实例详情查看（包含流程变量与活动历史）</li>
 * <li>历史流程实例状态查询（已完成、未完成、已终止状态）</li>
 * <li>历史流程实例活动历史分页查询（活动执行轨迹）</li>
 * <li>历史流程实例审计日志分页查询（用户操作记录）</li>
 * </ul>
 *
 * <p>
 * 设计原则：
 * <ul>
 * <li>RESTful API 设计，遵循标准 HTTP 方法语义</li>
 * <li>完善的参数验证和错误处理机制</li>
 * <li>支持租户隔离和多租户场景</li>
 * <li>提供完整的审计追溯能力</li>
 * <li>详细的审计日志记录</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/camunda/history/process-instances")
@RequiredArgsConstructor
@Tag(name = "Camunda 历史流程实例", description = "历史流程实例查询与审计 API")
@SecurityRequirement(name = "BearerAuth")
public class HistoricProcessInstanceController {

    private final HistoricProcessInstanceService historicProcessInstanceService;

    /**
     * 历史流程实例分页查询
     *
     * <p>
     * 支持多种过滤条件：
     * <ul>
     * <li>租户过滤</li>
     * <li>业务键过滤</li>
     * <li>流程定义 Key/ID 过滤</li>
     * <li>启动人过滤</li>
     * <li>完成状态过滤</li>
     * </ul>
     *
     * @param query 分页查询参数
     * @return 分页结果
     */
    @Operation(summary = "历史流程实例分页查询", description = "支持租户、业务键、流程定义、启动人和完成状态过滤的分页查询")
    @GetMapping
    public Result<PageResult<HistoricProcessInstanceDTO>> page(
            @ParameterObject @Valid ProcessInstanceHistoryQuery query) {
        PageResult<HistoricProcessInstanceDTO> result = historicProcessInstanceService.page(query);
        return Result.success(result);
    }

    /**
     * 获取历史流程实例详情
     *
     * <p>
     * 包含流程变量和活动执行历史，用于流程实例的完整追溯。
     *
     * @param instanceId 流程实例 ID
     * @return 历史流程实例详情
     */
    @Operation(summary = "获取历史流程实例详情", description = "查看历史流程实例详情，包含流程变量与活动历史轨迹")
    @GetMapping("/{instanceId}")
    public Result<HistoricProcessInstanceDetailDTO> detail(
            @Parameter(description = "流程实例 ID") @PathVariable @NotBlank String instanceId) {
        HistoricProcessInstanceDetailDTO dto = historicProcessInstanceService.detail(instanceId);
        return Result.success(dto);
    }

    /**
     * 查询历史流程实例状态
     *
     * @param instanceId 流程实例 ID
     * @return 流程实例状态信息
     */
    @Operation(summary = "查询历史流程实例状态", description = "查询历史流程实例的完成、未完成、终止状态")
    @GetMapping("/{instanceId}/status")
    public Result<HistoricProcessInstanceStatusDTO> status(
            @Parameter(description = "流程实例 ID") @PathVariable @NotBlank String instanceId) {
        HistoricProcessInstanceStatusDTO dto = historicProcessInstanceService.status(instanceId);
        return Result.success(dto);
    }

    /**
     * 查询历史流程实例活动历史
     *
     * <p>
     * 使用精简的分页查询对象，只接收 current 和 size 参数。
     * 分页参数通过 Query Parameters 传递（如 ?current=1&size=10）。
     *
     * @param instanceId 流程实例 ID
     * @param pageQuery  分页查询参数（仅包含 current/size）
     * @return 活动历史分页结果
     */
    @Operation(summary = "查询历史流程实例活动历史", description = "分页查询指定历史流程实例的活动执行轨迹，仅需分页参数（current/size）。\n\n"
            + "**请求示例**：\n"
            + "- 基本查询: GET /api/camunda/history/process-instances/{id}/activities?current=1&size=10\n"
            + "- 查询更多: GET /api/camunda/history/process-instances/{id}/activities?current=1&size=50\n"
            + "- 最大限制: GET /api/camunda/history/process-instances/{id}/activities?current=1&size=200\n\n"
            + "**参数约束**：\n"
            + "每页最大200条记录，支持标准分页参数current和size，遵循统一分页策略。")
    @GetMapping("/{instanceId}/activities")
    public Result<PageResult<HistoricActivityInstanceDTO>> activities(
            @Parameter(description = "流程实例 ID") @PathVariable @NotBlank String instanceId,
            @ParameterObject @Valid SimplePageQuery pageQuery) {
        // 转换为 Service 层所需的查询对象
        ProcessInstanceHistoryQuery query = new ProcessInstanceHistoryQuery();
        query.setCurrent(pageQuery.getCurrent());
        query.setSize(pageQuery.getSize());

        PageResult<HistoricActivityInstanceDTO> result = historicProcessInstanceService.activities(instanceId, query);
        return Result.success(result);
    }

    /**
     * 查询历史流程实例审计日志
     *
     * <p>
     * 使用精简的分页查询对象，只接收 current 和 size 参数。
     * 分页参数通过 Query Parameters 传递（如 ?current=1&size=10）。
     *
     * @param instanceId 流程实例 ID
     * @param pageQuery  分页查询参数（仅包含 current/size）
     * @return 审计日志分页结果
     */
    @Operation(summary = "查询历史流程实例审计日志", description = "查询指定历史流程实例的用户操作审计日志，仅需分页参数（current/size）。\n\n"
            + "**请求示例**：\n"
            + "- 基本查询: GET /api/camunda/history/process-instances/{id}/audit-logs?current=1&size=10\n"
            + "- 查询更多: GET /api/camunda/history/process-instances/{id}/audit-logs?current=1&size=50\n"
            + "- 最大限制: GET /api/camunda/history/process-instances/{id}/audit-logs?current=1&size=200\n\n"
            + "**参数约束**：\n"
            + "每页最大200条记录，支持标准分页参数current和size，遵循统一分页策略。")
    @GetMapping("/{instanceId}/audit-logs")
    public Result<PageResult<UserOperationLogDTO>> auditLogs(
            @Parameter(description = "流程实例 ID") @PathVariable @NotBlank String instanceId,
            @ParameterObject @Valid SimplePageQuery pageQuery) {
        // 转换为 Service 层所需的查询对象
        ProcessInstanceHistoryQuery query = new ProcessInstanceHistoryQuery();
        query.setCurrent(pageQuery.getCurrent());
        query.setSize(pageQuery.getSize());

        PageResult<UserOperationLogDTO> result = historicProcessInstanceService.auditLogs(instanceId, query);
        return Result.success(result);
    }

    /**
     * 获取流程跟踪信息（可视化）
     *
     * <p>
     * 返回流程实例的完整跟踪信息，包括：
     * <ul>
     * <li>BPMN XML 内容</li>
     * <li>当前活动节点列表（蓝色高亮）</li>
     * <li>已完成活动节点列表（绿色高亮）</li>
     * <li>失败活动节点列表（红色高亮）</li>
     * <li>活动历史详情</li>
     * </ul>
     *
     * @param instanceId 流程实例 ID
     * @return 流程跟踪信息
     */
    @Operation(summary = "获取流程跟踪信息", description = "获取流程实例的可视化跟踪信息，用于在流程图上高亮显示节点状态。"
            + "返回的数据包含 BPMN XML 和各状态节点列表（当前/已完成/失败），"
            + "前端可使用 bpmn-js 进行渲染和高亮展示。")
    @GetMapping("/{instanceId}/tracking")
    public Result<ProcessTrackingDTO> tracking(
            @Parameter(description = "流程实例 ID") @PathVariable @NotBlank String instanceId) {
        ProcessTrackingDTO dto = historicProcessInstanceService.getProcessTracking(instanceId);
        return Result.success(dto);
    }
}
