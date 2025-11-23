package com.basebackend.scheduler.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Controller 集成测试配置
 *
 * <p>为 Controller 层集成测试提供必要的配置。
 * 包括 JSON 序列化配置、Mock 服务等。
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@TestConfiguration
@EnableWebMvc
public class ControllerTestConfig {

    /**
     * ObjectMapper 配置
     *
     * <p>用于 JSON 序列化和反序列化测试数据。
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    /**
     * HTTP 消息转换器配置
     */
    @Bean
    @Primary
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
        MappingJackson2HttpMessageConverter converter =
                new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper());
        return converter;
    }
}
