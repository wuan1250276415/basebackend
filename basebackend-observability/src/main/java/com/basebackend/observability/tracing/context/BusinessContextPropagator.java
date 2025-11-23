package com.basebackend.observability.tracing.context;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.BaggageBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 业务上下文传播器
 * <p>
 * 负责在分布式系统中传播业务相关的上下文信息，如租户 ID、渠道 ID、请求 ID、用户 ID 等。
 * </p>
 * <p>
 * 实现原理：
 * <ul>
 *     <li>使用 OpenTelemetry {@link Baggage} 在 {@link Context} 中存储业务上下文</li>
 *     <li>通过 HTTP Header 在服务间传播业务上下文</li>
 *     <li>支持配置白名单，只传播允许的字段，提高安全性</li>
 *     <li>自动规范化 Baggage key 为小写（符合 OpenTelemetry 规范）</li>
 * </ul>
 * </p>
 * <p>
 * 使用场景：
 * <ul>
 *     <li>多租户系统：传播租户 ID，用于租户隔离和数据过滤</li>
 *     <li>渠道管理：传播渠道 ID，用于渠道统计和分析</li>
 *     <li>请求追踪：传播请求 ID，用于关联业务日志和追踪</li>
 *     <li>用户追踪：传播用户 ID，用于用户行为分析</li>
 * </ul>
 * </p>
 * <p>
 * 安全考虑：
 * <ul>
 *     <li>只传播白名单中的字段，避免敏感信息泄露</li>
 *     <li>不传播空值或空字符串，减少网络开销</li>
 *     <li>使用 Baggage 存储，与 Span 属性分离，避免影响追踪数据</li>
 * </ul>
 * </p>
 * <p>
 * <b>重要：</b>OpenTelemetry Baggage key 必须是小写的，本传播器会自动将 header 名转换为小写作为 Baggage key，
 * 但在注入到 carrier 时仍使用原始的 header 名（如 X-Tenant-Id）。
 * </p>
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 配置白名单
 * List<String> allowedKeys = List.of("X-Tenant-Id", "X-Channel-Id", "X-Request-Id");
 * BusinessContextPropagator propagator = new BusinessContextPropagator(allowedKeys);
 *
 * // 服务端提取上下文
 * Context context = propagator.extract(Context.current(), httpHeaders, getter);
 * Baggage baggage = Baggage.fromContext(context);
 * String tenantId = baggage.getEntryValue("x-tenant-id");  // 注意：key 是小写
 *
 * // 客户端注入上下文
 * propagator.inject(context, httpHeaders, setter);
 * }</pre>
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 * @see TextMapPropagator
 * @see Baggage
 */
public class BusinessContextPropagator implements TextMapPropagator {

    private final List<String> fields;

    /**
     * 构造函数
     *
     * @param allowedKeys 允许传播的 header 字段白名单，null 或空列表表示不传播任何字段
     */
    public BusinessContextPropagator(List<String> allowedKeys) {
        List<String> safeKeys = (allowedKeys == null) ? List.of() : new ArrayList<>(allowedKeys);
        this.fields = Collections.unmodifiableList(safeKeys);
    }

    /**
     * 获取需要传播的字段列表
     * <p>
     * 返回配置的白名单字段，这些字段会被注入到 carrier（如 HTTP Header）中。
     * </p>
     *
     * @return 不可修改的字段列表
     */
    @Override
    public List<String> fields() {
        return fields;
    }

    /**
     * 将上下文注入到 carrier
     * <p>
     * 从 {@link Context} 中读取 {@link Baggage}，将白名单中的字段注入到 carrier（如 HTTP Header）。
     * </p>
     * <p>
     * 注入规则：
     * <ul>
     *     <li>只注入白名单中的字段</li>
     *     <li>只注入非空值</li>
     *     <li>使用原始 header 名作为 carrier key（保持大小写）</li>
     *     <li>使用小写 header 名作为 Baggage key（符合 OpenTelemetry 规范）</li>
     * </ul>
     * </p>
     *
     * @param context 包含 Baggage 的 Context
     * @param carrier 目标 carrier（如 HTTP Header Map）
     * @param setter  用于设置 carrier 值的 setter
     * @param <C>     carrier 类型
     */
    @Override
    public <C> void inject(Context context, C carrier, TextMapSetter<C> setter) {
        Objects.requireNonNull(context, "context");
        if (carrier == null || setter == null || fields.isEmpty()) {
            return;
        }

        Baggage baggage = Baggage.fromContext(context);
        for (String headerName : fields) {
            // Baggage key 必须是小写
            String baggageKey = normalizeBaggageKey(headerName);
            String value = baggage.getEntryValue(baggageKey);
            if (value != null && !value.isEmpty()) {
                // 注入时使用原始 header 名（保持大小写）
                setter.set(carrier, headerName, value);
            }
        }
    }

    /**
     * 从 carrier 提取上下文
     * <p>
     * 从 carrier（如 HTTP Header）中读取白名单字段，构建 {@link Baggage} 并附加到 {@link Context}。
     * </p>
     * <p>
     * 提取规则：
     * <ul>
     *     <li>只提取白名单中的字段</li>
     *     <li>只提取非空值</li>
     *     <li>将 header 名规范化为小写作为 Baggage key</li>
     *     <li>合并现有 Baggage（而不是覆盖），保留上游传播的 Baggage</li>
     *     <li>如果没有提取到任何值，返回原 Context（避免无意义的 Context 复制）</li>
     * </ul>
     * </p>
     *
     * @param context 当前 Context（可能包含已存在的 Baggage）
     * @param carrier 源 carrier（如 HTTP Header Map）
     * @param getter  用于获取 carrier 值的 getter
     * @param <C>     carrier 类型
     * @return 包含提取的 Baggage 的新 Context，如果没有提取到任何值则返回原 Context
     */
    @Override
    public <C> Context extract(Context context, C carrier, TextMapGetter<C> getter) {
        Objects.requireNonNull(context, "context");
        if (carrier == null || getter == null || fields.isEmpty()) {
            return context;
        }

        // 从现有 Context 获取 Baggage，然后基于它构建新的 BaggageBuilder
        // 这样可以合并上游的 Baggage（如 W3C Baggage），而不是覆盖
        BaggageBuilder baggageBuilder = Baggage.fromContext(context).toBuilder();
        boolean hasValue = false;

        for (String headerName : fields) {
            String value = getter.get(carrier, headerName);
            if (value != null && !value.isEmpty()) {
                // Baggage key 必须是小写
                String baggageKey = normalizeBaggageKey(headerName);
                baggageBuilder.put(baggageKey, value);
                hasValue = true;
            }
        }

        // 如果没有提取到任何值，直接返回原 Context，避免无意义的对象创建
        if (!hasValue) {
            return context;
        }

        // 将合并后的 Baggage 附加到 Context
        return context.with(baggageBuilder.build());
    }

    /**
     * 规范化 Baggage key
     * <p>
     * OpenTelemetry Baggage key 必须符合以下规范：
     * <ul>
     *     <li>小写字母</li>
     *     <li>数字</li>
     *     <li>下划线、短横线</li>
     * </ul>
     * 本方法将 HTTP header 名转换为小写以符合 Baggage 规范。
     * </p>
     *
     * @param headerName HTTP header 名称
     * @return 规范化的 Baggage key（小写）
     */
    private String normalizeBaggageKey(String headerName) {
        return headerName.toLowerCase(Locale.ROOT);
    }
}
