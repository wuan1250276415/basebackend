package com.basebackend.observability.slo.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要进行 SLO 监控的方法
 * <p>
 * 通过 AOP 切面自动采集 SLI 指标，包括：
 * <ul>
 *     <li>请求总数（sli_requests_total, outcome=total）</li>
 *     <li>成功请求数（sli_requests_total, outcome=success）</li>
 *     <li>错误请求数（sli_requests_total, outcome=error）</li>
 *     <li>请求延迟（sli_latency）</li>
 * </ul>
 * </p>
 * <p>
 * 使用示例：
 * <pre>{@code
 * @SloMonitored(sloName = "user-api-availability")
 * public UserDto getUserById(Long id) {
 *     return userService.findById(id);
 * }
 *
 * @SloMonitored(
 *     sloName = "order-processing",
 *     service = "order-service",
 *     recordLatency = true,
 *     recordSuccess = true,
 *     recordError = true
 * )
 * public Order processOrder(OrderRequest request) {
 *     return orderService.process(request);
 * }
 * }</pre>
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 * @see com.basebackend.observability.slo.aspect.SloMonitoringAspect
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SloMonitored {

    /**
     * SLO 名称
     * <p>
     * 用于标识该方法关联的 SLO。如果为空，则使用 "类名.方法名" 作为默认值。
     * </p>
     * <p>
     * 示例：
     * <ul>
     *     <li>"user-api-availability" - 用户 API 可用性</li>
     *     <li>"payment-latency-p95" - 支付延迟 P95</li>
     *     <li>"order-error-rate" - 订单错误率</li>
     * </ul>
     * </p>
     *
     * @return SLO 名称
     */
    String sloName() default "";

    /**
     * 服务名称
     * <p>
     * 用于标识服务。默认使用 {@code ${spring.application.name}} 占位符，
     * 在运行时会被替换为实际的应用名称。
     * </p>
     * <p>
     * 可以显式指定服务名，例如 "order-service"、"user-service" 等。
     * </p>
     *
     * @return 服务名称
     */
    String service() default "${spring.application.name}";

    /**
     * 是否记录延迟
     * <p>
     * 启用时，会使用 Micrometer Timer 记录方法执行时间，用于计算延迟相关的 SLI（如 P50/P95/P99）。
     * </p>
     * <p>
     * 指标名：{@code sli_latency}
     * <br>
     * 标签：service, method, slo
     * </p>
     *
     * @return true 表示记录延迟，false 表示不记录
     */
    boolean recordLatency() default true;

    /**
     * 是否记录成功计数
     * <p>
     * 启用时，方法正常返回（无异常）会增加成功计数器，用于计算可用性 SLI。
     * </p>
     * <p>
     * 指标名：{@code sli_requests_total}
     * <br>
     * 标签：service, method, slo, outcome=success
     * </p>
     *
     * @return true 表示记录成功，false 表示不记录
     */
    boolean recordSuccess() default true;

    /**
     * 是否记录错误计数
     * <p>
     * 启用时，方法抛出异常会增加错误计数器，用于计算错误率 SLI。
     * </p>
     * <p>
     * 指标名：{@code sli_requests_total}
     * <br>
     * 标签：service, method, slo, outcome=error
     * </p>
     *
     * @return true 表示记录错误，false 表示不记录
     */
    boolean recordError() default true;
}
