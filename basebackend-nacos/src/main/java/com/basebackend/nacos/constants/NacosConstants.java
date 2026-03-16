/*
 * Decompiled with CFR 0.152.
 */
package com.basebackend.nacos.constants;

public final class NacosConstants {
    public static final String DEFAULT_NAMESPACE = "public";
    public static final String DEFAULT_GROUP = "DEFAULT_GROUP";
    public static final String DEFAULT_CLUSTER = "DEFAULT";
    public static final String DEFAULT_FILE_EXTENSION = "yml";
    public static final long DEFAULT_TIMEOUT_MS = 5000L;
    public static final String PREFIX_ENV = "env_";
    public static final String PREFIX_TENANT = "tenant_";
    public static final String PREFIX_APP = "app_";
    public static final String GRAY_TAG_VERSION = "gray.version";
    public static final String GRAY_TAG_GROUP = "gray.group";
    public static final String GRAY_TAG_CANARY = "canary";
    public static final int DEFAULT_MAX_RETRIES = 3;
    public static final long DEFAULT_RETRY_INITIAL_DELAY_MS = 1000L;
    public static final long DEFAULT_RETRY_MAX_DELAY_MS = 10000L;
    public static final String CACHE_NAME_CONFIG = "nacos-config-cache";
    public static final long CACHE_EXPIRE_SECONDS = 300L;
    public static final String METRIC_PREFIX = "nacos";
    public static final String METRIC_CONFIG_GET = "nacos.config.get";
    public static final String METRIC_CONFIG_PUBLISH = "nacos.config.publish";
    public static final String METRIC_CONFIG_CHANGE = "nacos.config.change";
    public static final String METRIC_GRAY_RELEASE = "nacos.gray.release";
    public static final String METRIC_SERVICE_DISCOVERY = "nacos.service.discovery";

    private NacosConstants() {
        throw new UnsupportedOperationException("\u5e38\u91cf\u7c7b\u4e0d\u80fd\u5b9e\u4f8b\u5316");
    }
}

