package com.basebackend.scheduler.form.engine;

import com.basebackend.scheduler.form.model.schema.FormSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 表单渲染器
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FormRenderer {
    
    public String render(FormSchema schema, Map<String, Object> data) {
        log.debug("Rendering form: {}", schema.getName());
        
        StringBuilder html = new StringBuilder();
        html.append("<form id='dynamic-form' class='dynamic-form'>");
        
        for (com.basebackend.scheduler.form.model.schema.FormField field : schema.getFields()) {
            html.append(renderField(field, data));
        }
        
        html.append("</form>");
        
        return html.toString();
    }
    
    private String renderField(com.basebackend.scheduler.form.model.schema.FormField field, Map<String, Object> data) {
        StringBuilder html = new StringBuilder();
        
        Object value = data.get(field.getName());
        
        html.append("<div class='form-field' style='width: ").append(field.getWidth()).append("%'>");
        
        if (field.getLabel() != null) {
            html.append("<label>").append(field.getLabel());
            if (field.isRequired()) {
                html.append("<span class='required'>*</span>");
            }
            html.append("</label>");
        }
        
        html.append("<div class='field-wrapper'>");
        
        switch (field.getType()) {
            case "text":
            case "email":
            case "password":
            case "url":
                html.append("<input type='")
                    .append(field.getType())
                    .append("' name='")
                    .append(field.getName())
                    .append("' value='")
                    .append(value != null ? value : "")
                    .append("'");
                if (field.getPlaceholder() != null) {
                    html.append(" placeholder='").append(field.getPlaceholder()).append("'");
                }
                if (field.isRequired()) {
                    html.append(" required");
                }
                if (field.isReadOnly()) {
                    html.append(" readonly");
                }
                html.append(" />");
                break;
                
            case "textarea":
                html.append("<textarea name='")
                    .append(field.getName())
                    .append("'");
                if (field.getPlaceholder() != null) {
                    html.append(" placeholder='").append(field.getPlaceholder()).append("'");
                }
                if (field.isRequired()) {
                    html.append(" required");
                }
                html.append(">")
                    .append(value != null ? value : "")
                    .append("</textarea>");
                break;
                
            case "number":
                html.append("<input type='number' name='")
                    .append(field.getName())
                    .append("' value='")
                    .append(value != null ? value : "")
                    .append("'");
                if (field.getPlaceholder() != null) {
                    html.append(" placeholder='").append(field.getPlaceholder()).append("'");
                }
                if (field.isRequired()) {
                    html.append(" required");
                }
                html.append(" />");
                break;
                
            case "select":
                html.append("<select name='")
                    .append(field.getName())
                    .append("'");
                if (field.isRequired()) {
                    html.append(" required");
                }
                html.append(">");
                
                if (field.getOptions() != null) {
                    for (com.basebackend.scheduler.form.model.schema.FormField.Option option : field.getOptions()) {
                        html.append("<option value='")
                            .append(option.getValue())
                            .append("'")
                            .append(option.getValue().equals(value) ? " selected" : "")
                            .append(">")
                            .append(option.getLabel())
                            .append("</option>");
                    }
                }
                
                html.append("</select>");
                break;
                
            case "checkbox":
                boolean checked = value instanceof Boolean ? (Boolean) value : false;
                html.append("<input type='checkbox' name='")
                    .append(field.getName())
                    .append("'")
                    .append(checked ? " checked" : "");
                if (field.isRequired()) {
                    html.append(" required");
                }
                html.append(" />");
                break;
                
            case "radio":
                if (field.getOptions() != null) {
                    for (com.basebackend.scheduler.form.model.schema.FormField.Option option : field.getOptions()) {
                        html.append("<label class='radio-label'>");
                        html.append("<input type='radio' name='")
                            .append(field.getName())
                            .append("' value='")
                            .append(option.getValue())
                            .append("'")
                            .append(option.getValue().equals(value) ? " checked" : "");
                        if (field.isRequired()) {
                            html.append(" required");
                        }
                        html.append(" />")
                            .append(option.getLabel())
                            .append("</label>");
                    }
                }
                break;
        }
        
        if (field.getHelpText() != null) {
            html.append("<small class='help-text'>").append(field.getHelpText()).append("</small>");
        }
        
        html.append("</div>"); // field-wrapper
        html.append("</div>"); // form-field
        
        return html.toString();
    }
}
