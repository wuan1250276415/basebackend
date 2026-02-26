package com.basebackend.ai.annotation;

import java.lang.annotation.*;

/**
 * AI 生成注解
 * <p>
 * 标注在方法上，自动调用 AI 生成内容并返回。
 * 方法参数会作为模板变量注入 Prompt。
 *
 * <pre>
 * &#64;AIGenerate(prompt = "将以下文本翻译为{{targetLang}}: {{text}}")
 * public String translate(String text, String targetLang) {
 *     return null; // 由切面填充返回值
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AIGenerate {

    /** Prompt 模板（支持 {{variable}} 占位符）或模板名称 */
    String prompt();

    /** 指定 Provider（空 = 使用默认） */
    String provider() default "";

    /** 指定模型（空 = 使用 Provider 默认模型） */
    String model() default "";

    /** 温度参数 */
    double temperature() default 0.7;

    /** 最大 Token 数（0 = 不限制） */
    int maxTokens() default 0;
}
