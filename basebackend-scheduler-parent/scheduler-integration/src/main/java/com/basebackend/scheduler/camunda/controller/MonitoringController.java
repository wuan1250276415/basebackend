package com.basebackend.scheduler.camunda.controller;

import com.basebackend.common.model.Result;
import com.basebackend.common.dto.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.management.JobDefinition;
import org.camunda.bpm.engine.runtime.Job;
import org.camunda.bpm.engine.runtime.JobQuery;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 监控与运维控制器
 *
 * @author BaseBackend Team
 * @since 2025-01-01
 */
@Slf4j
@RestController
@RequestMapping("/api/camunda/ops")
@RequiredArgsConstructor
@Tag(name = "Camunda 运维监控", description = "提供引擎运行状态、Job执行情况等监控指标")
public class MonitoringController {

    private final ManagementService managementService;

    @Operation(summary = "获取引擎概览指标", description = "获取当前Job执行器、部署数量、流程定义数量等概览信息")
    @GetMapping("/metrics")
    public Result<Map<String, Object>> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        long activeJobs = managementService.createJobQuery().active().count();
        long suspendedJobs = managementService.createJobQuery().suspended().count();
        long failedJobs = managementService.createJobQuery().withException().count();
        long timerJobs = managementService.createJobQuery().timers().count();
        long messageJobs = managementService.createJobQuery().messages().count();

        metrics.put("jobs.active", activeJobs);
        metrics.put("jobs.suspended", suspendedJobs);
        metrics.put("jobs.failed", failedJobs);
        metrics.put("jobs.timer", timerJobs);
        metrics.put("jobs.message", messageJobs);

        // Job Executor 配置信息
        metrics.put("jobExecutor.lockTimeInMillis",
                managementService.getProperties().get("jobExecutorLockTimeInMillis"));

        return Result.success(metrics);
    }

    @Operation(summary = "查询失败的Job", description = "获取当前所有异常/重试耗尽的Job")
    @GetMapping("/jobs/failed")
    public Result<List<Map<String, Object>>> getFailedJobs() {
        List<Job> jobs = managementService.createJobQuery()
                .withException()
                .orderByJobRetries().asc()
                .list();

        List<Map<String, Object>> result = jobs.stream().map(job -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", job.getId());
            map.put("processInstanceId", job.getProcessInstanceId());
            map.put("processDefinitionId", job.getProcessDefinitionId());
            map.put("executionId", job.getExecutionId());
            map.put("exceptionMessage", job.getExceptionMessage());
            map.put("retries", job.getRetries());
            map.put("dueDate", job.getDuedate());
            map.put("createTime", job.getCreateTime()); // Note: createTime might depend on version
            return map;
        }).collect(Collectors.toList());

        return Result.success(result);
    }
}
