package com.basebackend.security.zerotrust.policy;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * 零信任决策
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
@Data
@Builder
public class ZeroTrustDecision {

    private String userId;
    private String resource;
    private boolean allowed;
    private int trustScore;
    private String reason;
    private List<String> requiredActions;
    private Instant timestamp;
}
