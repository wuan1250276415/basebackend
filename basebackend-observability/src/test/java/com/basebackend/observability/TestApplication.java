package com.basebackend.observability;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Test configuration for integration tests
 * <p>
 * 这是一个测试专用的 Spring Boot 配置类，用于支持集成测试。
 * 由于 basebackend-observability 是一个库模块，没有实际的 Spring Boot 主类，
 * 因此需要这个测试配置类来启动 Spring 上下文。
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan("com.basebackend.observability")
public class TestApplication {
    // 无需额外配置，Spring Boot 会自动扫描并加载所有组件
}
