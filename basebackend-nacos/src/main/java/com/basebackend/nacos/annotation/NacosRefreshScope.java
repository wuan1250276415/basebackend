package com.basebackend.nacos.annotation;

import org.springframework.cloud.context.config.annotation.RefreshScope;

import java.lang.annotation.*;

/**
 * Nacos配置刷新作用域注解
 * <p>
 * 结合Spring的@RefreshScope实现配置自动刷新功能。
 * 使用此注解的Bean会自动监听Nacos配置变更并在配置变化时重新初始化。
 * </p>
 *
 * <p>
 * 使用示例：
 * <pre>
 * &#64;Component
 * &#64;NacosRefreshScope
 * public class MyConfigBean {
 *     &#64;Value("${my.config.key:default}")
 *     private String configKey;
 *
 *     public void printConfig() {
 *         System.out.println("当前配置：" + configKey);
 *     }
 * }
 * </pre>
 * </p>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RefreshScope
public @interface NacosRefreshScope {

    /**
     * 配置名称，用于标识需要刷新的配置
     * @return 配置名称
     */
    String value() default "";
}
