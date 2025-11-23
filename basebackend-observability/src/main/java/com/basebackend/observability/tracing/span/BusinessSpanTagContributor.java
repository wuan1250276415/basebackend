package com.basebackend.observability.tracing.span;

import com.basebackend.observability.tracing.config.TracingProperties;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpRequest;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

/**
 * 业务标签贡献器
 * <p>
 * 根据配置的自定义标签映射（{@code observability.tracing.span.custom-tags}），
 * 从 HTTP header 或 Baggage 中提取业务相关的值，并添加到 Span 属性中。
 * </p>
 * <p>
 * 标签来源优先级：
 * <ol>
 *     <li><b>HTTP Header</b>（服务端/客户端请求）- 直接从请求头读取</li>
 *     <li><b>Baggage</b>（上下文传播）- 从上游服务传播而来</li>
 * </ol>
 * </p>
 * <p>
 * 配置示例：
 * <pre>
 * observability:
 *   tracing:
 *     span:
 *       custom-tags:
 *         user.id: X-User-Id         # Span 属性名: HTTP Header 名
 *         tenant.id: X-Tenant-Id
 *         channel.id: X-Channel-Id
 *         request.id: X-Request-Id
 * </pre>
 * </p>
 * <p>
 * 使用场景：
 * <ul>
 *     <li>在服务端过滤器中，从 {@link HttpServletRequest} 提取业务标签</li>
 *     <li>在客户端拦截器中，从 {@link HttpRequest} 提取业务标签</li>
 *     <li>在任何追踪点，从 {@link Baggage} 提取上游传播的业务上下文</li>
 * </ul>
 * </p>
 * <p>
 * <b>条件加载：</b>仅在 {@code observability.tracing.enabled=true} 时生效（默认启用）。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 * @see SpanAttributesDecorator
 * @see TracingProperties.Span#getCustomTags()
 */
@Component
@ConditionalOnProperty(prefix = "observability.tracing", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BusinessSpanTagContributor implements SpanAttributesDecorator {

    private static final Logger log = LoggerFactory.getLogger(BusinessSpanTagContributor.class);

    private final TracingProperties tracingProperties;

    /**
     * 构造函数
     *
     * @param tracingProperties 追踪配置属性
     */
    public BusinessSpanTagContributor(TracingProperties tracingProperties) {
        this.tracingProperties = tracingProperties;
        log.info("业务标签贡献器已初始化: customTags={}",
                tracingProperties.getSpan().getCustomTags().size());
    }

    @Override
    public boolean supports(Span span, Context context, Map<String, Object> attributes) {
        // 只要配置了自定义标签就尝试填充
        Map<String, String> customTags = tracingProperties.getSpan().getCustomTags();
        if (customTags == null || customTags.isEmpty()) {
            return false;
        }

        // 支持 HTTP 请求场景或任何包含 Context 的场景（Baggage）
        return attributes.containsKey(HttpServletRequest.class.getName())
                || attributes.containsKey(HttpRequest.class.getName())
                || context != null;
    }

    @Override
    public void decorate(Span span, Context context, Map<String, Object> attributes) {
        Map<String, String> customTags = tracingProperties.getSpan().getCustomTags();
        if (customTags == null || customTags.isEmpty()) {
            log.trace("未配置自定义标签，跳过业务标签填充");
            return;
        }

        try {
            int addedCount = 0;

            // 遍历配置的自定义标签映射
            for (Map.Entry<String, String> entry : customTags.entrySet()) {
                String attributeName = entry.getKey();   // 如 "user.id"
                String headerName = entry.getValue();     // 如 "X-User-Id"

                String value = null;

                // 1. 优先从 HTTP 请求头提取（服务端场景）
                if (attributes.containsKey(HttpServletRequest.class.getName())) {
                    HttpServletRequest request = (HttpServletRequest) attributes.get(HttpServletRequest.class.getName());
                    if (request != null) {
                        value = request.getHeader(headerName);
                        if (value != null && !value.isEmpty()) {
                            log.trace("从 HttpServletRequest 提取标签: {}={} (header={})",
                                    attributeName, value, headerName);
                        }
                    }
                }

                // 2. 如果服务端没有，尝试从客户端请求头提取（客户端场景）
                if ((value == null || value.isEmpty())
                        && attributes.containsKey(HttpRequest.class.getName())) {
                    HttpRequest request = (HttpRequest) attributes.get(HttpRequest.class.getName());
                    if (request != null && request.getHeaders() != null) {
                        value = request.getHeaders().getFirst(headerName);
                        if (value != null && !value.isEmpty()) {
                            log.trace("从 HttpRequest 提取标签: {}={} (header={})",
                                    attributeName, value, headerName);
                        }
                    }
                }

                // 3. 如果 HTTP header 没有，从 Baggage 提取（上游传播场景）
                if ((value == null || value.isEmpty()) && context != null) {
                    Baggage baggage = Baggage.fromContext(context);
                    if (baggage != null) {
                        // Baggage key 是小写的，需要规范化
                        String baggageKey = normalizeBaggageKey(headerName);
                        value = baggage.getEntryValue(baggageKey);
                        if (value != null && !value.isEmpty()) {
                            log.trace("从 Baggage 提取标签: {}={} (baggageKey={})",
                                    attributeName, value, baggageKey);
                        }
                    }
                }

                // 4. 如果找到值，添加到 Span
                if (value != null && !value.isEmpty()) {
                    AttributeKey<String> key = AttributeKey.stringKey(attributeName);
                    span.setAttribute(key, value);
                    addedCount++;
                }
            }

            log.trace("业务标签填充完成: total={}, added={}", customTags.size(), addedCount);

        } catch (Exception ex) {
            log.debug("填充业务标签失败", ex);
        }
    }

    /**
     * 规范化 Baggage key 为小写
     * <p>
     * OpenTelemetry Baggage 规范要求 key 必须是小写的。
     * 此方法与 {@link com.basebackend.observability.tracing.context.BusinessContextPropagator}
     * 中的规范化逻辑保持一致。
     * </p>
     *
     * @param headerName HTTP header 名称
     * @return 小写的 Baggage key
     */
    private String normalizeBaggageKey(String headerName) {
        return headerName.toLowerCase(Locale.ROOT);
    }
}
