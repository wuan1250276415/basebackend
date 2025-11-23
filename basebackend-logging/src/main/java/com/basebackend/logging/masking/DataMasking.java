package com.basebackend.logging.masking;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注方法的返回值需要被脱敏处理
 *
 * 用于标识需要进行PII（个人信息）脱敏的方法，
 * 通过AOP拦截自动处理敏感信息，确保日志安全。
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataMasking {

    /**
     * 是否启用脱敏（覆盖全局配置）
     *
     * @return true=启用，false=禁用
     */
    boolean enabled() default true;

    /**
     * 指定要应用的脱敏规则名称
     * 空数组表示使用全局激活规则
     *
     * @return 规则名称数组
     */
    String[] ruleNames() default {};

    /**
     * 结果为null或空时是否跳过脱敏
     *
     * @return true=跳过，false=处理
     */
    boolean skipNull() default true;
}
