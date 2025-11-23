package com.basebackend.security.zerotrust.risk;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 行为分析结果
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
@Data
@Builder(builderClassName = "Builder")
public class BehaviorAnalysisResult {

    private String userId;
    private boolean isAnomalous;
    private RequestContext requestContext;
    private List<String> anomalies;
    private double anomalyScore;
    private List<String> recommendations;

    // 行为侧细分指标
    private boolean accessTimeAnomalous;
    private String anomalyReason;
    private int accessTimeScore;
    private int frequencyScore;
    private String geographicLocation;
    private boolean deviceConsistency;
    private String deviceId;
    private String userAgent;
    private String accessPattern;
    private String sessionBehavior;
}
