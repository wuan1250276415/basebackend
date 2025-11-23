package com.basebackend.security.zerotrust.risk;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 风险事件
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
@Data
@Builder
public class RiskEvent {

    private String eventId;
    private String userId;
    private String eventType;
    private RiskSeverity severity;
    private String description;
    private Instant timestamp;
    private RequestContext requestContext;
    private List<RiskFactor> riskFactors;
    private RiskLevel riskLevel;
    private int riskScore;
    private Map<String, Object> metadata;
}
