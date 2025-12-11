package com.basebackend.security.exception;

/**
 * 零信任安全相关异常
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public class ZeroTrustException extends SecurityException {

    private final String userId;
    private final Integer riskScore;

    public ZeroTrustException(String message) {
        super(message, ErrorCode.POLICY_VIOLATION);
        this.userId = null;
        this.riskScore = null;
    }

    public ZeroTrustException(String message, ErrorCode errorCode) {
        super(message, errorCode);
        this.userId = null;
        this.riskScore = null;
    }

    public ZeroTrustException(String message, String userId, Integer riskScore) {
        super(message, ErrorCode.HIGH_RISK_DETECTED);
        this.userId = userId;
        this.riskScore = riskScore;
    }

    public ZeroTrustException(String message, String userId, Integer riskScore, ErrorCode errorCode) {
        super(message, errorCode);
        this.userId = userId;
        this.riskScore = riskScore;
    }

    public String getUserId() {
        return userId;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder(super.getMessage());
        if (userId != null) {
            sb.append(", userId=").append(userId);
        }
        if (riskScore != null) {
            sb.append(", riskScore=").append(riskScore);
        }
        return sb.toString();
    }

    // 静态工厂方法
    public static ZeroTrustException highRiskDetected(String userId, int riskScore) {
        return new ZeroTrustException(
                "检测到高风险访问",
                userId,
                riskScore,
                ErrorCode.HIGH_RISK_DETECTED);
    }

    public static ZeroTrustException deviceNotTrusted(String userId, String deviceId) {
        return new ZeroTrustException(
                "设备不受信任: " + deviceId,
                userId,
                null,
                ErrorCode.DEVICE_NOT_TRUSTED);
    }

    public static ZeroTrustException policyViolation(String userId, String policyName) {
        return new ZeroTrustException(
                "违反安全策略: " + policyName,
                userId,
                null,
                ErrorCode.POLICY_VIOLATION);
    }

    public static ZeroTrustException riskAssessmentFailed(String userId, Throwable cause) {
        ZeroTrustException ex = new ZeroTrustException(
                "风险评估失败",
                userId,
                null,
                ErrorCode.RISK_ASSESSMENT_FAILED);
        ex.initCause(cause);
        return ex;
    }
}
