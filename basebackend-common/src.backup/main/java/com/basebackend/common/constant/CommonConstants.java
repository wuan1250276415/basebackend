package com.basebackend.common.constant;

/**
 * 公共常量
 */
public class CommonConstants {

    /**
     * 成功标记
     */
    public static final Integer SUCCESS = 200;

    /**
     * 失败标记
     */
    public static final Integer FAIL = 500;

    /**
     * UTF-8编码
     */
    public static final String UTF8 = "UTF-8";

    /**
     * JSON内容类型
     */
    public static final String CONTENT_TYPE_JSON = "application/json";

    /**
     * 默认页码
     */
    public static final Long DEFAULT_CURRENT = 1L;

    /**
     * 默认每页大小
     */
    public static final Long DEFAULT_SIZE = 10L;

    /**
     * Token请求头
     */
    public static final String TOKEN_HEADER = "Authorization";

    /**
     * Token前缀
     */
    public static final String TOKEN_PREFIX = "Bearer ";
}
