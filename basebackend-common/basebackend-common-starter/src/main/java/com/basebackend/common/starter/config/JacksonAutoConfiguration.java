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
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * Jackson 自动配置
 * <p>
 * 统一配置 JSON 序列化/反序列化规则:
 * <ul>
 *   <li>日期时间格式化 (支持 LocalDateTime、LocalDate、LocalTime)</li>
 *   <li>Long 类型转 String (避免前端精度丢失)</li>
 *   <li>空值处理 (可配置是否包含 null 字段)</li>
 *   <li>下划线/驼峰命名转换 (可选)</li>
 *   <li>未知属性处理 (忽略未知字段)</li>
 * </ul>
 * </p>
 *
 * <h3>配置示例：</h3>
 * <pre>
 * basebackend:
 *   common:
 *     jackson:
 *       enabled: true
 *       date-format: yyyy-MM-dd HH:mm:ss
 *       time-zone: GMT+8
 *       include-nulls: false
 *       snake-case-enabled: false
 *       fail-on-unknown-properties: false
 * </pre>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@RequiredArgsConstructor
@ConditionalOnClass(ObjectMapper.class)
@ConditionalOnProperty(prefix = "basebackend.common.jackson", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JacksonAutoConfiguration {

    private final CommonProperties commonProperties;

    /**
     * 自定义 Jackson 配置
     * <p>
     * 通过 {@link Jackson2ObjectMapperBuilderCustomizer} 定制序列化规则。
     * </p>
     *
     * @return Jackson 配置定制器
     */
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

            // 配置未知属性处理
            builder.featuresToEnable(
                    // 允许单引号
                    com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_SINGLE_QUOTES,
                    // 允许未引用的字段名
                    com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES
            );

            builder.featuresToDisable(
                    // 禁用将日期写为时间戳
                    SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                    // 禁用在空对象时失败
                    SerializationFeature.FAIL_ON_EMPTY_BEANS
            );

            if (!jackson.getFailOnUnknownProperties()) {
                builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            }

            // 注册自定义模块
            builder.modules(javaTimeModule(), longToStringModule());
        };
    }

    /**
     * Java 8 时间模块
     * <p>
     * 配置 LocalDateTime、LocalDate、LocalTime 的序列化/反序列化格式。
     * </p>
     *
     * @return Java 时间模块
     */
    private JavaTimeModule javaTimeModule() {
        CommonProperties.JacksonProperties jackson = commonProperties.getJackson();
        String dateFormat = jackson.getDateFormat();

        // 解析日期和时间格式
        String datePattern = "yyyy-MM-dd";
        String timePattern = "HH:mm:ss";
        String dateTimePattern = dateFormat != null ? dateFormat : "yyyy-MM-dd HH:mm:ss";

        JavaTimeModule module = new JavaTimeModule();

        // LocalDateTime 序列化/反序列化
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimePattern);
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));

        // LocalDate 序列化/反序列化
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(datePattern);
        module.addSerializer(LocalDate.class, new LocalDateSerializer(dateFormatter));
        module.addDeserializer(LocalDate.class, new LocalDateDeserializer(dateFormatter));

        // LocalTime 序列化/反序列化
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(timePattern);
        module.addSerializer(LocalTime.class, new LocalTimeSerializer(timeFormatter));
        module.addDeserializer(LocalTime.class, new LocalTimeDeserializer(timeFormatter));

        return module;
    }

    /**
     * Long 转 String 模块
     * <p>
     * 将 Long、BigInteger 类型序列化为字符串,避免前端 JavaScript 精度丢失。
     * JavaScript 的 Number 类型最大安全整数为 2^53 - 1,超过此值会丢失精度。
     * </p>
     *
     * @return Long 转 String 模块
     */
    private SimpleModule longToStringModule() {
        SimpleModule module = new SimpleModule();

        // Long 转 String
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);

        // BigInteger 转 String
        module.addSerializer(BigInteger.class, ToStringSerializer.instance);

        return module;
    }
}
