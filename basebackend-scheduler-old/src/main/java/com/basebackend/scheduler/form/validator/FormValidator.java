package com.basebackend.scheduler.form.validator;

import com.basebackend.scheduler.form.model.schema.FormSchema;
import com.basebackend.scheduler.form.model.schema.FormField;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 表单验证器
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FormValidator {
    
    public ValidationResult validate(FormSchema schema, Map<String, Object> data) {
        ValidationResult result = new ValidationResult();
        
        for (FormField field : schema.getFields()) {
            validateField(field, data, result);
        }
        
        return result;
    }
    
    private void validateField(FormField field, Map<String, Object> data, ValidationResult result) {
        String fieldName = field.getName();
        Object value = data.get(fieldName);
        
        if (field.isRequired() && (value == null || 
            (value instanceof String && ((String) value).trim().isEmpty()))) {
            result.addError(fieldName, "required", "Field is required");
            return;
        }
        
        if (value == null || (value instanceof String && ((String) value).trim().isEmpty())) {
            return;
        }
        
        validateType(field, value, result);
        validateRules(field, value, result);
    }
    
    private void validateType(FormField field, Object value, ValidationResult result) {
        String fieldName = field.getName();
        String type = field.getType();
        
        switch (type) {
            case "number":
                if (!(value instanceof Number)) {
                    result.addError(fieldName, "type", "Must be a number");
                }
                break;
            case "email":
                if (!(value instanceof String) || !value.toString().contains("@")) {
                    result.addError(fieldName, "type", "Invalid email format");
                }
                break;
        }
    }
    
    private void validateRules(FormField field, Object value, ValidationResult result) {
        if (field.getValidations() != null) {
            for (FormField.ValidationRule rule : field.getValidations()) {
                String ruleType = rule.getType();
                Object ruleValue = rule.getValue();
                
                boolean isValid = true;
                
                switch (ruleType) {
                    case "minLength":
                        isValid = value.toString().length() >= ((Number) ruleValue).intValue();
                        break;
                    case "maxLength":
                        isValid = value.toString().length() <= ((Number) ruleValue).intValue();
                        break;
                }
                
                if (!isValid) {
                    result.addError(field.getName(), ruleType, rule.getMessage());
                }
            }
        }
    }
    
    public static class ValidationResult {
        private boolean valid = true;
        private Map<String, List<ValidationError>> errors = new HashMap<>();
        
        public boolean isValid() {
            return valid;
        }
        
        public void setValid(boolean valid) {
            this.valid = valid;
        }
        
        public void addError(String field, String code, String message) {
            valid = false;
            errors.computeIfAbsent(field, k -> new ArrayList<>())
                  .add(new ValidationError(field, code, message));
        }
        
        public Map<String, List<ValidationError>> getErrors() {
            return errors;
        }
    }
    
    public static class ValidationError {
        private String field;
        private String code;
        private String message;
        
        public ValidationError(String field, String code, String message) {
            this.field = field;
            this.code = code;
            this.message = message;
        }
        
        public String getField() { return field; }
        public String getCode() { return code; }
        public String getMessage() { return message; }
    }
}
