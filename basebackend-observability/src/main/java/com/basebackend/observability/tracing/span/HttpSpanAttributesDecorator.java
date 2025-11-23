package com.basebackend.observability.tracing.span;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

/**
 * HTTP Span 属性装饰器
 * <p>
 * 根据 OpenTelemetry HTTP 语义约定为 Span 添加 HTTP 相关属性。
 * 支持服务端（Servlet）和客户端（Spring RestTemplate/WebClient）场景。
 * </p>
 * <p>
 * 服务端属性（基于 {@link HttpServletRequest} 和 {@link HttpServletResponse}）：
 * <ul>
 *     <li><b>http.method</b> - HTTP 方法（GET, POST, PUT, DELETE 等）</li>
 *     <li><b>http.url</b> - 完整 URL（包含协议、主机、端口、路径、查询参数）</li>
 *     <li><b>http.status_code</b> - HTTP 状态码（200, 404, 500 等）</li>
 *     <li><b>http.user_agent</b> - User-Agent header</li>
 * </ul>
 * </p>
 * <p>
 * 客户端属性（基于 {@link HttpRequest}）：
 * <ul>
 *     <li><b>http.method</b> - HTTP 方法</li>
 *     <li><b>http.url</b> - 完整 URL</li>
 *     <li><b>net.peer.name</b> - 目标主机名</li>
 *     <li><b>net.peer.port</b> - 目标端口</li>
 * </ul>
 * </p>
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 在 HTTP 服务端过滤器中
 * Map<String, Object> attrs = new HashMap<>();
 * attrs.put(HttpServletRequest.class.getName(), request);
 * attrs.put(HttpServletResponse.class.getName(), response);
 * decorator.decorate(span, context, attrs);
 *
 * // 在 HTTP 客户端拦截器中
 * Map<String, Object> attrs = new HashMap<>();
 * attrs.put(HttpRequest.class.getName(), request);
 * decorator.decorate(span, context, attrs);
 * }</pre>
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 * @see <a href="https://opentelemetry.io/docs/specs/semconv/http/">OpenTelemetry HTTP Semantic Conventions</a>
 */
@Component
public class HttpSpanAttributesDecorator implements SpanAttributesDecorator {

    private static final Logger log = LoggerFactory.getLogger(HttpSpanAttributesDecorator.class);

    // HTTP 语义约定属性键
    private static final AttributeKey<String> HTTP_METHOD = AttributeKey.stringKey("http.method");
    private static final AttributeKey<String> HTTP_URL = AttributeKey.stringKey("http.url");
    private static final AttributeKey<Long> HTTP_STATUS = AttributeKey.longKey("http.status_code");
    private static final AttributeKey<String> HTTP_USER_AGENT = AttributeKey.stringKey("http.user_agent");
    private static final AttributeKey<String> HTTP_SCHEME = AttributeKey.stringKey("http.scheme");
    private static final AttributeKey<String> HTTP_TARGET = AttributeKey.stringKey("http.target");
    private static final AttributeKey<String> HTTP_ROUTE = AttributeKey.stringKey("http.route");

    // 网络语义约定属性键
    private static final AttributeKey<String> NET_PEER_NAME = AttributeKey.stringKey("net.peer.name");
    private static final AttributeKey<Long> NET_PEER_PORT = AttributeKey.longKey("net.peer.port");
    private static final AttributeKey<String> NET_HOST_NAME = AttributeKey.stringKey("net.host.name");
    private static final AttributeKey<Long> NET_HOST_PORT = AttributeKey.longKey("net.host.port");

    @Override
    public boolean supports(Span span, Context context, Map<String, Object> attributes) {
        // 支持服务端或客户端 HTTP 场景
        return attributes.containsKey(HttpServletRequest.class.getName())
                || attributes.containsKey(HttpRequest.class.getName());
    }

    @Override
    public void decorate(Span span, Context context, Map<String, Object> attributes) {
        // 服务端场景
        if (attributes.containsKey(HttpServletRequest.class.getName())) {
            HttpServletRequest request = (HttpServletRequest) attributes.get(HttpServletRequest.class.getName());
            HttpServletResponse response = (HttpServletResponse) attributes.get(HttpServletResponse.class.getName());
            enrichServer(span, request, response);
            return;
        }

        // 客户端场景
        if (attributes.containsKey(HttpRequest.class.getName())) {
            HttpRequest request = (HttpRequest) attributes.get(HttpRequest.class.getName());
            enrichClient(span, request);
        }
    }

