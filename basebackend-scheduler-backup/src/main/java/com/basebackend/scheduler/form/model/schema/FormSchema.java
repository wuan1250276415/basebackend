package com.basebackend.scheduler.form.model.schema;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 表单Schema定义
 * 
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
public class FormSchema {
    /**
     * Schema版本
     */
    private String version = "1.0.0";
    
    /**
     * Schema类型
     */
    private String type = "form";
    
    /**
     * 显示名称
     */
    private String displayName;
    
    /**
     * 表单名称
     */
    private String name;
    
    /**
     * 表单描述
     */
    private String description;
    
    /**
     * 表单字段列表
     */
    private List<FormField> fields;
    
    /**
     * 表单布局
     */
    private FormLayout layout;
    
    /**
     * 表单样式
     */
    private FormStyle style;
    
    /**
     * 表单行为配置
     */
    private FormBehavior behavior;
    
    /**
     * 数据提交配置
     */
    private SubmissionConfig submission;
    
    /**
     * 验证配置
     */
    private ValidationConfig validation;
    
    /**
     * 事件处理器
     */
    private Map<String, Object> eventHandlers;
    
    /**
     * 自定义属性
     */
    private Map<String, Object> properties;
    
    /**
     * 表单分组
     */
    private List<FormGroup> groups;
    
    /**
     * 国际化配置
     */
    private Map<String, Map<String, String>> i18n;
    
    /**
     * 表单元数据
     */
    private FormMetadata metadata;
    
    @Data
    public static class FormLayout {
        private String type; // vertical, horizontal, inline, two-column
        private int labelWidth = 100;
        private int gutter = 20;
        private Map<String, Object> customConfig;
    }
    
    @Data
    public static class FormStyle {
        private String theme; // default, compact, card
        private Map<String, Object> customCSS;
        private String size; // small, default, large
        private boolean showLabel = true;
        private boolean showRequiredMark = true;
        private boolean showDescription = true;
    }
    
    @Data
    public static class FormBehavior {
        private boolean autoSave = false;
        private int autoSaveInterval = 30000; // 30秒
        private boolean showCancelButton = true;
        private boolean showResetButton = false;
        private boolean showSubmitButton = true;
        private boolean resetAfterSubmit = false;
        private String submitText = "提交";
        private String cancelText = "取消";
        private String resetText = "重置";
        private Map<String, Object> customActions;
    }
    
    @Data
    public static class SubmissionConfig {
        private String method = "POST";
        private String url;
        private Map<String, String> headers;
        private String contentType = "application/json";
        private boolean transformData = false;
        private String dataTransformer;
    }
    
    @Data
    public static class ValidationConfig {
        private boolean validateOnChange = true;
        private boolean validateOnBlur = true;
        private boolean showErrorMessage = true;
        private String errorMessagePosition = "bottom"; // bottom, right, tooltip
    }
    
    @Data
    public static class FormGroup {
        private String name;
        private String label;
        private String description;
        private List<String> fields;
        private boolean collapsible = false;
        private boolean collapsed = false;
        private Map<String, Object> style;
    }
    
    @Data
    public static class FormMetadata {
        private String createdBy;
        private long createdAt;
        private String updatedBy;
        private long updatedAt;
        private String category;
        private List<String> tags;
        private String icon;
        private String version;
        private Map<String, Object> custom;
    }
}
