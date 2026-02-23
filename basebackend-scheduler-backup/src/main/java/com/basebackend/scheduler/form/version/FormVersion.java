package com.basebackend.scheduler.form.version;

import com.basebackend.scheduler.form.model.schema.FormSchema;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 表单版本
 */
@Data
@Builder
public class FormVersion {
    private String id;
    private String templateId;
    private String version;
    private FormSchema schema;
    private String comment;
    private String createdBy;
    private long createdAt;
    private boolean isActive;
    private List<String> tags;
    private Map<String, Object> metadata;
}