    /**
     * 填充服务端 HTTP 属性
     *
     * @param span     当前 Span
     * @param request  HTTP 请求
     * @param response HTTP 响应（可能为 null）
     */
    private void enrichServer(Span span, HttpServletRequest request, HttpServletResponse response) {
        if (request == null) {
            log.trace("HttpServletRequest 为 null，跳过服务端属性填充");
            return;
        }

        try {
            // HTTP 方法
            String method = request.getMethod();
            if (method != null && !method.isEmpty()) {
                span.setAttribute(HTTP_METHOD, method);
            }

            // 完整 URL
            StringBuffer requestURL = request.getRequestURL();
            if (requestURL != null) {
                String url = requestURL.toString();
                String queryString = request.getQueryString();
                if (queryString != null && !queryString.isEmpty()) {
                    url = url + "?" + queryString;
                }
                span.setAttribute(HTTP_URL, url);
            }

            // HTTP 协议
            String scheme = request.getScheme();
            if (scheme != null && !scheme.isEmpty()) {
                span.setAttribute(HTTP_SCHEME, scheme);
            }

            // HTTP 目标（路径 + 查询参数）
            String requestURI = request.getRequestURI();
            String queryString = request.getQueryString();
            if (requestURI != null) {
                String target = queryString != null ? requestURI + "?" + queryString : requestURI;
                span.setAttribute(HTTP_TARGET, target);
            }

            // User-Agent
            String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
            if (userAgent != null && !userAgent.isEmpty()) {
                span.setAttribute(HTTP_USER_AGENT, userAgent);
            }

            // 服务器主机名和端口
            String serverName = request.getServerName();
            if (serverName != null && !serverName.isEmpty()) {
                span.setAttribute(NET_HOST_NAME, serverName);
            }

            int serverPort = request.getServerPort();
            if (serverPort > 0) {
                span.setAttribute(NET_HOST_PORT, (long) serverPort);
            }

            // HTTP 状态码（如果响应可用）
            if (response != null) {
                int status = response.getStatus();
                span.setAttribute(HTTP_STATUS, (long) status);
            }

            log.trace("服务端 HTTP 属性填充成功: method={}, url={}, status={}",
                    method, requestURL, response != null ? response.getStatus() : null);

        } catch (Exception ex) {
            log.debug("填充服务端 HTTP 属性失败", ex);
        }
    }

    /**
     * 填充客户端 HTTP 属性
     *
     * @param span    当前 Span
     * @param request HTTP 请求
     */
    private void enrichClient(Span span, HttpRequest request) {
        if (request == null) {
            log.trace("HttpRequest 为 null，跳过客户端属性填充");
            return;
        }

        try {
            // HTTP 方法
            String method = Objects.toString(request.getMethod(), "GET");
            span.setAttribute(HTTP_METHOD, method);

            // URI 信息
            URI uri = request.getURI();
            if (uri != null) {
                // 完整 URL
                span.setAttribute(HTTP_URL, uri.toString());

                // 协议
                String scheme = uri.getScheme();
                if (scheme != null && !scheme.isEmpty()) {
                    span.setAttribute(HTTP_SCHEME, scheme);
                }

                // 目标主机名
                String host = uri.getHost();
                if (host != null && !host.isEmpty()) {
                    span.setAttribute(NET_PEER_NAME, host);
                }

                // 目标端口
                int port = uri.getPort();
                if (port > 0) {
                    span.setAttribute(NET_PEER_PORT, (long) port);
                } else if (scheme != null) {
                    // 使用默认端口
                    int defaultPort = "https".equalsIgnoreCase(scheme) ? 443 : 80;
                    span.setAttribute(NET_PEER_PORT, (long) defaultPort);
                }

                // 目标路径
                String path = uri.getRawPath();
                String query = uri.getRawQuery();
                if (path != null) {
                    String target = query != null ? path + "?" + query : path;
                    span.setAttribute(HTTP_TARGET, target);
                }
            }

            log.trace("客户端 HTTP 属性填充成功: method={}, uri={}", method, uri);

        } catch (Exception ex) {
            log.debug("填充客户端 HTTP 属性失败", ex);
        }
    }
}
