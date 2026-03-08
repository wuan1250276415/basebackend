/*
 * Decompiled with CFR 0.152.
 */
package com.basebackend.nacos.exception;

public class NacosConfigException
extends RuntimeException {
    private final String dataId;
    private final String group;
    private final String namespace;
    private final ErrorCode errorCode;
    private final int retryCount;

    public NacosConfigException(String message, String dataId, String group, ErrorCode errorCode) {
        this(message, dataId, group, null, errorCode, 0, null);
    }

    public NacosConfigException(String message, String dataId, String group, ErrorCode errorCode, Throwable cause) {
        this(message, dataId, group, null, errorCode, 0, cause);
    }

    public NacosConfigException(String message, String dataId, String group, String namespace, ErrorCode errorCode, int retryCount, Throwable cause) {
        super(message, cause);
        this.dataId = dataId;
        this.group = group;
        this.namespace = namespace;
        this.errorCode = errorCode;
        this.retryCount = retryCount;
    }

    public String getDataId() {
        return this.dataId;
    }

    public String getGroup() {
        return this.group;
    }

    public String getNamespace() {
        return this.namespace;
    }

    public ErrorCode getErrorCode() {
        return this.errorCode;
    }

    public int getRetryCount() {
        return this.retryCount;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder(super.getMessage());
        sb.append(" [dataId=").append(this.dataId);
        sb.append(", group=").append(this.group);
        if (this.namespace != null) {
            sb.append(", namespace=").append(this.namespace);
        }
        sb.append(", errorCode=").append((Object)this.errorCode);
        if (this.retryCount > 0) {
            sb.append(", retryCount=").append(this.retryCount);
        }
        sb.append("]");
        return sb.toString();
    }

    public static NacosConfigException configNotFound(String dataId, String group) {
        return new NacosConfigException("\u914d\u7f6e\u4e0d\u5b58\u5728", dataId, group, ErrorCode.CONFIG_NOT_FOUND);
    }

    public static NacosConfigException connectionTimeout(String dataId, String group, Throwable cause) {
        return new NacosConfigException("\u8fde\u63a5Nacos\u670d\u52a1\u5668\u8d85\u65f6", dataId, group, ErrorCode.CONNECTION_TIMEOUT, cause);
    }

    public static NacosConfigException authFailed(String dataId, String group, Throwable cause) {
        return new NacosConfigException("Nacos\u8ba4\u8bc1\u5931\u8d25", dataId, group, ErrorCode.AUTH_FAILED, cause);
    }

    public static NacosConfigException publishFailed(String dataId, String group, Throwable cause) {
        return new NacosConfigException("\u914d\u7f6e\u53d1\u5e03\u5931\u8d25", dataId, group, ErrorCode.PUBLISH_FAILED, cause);
    }

    public static NacosConfigException deleteFailed(String dataId, String group, Throwable cause) {
        return new NacosConfigException("\u914d\u7f6e\u5220\u9664\u5931\u8d25", dataId, group, ErrorCode.DELETE_FAILED, cause);
    }

    public static enum ErrorCode {
        CONFIG_NOT_FOUND("CONFIG_NOT_FOUND", "\u914d\u7f6e\u4e0d\u5b58\u5728"),
        CONNECTION_TIMEOUT("CONNECTION_TIMEOUT", "\u8fde\u63a5\u8d85\u65f6"),
        READ_TIMEOUT("READ_TIMEOUT", "\u8bfb\u53d6\u8d85\u65f6"),
        AUTH_FAILED("AUTH_FAILED", "\u8ba4\u8bc1\u5931\u8d25"),
        PERMISSION_DENIED("PERMISSION_DENIED", "\u6743\u9650\u4e0d\u8db3"),
        SERVICE_UNAVAILABLE("SERVICE_UNAVAILABLE", "\u670d\u52a1\u4e0d\u53ef\u7528"),
        INVALID_FORMAT("INVALID_FORMAT", "\u914d\u7f6e\u683c\u5f0f\u9519\u8bef"),
        PUBLISH_FAILED("PUBLISH_FAILED", "\u914d\u7f6e\u53d1\u5e03\u5931\u8d25"),
        DELETE_FAILED("DELETE_FAILED", "\u914d\u7f6e\u5220\u9664\u5931\u8d25"),
        LISTENER_ADD_FAILED("LISTENER_ADD_FAILED", "\u76d1\u542c\u5668\u6dfb\u52a0\u5931\u8d25"),
        UNKNOWN("UNKNOWN", "\u672a\u77e5\u9519\u8bef");

        private final String code;
        private final String description;

        private ErrorCode(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return this.code;
        }

        public String getDescription() {
            return this.description;
        }
    }
}

