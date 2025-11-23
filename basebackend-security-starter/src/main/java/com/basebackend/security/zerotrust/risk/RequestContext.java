package com.basebackend.security.zerotrust.risk;

import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * 请求上下文
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
@Data
public class RequestContext {

    private String userId;
    private String clientIp;
    private String userAgent;
    private String sessionId;
    private String deviceId;
    private String location;
    private Long requestInterval;
    private Map<String, String> headers;
    private Instant timestamp;
    private String requestUri;
    private String httpMethod;
    private String referrer;
}
