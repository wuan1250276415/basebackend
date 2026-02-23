package com.basebackend.common.starter.config;

import com.basebackend.common.starter.properties.CommonProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * Jackson 自动配置
 * <p>
 * 统一配置 JSON 序列化/反序列化规则
 * </p>
 */
@Slf4j
@AutoConfiguration
@RequiredArgsConstructor
@ConditionalOnClass(ObjectMapper.class)
@ConditionalOnProperty(prefix = "basebackend.common.jackson", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JacksonAutoConfiguration {

    private final CommonProperties commonProperties;

    @Bean
    @ConditionalOnMissingBean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        CommonProperties.JacksonProperties jackson = commonProperties.getJackson();

        log.info("Initializing Jackson auto-configuration with dateFormat={}, timeZone={}, includeNulls={}, snakeCaseEnabled={}",
                jackson.getDateFormat(), jackson.getTimeZone(), jackson.getIncludeNulls(), jackson.getSnakeCaseEnabled());

        return builder -> {
            // 设置时区
            builder.timeZone(TimeZone.getTimeZone(jackson.getTimeZone()));

            // 配置空值处理
            if (!jackson.getIncludeNulls()) {
                builder.serializationInclusion(JsonInclude.Include.NON_NULL);
            }

            // 配置驼峰/下划线转换
            if (jackson.getSnakeCaseEnabled()) {
                builder.propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
            }

            // 禁用特性
            builder.featuresToDisable(
                    SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                    SerializationFeature.FAIL_ON_EMPTY_BEANS
            );

            if (!jackson.getFailOnUnknownProperties()) {
                builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            }

            // 注册自定义模块
            builder.modules(javaTimeModule(), longToStringModule());
        };
    }

    private JavaTimeModule javaTimeModule() {
        CommonProperties.JacksonProperties jackson = commonProperties.getJackson();
        String dateFormat = jackson.getDateFormat();

        String datePattern = "yyyy-MM-dd";
        String timePattern = "HH:mm:ss";
        String dateTimePattern = dateFormat != null ? dateFormat : "yyyy-MM-dd HH:mm:ss";

        JavaTimeModule module = new JavaTimeModule();

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimePattern);
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(datePattern);
        module.addSerializer(LocalDate.class, new LocalDateSerializer(dateFormatter));
        module.addDeserializer(LocalDate.class, new LocalDateDeserializer(dateFormatter));

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(timePattern);
        module.addSerializer(LocalTime.class, new LocalTimeSerializer(timeFormatter));
        module.addDeserializer(LocalTime.class, new LocalTimeDeserializer(timeFormatter));

        return module;
    }

    private SimpleModule longToStringModule() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        module.addSerializer(BigInteger.class, ToStringSerializer.instance);
        return module;
    }
}
