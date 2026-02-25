package com.basebackend.observability.metrics.annotations;

import java.lang.annotation.*;

/**
 * 性能计时注解
 * 自动记录方法执行时间到 Micrometer Timer
 *
 * <p>使用示例：</p>
 * <pre>
 * {@code
 * @Timed(name = "user.registration", description = "User registration time")
 * public void registerUser(UserDTO user) {
 *     // 业务逻辑
 * }
 * }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Timed {

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

    /**
     * 是否启用百分位统计
     * 启用后会统计 P50, P90, P95, P99 等
     */
    boolean percentiles() default true;

    /**
     * 是否启用直方图
     */
    boolean histogram() default false;
}
