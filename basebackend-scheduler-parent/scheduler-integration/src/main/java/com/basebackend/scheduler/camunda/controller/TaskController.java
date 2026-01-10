package com.basebackend.scheduler.camunda.controller;

import com.basebackend.common.dto.PageResult;
import com.basebackend.common.model.Result;
import com.basebackend.scheduler.camunda.dto.AttachmentDTO;
import com.basebackend.scheduler.camunda.dto.AttachmentRequest;
import com.basebackend.scheduler.camunda.dto.ClaimTaskRequest;
import com.basebackend.scheduler.camunda.dto.CommentDTO;
import com.basebackend.scheduler.camunda.dto.CommentRequest;
import com.basebackend.scheduler.camunda.dto.CompleteTaskRequest;
import com.basebackend.scheduler.camunda.dto.DelegateTaskRequest;
import com.basebackend.scheduler.camunda.dto.TaskDetailDTO;
import com.basebackend.scheduler.camunda.dto.TaskPageQuery;
import com.basebackend.scheduler.camunda.dto.TaskSummaryDTO;
import com.basebackend.scheduler.camunda.dto.VariableUpsertRequest;
import com.basebackend.scheduler.camunda.service.TaskManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Camunda 任务控制器
 *
 * <p>
 * 提供 Camunda 用户任务全生命周期管理功能，包括：
 * <ul>
 * <li>任务分页查询（租户、流程实例、任务分配人、候选用户/组过滤）</li>
 * <li>任务详情查看（包含流程变量）</li>
 * <li>任务操作（认领、释放、完成任务、委托）</li>
 * <li>任务变量管理（获取、设置、删除，支持本地变量）</li>
 * <li>任务附件管理（查询、添加）</li>
 * <li>任务评论管理（查询、添加）</li>
 * </ul>
 *
 * <p>
 * 设计原则：
 * <ul>
 * <li>RESTful API 设计，遵循标准 HTTP 方法语义</li>
 * <li>完善的参数验证和错误处理机制</li>
 * <li>支持租户隔离和多租户场景</li>
 * <li>集成缓存机制提升查询性能</li>
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
@RequestMapping("/api/camunda/tasks")
@RequiredArgsConstructor
@Tag(name = "Camunda 任务管理", description = "Camunda 用户任务生命周期管理 API")
@SecurityRequirement(name = "BearerAuth")
public class TaskController {

    private final TaskManagementService taskManagementService;

    /**
     * 分页查询任务
     *
     * <p>
     * 支持多种过滤条件：
     * <ul>
     * <li>租户过滤</li>
     * <li>流程实例 ID 过滤</li>
     * <li>任务分配人过滤</li>
     * <li>候选用户过滤</li>
     * <li>候选组过滤</li>
     * <li>任务名称模糊搜索</li>
     * </ul>
     *
     * @param query 分页查询参数
     * @return 分页结果
     */
    @Operation(summary = "分页查询任务", description = "支持租户、流程实例、任务分配人、候选用户/组过滤的任务分页查询")
    @GetMapping
    public Result<PageResult<TaskSummaryDTO>> page(@ParameterObject @Valid TaskPageQuery query) {
        PageResult<TaskSummaryDTO> result = taskManagementService.page(query);
        return Result.success(result);
    }

    /**
     * 获取任务详情
     *
     * @param taskId        任务 ID
     * @param withVariables 是否返回任务变量
     * @return 任务详情
     */
    @Operation(summary = "获取任务详情", description = "根据任务 ID 获取详细信息，可选择是否返回任务变量")
    @GetMapping("/{taskId}")
    public Result<TaskDetailDTO> detail(
            @Parameter(description = "任务 ID") @PathVariable @NotBlank String taskId,
            @Parameter(description = "是否返回任务变量") @RequestParam(name = "withVariables", defaultValue = "false") boolean withVariables) {
        TaskDetailDTO dto = taskManagementService.detail(taskId, withVariables);
        return Result.success(dto);
    }

    /**
     * 认领任务
     *
     * @param taskId  任务 ID
     * @param request 认领请求参数
     * @return 操作结果
     */
    @Operation(summary = "认领任务", description = "将候选任务认领给自己或其他用户")
    @PostMapping("/{taskId}/claim")
    public Result<String> claim(
            @Parameter(description = "任务 ID") @PathVariable @NotBlank String taskId,
            @Valid @RequestBody ClaimTaskRequest request) {
        taskManagementService.claim(taskId, request);
        return Result.success("任务认领成功");
    }

    /**
     * 释放任务
     *
     * @param taskId 任务 ID
     * @return 操作结果
     */
    @Operation(summary = "释放任务", description = "将已认领的任务释放，使其变为候选任务")
    @PostMapping("/{taskId}/unclaim")
    public Result<String> unclaim(@Parameter(description = "任务 ID") @PathVariable @NotBlank String taskId) {
        taskManagementService.unclaim(taskId);
        return Result.success("任务释放成功");
    }

