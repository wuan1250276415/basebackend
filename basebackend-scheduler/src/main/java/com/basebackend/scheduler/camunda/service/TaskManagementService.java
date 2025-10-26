package com.basebackend.scheduler.camunda.service;

import com.basebackend.scheduler.camunda.dto.TaskDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 任务管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskManagementService {

    private final TaskService taskService;
    private final HistoryService historyService;

    /**
     * 查询待办任务
     */
    public List<TaskDTO> listPendingTasks(String assignee) {
        return taskService.createTaskQuery()
                .taskAssignee(assignee)
                .orderByTaskCreateTime()
                .desc()
                .list()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 查询候选任务
     */
    public List<TaskDTO> listCandidateTasks(String candidateUser) {
        return taskService.createTaskQuery()
                .taskCandidateUser(candidateUser)
                .orderByTaskCreateTime()
                .desc()
                .list()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据流程实例ID查询任务
     */
    public List<TaskDTO> listTasksByProcessInstanceId(String processInstanceId) {
        return taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .orderByTaskCreateTime()
                .desc()
                .list()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据任务ID查询任务
     */
    public TaskDTO getTaskById(String taskId) {
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new RuntimeException("任务不存在: taskId=" + taskId);
        }

        return convertToDTO(task);
    }

    /**
     * 完成任务
     */
    public void completeTask(String taskId, Map<String, Object> variables) {
        try {
            taskService.complete(taskId, variables);
            log.info("任务完成: taskId={}, variables={}", taskId, variables != null ? variables.keySet() : "none");
        } catch (Exception e) {
            log.error("任务完成失败: taskId={}", taskId, e);
            throw new RuntimeException("任务完成失败: " + e.getMessage(), e);
        }
    }

    /**
     * 认领任务
     */
    public void claimTask(String taskId, String userId) {
        try {
            taskService.claim(taskId, userId);
            log.info("任务已认领: taskId={}, userId={}", taskId, userId);
        } catch (Exception e) {
            log.error("任务认领失败: taskId={}, userId={}", taskId, userId, e);
            throw new RuntimeException("任务认领失败: " + e.getMessage(), e);
        }
    }

    /**
     * 取消认领任务
     */
    public void unclaimTask(String taskId) {
        try {
            taskService.setAssignee(taskId, null);
            log.info("任务认领已取消: taskId={}", taskId);
        } catch (Exception e) {
            log.error("取消认领失败: taskId={}", taskId, e);
            throw new RuntimeException("取消认领失败: " + e.getMessage(), e);
        }
    }

    /**
     * 委派任务
     */
    public void delegateTask(String taskId, String userId) {
        try {
            taskService.delegateTask(taskId, userId);
            log.info("任务已委派: taskId={}, userId={}", taskId, userId);
        } catch (Exception e) {
            log.error("任务委派失败: taskId={}, userId={}", taskId, userId, e);
            throw new RuntimeException("任务委派失败: " + e.getMessage(), e);
        }
    }

    /**
     * 转办任务
     */
    public void assignTask(String taskId, String userId) {
        try {
            taskService.setAssignee(taskId, userId);
            log.info("任务已转办: taskId={}, userId={}", taskId, userId);
        } catch (Exception e) {
            log.error("任务转办失败: taskId={}, userId={}", taskId, userId, e);
            throw new RuntimeException("任务转办失败: " + e.getMessage(), e);
        }
    }

    /**
     * 设置任务变量
     */
    public void setTaskVariables(String taskId, Map<String, Object> variables) {
        taskService.setVariables(taskId, variables);
        log.info("任务变量已设置: taskId={}, variables={}", taskId, variables.keySet());
    }

    /**
     * 获取任务变量
     */
    public Map<String, Object> getTaskVariables(String taskId) {
        return taskService.getVariables(taskId);
    }

    /**
     * 设置任务优先级
     */
    public void setTaskPriority(String taskId, int priority) {
        taskService.setPriority(taskId, priority);
        log.info("任务优先级已设置: taskId={}, priority={}", taskId, priority);
    }

    /**
     * 查询历史任务
     */
    public List<TaskDTO> listHistoricTasksByProcessInstanceId(String processInstanceId) {
        return historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricTaskInstanceEndTime()
                .desc()
                .list()
                .stream()
                .map(this::convertHistoricToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 转换为DTO
     */
    private TaskDTO convertToDTO(Task task) {
        Map<String, Object> variables = taskService.getVariables(task.getId());

        return TaskDTO.builder()
                .id(task.getId())
                .name(task.getName())
                .taskDefinitionKey(task.getTaskDefinitionKey())
                .processInstanceId(task.getProcessInstanceId())
                .processDefinitionId(task.getProcessDefinitionId())
                .executionId(task.getExecutionId())
                .assignee(task.getAssignee())
                .createTime(task.getCreateTime())
                .dueDate(task.getDueDate())
                .followUpDate(task.getFollowUpDate())
                .priority(task.getPriority())
                .description(task.getDescription())
                .tenantId(task.getTenantId())
                .variables(variables != null ? variables : new HashMap<>())
                .build();
    }

    /**
     * 转换历史任务为DTO
     */
    private TaskDTO convertHistoricToDTO(HistoricTaskInstance task) {
        return TaskDTO.builder()
                .id(task.getId())
                .name(task.getName())
                .taskDefinitionKey(task.getTaskDefinitionKey())
                .processInstanceId(task.getProcessInstanceId())
                .processDefinitionId(task.getProcessDefinitionId())
                .executionId(task.getExecutionId())
                .assignee(task.getAssignee())
                .createTime(task.getStartTime())
                .dueDate(task.getDueDate())
                .followUpDate(task.getFollowUpDate())
                .priority(task.getPriority())
                .description(task.getDescription())
                .tenantId(task.getTenantId())
                .build();
    }
}
