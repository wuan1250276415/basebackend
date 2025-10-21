package com.basebackend.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * 对敏感操作强制校验 Origin / Referer，防御 CSRF
 */
@Slf4j
@RequiredArgsConstructor
public class OriginValidationFilter extends OncePerRequestFilter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final SecurityBaselineProperties properties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!requiresValidation(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        List<String> allowedOrigins = properties.getAllowedOrigins();
        if (CollectionUtils.isEmpty(allowedOrigins)) {
            filterChain.doFilter(request, response);
            return;
        }

        String origin = request.getHeader("Origin");
        if (isAllowedOrigin(origin, allowedOrigins)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (properties.isEnforceReferer()) {
            String referer = request.getHeader("Referer");
            if (isAllowedOrigin(referer, allowedOrigins)) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        log.warn("Blocked request due to invalid origin. method={}, uri={}, origin={}, referer={}",
                request.getMethod(), request.getRequestURI(), origin, request.getHeader("Referer"));
        respondForbidden(response);
    }

    private boolean requiresValidation(HttpServletRequest request) {
        HttpMethod method = HttpMethod.resolve(request.getMethod());
        if (method == null) {
            return false;
        }
        return !(HttpMethod.GET.equals(method) || HttpMethod.HEAD.equals(method) || HttpMethod.OPTIONS.equals(method))
                && request.getHeader("Cookie") != null;
    }

    private boolean isAllowedOrigin(String candidate, List<String> allowedOrigins) {
        if (candidate == null) {
            return false;
        }
        try {
            URI uri = new URI(candidate);
            String normalized = normalize(uri);
            return allowedOrigins.stream()
                    .map(this::normalize)
                    .anyMatch(allowed -> allowed.equals(normalized));
        } catch (URISyntaxException e) {
            log.debug("Invalid origin header detected: {}", candidate, e);
            return false;
        }
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
        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(new ErrorBody()));
    }

    private static class ErrorBody {
        public final int code = HttpServletResponse.SC_FORBIDDEN;
        public final String message = "Invalid request origin";
    }
}
