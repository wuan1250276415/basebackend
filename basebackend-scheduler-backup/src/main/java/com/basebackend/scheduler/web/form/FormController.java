package com.basebackend.scheduler.web.form;

import com.basebackend.scheduler.form.designer.FormDesignerService;
import com.basebackend.scheduler.form.engine.FormEngine;
import com.basebackend.scheduler.form.model.data.FormData;
import com.basebackend.scheduler.form.model.schema.FormSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 表单Web控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/form")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FormController {
    
    private final FormDesignerService designerService;
    private final FormEngine formEngine;
    
    @PostMapping("/templates")
    public ResponseEntity<String> createTemplate(@RequestBody FormSchema schema, 
                                                  @RequestParam String createdBy) {
        String templateId = designerService.createForm(schema, createdBy);
        return ResponseEntity.ok(templateId);
    }
    
    @PutMapping("/templates/{templateId}")
    public ResponseEntity<Void> updateTemplate(@PathVariable String templateId,
                                                @RequestBody FormSchema schema,
                                                @RequestParam String updatedBy) {
        designerService.updateForm(templateId, schema, updatedBy);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/templates/{templateId}")
    public ResponseEntity<FormSchema> getTemplate(@PathVariable String templateId) {
        return designerService.getForm(templateId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/templates")
    public ResponseEntity<List<FormSchema>> getTemplates() {
        List<FormSchema> templates = designerService.getForms();
        return ResponseEntity.ok(templates);
    }
    
    @DeleteMapping("/templates/{templateId}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable String templateId,
                                               @RequestParam String deletedBy) {
        designerService.deleteForm(templateId, deletedBy);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/templates/{templateId}/duplicate")
    public ResponseEntity<String> duplicateTemplate(@PathVariable String templateId,
                                                    @RequestParam String newName,
                                                    @RequestParam String createdBy) {
        String newTemplateId = designerService.duplicateForm(templateId, newName, createdBy);
        return ResponseEntity.ok(newTemplateId);
    }
    
    @GetMapping("/templates/{templateId}/render")
    public ResponseEntity<String> renderForm(@PathVariable String templateId,
                                             @RequestParam(required = false) Map<String, Object> data) {
        String html = formEngine.renderForm(templateId, data != null ? data : Map.of());
        return ResponseEntity.ok(html);
    }
    
    @GetMapping("/templates/{templateId}/schema")
    public ResponseEntity<FormSchema> getFormSchema(@PathVariable String templateId) {
        FormSchema schema = formEngine.renderFormSchema(templateId);
        return ResponseEntity.ok(schema);
    }
    
    @PostMapping("/data")
    public ResponseEntity<FormData> submitForm(@RequestParam String templateId,
                                               @RequestBody Map<String, Object> data,
                                               @RequestParam String submittedBy) {
        FormData formData = formEngine.submitForm(templateId, data, submittedBy);
        return ResponseEntity.ok(formData);
    }
    
    @GetMapping("/data/{dataId}")
    public ResponseEntity<FormData> getFormData(@PathVariable String dataId) {
        return formEngine.getFormData(dataId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/data/template/{templateId}")
    public ResponseEntity<List<FormData>> getFormDataList(@PathVariable String templateId) {
        List<FormData> dataList = formEngine.getFormDataList(templateId);
        return ResponseEntity.ok(dataList);
    }
    
    @PutMapping("/data/{dataId}")
    public ResponseEntity<Void> updateFormData(@PathVariable String dataId,
                                               @RequestBody Map<String, Object> data,
                                               @RequestParam String updatedBy) {
        formEngine.updateFormData(dataId, data, updatedBy);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/data/import")
    public ResponseEntity<List<String>> importFormData(@RequestParam String templateId,
                                                       @RequestBody List<Map<String, Object>> dataList,
                                                       @RequestParam String importedBy) {
        List<String> importedIds = formEngine.importFormData(templateId, dataList, importedBy);
        return ResponseEntity.ok(importedIds);
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Form service is running");
    }
}
