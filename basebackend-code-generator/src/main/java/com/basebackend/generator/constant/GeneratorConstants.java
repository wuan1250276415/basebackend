package com.basebackend.generator.constant;

/**
 * 代码生成器常量定义
 * 消除代码中的魔法值，提高可维护性
 */
public final class GeneratorConstants {

    private GeneratorConstants() {
        throw new UnsupportedOperationException("常量类不允许实例化");
    }

    // ==================== 状态常量 ====================

    /**
     * 启用状态
     */
    public static final int STATUS_ENABLED = 1;

    /**
     * 禁用状态
     */
    public static final int STATUS_DISABLED = 0;

    // ==================== 数据源连接池配置 ====================

    /**
     * 连接池初始大小
     */
    public static final int POOL_INITIAL_SIZE = 1;

    /**
     * 连接池最小空闲连接数
     */
    public static final int POOL_MIN_IDLE = 1;

    /**
     * 连接池最大活动连接数
     */
    public static final int POOL_MAX_ACTIVE = 5;

    /**
     * 获取连接的最大等待时间（毫秒）
     */
    public static final long POOL_MAX_WAIT_MILLIS = 60000L;

    /**
     * 连接验证查询
     */
    public static final String POOL_VALIDATION_QUERY = "SELECT 1";

    // ==================== 路径占位符 ====================

    /**
     * 包路径占位符
     */
    public static final String PLACEHOLDER_PACKAGE_PATH = "${packagePath}";

    /**
     * 类名占位符
     */
    public static final String PLACEHOLDER_CLASS_NAME = "${className}";

    /**
     * 变量名占位符
     */
    public static final String PLACEHOLDER_VARIABLE_NAME = "${variableName}";

    /**
     * 模板代码占位符
     */
    public static final String PLACEHOLDER_TEMPLATE_CODE = "${templateCode}";

    // ==================== 默认值 ====================

    /**
     * 默认输出路径模板
     */
    public static final String DEFAULT_OUTPUT_PATH = "${packagePath}/${templateCode}/${className}";

    /**
     * 默认Java类型（当类型映射不存在时）
     */
    public static final String DEFAULT_JAVA_TYPE = "String";

    /**
     * 默认TypeScript类型（当类型映射不存在时）
     */
    public static final String DEFAULT_TS_TYPE = "string";

    // ==================== 数据源缓存配置 ====================

    /**
     * 数据源缓存过期时间（秒）
     */
    public static final long DATASOURCE_CACHE_EXPIRE_SECONDS = 3600L;

    /**
     * 数据源缓存最大数量
     */
    public static final int DATASOURCE_CACHE_MAX_SIZE = 100;

    // ==================== 模板引擎配置 ====================

    /**
     * FreeMarker引擎类型
     */
    public static final String ENGINE_TYPE_FREEMARKER = "FREEMARKER";

    /**
     * Velocity引擎类型
     */
    public static final String ENGINE_TYPE_VELOCITY = "VELOCITY";

    /**
     * Thymeleaf引擎类型
     */
    public static final String ENGINE_TYPE_THYMELEAF = "THYMELEAF";

    // ==================== 生成类型 ====================

    /**
     * 下载模式
     */
    public static final String GENERATE_TYPE_DOWNLOAD = "DOWNLOAD";

    /**
     * 预览模式
     */
    public static final String GENERATE_TYPE_PREVIEW = "PREVIEW";

    /**
     * 增量更新模式
     */
    public static final String GENERATE_TYPE_INCREMENT = "INCREMENT";
}
