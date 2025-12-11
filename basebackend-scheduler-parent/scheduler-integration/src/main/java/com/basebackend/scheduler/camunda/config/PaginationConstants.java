package com.basebackend.scheduler.camunda.config;

/**
 * 分页配置常量
 *
 * <p>统一定义分页的默认值与上限，确保 Controller、Service 层保持一致，
 * 避免 API 文档与实际行为不匹配。
 *
 * <p>设计原则：
 * <ul>
 *   <li>单一数据源：所有分页配置从此类获取，避免硬编码</li>
 *   <li>性能优化：限制最大分页大小，防止大批量查询占用内存和数据库资源</li>
 *   <li>文档一致性：与 DTO 的 @Max 注解保持同步</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
public final class PaginationConstants {

    /**
     * 分页大小下限
     *
     * <p>限制单次查询的最小记录数，确保查询参数有效。
     */
    public static final int MIN_PAGE_SIZE = 1;

    /**
     * 默认分页大小
     *
     * <p>当客户端未指定分页大小时，使用此默认值。
     * 该值在性能和用户体验之间取得平衡。
     */
    public static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * 分页大小上限
     *
     * <p>限制单次查询的最大记录数，防止以下问题：
     * <ul>
     *   <li>内存溢出：大批量数据占用过多内存</li>
     *   <li>数据库压力：大结果集查询影响数据库性能</li>
     *   <li>响应超时：大数据传输导致请求超时</li>
     * </ul>
     *
     * <p>如需批量导出数据，建议使用专门的导出接口，而非提高分页上限。
     */
    public static final int MAX_PAGE_SIZE = 1000;

    /**
     * 分页大小下限（字符串形式）
     *
     * <p>用于 Swagger 注解的 minimum 属性和错误提示信息，
     * 确保文档与实际上限值保持一致。
     */
    public static final String MIN_PAGE_SIZE_STR = String.valueOf(MIN_PAGE_SIZE);

    /**
     * 默认分页大小（字符串形式）
     *
     * <p>用于 Swagger 注解的 defaultValue/example 属性，
     * 确保文档与实际默认值保持一致。
     */
    public static final String DEFAULT_PAGE_SIZE_STR = String.valueOf(DEFAULT_PAGE_SIZE);

    /**
     * 分页大小上限（字符串形式）
     *
     * <p>用于 Swagger 注解的 maximum 属性和错误提示信息，
     * 确保文档与实际上限值保持一致。
     */
    public static final String MAX_PAGE_SIZE_STR = String.valueOf(MAX_PAGE_SIZE);

    /**
     * 分页参数约束描述
     *
     * <p>用于 Controller 层 Swagger 文档，
     * 统一分页参数的约束说明，避免重复定义和硬编码。
     */
    public static final String PAGINATION_CONSTRAINTS_DESC =
        "- current: 必须 >= 1\n- size: 必须在 1-" + MAX_PAGE_SIZE + " 之间，超出范围将返回验证错误";

    /**
     * 私有构造函数，防止实例化工具类
     */
    private PaginationConstants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}
