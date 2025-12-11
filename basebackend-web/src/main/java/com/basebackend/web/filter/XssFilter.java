package com.basebackend.web.filter;

import com.basebackend.common.util.SanitizationUtils;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

/**
 * XSS 防护过滤器
 * <p>
 * 过滤请求参数中的恶意脚本和HTML内容。
 * 使用 {@link SanitizationUtils} 进行基于OWASP的统一XSS清洗。
 * </p>
 *
 * @author basebackend
 * @since 2025-11-23
 */
@Slf4j
@Component
public class XssFilter implements Filter {

    /**
     * 请求参数：设置为 "false" 时禁用XSS过滤
     */
    private static final String XSS_FILTER_ENABLED = "xss.filter.enabled";
    private static final String FALSE = "false";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // 检查是否禁用 XSS 过滤（默认启用）
        String enabled = request.getParameter(XSS_FILTER_ENABLED);
        if (FALSE.equalsIgnoreCase(enabled)) {
            // 明确禁用时，跳过XSS过滤
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 创建 XSS 清理后的请求包装器
        XssCleanedRequestWrapper wrappedRequest = new XssCleanedRequestWrapper(httpRequest);

        log.debug("Applying XSS filter to request: {}", httpRequest.getRequestURI());

        chain.doFilter(wrappedRequest, httpResponse);
    }

    /**
     * XSS 清理后的请求包装器
     * <p>
     * 对所有请求参数进行XSS清洗，使用OWASP的SanitizationUtils。
     * </p>
     */
    private static class XssCleanedRequestWrapper extends HttpServletRequestWrapper {

        private final Map<String, String[]> sanitizedParams;

        public XssCleanedRequestWrapper(HttpServletRequest request) {
            super(request);
            this.sanitizedParams = sanitizeParams(request.getParameterMap());
        }

        @Override
        public String getParameter(String name) {
            String value = super.getParameter(name);
            return value != null ? SanitizationUtils.sanitize(value) : null;
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) {
                return null;
            }
            String[] cleaned = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                cleaned[i] = values[i] != null ? SanitizationUtils.sanitize(values[i]) : null;
            }
            return cleaned;
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            return sanitizedParams;
        }

        @Override
        public Enumeration<String> getParameterNames() {
            return Collections.enumeration(sanitizedParams.keySet());
        }

        private Map<String, String[]> sanitizeParams(Map<String, String[]> originalParams) {
            Map<String, String[]> sanitized = new HashMap<>();
            for (Map.Entry<String, String[]> entry : originalParams.entrySet()) {
                String name = entry.getKey();
                String[] values = entry.getValue();
                String[] cleanValues = new String[values.length];
                for (int i = 0; i < values.length; i++) {
                    cleanValues[i] = SanitizationUtils.sanitize(values[i]);
                }
                sanitized.put(name, cleanValues);
            }
            return sanitized;
        }
    }
}
