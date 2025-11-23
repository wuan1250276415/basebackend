package com.basebackend.logging.audit.crypto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * 加密专用对象映射器
 *
 * 为哈希链和数字签名提供稳定的序列化机制，
 * 确保对象序列化的确定性。
 *
 * @author basebackend team
 * @since 2025-11-22
 */
public final class CryptoObjectMapper {

    /**
     * 共享实例
     */
    public static final ObjectMapper INSTANCE = createInstance();

    private CryptoObjectMapper() {
        // 工具类，私有构造器
    }

    /**
     * 创建专用对象映射器
     */
    private static ObjectMapper createInstance() {
        ObjectMapper mapper = new ObjectMapper();

        // 注册 Java 8 时间模块
        mapper.registerModule(new JavaTimeModule());

        // 按键排序，确保序列化稳定性
        mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

        // 禁用空值属性序列化
        // mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // 禁用缩进，提高性能
        mapper.configure(SerializationFeature.INDENT_OUTPUT, false);

        // 日期时间格式
        // mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        return mapper;
    }

    /**
     * 创建新的实例（避免共享状态）
     */
    public static ObjectMapper newInstance() {
        return createInstance();
    }
}
