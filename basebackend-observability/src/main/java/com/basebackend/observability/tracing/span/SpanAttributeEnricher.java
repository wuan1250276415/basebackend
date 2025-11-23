package com.basebackend.observability.tracing.span;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Span 属性填充器聚合器
 * <p>
 * 聚合所有 {@link SpanAttributesDecorator} 实现，并按顺序应用它们来填充 Span 属性。
 * 提供统一的入口来填充 HTTP、数据库、消息队列、业务等各类属性。
 * </p>
 * <p>
 * 核心特性：
 * <ul>
 *     <li><b>异常安全</b>：单个装饰器失败不会影响其他装饰器和业务逻辑</li>
 *     <li><b>顺序执行</b>：按照装饰器注册顺序依次执行</li>
 *     <li><b>条件执行</b>：只执行支持当前 Span 的装饰器</li>
 *     <li><b>零侵入</b>：装饰失败不会抛出异常或影响请求</li>
 * </ul>
 * </p>
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 在 HTTP 服务端过滤器中使用
 * Map<String, Object> attrs = new HashMap<>();
 * attrs.put(HttpServletRequest.class.getName(), request);
 * attrs.put(HttpServletResponse.class.getName(), response);
 * enricher.enrich(span, context, attrs);
 *
 * // 在 HTTP 客户端拦截器中使用
 * Map<String, Object> attrs = new HashMap<>();
 * attrs.put(HttpRequest.class.getName(), request);
 * enricher.enrich(span, context, attrs);
 * }</pre>
 * </p>
 * <p>
 * <b>性能考虑</b>：
 * <ul>
 *     <li>装饰器按顺序同步执行，总耗时 = 所有装饰器耗时之和</li>
 *     <li>每个装饰器应该快速完成（建议 < 1ms）</li>
 *     <li>如果装饰器数量较多或耗时较长，可能需要考虑异步执行或采样</li>
 * </ul>
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 * @see SpanAttributesDecorator
 */
@Component
public class SpanAttributeEnricher {

    private static final Logger log = LoggerFactory.getLogger(SpanAttributeEnricher.class);

    private final List<SpanAttributesDecorator> decorators;

    /**
     * 构造函数
     * <p>
     * 使用 {@link ObjectProvider} 延迟解析装饰器列表，避免循环依赖。
     * </p>
     *
     * @param decoratorsProvider 装饰器列表提供者
     */
    public SpanAttributeEnricher(ObjectProvider<List<SpanAttributesDecorator>> decoratorsProvider) {
        this.decorators = decoratorsProvider.getIfAvailable(Collections::emptyList);
        log.info("Span 属性填充器已初始化: decorators={}", decorators.size());
    }

    /**
     * 填充 Span 属性（异常安全）
     * <p>
     * 遍历所有装饰器，对于每个支持当前 Span 的装饰器，调用其 {@link SpanAttributesDecorator#decorate(Span, Context, Map)} 方法。
     * </p>
     * <p>
     * <b>异常处理</b>：单个装饰器抛出的异常会被捕获并忽略，不会影响其他装饰器和业务逻辑。
     * 这是因为追踪是辅助功能，不应该因为追踪失败而影响业务。
     * </p>
     * <p>
     * <b>空值处理</b>：
     * <ul>
     *     <li>如果 span 为 null，直接返回，不执行任何装饰</li>
     *     <li>如果 decorators 为空，直接返回，不执行任何装饰</li>
     *     <li>如果 attributes 为 null，使用空 Map</li>
     * </ul>
     * </p>
     *
     * @param span       当前 Span（不能为 null）
     * @param context    当前 Context（可以为 null，但建议传入）
     * @param attributes 上下文属性（可以为 null）
     *                   <p>
     *                   常用的 attribute key：
     *                   <ul>
     *                       <li>{@code HttpServletRequest.class.getName()} - HTTP 服务端请求</li>
     *                       <li>{@code HttpServletResponse.class.getName()} - HTTP 服务端响应</li>
     *                       <li>{@code HttpRequest.class.getName()} - HTTP 客户端请求</li>
     *                       <li>{@code ClientHttpResponse.class.getName()} - HTTP 客户端响应</li>
     *                       <li>自定义 key - 业务相关的上下文对象</li>
     *                   </ul>
     *                   </p>
     */
    public void enrich(Span span, Context context, Map<String, Object> attributes) {
        // 快速失败检查
        if (span == null) {
            log.trace("Span 为 null，跳过填充");
            return;
        }

        if (decorators.isEmpty()) {
            log.trace("没有注册任何装饰器，跳过填充");
            return;
        }

        // 确保 attributes 不为 null，避免装饰器中的 NPE
        Map<String, Object> safeAttributes = (attributes == null) ? Collections.emptyMap() : attributes;

        // 确保 context 不为 null
        Context safeContext = (context == null) ? Context.current() : context;

        log.trace("开始填充 Span 属性: decorators={}, attributes={}",
                decorators.size(), safeAttributes.keySet());

        // 遍历所有装饰器
        int successCount = 0;
        int skipCount = 0;
        int errorCount = 0;

        for (SpanAttributesDecorator decorator : decorators) {
            try {
                // 检查装饰器是否支持当前 Span
                if (!decorator.supports(span, safeContext, safeAttributes)) {
                    skipCount++;
                    log.trace("装饰器不支持当前 Span，跳过: decorator={}",
                            decorator.getClass().getSimpleName());
                    continue;
                }

                // 执行装饰
                decorator.decorate(span, safeContext, safeAttributes);
                successCount++;
                log.trace("装饰器执行成功: decorator={}", decorator.getClass().getSimpleName());

            } catch (Exception ex) {
                // 异常安全：捕获并忽略装饰器异常，避免影响业务逻辑
                errorCount++;
                log.debug("装饰器执行失败（已忽略）: decorator={}, error={}",
                        decorator.getClass().getSimpleName(), ex.getMessage(), ex);
            }
        }

        log.trace("Span 属性填充完成: total={}, success={}, skip={}, error={}",
                decorators.size(), successCount, skipCount, errorCount);
    }
}
