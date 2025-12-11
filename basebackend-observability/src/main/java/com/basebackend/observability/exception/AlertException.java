package com.basebackend.observability.exception;

/**
 * 告警相关异常
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public class AlertException extends ObservabilityException {

    private final Long ruleId;
    private final String ruleName;

    public AlertException(String message) {
        super(message, ErrorCode.ALERT_EVALUATION_FAILED);
        this.ruleId = null;
        this.ruleName = null;
    }

    public AlertException(String message, Long ruleId, String ruleName) {
        super(message, ErrorCode.ALERT_EVALUATION_FAILED);
        this.ruleId = ruleId;
        this.ruleName = ruleName;
    }

    public AlertException(String message, Long ruleId, String ruleName, ErrorCode errorCode) {
        super(message, errorCode);
        this.ruleId = ruleId;
        this.ruleName = ruleName;
    }

    public AlertException(String message, Long ruleId, String ruleName, Throwable cause) {
        super(message, ErrorCode.ALERT_EVALUATION_FAILED, cause);
        this.ruleId = ruleId;
        this.ruleName = ruleName;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder(super.getMessage());
        if (ruleId != null) {
            sb.append(" [ruleId=").append(ruleId);
            if (ruleName != null) {
                sb.append(", ruleName=").append(ruleName);
            }
            sb.append("]");
        }
        return sb.toString();
    }

    public static AlertException ruleInvalid(String reason) {
        return new AlertException("Invalid alert rule: " + reason, null, null, ErrorCode.ALERT_RULE_INVALID);
    }

    public static AlertException evaluationFailed(Long ruleId, String ruleName, Throwable cause) {
        return new AlertException("Alert evaluation failed", ruleId, ruleName, cause);
    }

    public static AlertException notificationFailed(Long ruleId, String ruleName, String channel) {
        return new AlertException("Failed to send notification via " + channel,
                ruleId, ruleName, ErrorCode.ALERT_NOTIFICATION_FAILED);
    }
}
