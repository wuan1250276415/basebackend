package com.basebackend.admin.dto.messaging;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Webhook配置DTO
 */
@Data
public class WebhookConfigDTO {

    private Long id;

    @NotBlank(message = "Webhook名称不能为空")
    private String name;

    @NotBlank(message = "Webhook URL不能为空")
    private String url;

    @NotBlank(message = "事件类型不能为空")
    private String eventTypes;

    private String secret;

    private Boolean signatureEnabled = true;

    private String method = "POST";

    private String headers;

    private Integer timeout = 30;

    private Integer maxRetries = 3;

    private Integer retryInterval = 60;

    private Boolean enabled = true;

    private String remark;
}
