package com.basebackend.admin.dto.messaging;

import jakarta.validation.constraints.NotBlank;

/**
 * Webhook配置DTO
 */
public record WebhookConfigDTO(
    Long id,
    @NotBlank(message = "Webhook名称不能为空")
    String name,
    @NotBlank(message = "Webhook URL不能为空")
    String url,
    @NotBlank(message = "事件类型不能为空")
    String eventTypes,
    String secret,
    Boolean signatureEnabled,
    String method,
    String headers,
    Integer timeout,
    Integer maxRetries,
    Integer retryInterval,
    Boolean enabled,
    String remark
) {
    public WebhookConfigDTO {
        if (signatureEnabled == null) signatureEnabled = true;
        if (method == null) method = "POST";
        if (timeout == null) timeout = 30;
        if (maxRetries == null) maxRetries = 3;
        if (retryInterval == null) retryInterval = 60;
        if (enabled == null) enabled = true;
    }
}
