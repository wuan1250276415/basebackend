package com.basebackend.admin.dto.messaging;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * 事件发布DTO
 */
public record EventPublishDTO(
    @NotBlank(message = "事件类型不能为空") String eventType,
    @NotNull(message = "事件数据不能为空") Object data,
    String source,
    Map<String, Object> metadata
) {}
