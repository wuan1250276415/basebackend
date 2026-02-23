package com.basebackend.messaging.webhook;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookEndpoint {

    private Long id;
    private String name;
    private String url;
    private String eventTypes;
    private String secret;
    private Boolean signatureEnabled;
    private String method;
    private String headers;
    private Integer timeout;
    private Integer maxRetries;
    private Integer retryInterval;
    private Boolean enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long createBy;
    private String remark;
}
