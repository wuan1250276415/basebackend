package com.basebackend.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * SQL注入安全验证注解
 * <p>
 * 用于验证字符串是否包含SQL注入攻击载荷。
 * </p>
 *
 * <h3>使用示例：</h3>
 * 
 * <pre>
 * {
 *     &#64;code
 *     public class QueryRequest {
 *         &#64;NoSqlInjection
 *         private String keyword;
 *
 *         @NoSqlInjection(message = "排序字段包含非法字符")
 *         private String orderBy;
 *     }
 * }
 * </pre>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Documented
@Constraint(validatedBy = NoSqlInjectionValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface NoSqlInjection {

    /**
     * 错误消息
     */
    String message() default "输入包含潜在的SQL注入攻击载荷";

    /**
     * 验证分组
     */
    Class<?>[] groups() default {};

    /**
     * 负载
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * 是否启用严格模式
     * <p>
     * 严格模式下会检测更多的SQL关键字和模式
     * </p>
     */
    boolean strict() default false;
}
