package com.basebackend.observability.tracing.sampler;

import com.basebackend.observability.tracing.config.TracingProperties;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * 基于 URL/方法/用户规则的采样器（head-sampling）
 * <p>
 * 规则按顺序匹配，第一个命中生效；否则走默认/动态采样率。
 * 符合 OpenTelemetry head-sampling 规范，在 Span 创建前做采样决策。
 * </p>
 * <p>
 * 核心特性：
 * <ul>
 *     <li><b>规则匹配</b>：支持 URL 模式、HTTP 方法、用户 ID 模式的组合匹配</li>
 *     <li><b>父级尊重</b>：已采样的父 Span 强制采样，未采样的父 Span 直接丢弃（保持 trace 一致性）</li>
 *     <li><b>性能优化</b>：正则表达式预编译、采样器缓存</li>
 *     <li><b>动态支持</b>：支持动态采样率提供者</li>
 * </ul>
 * </p>
 * <p>
 * 规则示例：
 * <pre>
 * observability:
 *   tracing:
 *     sampler:
 *       rules:
 *         - url-pattern: "/api/auth/.*"    # 认证 API 100% 采样
 *           rate: 1.0
 *         - http-method: GET               # GET 请求报表 50% 采样
 *           url-pattern: "/api/reports/.*"
 *           rate: 0.5
 *         - user-id-pattern: "admin-.*"    # admin 用户 100% 采样
 *           rate: 1.0
 * </pre>
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 * @see <a href="https://opentelemetry.io/docs/specs/otel/trace/sdk/#sampler">OpenTelemetry Sampler Specification</a>
 */
public final class RuleBasedSampler implements Sampler {

    // OpenTelemetry 语义约定属性键
    private static final AttributeKey<String> HTTP_METHOD = AttributeKey.stringKey("http.method");
    private static final AttributeKey<String> HTTP_ROUTE = AttributeKey.stringKey("http.route");
    private static final AttributeKey<String> HTTP_TARGET = AttributeKey.stringKey("http.target");
    private static final AttributeKey<String> HTTP_URL = AttributeKey.stringKey("http.url");
    private static final AttributeKey<String> USER_ID = AttributeKey.stringKey("user.id");
    private static final AttributeKey<String> ENDUSER_ID = AttributeKey.stringKey("enduser.id");

    private final List<CompiledRule> compiledRules;
    private final Supplier<Sampler> defaultSamplerSupplier;
    private final Map<Double, Sampler> ratioSamplers = new ConcurrentHashMap<>();

    /**
     * 构造函数
     *
     * @param samplerProps            采样配置属性
     * @param defaultSamplerSupplier  默认采样器提供者（可能是动态的）
     */
    public RuleBasedSampler(TracingProperties.Sampler samplerProps, Supplier<Sampler> defaultSamplerSupplier) {
        this.defaultSamplerSupplier = Objects.requireNonNull(defaultSamplerSupplier, "defaultSamplerSupplier");
        this.compiledRules = compileRules(samplerProps.getRules());
        // 预热默认采样率，避免首次请求时创建 Sampler
        ratioSamplers.computeIfAbsent(samplerProps.getDefaultRate(), Sampler::traceIdRatioBased);
    }

    @Override
    public SamplingResult shouldSample(Context parentContext, String traceId, String name, SpanKind spanKind,
                                       Attributes attributes, List<LinkData> parentLinks) {
        // 尊重父级采样决策：与 ParentBasedSampler 等效
        SpanContext parent = Span.fromContext(parentContext).getSpanContext();
        if (parent.isValid()) {
            // 已采样父级强制采样，未采样父级直接丢弃（保持 trace 一致性）
            return parent.isSampled() ? SamplingResult.recordAndSample() : SamplingResult.drop();
        }

        // 查找匹配的规则
        CompiledRule rule = findMatchingRule(name, attributes);
        Sampler sampler = (rule != null) ? rule.sampler : defaultSamplerSupplier.get();
        return sampler.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
    }

    @Override
    public String getDescription() {
        return "RuleBasedSampler{rules=" + compiledRules.size() + "}";
    }

    /**
     * 查找第一个匹配的规则
     *
     * @param name       Span 名称
     * @param attributes Span 属性
     * @return 匹配的规则，如果没有匹配则返回 null
     */
    private CompiledRule findMatchingRule(String name, Attributes attributes) {
        String method = attributes.get(HTTP_METHOD);
        String path = resolvePath(attributes, name);
        String userId = resolveUserId(attributes);
        for (CompiledRule rule : compiledRules) {
            if (rule.matches(method, path, userId)) {
                return rule;
            }
        }
        return null;
    }

