package com.basebackend.observability.metrics.annotations;

import java.lang.annotation.*;

/**
 * 速率计量注解
 * 记录方法调用的速率（每秒调用次数、平均响应时间等）
 *
 * <p>使用示例：</p>
 * <pre>
 * {@code
 * @Metered(name = "order.processing.rate", description = "Order processing rate")
 * public void processOrder(Order order) {
 *     // 业务逻辑
 * }
 * }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Metered {

    /**
     * 指标名称
     * 如果不指定，将使用方法全限定名
     */
    String name() default "";

    /**
     * 指标描述
     */
    String description() default "";

    /**
     * 额外的标签
     * 格式：["key1", "value1", "key2", "value2", ...]
     */
    String[] tags() default {};
}
