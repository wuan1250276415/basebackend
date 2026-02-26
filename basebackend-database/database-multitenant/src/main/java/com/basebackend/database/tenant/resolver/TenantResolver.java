package com.basebackend.database.tenant.resolver;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 租户解析器接口
 * <p>
 * 从 HTTP 请求中解析租户标识。支持多种解析策略：Header、Token、Domain 等。
 * 可注册多个解析器，按优先级依次尝试。
 */
public interface TenantResolver {

    /**
     * 从请求中解析租户 ID
     *
     * @param request HTTP 请求
     * @return 租户 ID，解析失败返回 null
     */
    String resolve(HttpServletRequest request);

    /**
     * 解析器优先级（数值越小优先级越高）
     *
     * @return 优先级，默认 100
     */
    default int getOrder() {
        return 100;
    }
}
