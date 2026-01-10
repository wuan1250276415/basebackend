package com.basebackend.scheduler.camunda.controller;

import com.basebackend.common.dto.PageResult;
import com.basebackend.common.model.Result;
import com.basebackend.scheduler.camunda.dto.JobDTO;
import com.basebackend.scheduler.camunda.dto.JobDetailDTO;
import com.basebackend.scheduler.camunda.dto.JobPageQuery;
import com.basebackend.scheduler.camunda.dto.JobRetryRequest;
import com.basebackend.scheduler.camunda.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Camunda 作业控制器
 *
 * <p>
 * 提供 Camunda 作业（Job）管理功能，包括：
 * <ul>
 * <li>作业分页查询（按状态、流程定义、执行时间过滤）</li>
 * <li>作业详情查看（包含异常堆栈）</li>
 * <li>作业操作（重试、立即执行、删除、挂起、激活）</li>
 * <li>批量操作（批量重试、批量删除）</li>
 * <li>统计信息（失败作业数、可执行作业数）</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/camunda/jobs")
@RequiredArgsConstructor
@Tag(name = "Camunda 作业管理", description = "Camunda 作业（Job）监控与管理 API")
@SecurityRequirement(name = "BearerAuth")
public class JobController {

    private final JobService jobService;

    // ========== 查询接口 ==========

    /**
     * 分页查询作业
     *
     * @param query 分页查询参数
     * @return 分页结果
     */
    @Operation(summary = "分页查询作业", description = "支持按状态、流程定义、执行时间等条件过滤的作业分页查询")
    @GetMapping
    public Result<PageResult<JobDTO>> page(@ParameterObject @Valid JobPageQuery query) {
        PageResult<JobDTO> result = jobService.page(query);
        return Result.success(result);
    }

    /**
     * 获取作业详情
     *
     * @param jobId 作业 ID
     * @return 作业详情
     */
    @Operation(summary = "获取作业详情", description = "根据作业 ID 获取详细信息，包含异常堆栈")
    @GetMapping("/{jobId}")
    public Result<JobDetailDTO> detail(
            @Parameter(description = "作业 ID") @PathVariable @NotBlank String jobId) {
        JobDetailDTO dto = jobService.detail(jobId);
        return Result.success(dto);
    }

    /**
     * 获取作业异常堆栈
     *
     * @param jobId 作业 ID
     * @return 异常堆栈字符串
     */
    @Operation(summary = "获取作业异常堆栈", description = "获取作业失败时的完整异常堆栈信息")
    @GetMapping("/{jobId}/exception-stacktrace")
    public Result<String> getExceptionStacktrace(
            @Parameter(description = "作业 ID") @PathVariable @NotBlank String jobId) {
        String stacktrace = jobService.getExceptionStacktrace(jobId);
        return Result.success(stacktrace);
    }

    /**
     * 查询失败作业列表
     *
     * @param maxResults 最大结果数
     * @return 失败作业列表
     */
    @Operation(summary = "查询失败作业列表", description = "快速获取失败作业列表（重试次数为0）")
    @GetMapping("/failed")
    public Result<List<JobDTO>> listFailedJobs(
            @Parameter(description = "最大结果数") @RequestParam(defaultValue = "100") int maxResults) {
        List<JobDTO> list = jobService.listFailedJobs(maxResults);
        return Result.success(list);
    }

    // ========== 操作接口 ==========

    /**
     * 重试作业
     *
     * @param jobId   作业 ID
     * @param request 重试请求参数
     * @return 操作结果
     */
    @Operation(summary = "重试作业", description = "手动重试失败的作业，可设置新的重试次数和截止时间")
    @PostMapping("/{jobId}/retry")
    public Result<String> retry(
            @Parameter(description = "作业 ID") @PathVariable @NotBlank String jobId,
            @Valid @RequestBody(required = false) JobRetryRequest request) {
        if (request == null) {
            request = new JobRetryRequest();
        }
        jobService.retry(jobId, request);
        return Result.success("作业已设置重试");
    }

    /**
     * 立即执行作业
     *
     * @param jobId 作业 ID
     * @return 操作结果
     */
    @Operation(summary = "立即执行作业", description = "立即同步执行指定的作业")
    @PostMapping("/{jobId}/execute")
    public Result<String> execute(
            @Parameter(description = "作业 ID") @PathVariable @NotBlank String jobId) {
        jobService.executeNow(jobId);
        return Result.success("作业执行成功");
    }

    /**
     * 删除作业
     *
     * @param jobId 作业 ID
     * @return 操作结果
     */
    @Operation(summary = "删除作业", description = "删除指定的作业")
    @DeleteMapping("/{jobId}")
    public Result<String> delete(
            @Parameter(description = "作业 ID") @PathVariable @NotBlank String jobId) {
        jobService.delete(jobId);
        return Result.success("作业删除成功");
    }

    /**
     * 修改作业截止时间
     *
     * @param jobId   作业 ID
     * @param duedate 新的截止时间
     * @return 操作结果
     */
    @Operation(summary = "修改作业截止时间", description = "设置作业的新截止时间（下次执行时间）")
    @PutMapping("/{jobId}/duedate")
    public Result<String> setDuedate(
            @Parameter(description = "作业 ID") @PathVariable @NotBlank String jobId,
            @Parameter(description = "新的截止时间") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") Date duedate) {
        jobService.setDuedate(jobId, duedate);
        return Result.success("截止时间修改成功");
    }

