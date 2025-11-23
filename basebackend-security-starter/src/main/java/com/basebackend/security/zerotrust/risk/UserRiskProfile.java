package com.basebackend.security.zerotrust.risk;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * 用户风险档案
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
@Data
@Builder
public class UserRiskProfile {

    private String userId;
    private int riskScore;
    private RiskLevel riskLevel;
    private Instant lastLoginTime;
    private String lastLoginIp;

    private Set<String> trustedDevices;
    private List<String> trustedLocations;
    private List<AccessRecord> accessHistory;
    private List<RiskEvent> riskEvents;

    private int totalAccessCount;
    private String lastKnownIp;
    private String lastKnownLocation;
    private Instant createdAt;
    private Instant lastUpdated;

    private int loginAttemptCount;
    private Instant lastActivityTime;
    private RiskSeverity lastRiskSeverity;
    private String lastRiskEvent;
}