    /**
     * 完成任务
     *
     * @param taskId  任务 ID
     * @param request 完成任务请求参数
     * @return 操作结果
     */
    @Operation(summary = "完成任务", description = "完成任务并推进流程实例，包含可选的输出变量")
    @PostMapping("/{taskId}/complete")
    public Result<String> complete(
            @Parameter(description = "任务 ID") @PathVariable @NotBlank String taskId,
            @Valid @RequestBody CompleteTaskRequest request) {
        taskManagementService.complete(taskId, request);
        return Result.success("任务完成成功");
    }

    /**
     * 委托任务
     *
     * @param taskId  任务 ID
     * @param request 委托请求参数
     * @return 操作结果
     */
    @Operation(summary = "委托任务", description = "将任务委托给其他用户处理")
    @PostMapping("/{taskId}/delegate")
    public Result<String> delegate(
            @Parameter(description = "任务 ID") @PathVariable @NotBlank String taskId,
            @Valid @RequestBody DelegateTaskRequest request) {
        taskManagementService.delegate(taskId, request);
        return Result.success("任务委托成功");
    }

    @Operation(summary = "转办任务", description = "将任务转办给其他用户（变更负责人）")
    @PostMapping("/{taskId}/transfer")
    public Result<String> transfer(
            @Parameter(description = "任务 ID") @PathVariable @NotBlank String taskId,
            @Parameter(description = "新负责人ID") @RequestParam String userId) {
        taskManagementService.transfer(taskId, userId);
        return Result.success("任务转办成功");
    }

    @Operation(summary = "任务回退", description = "将任务驳回到上一个历史节点")
    @PostMapping("/{taskId}/rollback")
    public Result<String> rollback(
            @Parameter(description = "任务 ID") @PathVariable @NotBlank String taskId,
            @Parameter(description = "回退原因") @RequestParam(required = false) String reason) {
        taskManagementService.rollback(taskId, reason);
        return Result.success("任务回退成功");
    }

    /**
     * 获取任务变量
     *
     * @param taskId 任务 ID
     * @param local  是否只获取本地变量
     * @return 变量集合
     */
    @Operation(summary = "获取任务变量", description = "返回任务的所有变量，支持本地变量过滤")
    @GetMapping("/{taskId}/variables")
    public Result<Map<String, Object>> variables(
            @Parameter(description = "任务 ID") @PathVariable @NotBlank String taskId,
            @Parameter(description = "true 只取本地变量，否则包含流程变量") @RequestParam(value = "local", defaultValue = "false") boolean local) {
        Map<String, Object> variables = taskManagementService.variables(taskId, local);
        return Result.success(variables);
    }

    /**
     * 设置/更新任务变量
     *
     * @param taskId  任务 ID
     * @param request 变量设置请求
     * @return 操作结果
     */
    @Operation(summary = "设置任务变量", description = "设置或更新指定任务变量，支持本地变量设置")
    @PutMapping("/{taskId}/variables")
    public Result<String> upsertVariable(
            @Parameter(description = "任务 ID") @PathVariable @NotBlank String taskId,
            @Valid @RequestBody VariableUpsertRequest request) {
        taskManagementService.upsertVariable(taskId, request);
        return Result.success("变量设置成功");
    }

    /**
     * 删除任务变量
     *
     * @param taskId 任务 ID
     * @param key    变量名
     * @param local  是否删除本地变量
     * @return 操作结果
     */
    @Operation(summary = "删除任务变量", description = "删除指定的任务变量，支持本地变量删除")
    @DeleteMapping("/{taskId}/variables/{key}")
    public Result<String> deleteVariable(
            @Parameter(description = "任务 ID") @PathVariable @NotBlank String taskId,
            @Parameter(description = "变量名") @PathVariable @NotBlank String key,
            @RequestParam(value = "local", defaultValue = "false") boolean local) {
        taskManagementService.deleteVariable(taskId, key, local);
        return Result.success("变量删除成功");
    }

    /**
     * 查询任务附件列表
     *
     * @param taskId 任务 ID
     * @return 附件列表
     */
    @Operation(summary = "查询任务附件", description = "获取任务的所有附件信息")
    @GetMapping("/{taskId}/attachments")
    public Result<List<AttachmentDTO>> attachments(
            @Parameter(description = "任务 ID") @PathVariable @NotBlank String taskId) {
        List<AttachmentDTO> attachments = taskManagementService.attachments(taskId);
        return Result.success(attachments);
    }

