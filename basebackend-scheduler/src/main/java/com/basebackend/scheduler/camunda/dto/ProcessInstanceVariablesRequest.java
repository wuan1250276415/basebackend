package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 流程实例变量设置请求
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@Schema(name = "ProcessInstanceVariablesRequest", description = "流程实例变量批量设置请求")
public class ProcessInstanceVariablesRequest {

    /**
     * 变量集合，key 为变量名，value 为变量值
     */
    @Schema(
        description = "变量集合，key 为变量名，value 为变量值",
        example = "{\"amount\": 1000, \"approved\": true, \"customerId\": \"C001\"}"
    )
    private Map<String, Object> variables = new HashMap<>();

    /**
     * 是否设置为本地变量
     */
    @Schema(
        description = "是否设置为本地变量，默认 false",
        defaultValue = "false"
    )
    private Boolean local = Boolean.FALSE;
}
