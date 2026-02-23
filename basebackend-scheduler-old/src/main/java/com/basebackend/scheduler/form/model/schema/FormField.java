package com.basebackend.scheduler.form.model.schema;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 表单字段定义
 * 
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
public class FormField {
    /**
     * 字段类型
     */
    private String type;
    
    /**
     * 字段名称
     */
    private String name;
    
    /**
     * 显示标签
     */
    private String label;
    
    /**
     * 占位符文本
     */
    private String placeholder;
    
    /**
     * 默认值
     */
    private Object defaultValue;
    
    /**
     * 是否必填
     */
    private boolean required;
    
    /**
     * 是否只读
     */
    private boolean readOnly;
    
    /**
     * 是否隐藏
     */
    private boolean hidden;
    
    /**
     * 字段宽度 (1-24)
     */
    private int width = 12;
    
    /**
     * 验证规则
     */
    private List<ValidationRule> validations;
    
    /**
     * 选项配置 (用于select, radio, checkbox等)
     */
    private List<Option> options;
    
    /**
     * 样式配置
     */
    private Map<String, Object> style;
    
    /**
     * 事件配置
     */
    private Map<String, Object> events;
    
    /**
     * 条件显示配置
     */
    private ConditionalDisplay conditionalDisplay;
    
    /**
     * 字段描述
     */
    private String description;
    
    /**
     * 帮助文本
     */
    private String helpText;
    
    /**
     * 字段图标
     */
    private String icon;
    
    /**
     * 子字段 (用于fieldset, checkbox-group等)
     */
    private List<FormField> fields;
    
    /**
     * 数据源配置
     */
    private DataSource dataSource;
    
    /**
     * 字段分组
     */
    private String group;
    
    /**
     * 字段排序
     */
    private int order;
    
    /**
     * 是否支持批量编辑
     */
    private boolean bulkEditable = true;
    
    @Data
    public static class ValidationRule {
        private String type;
        private Object value;
        private String message;
    }
    
    @Data
    public static class Option {
        private String label;
        private String value;
        private boolean disabled;
        private Map<String, Object> style;
    }
    
    @Data
    public static class ConditionalDisplay {
        private String field;
        private String operator; // equals, notEquals, contains, greaterThan, etc.
        private Object value;
    }
    
    @Data
    public static class DataSource {
        private String type; // static, api, function
        private String url;
        private String method;
        private Map<String, Object> headers;
        private String labelField;
        private String valueField;
        private List<Option> staticOptions;
    }
}
