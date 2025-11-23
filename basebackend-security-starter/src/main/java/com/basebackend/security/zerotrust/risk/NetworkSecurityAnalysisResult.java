package com.basebackend.security.zerotrust.risk;

import lombok.Builder;
import lombok.Data;

/**
 * 网络安全分析结果
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
@Data
@Builder(builderClassName = "Builder")
public class NetworkSecurityAnalysisResult {

    private String userId;
    private RequestContext requestContext;

    private boolean isSecure;
    private String ipAddress;
    private String country;
    private String city;
    private boolean isKnownThreat;
    private int threatScore;

    // 网络侧细分指标
    private String ipReputation;
    private boolean isVpnOrProxy;
    private boolean isTorNetwork;
    private boolean isDataCenterIp;
    private String networkSpeed;
    private String networkQuality;
}
