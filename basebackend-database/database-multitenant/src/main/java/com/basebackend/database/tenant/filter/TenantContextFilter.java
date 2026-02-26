package com.basebackend.database.tenant.filter;

import com.basebackend.database.tenant.context.TenantContext;
import com.basebackend.database.tenant.resolver.TenantResolver;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

/**
 * 租户上下文 Filter
 * <p>
 * 在请求入口自动解析租户标识并设置到 {@link TenantContext}。
 * 请求结束后自动清除上下文，防止内存泄漏。
 * <p>
 * 支持配置忽略路径（如健康检查、登录接口等）。
 */
@Slf4j
public class TenantContextFilter implements Filter {

    private final List<TenantResolver> resolvers;
    private final List<String> ignorePaths;
    private final boolean required;

    /**
     * @param resolvers   租户解析器列表（按 order 排序）
     * @param ignorePaths 忽略的路径前缀列表
     * @param required    是否必须解析到租户（true 则解析失败返回 403）
     */
    public TenantContextFilter(List<TenantResolver> resolvers, List<String> ignorePaths, boolean required) {
        this.resolvers = resolvers.stream()
                .sorted(Comparator.comparingInt(TenantResolver::getOrder))
                .toList();
        this.ignorePaths = ignorePaths != null ? ignorePaths : List.of();
        this.required = required;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();

        // 忽略路径直接放行
        if (isIgnoredPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            // 依次尝试解析租户
            String tenantId = resolveTenant(httpRequest);

            if (tenantId != null) {
                TenantContext.setTenantId(tenantId);
                log.debug("租户上下文已设置: tenantId={}, path={}", tenantId, path);
            } else if (required) {
                log.warn("未解析到租户标识, path={}", path);
                httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                httpResponse.setContentType("application/json;charset=UTF-8");
                httpResponse.getWriter().write("{\"code\":403,\"message\":\"租户标识缺失\"}");
                return;
            }

            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String resolveTenant(HttpServletRequest request) {
        for (TenantResolver resolver : resolvers) {
            String tenantId = resolver.resolve(request);
            if (tenantId != null) {
                return tenantId;
            }
        }
        return null;
    }

    private boolean isIgnoredPath(String path) {
        return ignorePaths.stream().anyMatch(path::startsWith);
    }
}
