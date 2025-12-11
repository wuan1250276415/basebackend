package com.basebackend.security.filter;

import com.basebackend.security.config.SecurityBaselineProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 对敏感操作强制校验 Origin / Referer，防御 CSRF
 */
@Slf4j
public class OriginValidationFilter extends OncePerRequestFilter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Set<HttpMethod> SAFE_METHODS = Set.of(
            HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS);
    private static final String FORBIDDEN_MESSAGE;

    static {
        String serialized;
        try {
            serialized = OBJECT_MAPPER.writeValueAsString(new ErrorBody());
        } catch (JsonProcessingException e) {
            serialized = "{\"code\":403,\"message\":\"Invalid request origin\"}";
        }
        FORBIDDEN_MESSAGE = serialized;
    }

    private final SecurityBaselineProperties properties;
    private final Set<String> normalizedAllowedOrigins;

    public OriginValidationFilter(SecurityBaselineProperties properties) {
        this.properties = properties;
        this.normalizedAllowedOrigins = normalizeAllowedOrigins(properties.getAllowedOrigins());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!requiresValidation(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (CollectionUtils.isEmpty(normalizedAllowedOrigins)) {
            filterChain.doFilter(request, response);
            return;
        }

        String origin = request.getHeader("Origin");
        if (isAllowedOrigin(origin)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (properties.isEnforceReferer()) {
            String referer = request.getHeader("Referer");
            if (isAllowedOrigin(referer)) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        log.warn("Blocked request due to invalid origin. method={}, uri={}, origin={}, referer={}",
                request.getMethod(), request.getRequestURI(), origin, request.getHeader("Referer"));
        respondForbidden(response);
    }

    private boolean requiresValidation(HttpServletRequest request) {
        try {
            HttpMethod method = HttpMethod.valueOf(request.getMethod());
            return !SAFE_METHODS.contains(method)
                    && request.getHeader("Cookie") != null;
        } catch (IllegalArgumentException e) {
            // 无效的HTTP方法
            return false;
        }
    }

    private boolean isAllowedOrigin(String candidate) {
        if (!StringUtils.hasText(candidate)) {
            return false;
        }
        try {
            URI uri = new URI(candidate);
            String normalized = normalize(uri);
            return normalizedAllowedOrigins.contains(normalized);
        } catch (URISyntaxException e) {
            log.debug("Invalid origin header detected: {}", candidate, e);
            return false;
        }
    }

    private Set<String> normalizeAllowedOrigins(List<String> allowedOrigins) {
        if (CollectionUtils.isEmpty(allowedOrigins)) {
            return Collections.emptySet();
        }
        return allowedOrigins.stream()
                .filter(StringUtils::hasText)
                .map(this::normalize)
                .collect(Collectors.toUnmodifiableSet());
    }

    private String normalize(String origin) {
        try {
            return normalize(new URI(origin));
        } catch (URISyntaxException e) {
            return origin;
        }
    }

    private String normalize(URI uri) {
        int port = uri.getPort();
        String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase(Locale.ROOT) : "";
        String host = uri.getHost() != null ? uri.getHost().toLowerCase(Locale.ROOT) : "";
        if (port == -1) {
            return scheme + "://" + host;
        }
        return scheme + "://" + host + ":" + port;
    }

    private void respondForbidden(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(FORBIDDEN_MESSAGE);
    }

    private static class ErrorBody {
        public final int code = HttpServletResponse.SC_FORBIDDEN;
        public final String message = "Invalid request origin";
    }
}
