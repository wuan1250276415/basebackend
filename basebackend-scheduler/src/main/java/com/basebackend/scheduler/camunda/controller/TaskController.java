package com.basebackend.scheduler.camunda.controller;

import com.basebackend.scheduler.camunda.dto.TaskDTO;
import com.basebackend.scheduler.camunda.service.TaskManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务管理接口
 */
@Slf4j
@RestController
@RequestMapping("/api/workflow/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskManagementService taskManagementService;

    /**
     * 查询待办任务
     */
    @GetMapping("/pending/{assignee}")
    public ResponseEntity<Map<String, Object>> listPendingTasks(@PathVariable String assignee) {
        List<TaskDTO> tasks = taskManagementService.listPendingTasks(assignee);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", tasks);
        response.put("total", tasks.size());

        return ResponseEntity.ok(response);
    }

    /**
     * 查询候选任务
     */
    @GetMapping("/candidate/{candidateUser}")
    public ResponseEntity<Map<String, Object>> listCandidateTasks(@PathVariable String candidateUser) {
        List<TaskDTO> tasks = taskManagementService.listCandidateTasks(candidateUser);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", tasks);
        response.put("total", tasks.size());

        return ResponseEntity.ok(response);
    }

    /**
     * 根据流程实例ID查询任务
     */
    @GetMapping("/process-instance/{processInstanceId}")
    public ResponseEntity<Map<String, Object>> listTasksByProcessInstanceId(@PathVariable String processInstanceId) {
        List<TaskDTO> tasks = taskManagementService.listTasksByProcessInstanceId(processInstanceId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", tasks);
        response.put("total", tasks.size());

        return ResponseEntity.ok(response);
    }

    /**
     * 根据任务ID查询任务
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<Map<String, Object>> getTaskById(@PathVariable String taskId) {
        TaskDTO task = taskManagementService.getTaskById(taskId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", task);

        return ResponseEntity.ok(response);
    }

    /**
     * 完成任务
     */
    @PostMapping("/{taskId}/complete")
    public ResponseEntity<Map<String, Object>> completeTask(
            @PathVariable String taskId,
            @RequestBody(required = false) Map<String, Object> variables) {

        taskManagementService.completeTask(taskId, variables);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "任务完成");

        return ResponseEntity.ok(response);
    }

    /**
     * 认领任务
     */
    @PostMapping("/{taskId}/claim")
    public ResponseEntity<Map<String, Object>> claimTask(
            @PathVariable String taskId,
            @RequestBody Map<String, Object> request) {

        String userId = (String) request.get("userId");
        taskManagementService.claimTask(taskId, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "任务已认领");

        return ResponseEntity.ok(response);
    }

    /**
     * 取消认领任务
     */
    @PostMapping("/{taskId}/unclaim")
    public ResponseEntity<Map<String, Object>> unclaimTask(@PathVariable String taskId) {
        taskManagementService.unclaimTask(taskId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "任务认领已取消");

        return ResponseEntity.ok(response);
    }

    /**
     * 委派任务
     */
    @PostMapping("/{taskId}/delegate")
    public ResponseEntity<Map<String, Object>> delegateTask(
            @PathVariable String taskId,
            @RequestBody Map<String, Object> request) {

        String userId = (String) request.get("userId");
        taskManagementService.delegateTask(taskId, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "任务已委派");

        return ResponseEntity.ok(response);
    }

    /**
     * 转办任务
     */
    @PostMapping("/{taskId}/assign")
    public ResponseEntity<Map<String, Object>> assignTask(
            @PathVariable String taskId,
            @RequestBody Map<String, Object> request) {

        String userId = (String) request.get("userId");
        taskManagementService.assignTask(taskId, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "任务已转办");

        return ResponseEntity.ok(response);
    }

    /**
     * 设置任务变量
     */
    @PutMapping("/{taskId}/variables")
    public ResponseEntity<Map<String, Object>> setTaskVariables(
            @PathVariable String taskId,
            @RequestBody Map<String, Object> variables) {

        taskManagementService.setTaskVariables(taskId, variables);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "任务变量已设置");

        return ResponseEntity.ok(response);
    }

    /**
     * 获取任务变量
     */
    @GetMapping("/{taskId}/variables")
    public ResponseEntity<Map<String, Object>> getTaskVariables(@PathVariable String taskId) {
        Map<String, Object> variables = taskManagementService.getTaskVariables(taskId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", variables);

        return ResponseEntity.ok(response);
    }

    /**
     * 查询历史任务
     */
    @GetMapping("/history/process-instance/{processInstanceId}")
    public ResponseEntity<Map<String, Object>> listHistoricTasksByProcessInstanceId(
            @PathVariable String processInstanceId) {

        List<TaskDTO> tasks = taskManagementService.listHistoricTasksByProcessInstanceId(processInstanceId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", tasks);
        response.put("total", tasks.size());

        return ResponseEntity.ok(response);
    }

    /**
     * 批量完成任务
     */
    @PostMapping("/batch-complete")
    public ResponseEntity<Map<String, Object>> batchCompleteTasks(
            @RequestBody Map<String, Object> request) {

        @SuppressWarnings("unchecked")
        List<String> taskIds = (List<String>) request.get("taskIds");
        @SuppressWarnings("unchecked")
        Map<String, Object> variables = (Map<String, Object>) request.get("variables");

        int successCount = 0;
        int failCount = 0;
        Map<String, String> errors = new HashMap<>();

        for (String taskId : taskIds) {
            try {
                taskManagementService.completeTask(taskId, variables);
                successCount++;
            } catch (Exception e) {
                failCount++;
                errors.put(taskId, e.getMessage());
                log.error("批量完成任务失败: taskId={}", taskId, e);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", failCount == 0);
        response.put("message", String.format("批量完成任务: 成功%d个，失败%d个", successCount, failCount));
        response.put("successCount", successCount);
        response.put("failCount", failCount);
        response.put("errors", errors);

        return ResponseEntity.ok(response);
    }

    /**
     * 批量分配任务
     */
    @PostMapping("/batch-assign")
    public ResponseEntity<Map<String, Object>> batchAssignTasks(
            @RequestBody Map<String, Object> request) {

        @SuppressWarnings("unchecked")
        List<String> taskIds = (List<String>) request.get("taskIds");
        String userId = (String) request.get("userId");

        int successCount = 0;
        int failCount = 0;
        Map<String, String> errors = new HashMap<>();

        for (String taskId : taskIds) {
            try {
                taskManagementService.assignTask(taskId, userId);
                successCount++;
            } catch (Exception e) {
                failCount++;
                errors.put(taskId, e.getMessage());
                log.error("批量分配任务失败: taskId={}", taskId, e);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", failCount == 0);
        response.put("message", String.format("批量分配任务: 成功%d个，失败%d个", successCount, failCount));
        response.put("successCount", successCount);
        response.put("failCount", failCount);
        response.put("errors", errors);

        return ResponseEntity.ok(response);
    }
}
