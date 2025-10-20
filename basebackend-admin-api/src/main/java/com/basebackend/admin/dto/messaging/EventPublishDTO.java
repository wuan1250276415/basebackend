package com.basebackend.admin.dto.messaging;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * 事件发布DTO
 */
@Data
public class EventPublishDTO {

    @NotBlank(message = "事件类型不能为空")
    private String eventType;

    @NotNull(message = "事件数据不能为空")
    private Object data;

    private String source;

    private Map<String, Object> metadata;
}
