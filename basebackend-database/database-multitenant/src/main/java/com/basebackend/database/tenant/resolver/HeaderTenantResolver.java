package com.basebackend.database.tenant.resolver;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * 基于 HTTP Header 的租户解析器
 * <p>
 * 从请求头中提取租户 ID，默认 Header 名称为 {@code X-Tenant-Id}。
 */
@Slf4j
public class HeaderTenantResolver implements TenantResolver {

    private final String headerName;

    public HeaderTenantResolver() {
        this("X-Tenant-Id");
    }

    public HeaderTenantResolver(String headerName) {
        this.headerName = headerName;
    }

    @Override
    public String resolve(HttpServletRequest request) {
        String tenantId = request.getHeader(headerName);
        if (tenantId != null && !tenantId.isBlank()) {
            log.debug("从 Header '{}' 解析到租户: {}", headerName, tenantId);
            return tenantId.trim();
        }
        return null;
    }

    @Override
    public int getOrder() {
        return 10;
    }
}