    /**
     * 解析 HTTP 路径
     * <p>
     * 优先级：http.route > http.target > http.url > Span 名称兜底
     * </p>
     *
     * @param attributes Span 属性
     * @param name       Span 名称
     * @return HTTP 路径，如果无法解析则返回 null
     */
    private String resolvePath(Attributes attributes, String name) {
        // 优先使用 http.route（路由模式，如 /api/users/{id}）
        String route = attributes.get(HTTP_ROUTE);
        if (route != null && !route.isEmpty()) {
            return route;
        }
        // 其次使用 http.target（路径+查询参数）
        String target = attributes.get(HTTP_TARGET);
        if (target != null && !target.isEmpty()) {
            return target;
        }
        // 再次使用 http.url（完整 URL）
        String url = attributes.get(HTTP_URL);
        if (url != null && !url.isEmpty()) {
            return url;
        }
        // 最后从 Span 名称兜底：HTTP GET /path
        if (name != null && name.startsWith("HTTP ")) {
            int idx = name.indexOf(' ', 5);
            if (idx > 0 && idx < name.length() - 1) {
                return name.substring(idx + 1);
            }
        }
        return null;
    }

    /**
     * 解析用户 ID
     * <p>
     * 优先级：user.id > enduser.id
     * </p>
     *
     * @param attributes Span 属性
     * @return 用户 ID，如果无法解析则返回 null
     */
    private String resolveUserId(Attributes attributes) {
        String userId = attributes.get(USER_ID);
        if (userId != null && !userId.isEmpty()) {
            return userId;
        }
        String endUserId = attributes.get(ENDUSER_ID);
        if (endUserId != null && !endUserId.isEmpty()) {
            return endUserId;
        }
        return null;
    }

    /**
     * 编译采样规则
     * <p>
     * 将配置的规则转换为可快速匹配的编译规则，包括：
     * <ul>
     *     <li>正则表达式预编译</li>
     *     <li>采样器缓存（相同采样率共享采样器实例）</li>
     * </ul>
     * </p>
     *
     * @param rules 配置的采样规则列表
     * @return 编译后的规则列表（不可修改）
     */
    private List<CompiledRule> compileRules(List<TracingProperties.Sampler.SamplingRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return List.of();
        }
        List<CompiledRule> compiled = new ArrayList<>();
        for (TracingProperties.Sampler.SamplingRule rule : rules) {
            if (rule == null) {
                continue;
            }
            // 编译 URL 模式
            Pattern urlPattern = (rule.getUrlPattern() == null || rule.getUrlPattern().isBlank())
                    ? null
                    : Pattern.compile(rule.getUrlPattern());
            // 编译用户 ID 模式
            Pattern userPattern = (rule.getUserIdPattern() == null || rule.getUserIdPattern().isBlank())
                    ? null
                    : Pattern.compile(rule.getUserIdPattern());
            // 规范化 HTTP 方法为大写
            String method = (rule.getHttpMethod() == null) ? null : rule.getHttpMethod().toUpperCase(Locale.ROOT);
            // 限制采样率在 [0.0, 1.0] 范围内
            double rate = Math.min(1.0d, Math.max(0.0d, rule.getRate()));
            // 缓存采样器（相同采样率共享实例）
            Sampler sampler = ratioSamplers.computeIfAbsent(rate, Sampler::traceIdRatioBased);
            compiled.add(new CompiledRule(urlPattern, userPattern, method, sampler));
        }
        return Collections.unmodifiableList(compiled);
    }

    /**
     * 编译后的规则
     * <p>
     * 内部类，用于快速匹配和采样决策。
     * </p>
     */
    private static final class CompiledRule {
        private final Pattern urlPattern;
        private final Pattern userPattern;
        private final String httpMethod;
        private final Sampler sampler;

        private CompiledRule(Pattern urlPattern, Pattern userPattern, String httpMethod, Sampler sampler) {
            this.urlPattern = urlPattern;
            this.userPattern = userPattern;
            this.httpMethod = httpMethod;
            this.sampler = sampler;
        }

        /**
         * 检查是否匹配此规则
         * <p>
         * 所有配置的条件都必须匹配才返回 true。
         * </p>
         *
         * @param method HTTP 方法
         * @param path   HTTP 路径
         * @param userId 用户 ID
         * @return true 如果匹配，false 否则
         */
        private boolean matches(String method, String path, String userId) {
            // 检查 HTTP 方法
            if (httpMethod != null && (method == null || !httpMethod.equalsIgnoreCase(method))) {
                return false;
            }
            // 检查 URL 模式
            if (urlPattern != null && (path == null || !urlPattern.matcher(path).matches())) {
                return false;
            }
            // 检查用户 ID 模式
            if (userPattern != null && (userId == null || !userPattern.matcher(userId).matches())) {
                return false;
            }
            return true;
        }
    }
}
