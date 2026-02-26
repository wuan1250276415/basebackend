package com.basebackend.database.tenant.resolver;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * 基于域名的租户解析器
 * <p>
 * 从请求域名中提取租户标识，支持子域名模式。
 * 例如：{@code tenant1.example.com} → 租户 ID 为 {@code tenant1}
 */
@Slf4j
public class DomainTenantResolver implements TenantResolver {

    @Override
    public String resolve(HttpServletRequest request) {
        String host = request.getServerName();
        if (host == null || host.isBlank()) {
            return null;
        }

        // 提取子域名作为租户标识
        // 例如: tenant1.api.example.com → tenant1
        String[] parts = host.split("\\.");
        if (parts.length >= 3) {
            String tenantId = parts[0];
            // 排除常见的非租户子域名
            if (!"www".equals(tenantId) && !"api".equals(tenantId)
                    && !"admin".equals(tenantId) && !"localhost".equals(tenantId)) {
                log.debug("从域名 '{}' 解析到租户: {}", host, tenantId);
                return tenantId;
            }
        }
        return null;
    }

    @Override
    public int getOrder() {
        return 30;
    }
}
