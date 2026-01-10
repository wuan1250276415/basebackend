package com.basebackend.scheduler.camunda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 流程实例修改请求（跳转/回退/取消）
 *
 * @author BaseBackend Team
 * @since 2025-01-01
 */
@Data
@Schema(description = "流程实例修改请求")
public class ProcessInstanceModificationRequest {

    @Schema(description = "取消的活动ID列表")
    private List<String> cancelActivityIds;

    @Schema(description = "在指定活动之前启动（跳转到此前）")
    private List<String> startBeforeActivityIds;

    @Schema(description = "在指定活动之后启动（跳转到此后）")
    private List<String> startAfterActivityIds;

    @Schema(description = "要在启动时设置的变量")
    private Map<String, Object> variables;

    @Schema(description = "是否跳过自定义监听器")
    private boolean skipCustomListeners = false;

    @Schema(description = "修改原因备注")
    private String annotation;
}