    /**
     * 添加任务附件
     *
     * @param taskId  任务 ID
     * @param request 附件添加请求
     * @return 附件信息
     */
    @Operation(summary = "添加任务附件", description = "为任务添加新的附件，支持外部 URL 引用")
    @PostMapping("/{taskId}/attachments")
    public Result<AttachmentDTO> addAttachment(
            @Parameter(description = "任务 ID") @PathVariable @NotBlank String taskId,
            @Valid @RequestBody AttachmentRequest request) {
        AttachmentDTO dto = taskManagementService.addAttachment(taskId, request);
        return Result.success(dto);
    }

    /**
     * 查询任务评论列表
     *
     * @param taskId 任务 ID
     * @return 评论列表
     */
    @Operation(summary = "查询任务评论", description = "获取任务的所有评论信息")
    @GetMapping("/{taskId}/comments")
    public Result<List<CommentDTO>> comments(
            @Parameter(description = "任务 ID") @PathVariable @NotBlank String taskId) {
        List<CommentDTO> comments = taskManagementService.comments(taskId);
        return Result.success(comments);
    }

    /**
     * 添加任务评论
     *
     * @param taskId  任务 ID
     * @param request 评论添加请求
     * @return 评论信息
     */
    @Operation(summary = "添加任务评论", description = "为任务添加新的评论信息")
    @PostMapping("/{taskId}/comments")
    public Result<CommentDTO> addComment(
            @Parameter(description = "任务 ID") @PathVariable @NotBlank String taskId,
            @Valid @RequestBody CommentRequest request) {
        CommentDTO dto = taskManagementService.addComment(taskId, request);
        return Result.success(dto);
    }

    // ========== 超时任务查询接口 ==========

    /**
     * 查询已超时任务
     *
     * @param query 分页查询参数
     * @return 超时任务分页结果
     */
    @Operation(summary = "查询已超时任务", description = "查询截止时间已过的任务列表")
    @GetMapping("/overdue")
    public Result<PageResult<TaskSummaryDTO>> listOverdueTasks(@ParameterObject @Valid TaskPageQuery query) {
        PageResult<TaskSummaryDTO> result = taskManagementService.listOverdueTasks(query);
        return Result.success(result);
    }

    /**
     * 查询即将超时任务
     *
     * @param query 分页查询参数
     * @param hours 距离超时的小时数（默认24小时）
     * @return 即将超时任务分页结果
     */
    @Operation(summary = "查询即将超时任务", description = "查询截止时间在指定时间范围内的任务")
    @GetMapping("/due-soon")
    public Result<PageResult<TaskSummaryDTO>> listDueSoonTasks(
            @ParameterObject @Valid TaskPageQuery query,
            @Parameter(description = "距离超时的小时数") @RequestParam(defaultValue = "24") int hours) {
        PageResult<TaskSummaryDTO> result = taskManagementService.listDueSoonTasks(query, hours);
        return Result.success(result);
    }

    /**
     * 统计超时任务数量
     *
     * @return 超时任务数量
     */
    @Operation(summary = "统计超时任务", description = "获取当前系统中已超时任务的数量")
    @GetMapping("/overdue/count")
    public Result<Long> countOverdueTasks() {
        long count = taskManagementService.countOverdueTasks();
        return Result.success(count);
    }

    /**
     * 统计即将超时任务数量
     *
     * @param hours 距离超时的小时数（默认24小时）
     * @return 即将超时任务数量
     */
    @Operation(summary = "统计即将超时任务", description = "获取指定时间范围内即将超时任务的数量")
    @GetMapping("/due-soon/count")
    public Result<Long> countDueSoonTasks(
            @Parameter(description = "距离超时的小时数") @RequestParam(defaultValue = "24") int hours) {
        long count = taskManagementService.countDueSoonTasks(hours);
        return Result.success(count);
    }

    // ========== 批量任务操作接口 ==========

    /**
     * 批量完成任务
     *
     * @param taskIds   任务 ID 列表
     * @param variables 流程变量（可选）
     * @return 成功完成的任务数量
     */
    @Operation(summary = "批量完成任务", description = "批量完成多个任务，返回成功完成的任务数量")
    @PostMapping("/batch-complete")
    public Result<Map<String, Object>> batchComplete(
            @Parameter(description = "任务 ID 列表") @RequestBody List<String> taskIds,
            @Parameter(description = "流程变量") @RequestParam(required = false) Map<String, Object> variables) {
        int successCount = taskManagementService.batchComplete(taskIds, variables);
        return Result.success(Map.of(
                "total", taskIds.size(),
                "success", successCount,
                "failed", taskIds.size() - successCount));
    }

