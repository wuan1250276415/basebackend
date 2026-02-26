package com.basebackend.observability.tracing.span;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;

import java.util.Map;

/**
 * Span 属性装饰器接口
 * <p>
 * 定义了装饰器的契约，用于向 Span 添加属性。实现类可以根据不同的场景
 * （HTTP、数据库、消息队列、业务标签等）添加相应的属性。
 * </p>
 * <p>
 * 装饰器模式的优势：
 * <ul>
 *     <li>解耦：每个装饰器独立负责一类属性</li>
 *     <li>可扩展：添加新的装饰器无需修改现有代码</li>
 *     <li>可测试：每个装饰器可以独立测试</li>
 *     <li>可配置：可以通过配置启用/禁用特定装饰器</li>
 * </ul>
 * </p>
 * <p>
 * 实现示例：
 * <pre>{@code
 * public class MyDecorator implements SpanAttributesDecorator {
 *     @Override
 *     public boolean supports(Span span, Context context, Map<String, Object> attributes) {
 *         return attributes.containsKey("myKey");
 *     }
 *
 *     @Override
 *     public void decorate(Span span, Context context, Map<String, Object> attributes) {
 *         MyObject obj = (MyObject) attributes.get("myKey");
 *         span.setAttribute("my.attribute", obj.getValue());
 *     }
 * }
 * }</pre>
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 * @see SpanAttributeEnricher
 */
public interface SpanAttributesDecorator {

    /**
     * 判断此装饰器是否支持当前 Span
     * <p>
     * 此方法用于快速判断装饰器是否应该执行。装饰器可以根据：
     * <ul>
     *     <li>Span 类型（SERVER, CLIENT, INTERNAL 等）</li>
     *     <li>Context 中的数据</li>
     *     <li>attributes 中的对象类型</li>
     * </ul>
     * 来决定是否执行装饰逻辑。
     * </p>
     * <p>
     * <b>性能考虑</b>：此方法会被频繁调用，应该尽可能快速地返回结果。
     * 避免在此方法中执行复杂的逻辑或 I/O 操作。
     * </p>
     *
     * @param span       当前 Span
     * @param context    当前 Context（包含 Baggage、追踪上下文等）
     * @param attributes 上下文属性（如 HttpServletRequest、HttpRequest 等）
     * @return true 表示支持，false 表示不支持
     */
    boolean supports(Span span, Context context, Map<String, Object> attributes);

    /**
     * 装饰 Span，添加属性
     * <p>
     * 仅在 {@link #supports(Span, Context, Map)} 返回 true 时调用。
     * </p>
     * <p>
     * <b>异常安全</b>：实现类必须保证异常安全，即使装饰失败也不应该影响业务逻辑。
     * 建议在方法内部捕获所有异常，并记录日志（如果需要）。
     * </p>
     * <p>
     * <b>性能考虑</b>：装饰操作应该快速完成，避免阻塞请求。如果需要执行耗时操作
     * （如查询数据库、调用外部服务），应该考虑异步执行或缓存结果。
     * </p>
     * <p>
     * <b>属性命名规范</b>：建议遵循 OpenTelemetry 语义约定：
     * <ul>
     *     <li>HTTP：http.method, http.url, http.status_code 等</li>
     *     <li>数据库：db.system, db.name, db.statement 等</li>
     *     <li>消息队列：messaging.system, messaging.destination 等</li>
     *     <li>业务属性：使用点分隔的小写命名，如 user.id, tenant.id</li>
     * </ul>
     * </p>
     *
     * @param span       当前 Span
     * @param context    当前 Context
     * @param attributes 上下文属性
     */
    void decorate(Span span, Context context, Map<String, Object> attributes);
}
