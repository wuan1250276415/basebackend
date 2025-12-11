package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.Map;

/**
 * 流程定义启动请求
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@Schema(name = "ProcessDefinitionStartRequest", description = "流程实例启动请求")
public class ProcessDefinitionStartRequest {

    /**
     * 流程定义 Key
     */
    @Schema(
        description = "流程定义 Key",
        example = "order_approval"
    )
    private String processDefinitionKey;

    /**
     * 流程定义 ID
     */
    @Schema(
        description = "流程定义 ID",
        example = "order_approval:1:12345"
    )
    private String processDefinitionId;

    /**
     * 业务关联键
     */
    @Schema(
        description = "业务关联键，用于关联业务系统数据",
        example = "ORDER_20250101_001"
    )
    private String businessKey;

    /**
     * 租户 ID（仅在通过 Key 启动时使用）
     */
    @Schema(
        description = "租户 ID（仅在通过 Key 启动时使用）",
        example = "tenant_001"
    )
    private String tenantId;

    /**
     * 流程变量
     */
    @Schema(
        description = "流程变量，传递给流程实例的输入参数",
        example = "{\"amount\": 1000, \"customerId\": \"C001\"}"
    )
    private Map<String, Object> variables = Collections.emptyMap();

    /**
     * 验证至少提供了一个标识符
     */
    @AssertTrue(message = "必须提供流程定义 Key 或 ID")
    public boolean hasDefinitionIdentifier() {
        return StringUtils.isNotBlank(processDefinitionKey) || StringUtils.isNotBlank(processDefinitionId);
    }

    // ========== 兼容性方法（用于测试兼容）==========

    /**
     * 设置流程定义Key（兼容性方法）
     * @param definitionKey 流程定义Key
     */
    public void setDefinitionKey(String definitionKey) {
        this.processDefinitionKey = definitionKey;
    }

    /**
     * 获取流程定义Key（兼容性方法）
     * @return 流程定义Key
     */
    public String getDefinitionKey() {
        return this.processDefinitionKey;
    }
}
