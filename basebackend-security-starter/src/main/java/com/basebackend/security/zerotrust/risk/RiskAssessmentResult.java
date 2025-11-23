package com.basebackend.security.zerotrust.risk;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * 风险评估结果
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
@Data
@Builder
public class RiskAssessmentResult {

    private String userId;
    private RequestContext requestContext;
    private RiskScore riskScore;
    private RiskLevel riskLevel;
    private List<RiskFactor> riskFactors;
    private boolean isHighRisk;
    private boolean requiresAdditionalVerification;
    private List<String> recommendedActions;
    private Instant timestamp;
}
