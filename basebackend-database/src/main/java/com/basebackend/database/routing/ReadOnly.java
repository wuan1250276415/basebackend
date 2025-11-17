package com.basebackend.database.routing;

import java.lang.annotation.*;

/**
 * 只读数据源注解
 * 标记方法使用从库进行只读查询
 *
 * 使用场景：
 * 1. 查询操作
 * 2. 统计报表
 * 3. 对数据实时性要求不高的场景
 *
 * 注意：
 * 1. 标记了此注解的方法不应包含写操作
 * 2. 主从延迟可能导致查询到旧数据
 * 3. 如需实时数据，请使用 @MasterOnly
 *
 * @author 浮浮酱
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ReadOnly {
    /**
     * 描述信息
     */
    String value() default "";
}
