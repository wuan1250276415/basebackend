package com.basebackend.backup.infrastructure.executor;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 增量链验证结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChainValidationResult {

    /**
     * 验证是否通过
     */
    private boolean valid;

    /**
     * 错误列表
     */
    private List<String> errors;

    /**
     * 警告列表
     */
    private List<String> warnings;

    /**
     * 增量链对象
     */
    private IncrementalChain chain;

    /**
     * 是否有错误
     */
    private boolean hasErrors;

    /**
     * 是否有警告
     */
    private boolean hasWarnings;

    /**
     * 添加错误
     */
    public void addError(String error) {
        if (errors == null) {
            errors = new ArrayList<>();
        }
        errors.add(error);
        hasErrors = true;
    }

    /**
     * 添加警告
     */
    public void addWarning(String warning) {
        if (warnings == null) {
            warnings = new ArrayList<>();
        }
        warnings.add(warning);
        hasWarnings = true;
    }

    /**
     * 获取错误详情
     */
    public String getErrorDetails() {
        if (errors == null || errors.isEmpty()) {
            return "无错误";
        }
        return String.join("\n", errors);
    }

    /**
     * 获取警告详情
     */
    public String getWarningDetails() {
        if (warnings == null || warnings.isEmpty()) {
            return "无警告";
        }
        return String.join("\n", warnings);
    }
}
