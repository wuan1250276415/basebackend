package com.basebackend.scheduler.form.engine;

import com.basebackend.scheduler.form.model.data.FormData;
import com.basebackend.scheduler.form.model.schema.FormSchema;
import com.basebackend.scheduler.form.validator.FormValidator;
import com.basebackend.scheduler.form.storage.FormStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 表单引擎
 * 
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FormEngine {
    
    private final FormStorageService storageService;
    private final FormValidator validator;
    private final FormRenderer renderer;
    
    /**
     * 渲染表单
     */
    public String renderForm(String templateId, Map<String, Object> data) {
        log.debug("Rendering form template: {}", templateId);
        
        Optional<FormSchema> schemaOpt = storageService.getTemplate(templateId);
        if (schemaOpt.isEmpty()) {
            throw new RuntimeException("Form template not found: " + templateId);
        }
        
        FormSchema schema = schemaOpt.get();
        return renderer.render(schema, data);
    }
    
    /**
     * 渲染表单JSON
     */
    public FormSchema renderFormSchema(String templateId) {
        Optional<FormSchema> schemaOpt = storageService.getTemplate(templateId);
        if (schemaOpt.isEmpty()) {
            throw new RuntimeException("Form template not found: " + templateId);
        }
        return schemaOpt.get();
    }
    
    /**
     * 提交表单数据
     */
    public FormData submitForm(String templateId, Map<String, Object> data, String submittedBy) {
        log.info("Submitting form data for template: {}", templateId);
        
        Optional<FormSchema> schemaOpt = storageService.getTemplate(templateId);
        if (schemaOpt.isEmpty()) {
            throw new RuntimeException("Form template not found: " + templateId);
        }
        
        FormSchema schema = schemaOpt.get();
        
        // 验证数据
        FormValidator.ValidationResult validationResult = validator.validate(schema, data);
        if (!validationResult.isValid()) {
            throw new ValidationException("Form validation failed", validationResult);
        }
        
        // 创建表单数据
        FormData formData = new FormData();
        formData.setId(UUID.randomUUID().toString());
        formData.setTemplateId(templateId);
        formData.setData(new HashMap<>(data));
        formData.setSubmittedBy(submittedBy);
        formData.setSubmittedAt(Instant.now());
        formData.setLastModifiedBy(submittedBy);
        formData.setLastModifiedAt(Instant.now());
        formData.setStatus(FormData.DataStatus.SUBMITTED);
        
        // 保存数据
        storageService.saveData(formData.getId(), formData);
        
        log.info("Form data submitted successfully with ID: {}", formData.getId());
        return formData;
    }
    
    /**
     * 获取表单数据
     */
    public Optional<FormData> getFormData(String dataId) {
        return storageService.getData(dataId);
    }
    
    /**
     * 获取表单数据列表
     */
    public List<FormData> getFormDataList(String templateId) {
        return storageService.getDataList(templateId);
    }
    
    /**
     * 更新表单数据
     */
    public void updateFormData(String dataId, Map<String, Object> data, String updatedBy) {
        log.info("Updating form data: {}", dataId);
        
        Optional<FormData> formDataOpt = storageService.getData(dataId);
        if (formDataOpt.isEmpty()) {
            throw new RuntimeException("Form data not found: " + dataId);
        }
        
        FormData formData = formDataOpt.get();
        
        // 验证数据
        Optional<FormSchema> schemaOpt = storageService.getTemplate(formData.getTemplateId());
        if (schemaOpt.isEmpty()) {
            throw new RuntimeException("Form template not found: " + formData.getTemplateId());
        }
        
        FormValidator.ValidationResult validationResult = validator.validate(schemaOpt.get(), data);
        if (!validationResult.isValid()) {
            throw new ValidationException("Form validation failed", validationResult);
        }
        
        // 更新数据
        formData.setData(new HashMap<>(data));
        formData.setLastModifiedBy(updatedBy);
        formData.setLastModifiedAt(Instant.now());
        
        storageService.saveData(dataId, formData);
        
        log.info("Form data updated successfully: {}", dataId);
    }
    
    /**
     * 导入表单数据
     */
    public List<String> importFormData(String templateId, List<Map<String, Object>> dataList, String importedBy) {
        log.info("Importing {} records for template: {}", dataList.size(), templateId);
        
        Optional<FormSchema> schemaOpt = storageService.getTemplate(templateId);
        if (schemaOpt.isEmpty()) {
            throw new RuntimeException("Form template not found: " + templateId);
        }
        
        FormSchema schema = schemaOpt.get();
        List<String> importedIds = new java.util.ArrayList<>();
        
        for (Map<String, Object> data : dataList) {
            try {
                // 验证数据
                FormValidator.ValidationResult validationResult = validator.validate(schema, data);
                if (!validationResult.isValid()) {
                    log.warn("Skipping invalid data: {}", validationResult.getErrors());
                    continue;
                }
                
                // 创建表单数据
                FormData formData = new FormData();
                formData.setId(UUID.randomUUID().toString());
                formData.setTemplateId(templateId);
                formData.setData(new HashMap<>(data));
                formData.setSubmittedBy(importedBy);
                formData.setSubmittedAt(Instant.now());
                formData.setLastModifiedBy(importedBy);
                formData.setLastModifiedAt(Instant.now());
                
                // 保存数据
                storageService.saveData(formData.getId(), formData);
                importedIds.add(formData.getId());
                
            } catch (Exception e) {
                log.error("Failed to import data: {}", e.getMessage(), e);
            }
        }
        
        log.info("Imported {} records successfully", importedIds.size());
        return importedIds;
    }
    
    /**
     * 验证异常
     */
    public static class ValidationException extends RuntimeException {
        private final FormValidator.ValidationResult validationResult;
        
        public ValidationException(String message, FormValidator.ValidationResult validationResult) {
            super(message);
            this.validationResult = validationResult;
        }
        
        public FormValidator.ValidationResult getValidationResult() {
            return validationResult;
        }
    }
}