    /**
     * 修改作业优先级
     *
     * @param jobId    作业 ID
     * @param priority 新的优先级
     * @return 操作结果
     */
    @Operation(summary = "修改作业优先级", description = "设置作业的新优先级")
    @PutMapping("/{jobId}/priority")
    public Result<String> setPriority(
            @Parameter(description = "作业 ID") @PathVariable @NotBlank String jobId,
            @Parameter(description = "新的优先级") @RequestParam long priority) {
        jobService.setPriority(jobId, priority);
        return Result.success("优先级修改成功");
    }

    /**
     * 修改作业重试次数
     *
     * @param jobId   作业 ID
     * @param retries 新的重试次数
     * @return 操作结果
     */
    @Operation(summary = "修改作业重试次数", description = "设置作业的新重试次数")
    @PutMapping("/{jobId}/retries")
    public Result<String> setRetries(
            @Parameter(description = "作业 ID") @PathVariable @NotBlank String jobId,
            @Parameter(description = "新的重试次数") @RequestParam int retries) {
        jobService.setRetries(jobId, retries);
        return Result.success("重试次数修改成功");
    }

    /**
     * 挂起作业
     *
     * @param jobId 作业 ID
     * @return 操作结果
     */
    @Operation(summary = "挂起作业", description = "挂起指定的作业，暂停其执行")
    @PostMapping("/{jobId}/suspend")
    public Result<String> suspend(
            @Parameter(description = "作业 ID") @PathVariable @NotBlank String jobId) {
        jobService.suspend(jobId);
        return Result.success("作业挂起成功");
    }

    /**
     * 激活作业
     *
     * @param jobId 作业 ID
     * @return 操作结果
     */
    @Operation(summary = "激活作业", description = "激活已挂起的作业，恢复其执行")
    @PostMapping("/{jobId}/activate")
    public Result<String> activate(
            @Parameter(description = "作业 ID") @PathVariable @NotBlank String jobId) {
        jobService.activate(jobId);
        return Result.success("作业激活成功");
    }

    // ========== 批量操作接口 ==========

    /**
     * 批量重试作业
     *
     * @param jobIds  作业 ID 列表
     * @param retries 重试次数（可选）
     * @return 成功重试的数量
     */
    @Operation(summary = "批量重试作业", description = "批量重试多个失败的作业")
    @PostMapping("/batch-retry")
    public Result<Map<String, Object>> batchRetry(
            @Parameter(description = "作业 ID 列表") @RequestBody @NotEmpty List<String> jobIds,
            @Parameter(description = "重试次数") @RequestParam(required = false, defaultValue = "3") Integer retries) {
        int successCount = jobService.batchRetry(jobIds, retries);
        return Result.success(Map.of(
                "total", jobIds.size(),
                "success", successCount,
                "failed", jobIds.size() - successCount));
    }

    /**
     * 批量删除作业
     *
     * @param jobIds 作业 ID 列表
     * @return 成功删除的数量
     */
    @Operation(summary = "批量删除作业", description = "批量删除多个作业")
    @PostMapping("/batch-delete")
    public Result<Map<String, Object>> batchDelete(
            @Parameter(description = "作业 ID 列表") @RequestBody @NotEmpty List<String> jobIds) {
        int successCount = jobService.batchDelete(jobIds);
        return Result.success(Map.of(
                "total", jobIds.size(),
                "success", successCount,
                "failed", jobIds.size() - successCount));
    }

    // ========== 统计接口 ==========

    /**
     * 获取作业统计信息
     *
     * @return 统计信息
     */
    @Operation(summary = "获取作业统计信息", description = "获取失败作业数、可执行作业数等统计信息")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics() {
        long failedCount = jobService.countFailedJobs();
        long executableCount = jobService.countExecutableJobs();
        Map<String, Long> failedByDefinition = jobService.countFailedJobsByProcessDefinition();

        return Result.success(Map.of(
                "failedJobCount", failedCount,
                "executableJobCount", executableCount,
                "failedJobsByProcessDefinition", failedByDefinition));
    }

    /**
     * 统计失败作业数量
     *
     * @return 失败作业数量
     */
    @Operation(summary = "统计失败作业数量", description = "获取当前失败作业（重试次数为0）的总数")
    @GetMapping("/statistics/failed-count")
    public Result<Long> countFailedJobs() {
        long count = jobService.countFailedJobs();
        return Result.success(count);
    }

    /**
     * 按流程定义统计失败作业
     *
     * @return 按流程定义 Key 分组的失败作业数量
     */
    @Operation(summary = "按流程定义统计失败作业", description = "获取按流程定义 Key 分组的失败作业数量")
    @GetMapping("/statistics/failed-by-definition")
    public Result<Map<String, Long>> countFailedJobsByProcessDefinition() {
        Map<String, Long> result = jobService.countFailedJobsByProcessDefinition();
        return Result.success(result);
    }
}
