package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 流程变量数据传输对象
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ProcessVariableDTO", description = "流程变量信息")
public class ProcessVariableDTO {

    /**
     * 变量名
     */
    @Schema(description = "变量名", example = "amount")
    private String name;
    /**
     * 变量类型
     */
    @Schema(description = "变量类型", example = "Integer")
    private String type;

    /**
     * 变量值
     */
    @Schema(description = "变量值")
    private Object value;

    /**
     * 流程实例 ID
     */
    @Schema(description = "流程实例 ID", example = "proc_12345")
    private String processInstanceId;

    /**
     * 执行实例 ID
     */
    @Schema(description = "执行实例 ID", example = "exec_12345")
    private String executionId;

    /**
     * 任务 ID
     */
    @Schema(description = "任务 ID", example = "task_12345")
    private String taskId;

    /**
     * 从 Map.Entry 转换
     *
     * @param entry Map 条目
     * @return DTO 对象
     */
    public static ProcessVariableDTO fromEntry(Map.Entry<String, Object> entry) {
        return of(entry.getKey(), entry.getValue());
    }

    /**
     * 创建 DTO
     *
     * @param name 变量名
     * @param value 变量值
     * @return DTO 对象
     */
    public static ProcessVariableDTO of(String name, Object value) {
        String type = value == null ? "null" : value.getClass().getSimpleName();
        return ProcessVariableDTO.builder()
                .name(name)
                .type(type)
                .value(value)
                .build();
    }
}
