package com.basebackend.messaging.management.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WebhookUpsertRequest(
        @NotBlank(message = "Webhook名称不能为空")
        String name,
        @NotBlank(message = "Webhook地址不能为空")
        String url,
        @NotBlank(message = "事件类型不能为空")
        String eventTypes,
        String secret,
        @NotNull(message = "签名开关不能为空")
        Boolean signatureEnabled,
        @NotBlank(message = "HTTP方法不能为空")
        String method,
        String headers,
        @Min(value = 1, message = "超时时间不能小于1秒")
        @Max(value = 300, message = "超时时间不能大于300秒")
        Integer timeout,
        @Min(value = 0, message = "最大重试次数不能小于0")
        @Max(value = 10, message = "最大重试次数不能大于10")
        Integer maxRetries,
        @Min(value = 1, message = "重试间隔不能小于1秒")
        @Max(value = 3600, message = "重试间隔不能大于3600秒")
        Integer retryInterval,
        @NotNull(message = "启用状态不能为空")
        Boolean enabled,
        String remark
) {
}
