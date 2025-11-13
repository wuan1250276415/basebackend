package com.basebackend.observability.metrics.annotations;

import java.lang.annotation.*;

/**
 * 计数器注解
 * 自动记录方法调用次数到 Micrometer Counter
 *
 * <p>使用示例：</p>
 * <pre>
 * {@code
 * @Counted(name = "user.login.attempts", description = "User login attempts")
 * public boolean login(String username, String password) {
 *     // 业务逻辑
 * }
 * }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Counted {

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
     * 是否记录失败次数
     * 如果为 true，会添加 "result" 标签（success/failure）
     */
    boolean recordFailures() default true;
}
