package com.basebackend.scheduler.camunda.service;

import com.basebackend.scheduler.camunda.dto.ProcessStatisticsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 流程统计服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessStatisticsService {

    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final TaskService taskService;
    private final RepositoryService repositoryService;

    /**
     * 获取流程总体统计信息
     */
    public ProcessStatisticsDTO getProcessStatistics() {
        // 计算总流程实例数（包括历史）
        long totalInstances = historyService.createHistoricProcessInstanceQuery().count();

        // 运行中的流程实例数
        long runningInstances = runtimeService.createProcessInstanceQuery().active().count();

        // 已完成的流程实例数
        long completedInstances = historyService.createHistoricProcessInstanceQuery().finished().count();

        // 已挂起的流程实例数
        long suspendedInstances = runtimeService.createProcessInstanceQuery().suspended().count();

        // 已终止的流程实例数
        long terminatedInstances = historyService.createHistoricProcessInstanceQuery()
                .completed()
                .count() - completedInstances;

        // 待办任务总数
        long totalTasks = taskService.createTaskQuery().active().count();

        // 流程定义总数
        long totalDefinitions = repositoryService.createProcessDefinitionQuery().count();

        // 活跃流程定义数（未挂起）
        long activeDefinitions = repositoryService.createProcessDefinitionQuery().active().count();

        // 今日统计
        Date todayStart = getStartOfDay(LocalDate.now());
        Date todayEnd = getEndOfDay(LocalDate.now());
        long todayStarted = historyService.createHistoricProcessInstanceQuery()
                .startedAfter(todayStart)
                .startedBefore(todayEnd)
                .count();
        long todayCompleted = historyService.createHistoricProcessInstanceQuery()
                .finishedAfter(todayStart)
                .finishedBefore(todayEnd)
                .count();

        // 本周统计
        Date weekStart = getStartOfWeek();
        Date weekEnd = getEndOfDay(LocalDate.now());
        long weekStarted = historyService.createHistoricProcessInstanceQuery()
                .startedAfter(weekStart)
                .startedBefore(weekEnd)
                .count();
        long weekCompleted = historyService.createHistoricProcessInstanceQuery()
                .finishedAfter(weekStart)
                .finishedBefore(weekEnd)
                .count();

        return ProcessStatisticsDTO.builder()
                .totalInstances(totalInstances)
                .runningInstances(runningInstances)
                .completedInstances(completedInstances)
                .suspendedInstances(suspendedInstances)
                .terminatedInstances(terminatedInstances)
                .totalTasks(totalTasks)
                .totalDefinitions(totalDefinitions)
                .activeDefinitions(activeDefinitions)
                .todayStarted(todayStarted)
                .todayCompleted(todayCompleted)
                .weekStarted(weekStarted)
                .weekCompleted(weekCompleted)
                .build();
    }

    /**
     * 按流程定义统计
     */
    public List<ProcessStatisticsDTO.DefinitionStatistics> getStatisticsByDefinition() {
        // 获取所有最新版本的流程定义
        List<ProcessDefinition> definitions = repositoryService.createProcessDefinitionQuery()
                .latestVersion()
                .orderByProcessDefinitionKey()
                .asc()
                .list();

        return definitions.stream()
                .map(this::buildDefinitionStatistics)
                .collect(Collectors.toList());
    }

    /**
     * 构建单个流程定义的统计信息
     */
    private ProcessStatisticsDTO.DefinitionStatistics buildDefinitionStatistics(ProcessDefinition definition) {
        String key = definition.getKey();

        // 运行中实例数
        long runningInstances = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(key)
                .active()
                .count();

        // 已完成实例数
        long completedInstances = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(key)
                .finished()
                .count();

        // 待办任务数
        long pendingTasks = taskService.createTaskQuery()
                .processDefinitionKey(key)
                .active()
                .count();

        // 计算平均完成时间
        Long avgDuration = calculateAverageDuration(key);

        return ProcessStatisticsDTO.DefinitionStatistics.builder()
                .processDefinitionKey(key)
                .processDefinitionName(definition.getName())
                .version(definition.getVersion())
                .runningInstances(runningInstances)
                .completedInstances(completedInstances)
                .pendingTasks(pendingTasks)
                .avgDurationInMillis(avgDuration)
                .build();
    }

    /**
     * 计算平均完成时间
     */
    private Long calculateAverageDuration(String processDefinitionKey) {
        List<HistoricProcessInstance> completedInstances = historyService
                .createHistoricProcessInstanceQuery()
                .processDefinitionKey(processDefinitionKey)
                .finished()
                .list();

        if (completedInstances.isEmpty()) {
            return null;
        }

        long totalDuration = 0L;
        int count = 0;

        for (HistoricProcessInstance instance : completedInstances) {
            if (instance.getStartTime() != null && instance.getEndTime() != null) {
                long duration = instance.getEndTime().getTime() - instance.getStartTime().getTime();
                totalDuration += duration;
                count++;
            }
        }

        return count > 0 ? totalDuration / count : null;
    }

    /**
     * 获取一天的开始时间
     */
    private Date getStartOfDay(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 获取一天的结束时间
     */
    private Date getEndOfDay(LocalDate date) {
        return Date.from(date.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 获取本周开始时间（周一）
     */
    private Date getStartOfWeek() {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - 1);
        return getStartOfDay(monday);
    }
}
