package com.basebackend.security.zerotrust.risk;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * 单次访问记录，用于丰富风险分析上下文
 */
@Data
@Builder
public class AccessRecord {

    private Instant timestamp;
    private String ipAddress;
    private String userAgent;
    private int riskScore;
    private RiskLevel riskLevel;
}
