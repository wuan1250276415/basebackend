package com.basebackend.scheduler.camunda.controller;

import com.basebackend.scheduler.camunda.dto.ProcessInstanceDTO;
import com.basebackend.scheduler.camunda.service.ProcessInstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程实例管理接口
 */
@Slf4j
@RestController
@RequestMapping("/api/workflow/instances")
@RequiredArgsConstructor
public class ProcessInstanceController {

    private final ProcessInstanceService processInstanceService;

    /**
     * 启动流程实例
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startProcessInstance(
            @RequestBody Map<String, Object> request) {

        String processDefinitionKey = (String) request.get("processDefinitionKey");
        String businessKey = (String) request.get("businessKey");
        @SuppressWarnings("unchecked")
        Map<String, Object> variables = (Map<String, Object>) request.get("variables");

        ProcessInstanceDTO instance = processInstanceService.startProcessInstance(
                processDefinitionKey, businessKey, variables);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", instance);
        response.put("message", "流程实例启动成功");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 查询运行中的流程实例
     */
    @GetMapping("/running")
    public ResponseEntity<Map<String, Object>> listRunningProcessInstances() {
        List<ProcessInstanceDTO> instances = processInstanceService.listRunningProcessInstances();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", instances);
        response.put("total", instances.size());

        return ResponseEntity.ok(response);
    }

    /**
     * 根据流程定义Key查询运行中的流程实例
     */
    @GetMapping("/running/key/{key}")
    public ResponseEntity<Map<String, Object>> listRunningProcessInstancesByKey(@PathVariable String key) {
        List<ProcessInstanceDTO> instances = processInstanceService.listRunningProcessInstancesByKey(key);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", instances);
        response.put("total", instances.size());

        return ResponseEntity.ok(response);
    }

    /**
     * 根据ID查询流程实例
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getProcessInstanceById(@PathVariable String id) {
        ProcessInstanceDTO instance = processInstanceService.getProcessInstanceById(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", instance);

        return ResponseEntity.ok(response);
    }

    /**
     * 根据业务键查询流程实例
     */
    @GetMapping("/business-key/{businessKey}")
    public ResponseEntity<Map<String, Object>> getProcessInstanceByBusinessKey(@PathVariable String businessKey) {
        ProcessInstanceDTO instance = processInstanceService.getProcessInstanceByBusinessKey(businessKey);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", instance);

        return ResponseEntity.ok(response);
    }

    /**
     * 挂起流程实例
     */
    @PutMapping("/{id}/suspend")
    public ResponseEntity<Map<String, Object>> suspendProcessInstance(@PathVariable String id) {
        processInstanceService.suspendProcessInstance(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "流程实例已挂起");

        return ResponseEntity.ok(response);
    }

    /**
     * 激活流程实例
     */
    @PutMapping("/{id}/activate")
    public ResponseEntity<Map<String, Object>> activateProcessInstance(@PathVariable String id) {
        processInstanceService.activateProcessInstance(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "流程实例已激活");

        return ResponseEntity.ok(response);
    }

    /**
     * 删除流程实例
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteProcessInstance(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "Deleted by user") String deleteReason) {

        processInstanceService.deleteProcessInstance(id, deleteReason);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "流程实例已删除");

        return ResponseEntity.ok(response);
    }

    /**
     * 设置流程变量
     */
    @PutMapping("/{id}/variables")
    public ResponseEntity<Map<String, Object>> setVariables(
            @PathVariable String id,
            @RequestBody Map<String, Object> variables) {

        processInstanceService.setVariables(id, variables);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "流程变量已设置");

        return ResponseEntity.ok(response);
    }

    /**
     * 获取流程变量
     */
    @GetMapping("/{id}/variables")
    public ResponseEntity<Map<String, Object>> getVariables(@PathVariable String id) {
        Map<String, Object> variables = processInstanceService.getVariables(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", variables);

        return ResponseEntity.ok(response);
    }
}
