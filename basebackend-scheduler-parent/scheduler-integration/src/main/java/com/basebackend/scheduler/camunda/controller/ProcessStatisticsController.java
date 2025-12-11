package com.basebackend.scheduler.camunda.controller;

import com.basebackend.common.model.Result;
import com.basebackend.scheduler.camunda.dto.InstanceStatisticsDTO;
import com.basebackend.scheduler.camunda.dto.ProcessStatisticsDTO;
import com.basebackend.scheduler.camunda.dto.StatisticsQuery;
import com.basebackend.scheduler.camunda.dto.TaskStatisticsDTO;
import com.basebackend.scheduler.camunda.service.ProcessStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Camunda 流程统计分析控制器
 *
 * <p>提供工作流运行状态的统计分析功能，包括：
 * <ul>
 *   <li>流程定义统计（部署数量、版本分布）</li>
 *   <li>流程实例统计（运行中、已完成、已终止数量）</li>
 *   <li>任务统计（待办、已完成、逾期任务统计）</li>
 *   <li>流程性能分析（平均处理时长、吞吐量）</li>
 *   <li>时间范围统计分析</li>
 * </ul>
 *
 * <p>设计原则：
 * <ul>
 *   <li>提供实时统计数据，支持业务决策</li>
 *   <li>支持多维度统计（时间、租户、流程定义）</li>
 *   <li>高性能查询，缓存常用统计结果</li>
 *   <li>详细的审计日志记录</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Validated
@RestController
@RequestMapping("/api/camunda/statistics")
@RequiredArgsConstructor
@Tag(name = "Camunda 统计分析", description = "流程、实例、任务统计分析 API")
@SecurityRequirement(name = "BearerAuth")
public class ProcessStatisticsController {

    private final ProcessStatisticsService processStatisticsService;

    /**
     * 获取流程定义统计信息
     *
     * @param query 统计查询参数
     * @return 流程定义统计信息
     */
    @Operation(
        summary = "获取流程定义统计",
        description = "统计流程定义的部署数量、版本分布等信息"
    )
    @GetMapping("/process-definitions")
    public Result<ProcessStatisticsDTO> processDefinitions(@ParameterObject @Valid StatisticsQuery query) {
        ProcessStatisticsDTO statistics = processStatisticsService.processDefinitions(query);
        return Result.success(statistics);
    }

    /**
     * 获取流程实例统计信息
     *
     * @param query 统计查询参数
     * @return 流程实例统计信息
     */
    @Operation(
        summary = "获取流程实例统计",
        description = "统计运行中、已完成、已终止的流程实例数量"
    )
    @GetMapping("/instances")
    public Result<InstanceStatisticsDTO> instances(@ParameterObject @Valid StatisticsQuery query) {
        InstanceStatisticsDTO statistics = processStatisticsService.instances(query);
        return Result.success(statistics);
    }

    /**
     * 获取任务统计信息
     *
     * @param query 统计查询参数
     * @return 任务统计信息
     */
    @Operation(
        summary = "获取任务统计",
        description = "统计待办、已完成、逾期任务数量"
    )
    @GetMapping("/tasks")
    public Result<TaskStatisticsDTO> tasks(@ParameterObject @Valid StatisticsQuery query) {
        TaskStatisticsDTO statistics = processStatisticsService.tasks(query);
        return Result.success(statistics);
    }

    /**
     * 获取工作流运行状态概览
     *
     * @param query 统计查询参数
     * @return 工作流运行状态概览
     */
    @Operation(
        summary = "获取工作流运行状态概览",
        description = "获取流程、实例、任务的综合统计概览"
    )
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview(@ParameterObject @Valid StatisticsQuery query) {
        Map<String, Object> overview = processStatisticsService.overview(query);
        return Result.success(overview);
    }
}
