package com.basebackend.security.zerotrust.policy;

import lombok.Data;

/**
 * 信任策略
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
@Data
public class TrustPolicy {

    private String id;
    private String name;
    private int minTrustScore;
    private boolean requiresMfa;
    private boolean requiresDeviceVerification;
    private long maxSessionDuration;
}
