package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 变量设置/更新请求
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@Schema(name = "VariableUpsertRequest", description = "变量设置/更新请求参数")
public class VariableUpsertRequest {

    /**
     * 变量名
     */
    @Schema(
        description = "变量名",
        example = "approved",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "变量名不能为空")
    private String key;

    /**
     * 变量值
     */
    @Schema(
        description = "变量值",
        example = "true"
    )
    private Object value;

    /**
     * 是否设置为本地变量
     */
    @Schema(
        description = "是否设置为本地变量，默认 false",
        defaultValue = "false"
    )
    private Boolean local = Boolean.FALSE;

    // 手动添加 getter/setter 方法以确保编译成功
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Boolean getLocal() {
        return local;
    }

    public void setLocal(Boolean local) {
        this.local = local;
    }

    public boolean isLocal() {
        return local != null ? local : false;
    }

    // ========== 兼容性方法（用于测试兼容）==========

    /**
     * 设置变量名（兼容性方法）
     * @param variableName 变量名
     */
    public void setVariableName(String variableName) {
        this.key = variableName;
    }

    /**
     * 获取变量名（兼容性方法）
     * @return 变量名
     */
    public String getVariableName() {
        return this.key;
    }

    /**
     * 设置变量值（兼容性方法）
     * @param variableValue 变量值
     */
    public void setVariableValue(Object variableValue) {
        this.value = variableValue;
    }

    /**
     * 获取变量值（兼容性方法）
     * @return 变量值
     */
    public Object getVariableValue() {
        return this.value;
    }
}
