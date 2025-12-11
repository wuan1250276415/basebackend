package com.basebackend.featuretoggle.context;

import com.basebackend.featuretoggle.model.FeatureContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 基于HTTP请求头解析特性上下文的解析器。
 * <p>
 * 默认从 {@code X-User-Id}、{@code X-Tenant-Id}、{@code X-Session-Id}、{@code X-Environment} 中提取信息，
 * 并结合请求源IP构建上下文。
 */
@Component
public class HttpFeatureContextResolver implements FeatureContextResolver {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_TENANT_ID = "X-Tenant-Id";
    public static final String HEADER_SESSION_ID = "X-Session-Id";
    public static final String HEADER_ENVIRONMENT = "X-Environment";
    private static final String HEADER_FORWARDED_FOR = "X-Forwarded-For";

    @Override
    public FeatureContext resolve(HttpServletRequest request) {
        if (request == null) {
            return FeatureContext.empty();
        }

        FeatureContext.Builder builder = FeatureContext.builder()
                .userId(trimToNull(request.getHeader(HEADER_USER_ID)))
                .tenantId(trimToNull(request.getHeader(HEADER_TENANT_ID)))
                .sessionId(trimToNull(request.getHeader(HEADER_SESSION_ID)))
                .environment(trimToNull(request.getHeader(HEADER_ENVIRONMENT)));

        String ipAddress = resolveClientIp(request);
        if (StringUtils.hasText(ipAddress)) {
            builder.ipAddress(ipAddress);
        }

        return builder.build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = trimToNull(request.getHeader(HEADER_FORWARDED_FOR));
        if (StringUtils.hasText(forwarded)) {
            int index = forwarded.indexOf(',');
            return index > 0 ? forwarded.substring(0, index).trim() : forwarded;
        }
        return trimToNull(request.getRemoteAddr());
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
