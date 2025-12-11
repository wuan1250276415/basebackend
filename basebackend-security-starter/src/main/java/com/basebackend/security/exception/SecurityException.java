package com.basebackend.security.exception;

import lombok.Getter;

/**
 * 安全模块异常基类
 * <p>
 * 提供统一的安全异常处理，包含详细的错误信息和错误码。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Getter
public class SecurityException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String details;

    public SecurityException(String message) {
        super(message);
        this.errorCode = ErrorCode.UNKNOWN;
        this.details = null;
    }

    public SecurityException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.details = null;
    }

    public SecurityException(String message, ErrorCode errorCode, String details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public SecurityException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = ErrorCode.UNKNOWN;
        this.details = null;
    }

    public SecurityException(String message, ErrorCode errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = null;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder(super.getMessage());
        sb.append(" [code=").append(errorCode.getCode());
        if (details != null) {
            sb.append(", details=").append(details);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 安全错误码枚举
     */
    public enum ErrorCode {
        // 通用错误
        UNKNOWN("SEC_0000", "未知安全错误"),
        CONFIGURATION_ERROR("SEC_0001", "配置错误"),
        INITIALIZATION_FAILED("SEC_0002", "初始化失败"),

        // 认证错误 (1xxx)
        AUTHENTICATION_FAILED("SEC_1001", "认证失败"),
        TOKEN_INVALID("SEC_1002", "令牌无效"),
        TOKEN_EXPIRED("SEC_1003", "令牌已过期"),
        CREDENTIALS_INVALID("SEC_1004", "凭证无效"),

        // 授权错误 (2xxx)
        ACCESS_DENIED("SEC_2001", "访问被拒绝"),
        PERMISSION_DENIED("SEC_2002", "权限不足"),
        RESOURCE_FORBIDDEN("SEC_2003", "资源禁止访问"),

        // 零信任错误 (3xxx)
        RISK_ASSESSMENT_FAILED("SEC_3001", "风险评估失败"),
        HIGH_RISK_DETECTED("SEC_3002", "检测到高风险"),
        DEVICE_NOT_TRUSTED("SEC_3003", "设备不受信任"),
        POLICY_VIOLATION("SEC_3004", "违反安全策略"),

        // mTLS错误 (4xxx)
        SSL_INIT_FAILED("SEC_4001", "SSL初始化失败"),
        CERTIFICATE_INVALID("SEC_4002", "证书无效"),
        CERTIFICATE_EXPIRED("SEC_4003", "证书已过期"),
        CERTIFICATE_NOT_FOUND("SEC_4004", "证书未找到"),
        KEYSTORE_ERROR("SEC_4005", "密钥库错误"),

        // OAuth2错误 (5xxx)
        OAUTH2_CONFIG_ERROR("SEC_5001", "OAuth2配置错误"),
        OAUTH2_TOKEN_ERROR("SEC_5002", "OAuth2令牌错误");

        private final String code;
        private final String description;

        ErrorCode(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }
}
