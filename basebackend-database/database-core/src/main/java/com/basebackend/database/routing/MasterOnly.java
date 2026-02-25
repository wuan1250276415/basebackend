package com.basebackend.database.routing;

import java.lang.annotation.*;

/**
 * 强制主库读取注解
 * 用于确保读取最新数据（如刚创建的记录）
 *
 * 使用场景：
 * 1. 创建后立即查询
 * 2. 对数据实时性要求高的查询
 * 3. 主从延迟期间需要最新数据的场景
 *
 * @author BaseBackend
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MasterOnly {
    /**
     * 描述信息
     */
    String value() default "";
}
