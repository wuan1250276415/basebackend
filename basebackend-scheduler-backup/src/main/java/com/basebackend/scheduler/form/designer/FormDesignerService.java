package com.basebackend.scheduler.form.designer;

import com.basebackend.scheduler.form.model.schema.FormSchema;
import com.basebackend.scheduler.form.version.FormVersionService;
import com.basebackend.scheduler.form.storage.FormStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 表单设计器服务
 * 
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FormDesignerService {
    
    private final FormStorageService storageService;
    private final FormVersionService versionService;
    
    /**
     * 创建新表单
     */
    public String createForm(FormSchema schema, String createdBy) {
        log.info("Creating new form template: {}", schema.getName());
        
        // 验证Schema
        validateSchema(schema);
        
        // 生成ID
        String templateId = UUID.randomUUID().toString();
        
        // 设置元数据
        schema.getMetadata().setCreatedBy(createdBy);
        schema.getMetadata().setCreatedAt(Instant.now().toEpochMilli());
        schema.getMetadata().setUpdatedBy(createdBy);
        schema.getMetadata().setUpdatedAt(Instant.now().toEpochMilli());
        schema.getMetadata().setVersion("1.0.0");
        
        // 保存到存储
        storageService.saveTemplate(templateId, schema);
        
        // 创建初始版本
        versionService.createVersion(templateId, schema, "1.0.0", "Initial version", createdBy);
        
        log.info("Form template created successfully with ID: {}", templateId);
        return templateId;
    }
    
    /**
     * 更新表单
     */
    public void updateForm(String templateId, FormSchema schema, String updatedBy) {
        log.info("Updating form template: {}", templateId);
        
        // 获取当前版本
        Optional<FormSchema> currentOpt = storageService.getTemplate(templateId);
        if (currentOpt.isEmpty()) {
            throw new RuntimeException("Form template not found: " + templateId);
        }
        
        FormSchema current = currentOpt.get();
        
        // 验证Schema
        validateSchema(schema);
        
        // 更新元数据
        schema.getMetadata().setUpdatedBy(updatedBy);
        schema.getMetadata().setUpdatedAt(Instant.now().toEpochMilli());
        
        // 保存到存储
        storageService.saveTemplate(templateId, schema);
        
        log.info("Form template updated successfully: {}", templateId);
    }
    
    /**
     * 获取表单
     */
    public Optional<FormSchema> getForm(String templateId) {
        return storageService.getTemplate(templateId);
    }
    
    /**
     * 获取表单列表
     */
    public List<FormSchema> getForms() {
        return storageService.getAllTemplates();
    }
    
    /**
     * 删除表单
     */
    public void deleteForm(String templateId, String deletedBy) {
        log.info("Deleting form template: {}", templateId);
        storageService.deleteTemplate(templateId, deletedBy);
    }
    
    /**
     * 复制表单
     */
    public String duplicateForm(String templateId, String newName, String createdBy) {
        log.info("Duplicating form template: {}", templateId);
        
        Optional<FormSchema> originalOpt = storageService.getTemplate(templateId);
        if (originalOpt.isEmpty()) {
            throw new RuntimeException("Form template not found: " + templateId);
        }
        
        FormSchema original = originalOpt.get();
        
        // 创建副本
        FormSchema copy = deepCopy(original);
        copy.setName(newName);
        copy.setDisplayName(newName + " (Copy)");
        
        // 重置元数据
        copy.getMetadata().setCreatedBy(createdBy);
        copy.getMetadata().setCreatedAt(Instant.now().toEpochMilli());
        copy.getMetadata().setUpdatedBy(createdBy);
        copy.getMetadata().setUpdatedAt(Instant.now().toEpochMilli());
        
        return createForm(copy, createdBy);
    }
    
    /**
     * 验证Schema
     */
    private void validateSchema(FormSchema schema) {
        if (schema.getName() == null || schema.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Form name is required");
        }
        
        if (schema.getDisplayName() == null || schema.getDisplayName().trim().isEmpty()) {
            throw new IllegalArgumentException("Form display name is required");
        }
        
        if (schema.getFields() == null || schema.getFields().isEmpty()) {
            throw new IllegalArgumentException("Form must have at least one field");
        }
        
        // 验证字段名称唯一性
        long uniqueFieldNames = schema.getFields().stream()
                .map(field -> field.getName())
                .distinct()
                .count();
        
        if (uniqueFieldNames != schema.getFields().size()) {
            throw new IllegalArgumentException("Field names must be unique");
        }
    }
    
    /**
     * 深度复制Schema
     */
    private FormSchema deepCopy(FormSchema original) {
        // 简单实现，实际应使用序列化工具
        String json = convertToJson(original);
        return convertFromJson(json, FormSchema.class);
    }
    
    /**
     * 转换为JSON (示例实现)
     */
    private String convertToJson(FormSchema schema) {
        // 实际实现应使用Jackson等库
        return "";
    }
    
    /**
     * 从JSON转换 (示例实现)
     */
    private <T> T convertFromJson(String json, Class<T> clazz) {
        // 实际实现应使用Jackson等库
        return null;
    }
}
