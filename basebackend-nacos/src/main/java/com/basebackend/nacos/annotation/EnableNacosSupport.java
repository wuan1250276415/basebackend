package com.basebackend.nacos.annotation;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 启用Nacos支持注解
 * <p>
 * 可替代在application.yml中配置nacos.enabled=true
 * 使用此注解后，会自动导入NacosAutoConfiguration配置类。
 * </p>
 *
 * <p>
 * 使用示例：
 * <pre>
 * &#64;Configuration
 * &#64;EnableNacosSupport(config = true, discovery = true)
 * public class NacosConfig {
 *     // 配置类内容
 * }
 * </pre>
 * </p>
 *
 * <p>
 * 注意：注解参数目前作为标记使用，实际启用还是取决于nacos.enabled配置。
 * 未来版本将完善参数控制功能。
 * </p>
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(NacosImportSelector.class)
public @interface EnableNacosSupport {

    /**
     * 是否启用配置中心
     * @return 是否启用
     */
    boolean config() default true;

    /**
     * 是否启用服务发现
     * @return 是否启用
     */
    boolean discovery() default true;
}

/**
 * 用于@ConditionalOnMissingBean的标记类
 */
class EnableNacosSupportMarker {
}
