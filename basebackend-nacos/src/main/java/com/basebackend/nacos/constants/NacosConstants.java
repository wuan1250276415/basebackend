package com.basebackend.nacos.constants;

/**
 * Nacos常量类
 * <p>
 * 定义Nacos相关的常量值，避免硬编码。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public final class NacosConstants {

    private NacosConstants() {
        throw new UnsupportedOperationException("常量类不能实例化");
    }

    // ========== 默认值 ==========

    /** 默认命名空间 */
    public static final String DEFAULT_NAMESPACE = "public";

    /** 默认分组 */
    public static final String DEFAULT_GROUP = "DEFAULT_GROUP";

    /** 默认集群名 */
    public static final String DEFAULT_CLUSTER = "DEFAULT";

    /** 默认文件扩展名 */
    public static final String DEFAULT_FILE_EXTENSION = "yml";

    /** 默认超时时间（毫秒） */
    public static final long DEFAULT_TIMEOUT_MS = 5000;

    // ========== 配置前缀 ==========

    /** 配置前缀 - 环境 */
    public static final String PREFIX_ENV = "env_";

    /** 配置前缀 - 租户 */
    public static final String PREFIX_TENANT = "tenant_";

    /** 配置前缀 - 应用 */
    public static final String PREFIX_APP = "app_";

    // ========== 灰度发布 ==========

    /** 灰度标签 - 版本 */
    public static final String GRAY_TAG_VERSION = "gray.version";

    /** 灰度标签 - 分组 */
    public static final String GRAY_TAG_GROUP = "gray.group";

    /** 灰度标签 - 金丝雀 */
    public static final String GRAY_TAG_CANARY = "canary";

    // ========== 重试配置 ==========

    /** 默认最大重试次数 */
    public static final int DEFAULT_MAX_RETRIES = 3;

    /** 默认初始重试延迟（毫秒） */
    public static final long DEFAULT_RETRY_INITIAL_DELAY_MS = 1000;

    /** 默认最大重试延迟（毫秒） */
    public static final long DEFAULT_RETRY_MAX_DELAY_MS = 10000;

    // ========== 缓存配置 ==========

    /** 配置缓存名称 */
    public static final String CACHE_NAME_CONFIG = "nacos-config-cache";

    /** 缓存过期时间（秒） */
    public static final long CACHE_EXPIRE_SECONDS = 300;

    // ========== 监控指标名称 ==========

    /** 指标前缀 */
    public static final String METRIC_PREFIX = "nacos";

    /** 配置获取指标 */
    public static final String METRIC_CONFIG_GET = METRIC_PREFIX + ".config.get";

    /** 配置发布指标 */
    public static final String METRIC_CONFIG_PUBLISH = METRIC_PREFIX + ".config.publish";

    /** 配置变更指标 */
    public static final String METRIC_CONFIG_CHANGE = METRIC_PREFIX + ".config.change";

    /** 灰度发布指标 */
    public static final String METRIC_GRAY_RELEASE = METRIC_PREFIX + ".gray.release";

    /** 服务发现指标 */
    public static final String METRIC_SERVICE_DISCOVERY = METRIC_PREFIX + ".service.discovery";
}
