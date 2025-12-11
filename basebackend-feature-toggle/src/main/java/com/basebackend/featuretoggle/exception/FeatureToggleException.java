package com.basebackend.featuretoggle.exception;

/**
 * 特性开关异常基类
 * <p>
 * 所有特性开关相关的业务异常都继承自此类。
 * 提供统一的异常类型和错误码管理。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public class FeatureToggleException extends RuntimeException {

    /**
     * 错误代码
     */
    private final String errorCode;

    /**
     * 错误参数（用于格式化错误消息）
     */
    private final Object[] args;

    public FeatureToggleException(String message) {
        super(message);
        this.errorCode = "FEATURE_TOGGLE_ERROR";
        this.args = new Object[0];
    }

    public FeatureToggleException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "FEATURE_TOGGLE_ERROR";
        this.args = new Object[0];
    }

    public FeatureToggleException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.args = new Object[0];
    }

    public FeatureToggleException(String errorCode, String message, Object... args) {
        super(message);
        this.errorCode = errorCode;
        this.args = args != null ? args : new Object[0];
    }

    public FeatureToggleException(String errorCode, String message, Throwable cause, Object... args) {
        super(message, cause);
        this.errorCode = errorCode;
        this.args = args != null ? args : new Object[0];
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Object[] getArgs() {
        return args;
    }

    /**
     * 创建特性开关异常
     *
     * @param message 错误消息
     * @return 异常实例
     */
    public static FeatureToggleException of(String message) {
        return new FeatureToggleException(message);
    }

    /**
     * 创建特性开关异常（带错误码）
     *
     * @param errorCode 错误代码
     * @param message 错误消息
     * @return 异常实例
     */
    public static FeatureToggleException of(String errorCode, String message) {
        return new FeatureToggleException(errorCode, message);
    }

    /**
     * 创建特性开关异常（带格式化参数）
     *
     * @param message 错误消息模板
     * @param args 格式化参数
     * @return 异常实例
     */
    public static FeatureToggleException of(String message, Object... args) {
        return new FeatureToggleException("FEATURE_TOGGLE_ERROR", message, args);
    }

    /**
     * 创建特性开关异常（带错误码和格式化参数）
     *
     * @param errorCode 错误代码
     * @param message 错误消息模板
     * @param args 格式化参数
     * @return 异常实例
     */
    public static FeatureToggleException of(String errorCode, String message, Object... args) {
        return new FeatureToggleException(errorCode, message, args);
    }
}
