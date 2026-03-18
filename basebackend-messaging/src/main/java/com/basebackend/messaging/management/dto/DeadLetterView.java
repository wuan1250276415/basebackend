package com.basebackend.messaging.management.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DeadLetterView {

    private Long id;
    private String messageId;
    private String topic;
    private String routingKey;
    private String tags;
    private String messageType;
    private String payload;
    private String headers;
    private String originalQueue;
    private String status;
    private Integer retryCount;
    private String errorMessage;
    private String originalMessage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime handledTime;
    private Long handledBy;
}
