package com.basebackend.feign.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import feign.Request;
import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.concurrent.TimeUnit;

/**
 * 文件服务 Feign 客户端配置
 * <p>
 * 专门用于支持 Multipart 文件上传的编码器配置。
 * 通过 FeignClient 的 configuration 属性指定时,
 * 此配置类会被隔离加载,不会影响其他 FeignClient。
 * </p>
 *
 * @author Claude Code
 * @since 2025-01-07
 */
@Configuration
public class FileFeignConfig {

    /**
     * 配置 Multipart 表单编码器
     * <p>
     * 使用 SpringFormEncoder 包装 SpringEncoder,
     * 以支持 MultipartFile 类型参数的编码。
     * </p>
     *
     * @param messageConverters Spring HTTP 消息转换器工厂
     * @return Feign 编码器
     */
    @Bean
    public Encoder multipartFormEncoder(ObjectFactory<HttpMessageConverters> messageConverters) {
        return new SpringFormEncoder(new SpringEncoder(messageConverters));
    }

    /**
     * 配置文件上传超时时间
     * <p>
     * 文件上传操作通常需要较长时间,特别是大文件上传。
     * 设置较长的连接超时和读取超时,避免上传过程中超时。
     * </p>
     *
     * @return Feign 请求配置
     */
    @Bean
    public Request.Options feignOptions() {
        // 连接超时 30秒, 读取超时 5分钟
        return new Request.Options(
                30, TimeUnit.SECONDS,
                300, TimeUnit.SECONDS,
                true
        );
    }

}
