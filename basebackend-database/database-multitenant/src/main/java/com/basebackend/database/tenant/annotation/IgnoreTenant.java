package com.basebackend.database.tenant.annotation;

import java.lang.annotation.*;

/**
 * 忽略租户过滤注解
 * <p>
 * 标注在方法或类上，跳过租户自动过滤，适用于需要跨租户查询的场景。
 *
 * <pre>
 * &#64;IgnoreTenant
 * public List&lt;User&gt; listAllTenantUsers() {
 *     // 不会自动添加 tenant_id 过滤条件
 * }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IgnoreTenant {
}
