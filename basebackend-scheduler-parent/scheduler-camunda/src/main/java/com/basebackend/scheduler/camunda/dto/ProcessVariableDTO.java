package com.basebackend.scheduler.camunda.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 流程变量 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessVariableDTO {

    /**
     * 变量名
     */
    private String name;

    /**
     * 变量值
     */
    private Object value;

    /**
     * 变量类型
     */
    private String type;

    /**
     * 值信息（序列化相关）
     */
    private Map<String, Object> valueInfo;

    /**
     * 流程实例 ID
     */
    private String processInstanceId;

    /**
     * 执行 ID
     */
    private String executionId;

    /**
     * 任务 ID
     */
    private String taskId;
}
