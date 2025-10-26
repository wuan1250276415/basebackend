package com.basebackend.scheduler.camunda.controller;

import com.basebackend.scheduler.camunda.dto.ProcessDefinitionDTO;
import com.basebackend.scheduler.camunda.service.ProcessDefinitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程定义管理接口
 */
@Slf4j
@RestController
@RequestMapping("/api/workflow/definitions")
@RequiredArgsConstructor
public class ProcessDefinitionController {

    private final ProcessDefinitionService processDefinitionService;

    /**
     * 查询所有流程定义
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listProcessDefinitions() {
        List<ProcessDefinitionDTO> definitions = processDefinitionService.listProcessDefinitions();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", definitions);
        response.put("total", definitions.size());
        return ResponseEntity.ok(response);
    }

    /**
     * 根据Key查询流程定义
     */
    @GetMapping("/key/{key}")
    public ResponseEntity<Map<String, Object>> getProcessDefinitionByKey(@PathVariable String key) {
        ProcessDefinitionDTO definition = processDefinitionService.getProcessDefinitionByKey(key);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", definition);
        return ResponseEntity.ok(response);
    }

    /**
     * 根据ID查询流程定义
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getProcessDefinitionById(@PathVariable String id) {
        ProcessDefinitionDTO definition = processDefinitionService.getProcessDefinitionById(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", definition);
        return ResponseEntity.ok(response);
    }

    /**
     * 部署流程定义
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> deployProcessDefinition(
            @RequestParam("name") String name,
            @RequestParam("file") MultipartFile file) {

        String deploymentId = processDefinitionService.deployProcessDefinition(name, file);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("deploymentId", deploymentId);
        response.put("message", "流程部署成功");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 挂起流程定义
     */
    @PutMapping("/{id}/suspend")
    public ResponseEntity<Map<String, Object>> suspendProcessDefinition(@PathVariable String id) {
        processDefinitionService.suspendProcessDefinition(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "流程定义已挂起");

        return ResponseEntity.ok(response);
    }

    /**
     * 激活流程定义
     */
    @PutMapping("/{id}/activate")
    public ResponseEntity<Map<String, Object>> activateProcessDefinition(@PathVariable String id) {
        processDefinitionService.activateProcessDefinition(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "流程定义已激活");

        return ResponseEntity.ok(response);
    }

    /**
     * 删除部署
     */
    @DeleteMapping("/deployment/{deploymentId}")
    public ResponseEntity<Map<String, Object>> deleteDeployment(
            @PathVariable String deploymentId,
            @RequestParam(defaultValue = "false") boolean cascade) {

        processDefinitionService.deleteDeployment(deploymentId, cascade);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "部署已删除");

        return ResponseEntity.ok(response);
    }

    /**
     * 获取流程定义XML
     */
    @GetMapping("/{id}/xml")
    public ResponseEntity<Map<String, Object>> getProcessDefinitionXml(@PathVariable String id) {
        String xml = processDefinitionService.getProcessDefinitionXml(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("xml", xml);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取流程图
     */
    @GetMapping(value = "/{id}/diagram", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getProcessDefinitionDiagram(@PathVariable String id) {
        try {
            InputStream inputStream = processDefinitionService.getProcessDefinitionDiagram(id);
            byte[] bytes = inputStream.readAllBytes();
            return ResponseEntity.ok(bytes);
        } catch (Exception e) {
            log.error("获取流程图失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
