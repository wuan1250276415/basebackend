package com.basebackend.security.zerotrust.risk;

import lombok.Builder;
import lombok.Data;

/**
 * 风险分数
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
@Data
@Builder
public class RiskScore {

    private int totalScore;
    private int behaviorScore;
    private int networkScore;
    private int deviceScore;
    private int locationScore;
    private int timeScore;
    private RiskLevel riskLevel;
}
