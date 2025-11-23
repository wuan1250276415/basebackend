package com.basebackend.nacos.example;

import com.alibaba.nacos.api.config.ConfigService;
import com.basebackend.nacos.annotation.EnableNacosSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * 注解驱动示例
 * <p>
 * 展示如何使用 @EnableNacosSupport 和 @NacosRefreshScope 注解
 * </p>
 *
 * <pre>
 * 使用步骤：
 * 1. 在启动类或配置类上添加 @EnableNacosSupport 注解
 * 2. 在需要刷新配置的 Bean 上添加 @NacosRefreshScope 注解
 * </pre>
 *
 * <pre>
 * 示例启动类：
 * &#64;SpringBootApplication
 * &#64;EnableNacosSupport(config = true, discovery = true)
 * public class MyApplication {
 *     public static void main(String[] args) {
 *         SpringApplication.run(MyApplication.class, args);
 *     }
 * }
 * </pre>
 */
@Slf4j
@Component
public class AnnotationDrivenExample {

    private final ConfigService configService;

    // 使用 @NacosRefreshScope 注解实现配置自动刷新
    // 当配置变更时，这个 Bean 会自动重新初始化
    // @NacosRefreshScope
    // public static class RefreshableConfig {
    //     @Value("${my.config.key:default}")
    //     private String configKey;
    //
    //     public void printConfig() {
    //         System.out.println("当前配置：" + configKey);
    //     }
    // }

    public AnnotationDrivenExample(ConfigService configService) {
        this.configService = configService;
    }

    @PostConstruct
    public void init() {
        log.info("=== 注解驱动示例 ===");

        // 示例1: 使用 @EnableNacosSupport 注解启用 Nacos
        useEnableNacosSupportAnnotation();

        // 示例2: 配置刷新 Bean 示例
        configRefreshExample();
    }

    /**
     * 示例1: @EnableNacosSupport 注解使用
     *
     * 使用 @EnableNacosSupport 注解可以替代在 application.yml 中配置 nacos.enabled=true
     * 这个注解会触发 NacosAutoConfiguration 的加载，自动配置 Nacos 相关 Bean。
     *
     * 配置参数：
     * - config: 是否启用配置中心，默认为 true
     * - discovery: 是否启用服务发现，默认为 true
     */
    private void useEnableNacosSupportAnnotation() {
        log.info("\n--- 示例1: @EnableNacosSupport 注解 ---");
        log.info("通过 @EnableNacosSupport 注解可以自动加载 Nacos 配置");
        log.info("支持的参数：");
        log.info("  - config: 是否启用配置中心 (默认: true)");
        log.info("  - discovery: 是否启用服务发现 (默认: true)");
    }

    /**
     * 示例2: 配置刷新 Bean 示例
     *
     * @NacosRefreshScope 注解结合 Spring 的 @RefreshScope 实现配置自动刷新。
     * 使用此注解的 Bean 会在配置变更时自动重新初始化，从而获取最新的配置值。
     */
    private void configRefreshExample() {
        log.info("\n--- 示例2: @NacosRefreshScope 配置刷新 ---");
        log.info("@NacosRefreshScope 注解示例：");
        log.info("```java");
        log.info("@Component");
        log.info("@NacosRefreshScope");
        log.info("public class MyConfigBean {");
        log.info("    @Value(\"\\${my.config.key:default}\")");
        log.info("    private String configKey;");
        log.info("");
        log.info("    public void printConfig() {");
        log.info("        System.out.println(\"当前配置：\" + configKey);");
        log.info("    }");
        log.info("}");
        log.info("```");

        // 实际使用示例
        try {
            // 获取配置
            String config = configService.getConfig("my-config.yml", "DEFAULT_GROUP", 5000);
            log.info("当前配置内容：{}", config);
        } catch (Exception e) {
            log.error("获取配置失败", e);
        }
    }
}

/**
 * 配置示例：
 *
 * <pre>
 * # application.yml
 * nacos:
 *   config:
 *     enabled: true
 *     server-addr: 127.0.0.1:8848
 *     shared-configs:
 *       - data-id: my-config.yml
 *         refresh: true
 * </pre>
 *
 * 启动类示例：
 *
 * <pre>
 * &#64;SpringBootApplication
 * &#64;EnableNacosSupport(config = true, discovery = true)
 * public class MyApplication {
 *     public static void main(String[] args) {
 *         SpringApplication.run(MyApplication.class, args);
 *     }
 * }
 * </pre>
 */
