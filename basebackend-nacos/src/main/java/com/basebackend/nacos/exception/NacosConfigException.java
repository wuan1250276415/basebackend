package com.basebackend.nacos.exception;

/**
 * Nacos配置异常
 * <p>
 * 统一的Nacos配置相关异常类，包含详细的上下文信息。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public class NacosConfigException extends RuntimeException {

    /**
     * 数据ID
     */
    private final String dataId;

    /**
     * 分组
     */
    private final String group;

    /**
     * 命名空间
     */
    private final String namespace;

    /**
     * 错误码
     */
    private final ErrorCode errorCode;

    /**
     * 重试次数
     */
    private final int retryCount;

    public NacosConfigException(String message, String dataId, String group, ErrorCode errorCode) {
        this(message, dataId, group, null, errorCode, 0, null);
    }

    public NacosConfigException(String message, String dataId, String group,
            ErrorCode errorCode, Throwable cause) {
        this(message, dataId, group, null, errorCode, 0, cause);
    }

    public NacosConfigException(String message, String dataId, String group, String namespace,
            ErrorCode errorCode, int retryCount, Throwable cause) {
        super(message, cause);
        this.dataId = dataId;
        this.group = group;
        this.namespace = namespace;
        this.errorCode = errorCode;
        this.retryCount = retryCount;
    }

    // Getters
    public String getDataId() {
        return dataId;
    }

    public String getGroup() {
        return group;
    }

    public String getNamespace() {
        return namespace;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public int getRetryCount() {
        return retryCount;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder(super.getMessage());
        sb.append(" [dataId=").append(dataId);
        sb.append(", group=").append(group);
        if (namespace != null) {
            sb.append(", namespace=").append(namespace);
        }
        sb.append(", errorCode=").append(errorCode);
        if (retryCount > 0) {
            sb.append(", retryCount=").append(retryCount);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 错误码枚举
     */
    public enum ErrorCode {
        /** 配置不存在 */
        CONFIG_NOT_FOUND("CONFIG_NOT_FOUND", "配置不存在"),

        /** 连接超时 */
        CONNECTION_TIMEOUT("CONNECTION_TIMEOUT", "连接超时"),

        /** 读取超时 */
        READ_TIMEOUT("READ_TIMEOUT", "读取超时"),

        /** 认证失败 */
        AUTH_FAILED("AUTH_FAILED", "认证失败"),

        /** 权限不足 */
        PERMISSION_DENIED("PERMISSION_DENIED", "权限不足"),

        /** 服务不可用 */
        SERVICE_UNAVAILABLE("SERVICE_UNAVAILABLE", "服务不可用"),

        /** 格式错误 */
        INVALID_FORMAT("INVALID_FORMAT", "配置格式错误"),

        /** 发布失败 */
        PUBLISH_FAILED("PUBLISH_FAILED", "配置发布失败"),

        /** 删除失败 */
        DELETE_FAILED("DELETE_FAILED", "配置删除失败"),

        /** 监听器添加失败 */
        LISTENER_ADD_FAILED("LISTENER_ADD_FAILED", "监听器添加失败"),

        /** 未知错误 */
        UNKNOWN("UNKNOWN", "未知错误");

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

    // 静态工厂方法
    public static NacosConfigException configNotFound(String dataId, String group) {
        return new NacosConfigException("配置不存在", dataId, group, ErrorCode.CONFIG_NOT_FOUND);
    }

    public static NacosConfigException connectionTimeout(String dataId, String group, Throwable cause) {
        return new NacosConfigException("连接Nacos服务器超时", dataId, group, ErrorCode.CONNECTION_TIMEOUT, cause);
    }

    public static NacosConfigException authFailed(String dataId, String group, Throwable cause) {
        return new NacosConfigException("Nacos认证失败", dataId, group, ErrorCode.AUTH_FAILED, cause);
    }

    public static NacosConfigException publishFailed(String dataId, String group, Throwable cause) {
        return new NacosConfigException("配置发布失败", dataId, group, ErrorCode.PUBLISH_FAILED, cause);
    }

    public static NacosConfigException deleteFailed(String dataId, String group, Throwable cause) {
        return new NacosConfigException("配置删除失败", dataId, group, ErrorCode.DELETE_FAILED, cause);
    }
}
