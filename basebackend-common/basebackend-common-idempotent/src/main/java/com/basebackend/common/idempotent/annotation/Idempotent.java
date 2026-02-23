package com.basebackend.common.idempotent.annotation;

import com.basebackend.common.idempotent.enums.IdempotentScene;
import com.basebackend.common.idempotent.enums.IdempotentStrategy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 幂等性注解
 * <p>
 * 标注在 Controller 方法上，防止重复提交。
 * 支持 TOKEN、PARAM、SPEL 三种策略。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // TOKEN 策略：前端在 Header 中携带 X-Idempotent-Token
 * @Idempotent(strategy = IdempotentStrategy.TOKEN)
 * @PostMapping("/order")
 * public Result createOrder(@RequestBody OrderDTO dto) { ... }
 *
 * // PARAM 策略：基于请求参数 MD5 + 用户ID
 * @Idempotent(strategy = IdempotentStrategy.PARAM, timeout = 10)
 * @PostMapping("/submit")
 * public Result submit(@RequestBody SubmitDTO dto) { ... }
 *
 * // SPEL 策略：基于 SpEL 表达式
 * @Idempotent(strategy = IdempotentStrategy.SPEL, key = "#dto.orderNo")
 * @PostMapping("/pay")
 * public Result pay(@RequestBody PayDTO dto) { ... }
 * }</pre>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /**
     * 幂等 key（支持 SpEL，默认空=自动生成）
     */
    String key() default "";

    /**
     * 幂等窗口时间
     */
    long timeout() default 5;

    /**
     * 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 重复提交时的错误消息
     */
    String message() default "请勿重复提交";

    /**
     * 幂等策略
     */
    IdempotentStrategy strategy() default IdempotentStrategy.PARAM;

    /**
     * 幂等场景
     */
    IdempotentScene scene() default IdempotentScene.API;
}