    /**
     * 批量认领任务
     *
     * @param taskIds 任务 ID 列表
     * @param userId  用户 ID
     * @return 成功认领的任务数量
     */
    @Operation(summary = "批量认领任务", description = "批量认领多个任务给指定用户")
    @PostMapping("/batch-claim")
    public Result<Map<String, Object>> batchClaim(
            @Parameter(description = "任务 ID 列表") @RequestBody List<String> taskIds,
            @Parameter(description = "用户 ID") @RequestParam String userId) {
        int successCount = taskManagementService.batchClaim(taskIds, userId);
        return Result.success(Map.of(
                "total", taskIds.size(),
                "success", successCount,
                "failed", taskIds.size() - successCount));
    }

    /**
     * 批量委派任务
     *
     * @param taskIds 任务 ID 列表
     * @param userId  被委派人 ID
     * @return 成功委派的任务数量
     */
    @Operation(summary = "批量委派任务", description = "批量委派多个任务给指定用户")
    @PostMapping("/batch-delegate")
    public Result<Map<String, Object>> batchDelegate(
            @Parameter(description = "任务 ID 列表") @RequestBody List<String> taskIds,
            @Parameter(description = "被委派人 ID") @RequestParam String userId) {
        int successCount = taskManagementService.batchDelegate(taskIds, userId);
        return Result.success(Map.of(
                "total", taskIds.size(),
                "success", successCount,
                "failed", taskIds.size() - successCount));
    }

    // ========== 扩展任务操作接口 ==========

    /**
     * 解决任务
     *
     * @param taskId 任务 ID
     * @return 操作结果
     */
    @Operation(summary = "解决任务", description = "解决任务（委托任务处理完成后归还）")
    @PostMapping("/{taskId}/resolve")
    public Result<String> resolve(@Parameter(description = "任务 ID") @PathVariable @NotBlank String taskId) {
        taskManagementService.resolve(taskId);
        return Result.success("任务解决成功");
    }

    /**
     * 抄送任务
     *
     * @param taskId      任务 ID
     * @param userIds     接收人 ID 列表(逗号分隔)
     * @param initiatorId 发起人 ID
     * @return 操作结果
     */
    @Operation(summary = "抄送任务", description = "将任务抄送给指定用户")
    @PostMapping("/{taskId}/cc")
    public Result<String> cc(
            @Parameter(description = "任务 ID") @PathVariable @NotBlank String taskId,
            @Parameter(description = "接收人 ID 列表") @RequestParam String userIds,
            @Parameter(description = "发起人 ID") @RequestParam String initiatorId) {
        taskManagementService.cc(taskId, userIds, initiatorId);
        return Result.success("任务抄送成功");
    }

    /**
     * 标记抄送任务已读
     *
     * @param id 抄送记录 ID
     * @return 操作结果
     */
    @Operation(summary = "标记抄送任务已读", description = "将抄送记录标记为已读")
    @PostMapping("/cc/{id}/read")
    public Result<String> markCCAsRead(@Parameter(description = "抄送记录 ID") @PathVariable @NotNull Long id) {
        taskManagementService.markCCAsRead(id);
        return Result.success("标记成功");
    }

    // ========== 扩展查询接口 ==========

    /**
     * 分页查询已办任务
     *
     * @param query 分页查询参数
     * @return 分页结果
     */
    @Operation(summary = "查询已办任务", description = "查询当前用户已完成的历史任务")
    @GetMapping("/done")
    public Result<PageResult<TaskSummaryDTO>> listDoneTasks(@ParameterObject @Valid TaskPageQuery query) {
        PageResult<TaskSummaryDTO> result = taskManagementService.pageHistoric(query);
        return Result.success(result);
    }

    /**
     * 分页查询我的抄送
     *
     * @param query 分页查询参数
     * @return 分页结果
     */
    @Operation(summary = "查询我的抄送", description = "查询抄送给当前用户的任务")
    @GetMapping("/cc")
    public Result<PageResult<com.basebackend.scheduler.camunda.dto.TaskCCDTO>> listCCTasks(
            @ParameterObject @Valid TaskPageQuery query) {
        PageResult<com.basebackend.scheduler.camunda.dto.TaskCCDTO> result = taskManagementService.pageCC(query);
        return Result.success(result);
    }

    /**
     * 分页查询我发起的流程
     *
     * @param query 分页查询参数
     * @return 分页结果
     */
    @Operation(summary = "查询我发起的流程", description = "查询当前用户发起的流程实例")
    @GetMapping("/initiated")
    public Result<PageResult<com.basebackend.scheduler.camunda.dto.ProcessInstanceDTO>> listInitiated(
            @ParameterObject @Valid TaskPageQuery query) {
        PageResult<com.basebackend.scheduler.camunda.dto.ProcessInstanceDTO> result = taskManagementService
                .pageInitiated(query);
        return Result.success(result);
    }
}
