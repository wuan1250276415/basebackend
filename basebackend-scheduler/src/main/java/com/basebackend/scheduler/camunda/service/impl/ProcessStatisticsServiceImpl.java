package com.basebackend.scheduler.camunda.service.impl;

import com.basebackend.scheduler.camunda.config.PaginationConstants;
import com.basebackend.scheduler.camunda.dto.InstanceStatisticsDTO;
import com.basebackend.scheduler.camunda.dto.ProcessStatisticsDTO;
import com.basebackend.scheduler.camunda.dto.StatisticsQuery;
import com.basebackend.scheduler.camunda.dto.TaskStatisticsDTO;
import com.basebackend.scheduler.camunda.exception.CamundaServiceException;
import com.basebackend.scheduler.camunda.service.ProcessStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 统计分析服务实现类
 *
 * <p>提供流程定义、流程实例、任务的统计分析功能，包括运行状态概览。
 * 支持缓存提升查询性能。
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessStatisticsServiceImpl implements ProcessStatisticsService {

    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;

    @Override
    @Transactional(readOnly = true)
    public ProcessStatisticsDTO processDefinitions(StatisticsQuery query) {
        try {
            log.info("Getting process definition statistics");

            ProcessStatisticsDTO dto = new ProcessStatisticsDTO();

            // 统计流程定义总数
            long totalDefinitions = repositoryService.createProcessDefinitionQuery().count();
            dto.setTotalDefinitions(totalDefinitions);

            // 统计最新版本数
            long latestDefinitions = repositoryService.createProcessDefinitionQuery()
                    .latestVersion()
                    .count();
            dto.setLatestVersionDefinitions(latestDefinitions);

            // 统计活跃的流程定义
            long activeDefinitions = repositoryService.createProcessDefinitionQuery()
                    .active()
                    .latestVersion()
                    .count();
            dto.setActiveDefinitions(activeDefinitions);

            // 统计挂起的流程定义
            long suspendedDefinitions = repositoryService.createProcessDefinitionQuery()
                    .suspended()
                    .latestVersion()
                    .count();
            dto.setSuspendedDefinitions(suspendedDefinitions);

            // 统计部署总数
            long totalDeployments = repositoryService.createDeploymentQuery().count();
            dto.setTotalDeployments(totalDeployments);

            // 如果指定了流程定义Key，获取该流程的详细统计
            if (StringUtils.hasText(query.getProcessDefinitionKey())) {
                long keyInstancesRunning = runtimeService.createProcessInstanceQuery()
                        .processDefinitionKey(query.getProcessDefinitionKey())
                        .count();
                dto.setKeyInstancesRunning(keyInstancesRunning);

                long keyInstancesCompleted = historyService.createHistoricProcessInstanceQuery()
                        .processDefinitionKey(query.getProcessDefinitionKey())
                        .finished()
                        .count();
                dto.setKeyInstancesCompleted(keyInstancesCompleted);
            }

            log.info("Process definition statistics: total={}, latest={}, active={}, suspended={}",
                    totalDefinitions, latestDefinitions, activeDefinitions, suspendedDefinitions);

            return dto;
        } catch (Exception ex) {
            log.error("Failed to get process definition statistics", ex);
            throw new CamundaServiceException("获取流程定义统计失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public InstanceStatisticsDTO instances(StatisticsQuery query) {
        try {
            log.info("Getting process instance statistics");

            InstanceStatisticsDTO dto = new InstanceStatisticsDTO();

            // 统计运行中的实例
            long runningInstances = runtimeService.createProcessInstanceQuery().count();
            dto.setRunningInstances(runningInstances);

            // 统计活跃实例
            long activeInstances = runtimeService.createProcessInstanceQuery()
                    .active()
                    .count();
            dto.setActiveInstances(activeInstances);

            // 统计挂起的实例
            long suspendedInstances = runtimeService.createProcessInstanceQuery()
                    .suspended()
                    .count();
            dto.setSuspendedInstances(suspendedInstances);

            // 统计已完成的实例
            long completedInstances = historyService.createHistoricProcessInstanceQuery()
                    .finished()
                    .count();
            dto.setCompletedInstances(completedInstances);

            // 统计异常终止的实例（使用变量过滤代替 deleteReasonLike）
            long terminatedInstances = historyService.createHistoricProcessInstanceQuery()
                    .finished()
                    .count();
            // Note: deleteReasonLike is not available in this Camunda version
            // Consider using process variables or other filtering mechanisms
            dto.setTerminatedInstances(0L); // Placeholder - implement custom logic if needed

            // 计算总实例数
            long totalInstances = runningInstances + completedInstances;
            dto.setTotalInstances(totalInstances);

            // 如果有时间范围，统计该时间范围内的数据
            if (query.getStartTime() != null && query.getEndTime() != null) {
                long periodInstances = historyService.createHistoricProcessInstanceQuery()
                        .startedAfter(com.basebackend.scheduler.util.DateTimeUtil.toDate(query.getStartTime()))
                        .startedBefore(com.basebackend.scheduler.util.DateTimeUtil.toDate(query.getEndTime()))
                        .count();
                dto.setPeriodInstances(periodInstances);

                long periodCompleted = historyService.createHistoricProcessInstanceQuery()
                        .startedAfter(com.basebackend.scheduler.util.DateTimeUtil.toDate(query.getStartTime()))
                        .startedBefore(com.basebackend.scheduler.util.DateTimeUtil.toDate(query.getEndTime()))
                        .finished()
                        .count();
                dto.setPeriodCompletedInstances(periodCompleted);

                // 计算完成率
                if (periodInstances > 0) {
                    double completionRate = (double) periodCompleted / periodInstances * 100;
                    dto.setCompletionRate(completionRate);
                }

                // 计算平均持续时间（限制在时间范围内，避免加载过多数据）
                try {
                    List<Long> durations = historyService.createHistoricProcessInstanceQuery()
                            .startedAfter(com.basebackend.scheduler.util.DateTimeUtil.toDate(query.getStartTime()))
                            .startedBefore(com.basebackend.scheduler.util.DateTimeUtil.toDate(query.getEndTime()))
                            .finished()
                            .listPage(0, PaginationConstants.MAX_PAGE_SIZE) // 限制最多200条
                            .stream()
                            .filter(instance -> instance.getDurationInMillis() != null)
                            .map(instance -> instance.getDurationInMillis())
                            .collect(Collectors.toList());

                    if (!durations.isEmpty()) {
                        double avgDuration = durations.stream()
                                .mapToLong(Long::longValue)
                                .average()
                                .orElse(0.0);
                        dto.setAverageDurationInMillis(Math.round(avgDuration));
                    }
                } catch (Exception ex) {
                    log.warn("Failed to calculate average duration in time range", ex);
                }
            }

            log.info("Process instance statistics: running={}, completed={}, total={}",
                    runningInstances, completedInstances, totalInstances);

            return dto;
        } catch (Exception ex) {
            log.error("Failed to get process instance statistics", ex);
            throw new CamundaServiceException("获取流程实例统计失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TaskStatisticsDTO tasks(StatisticsQuery query) {
        try {
            log.info("Getting task statistics");

            TaskStatisticsDTO dto = new TaskStatisticsDTO();

            // 统计待办任务总数
            long totalTasks = taskService.createTaskQuery().count();
            dto.setTotalTasks(totalTasks);

            // 统计已分配的任务
            long assignedTasks = taskService.createTaskQuery()
                    .taskAssigned()
                    .count();
            dto.setAssignedTasks(assignedTasks);

            // 统计未分配的任务
            long unassignedTasks = taskService.createTaskQuery()
                    .taskUnassigned()
                    .count();
            dto.setUnassignedTasks(unassignedTasks);

            // 统计已完成的任务
            long completedTasks = historyService.createHistoricTaskInstanceQuery()
                    .finished()
                    .count();
            dto.setCompletedTasks(completedTasks);

            // 如果指定了用户，统计该用户的任务
            if (StringUtils.hasText(query.getAssignee())) {
                long userTasks = taskService.createTaskQuery()
                        .taskAssignee(query.getAssignee())
                        .count();
                dto.setUserTasks(userTasks);

                long userCompletedTasks = historyService.createHistoricTaskInstanceQuery()
                        .taskAssignee(query.getAssignee())
                        .finished()
                        .count();
                dto.setUserCompletedTasks(userCompletedTasks);
            }

            // 如果有时间范围，统计该时间范围内的数据
            if (query.getStartTime() != null && query.getEndTime() != null) {
                long periodTasks = historyService.createHistoricTaskInstanceQuery()
                        .finishedAfter(com.basebackend.scheduler.util.DateTimeUtil.toDate(query.getStartTime()))
                        .finishedBefore(com.basebackend.scheduler.util.DateTimeUtil.toDate(query.getEndTime()))
                        .count();
                dto.setPeriodTasks(periodTasks);

                long periodCompleted = historyService.createHistoricTaskInstanceQuery()
                        .finishedAfter(com.basebackend.scheduler.util.DateTimeUtil.toDate(query.getStartTime()))
                        .finishedBefore(com.basebackend.scheduler.util.DateTimeUtil.toDate(query.getEndTime()))
                        .finished()
                        .count();
                dto.setPeriodCompletedTasks(periodCompleted);

                // 计算平均处理时间（限制在时间范围内，避免加载过多数据）
                try {
                    List<Long> durations = historyService.createHistoricTaskInstanceQuery()
                            .finishedAfter(com.basebackend.scheduler.util.DateTimeUtil.toDate(query.getStartTime()))
                            .finishedBefore(com.basebackend.scheduler.util.DateTimeUtil.toDate(query.getEndTime()))
                            .finished()
                            .listPage(0, PaginationConstants.MAX_PAGE_SIZE) // 限制最多200条
                            .stream()
                            .filter(task -> task.getDurationInMillis() != null)
                            .map(task -> task.getDurationInMillis())
                            .collect(Collectors.toList());

                    if (!durations.isEmpty()) {
                        double avgDuration = durations.stream()
                                .mapToLong(Long::longValue)
                                .average()
                                .orElse(0.0);
                        dto.setAverageDurationInMillis(Math.round(avgDuration));
                    }
                } catch (Exception ex) {
                    log.warn("Failed to calculate average task duration in time range", ex);
                }
            }

            log.info("Task statistics: total={}, assigned={}, unassigned={}, completed={}",
                    totalTasks, assignedTasks, unassignedTasks, completedTasks);

            return dto;
        } catch (Exception ex) {
            log.error("Failed to get task statistics", ex);
            throw new CamundaServiceException("获取任务统计失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> overview(StatisticsQuery query) {
        try {
            log.info("Getting workflow overview statistics");

            Map<String, Object> overview = new HashMap<>();

            // 流程定义统计
            ProcessStatisticsDTO processStats = processDefinitions(query);
            overview.put("processDefinitions", processStats);

            // 流程实例统计
            InstanceStatisticsDTO instanceStats = instances(query);
            overview.put("processInstances", instanceStats);

            // 任务统计
            TaskStatisticsDTO taskStats = tasks(query);
            overview.put("tasks", taskStats);

            // 系统健康指标
            Map<String, Object> healthMetrics = new HashMap<>();

            // 计算任务积压率（未分配任务占比）
            if (taskStats.getTotalTasks() > 0) {
                double backlogRate = (double) taskStats.getUnassignedTasks()
                        / taskStats.getTotalTasks() * 100;
                healthMetrics.put("taskBacklogRate", backlogRate);
            }

            // 计算流程完成率
            if (instanceStats.getTotalInstances() > 0) {
                double completionRate = (double) instanceStats.getCompletedInstances()
                        / instanceStats.getTotalInstances() * 100;
                healthMetrics.put("processCompletionRate", completionRate);
            }

            // 计算任务分配率
            if (taskStats.getTotalTasks() > 0) {
                double assignmentRate = (double) taskStats.getAssignedTasks()
                        / taskStats.getTotalTasks() * 100;
                healthMetrics.put("taskAssignmentRate", assignmentRate);
            }

            overview.put("healthMetrics", healthMetrics);

            // 添加查询时间戳
            overview.put("timestamp", System.currentTimeMillis());

            log.info("Workflow overview statistics generated successfully");

            return overview;
        } catch (Exception ex) {
            log.error("Failed to get workflow overview statistics", ex);
            throw new CamundaServiceException("获取工作流概览统计失败: " + ex.getMessage(), ex);
        }
    }
}
