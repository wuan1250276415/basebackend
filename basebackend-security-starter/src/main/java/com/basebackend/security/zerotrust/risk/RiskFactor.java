package com.basebackend.security.zerotrust.risk;

import lombok.Builder;
import lombok.Data;

/**
 * 风险因子
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
@Data
@Builder
public class RiskFactor {

    private RiskCategory category;
    private RiskType type;
    private String description;
    private int score;
    private RiskSeverity severity;
    private Object value;
}
