package com.basebackend.scheduler.camunda.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.scheduler.camunda.dto.FormTemplateDTO;
import com.basebackend.scheduler.camunda.service.FormTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流表单模板接口
 */
@Slf4j
@RestController
@RequestMapping("/api/workflow/form-templates")
@RequiredArgsConstructor
public class FormTemplateController {

    private final FormTemplateService formTemplateService;

    /**
     * 查询表单模板列表（分页）
     *
     * @param page 页码（默认1）
     * @param size 每页大小（默认10）
     * @param name 模板名称（可选，模糊查询）
     * @param processDefinitionKey 流程定义Key（可选）
     * @param status 状态（可选，0-禁用，1-启用）
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listFormTemplates(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String processDefinitionKey,
            @RequestParam(required = false) Integer status) {

        Page<FormTemplateDTO> pageResult = formTemplateService.listFormTemplates(page, size, name, processDefinitionKey, status);

        Map<String, Object> data = new HashMap<>();
        data.put("list", pageResult.getRecords());
        data.put("total", pageResult.getTotal());
        data.put("page", pageResult.getCurrent());
        data.put("size", pageResult.getSize());
        data.put("pages", pageResult.getPages());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    /**
     * 根据ID查询表单模板详情
     *
     * @param id 模板ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getFormTemplateById(@PathVariable Long id) {
        FormTemplateDTO template = formTemplateService.getFormTemplateById(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", template);

        return ResponseEntity.ok(response);
    }

    /**
     * 根据表单Key查询表单模板
     *
     * @param formKey 表单Key
     */
    @GetMapping("/by-key/{formKey}")
    public ResponseEntity<Map<String, Object>> getFormTemplateByFormKey(@PathVariable String formKey) {
        FormTemplateDTO template = formTemplateService.getFormTemplateByFormKey(formKey);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", template);

        return ResponseEntity.ok(response);
    }

    /**
     * 根据流程定义Key查询表单模板列表
     *
     * @param processDefinitionKey 流程定义Key
     */
    @GetMapping("/by-process/{processDefinitionKey}")
    public ResponseEntity<Map<String, Object>> getFormTemplatesByProcessDefinitionKey(
            @PathVariable String processDefinitionKey) {

        List<FormTemplateDTO> templates = formTemplateService.getFormTemplatesByProcessDefinitionKey(processDefinitionKey);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", templates);
        response.put("total", templates.size());

        return ResponseEntity.ok(response);
    }

    /**
     * 查询所有启用的表单模板
     */
    @GetMapping("/enabled")
    public ResponseEntity<Map<String, Object>> getEnabledFormTemplates() {
        List<FormTemplateDTO> templates = formTemplateService.getEnabledFormTemplates();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", templates);
        response.put("total", templates.size());

        return ResponseEntity.ok(response);
    }

    /**
     * 创建表单模板
     *
     * @param dto 表单模板数据
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createFormTemplate(@RequestBody FormTemplateDTO dto) {
        FormTemplateDTO created = formTemplateService.createFormTemplate(dto);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", created);
        response.put("message", "表单模板创建成功");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 更新表单模板
     *
     * @param id 模板ID
     * @param dto 表单模板数据
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateFormTemplate(
            @PathVariable Long id,
            @RequestBody FormTemplateDTO dto) {

        FormTemplateDTO updated = formTemplateService.updateFormTemplate(id, dto);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", updated);
        response.put("message", "表单模板更新成功");

        return ResponseEntity.ok(response);
    }

    /**
     * 删除表单模板（逻辑删除）
     *
     * @param id 模板ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteFormTemplate(@PathVariable Long id) {
        formTemplateService.deleteFormTemplate(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "表单模板删除成功");

        return ResponseEntity.ok(response);
    }

    /**
     * 启用/禁用表单模板
     *
     * @param id 模板ID
     * @param status 状态（0-禁用，1-启用）
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateFormTemplateStatus(
            @PathVariable Long id,
            @RequestParam Integer status) {

        formTemplateService.updateFormTemplateStatus(id, status);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "表单模板状态更新成功");

        return ResponseEntity.ok(response);
    }
}
