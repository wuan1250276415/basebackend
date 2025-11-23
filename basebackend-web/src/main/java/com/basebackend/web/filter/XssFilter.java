package com.basebackend.web.filter;

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
 * 过滤请求参数中的恶意脚本和HTML内容
 *
 * @author basebackend
 * @since 2025-11-23
 */
@Slf4j
@Component
public class XssFilter implements Filter {

    private static final String XSS_FILTER_ENABLED = "xss.filter.enabled";
    private static final String TRUE = "true";

    /**
     * XSS 攻击模式匹配正则
     */
    private static final String[] XSS_PATTERNS = {
            "<script[^>]*>.*?</script>",
            "<iframe[^>]*>.*?</iframe>",
            "<object[^>]*>.*?</object>",
            "<embed[^>]*>.*?</embed>",
            "javascript:",
            "vbscript:",
            "onload\\s*=",
            "onerror\\s*=",
            "<img[^>]*src\\s*=\\s*[\"']\\s*javascript:",
            "<svg[^>]*>\\s*<script[^>]*>.*?</script>",
            "<[^>]*on[a-z]+\\s*=\\s*['\"].*?['\"]"
    };

    private static final List<String> ALLOWED_HTML_TAGS = Arrays.asList("p", "br", "strong", "em", "u", "h1", "h2", "h3", "h4", "h5", "h6", "ul", "ol", "li");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // 检查是否启用 XSS 过滤
        String enabled = request.getParameter(XSS_FILTER_ENABLED);
        if (TRUE.equals(enabled)) {
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
     * 清理 XSS 内容
     */
    private String cleanXss(String value) {
        if (value == null) {
            return null;
        }

        String result = value;

        // 移除或转义 XSS 模式
        for (String pattern : XSS_PATTERNS) {
            if (result.matches("(?s).*" + pattern + ".*")) {
                result = result.replaceAll(pattern, "");
            }
        }

        // HTML 实体编码
        result = result.replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll("\"", "&quot;")
                .replaceAll("'", "&#x27;")
                .replaceAll("/", "&#x2F;");

        return result;
    }

    /**
     * XSS 清理后的请求包装器
     */
    private class XssCleanedRequestWrapper extends HttpServletRequestWrapper {

        private final Map<String, String[]> sanitizedParams;

        public XssCleanedRequestWrapper(HttpServletRequest request) {
            super(request);
            this.sanitizedParams = sanitizeParams(request.getParameterMap());
        }

        @Override
        public String getParameter(String name) {
            String value = super.getParameter(name);
            return value != null ? cleanXss(value) : null;
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) {
                return null;
            }
            String[] cleaned = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                cleaned[i] = values[i] != null ? cleanXss(values[i]) : null;
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
                    cleanValues[i] = cleanXss(values[i]);
                }
                sanitized.put(name, cleanValues);
            }
            return sanitized;
        }
    }
}
