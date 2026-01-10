package com.basebackend.scheduler.camunda.controller;

import com.basebackend.common.dto.PageResult;
import com.basebackend.common.model.Result;
import com.basebackend.scheduler.camunda.dto.IncidentDTO;
import com.basebackend.scheduler.camunda.dto.IncidentPageQuery;
import com.basebackend.scheduler.camunda.service.IncidentService;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Camunda 异常事件控制器
 *
 * <p>
 * 提供 Camunda 异常事件（Incident）管理功能，包括：
 * <ul>
 * <li>异常事件分页查询</li>
 * <li>异常事件详情查看</li>
 * <li>异常事件解决（重试关联作业）</li>
 * <li>异常事件注解管理</li>
 * <li>异常事件统计</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/camunda/incidents")
@RequiredArgsConstructor
@Tag(name = "Camunda 异常事件管理", description = "Camunda 异常事件（Incident）监控与处理 API")
@SecurityRequirement(name = "BearerAuth")
public class IncidentController {

    private final IncidentService incidentService;

    // ========== 查询接口 ==========

    /**
     * 分页查询异常事件
     *
     * @param query 分页查询参数
     * @return 分页结果
     */
    @Operation(summary = "分页查询异常事件", description = "支持按类型、流程定义、时间等条件过滤的异常事件分页查询")
    @GetMapping
    public Result<PageResult<IncidentDTO>> page(@ParameterObject @Valid IncidentPageQuery query) {
        PageResult<IncidentDTO> result = incidentService.page(query);
        return Result.success(result);
    }

    /**
     * 获取异常事件详情
     *
     * @param incidentId 异常事件 ID
     * @return 异常事件详情
     */
    @Operation(summary = "获取异常事件详情", description = "根据异常事件 ID 获取详细信息")
    @GetMapping("/{incidentId}")
    public Result<IncidentDTO> detail(
            @Parameter(description = "异常事件 ID") @PathVariable @NotBlank String incidentId) {
        IncidentDTO dto = incidentService.detail(incidentId);
        return Result.success(dto);
    }

    /**
     * 获取流程实例的异常事件列表
     *
     * @param processInstanceId 流程实例 ID
     * @return 异常事件列表
     */
    @Operation(summary = "获取流程实例的异常事件", description = "根据流程实例 ID 获取所有相关异常事件")
    @GetMapping("/process-instance/{processInstanceId}")
    public Result<List<IncidentDTO>> listByProcessInstance(
            @Parameter(description = "流程实例 ID") @PathVariable @NotBlank String processInstanceId) {
        List<IncidentDTO> list = incidentService.listByProcessInstance(processInstanceId);
        return Result.success(list);
    }

    /**
     * 获取最近的异常事件列表
     *
     * @param maxResults 最大结果数
     * @return 异常事件列表
     */
    @Operation(summary = "获取最近的异常事件", description = "获取最近发生的异常事件列表")
    @GetMapping("/recent")
    public Result<List<IncidentDTO>> listRecentIncidents(
            @Parameter(description = "最大结果数") @RequestParam(defaultValue = "20") int maxResults) {
        List<IncidentDTO> list = incidentService.listRecentIncidents(maxResults);
        return Result.success(list);
    }

    // ========== 操作接口 ==========

    /**
     * 解决异常事件
     *
     * @param incidentId 异常事件 ID
     * @return 操作结果
     */
    @Operation(summary = "解决异常事件", description = "通过重试关联的失败作业来解决异常事件")
    @PostMapping("/{incidentId}/resolve")
    public Result<String> resolve(
            @Parameter(description = "异常事件 ID") @PathVariable @NotBlank String incidentId) {
        incidentService.resolve(incidentId);
        return Result.success("异常事件处理已启动");
    }

    /**
     * 设置异常事件注解
     *
     * @param incidentId 异常事件 ID
     * @param annotation 注解内容
     * @return 操作结果
     */
    @Operation(summary = "设置异常事件注解", description = "为异常事件添加或更新注解信息")
    @PutMapping("/{incidentId}/annotation")
    public Result<String> setAnnotation(
            @Parameter(description = "异常事件 ID") @PathVariable @NotBlank String incidentId,
            @Parameter(description = "注解内容") @RequestParam String annotation) {
        incidentService.setAnnotation(incidentId, annotation);
        return Result.success("注解设置成功");
    }

    /**
     * 清除异常事件注解
     *
     * @param incidentId 异常事件 ID
     * @return 操作结果
     */
    @Operation(summary = "清除异常事件注解", description = "清除异常事件的注解信息")
    @DeleteMapping("/{incidentId}/annotation")
    public Result<String> clearAnnotation(
            @Parameter(description = "异常事件 ID") @PathVariable @NotBlank String incidentId) {
        incidentService.setAnnotation(incidentId, null);
        return Result.success("注解已清除");
    }

    // ========== 统计接口 ==========

    /**
     * 获取异常事件统计信息
     *
     * @return 统计信息
     */
    @Operation(summary = "获取异常事件统计", description = "获取异常事件的总数、按类型分组、按流程定义分组等统计信息")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics() {
        long totalCount = incidentService.countIncidents();
        Map<String, Long> byType = incidentService.countByType();
        Map<String, Long> byProcessDefinition = incidentService.countByProcessDefinition();

        return Result.success(Map.of(
                "totalCount", totalCount,
                "byType", byType,
                "byProcessDefinition", byProcessDefinition));
    }

    /**
     * 统计异常事件总数
     *
     * @return 异常事件总数
     */
    @Operation(summary = "统计异常事件总数", description = "获取当前系统中的异常事件总数")
    @GetMapping("/statistics/count")
    public Result<Long> countIncidents() {
        long count = incidentService.countIncidents();
        return Result.success(count);
    }

    /**
     * 按类型统计异常事件
     *
     * @return 按异常类型分组的数量
     */
    @Operation(summary = "按类型统计异常事件", description = "获取按异常类型分组的异常事件数量")
    @GetMapping("/statistics/by-type")
    public Result<Map<String, Long>> countByType() {
        Map<String, Long> result = incidentService.countByType();
        return Result.success(result);
    }

    /**
     * 按流程定义统计异常事件
     *
     * @return 按流程定义 ID 分组的异常事件数量
     */
    @Operation(summary = "按流程定义统计异常事件", description = "获取按流程定义 ID 分组的异常事件数量")
    @GetMapping("/statistics/by-definition")
    public Result<Map<String, Long>> countByProcessDefinition() {
        Map<String, Long> result = incidentService.countByProcessDefinition();
        return Result.success(result);
    }
}
