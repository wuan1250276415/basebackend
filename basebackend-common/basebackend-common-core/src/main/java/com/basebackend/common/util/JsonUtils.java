package com.basebackend.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

/**
 * JSON 工具类（基于 Jackson）
 * <p>
 * 替代 fastjson2 的 JSON.toJSONString() / JSON.parseObject() 等静态方法，
 * 提供统一的 JSON 序列化/反序列化入口。
 * </p>
 *
 * @author BaseBackend
 */
@Slf4j
public final class JsonUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
        OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        OBJECT_MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    private JsonUtils() {
    }

    /**
     * 获取共享的 ObjectMapper 实例
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    /**
     * 对象序列化为 JSON 字符串
     * <p>替代 {@code JSON.toJSONString(obj)}</p>
     */
    public static String toJsonString(Object obj) {
        if (obj == null) {
            return "null";
        }
        try {
            // String 也必须按 JSON 字符串编码（带引号），保证与 toJsonBytes 语义一致
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("JSON序列化失败: {}", obj.getClass().getSimpleName(), e);
            return obj.toString();
        }
    }

    /**
     * 对象序列化为 JSON 字节数组
     * <p>替代 {@code JSON.toJSONBytes(obj)}</p>
     */
    public static byte[] toJsonBytes(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            log.warn("JSON序列化为字节数组失败: {}", obj.getClass().getSimpleName(), e);
            return new byte[0];
        }
    }

    /**
     * 严格模式：对象序列化为 JSON 字符串
     * <p>
     * 与 {@link #toJsonString(Object)} 不同，序列化失败时会直接抛出异常，
     * 适用于必须保证序列化成功的场景。
     * </p>
     */
    public static String toJsonStringStrict(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw buildStrictSerializationException(obj, "String", e);
        }
    }

    /**
     * 严格模式：对象序列化为 JSON 字节数组
     * <p>
     * 与 {@link #toJsonBytes(Object)} 不同，序列化失败时会直接抛出异常，
     * 适用于必须保证序列化成功的场景。
     * </p>
     */
    public static byte[] toJsonBytesStrict(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            throw buildStrictSerializationException(obj, "byte[]", e);
        }
    }

    private static IllegalStateException buildStrictSerializationException(
        Object obj, String targetType, JsonProcessingException cause) {
        String sourceType = obj == null ? "null" : obj.getClass().getName();
        return new IllegalStateException(
            "JSON严格序列化失败: sourceType=" + sourceType + ", targetType=" + targetType, cause);
    }

    /**
     * JSON 字符串反序列化为对象
     * <p>替代 {@code JSON.parseObject(json, clazz)}</p>
     */
    public static <T> T parseObject(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.warn("JSON反序列化失败: target={}", clazz.getSimpleName(), e);
            return null;
        }
    }

    /**
     * JSON 字符串反序列化为泛型对象
     * <p>替代 {@code JSON.parseObject(json, new TypeReference<Map<String, Object>>(){})}</p>
     */
    public static <T> T parseObject(String json, TypeReference<T> typeRef) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            log.warn("JSON反序列化失败: target={}", typeRef.getType(), e);
            return null;
        }
    }

    /**
     * JSON 字节数组反序列化为对象
     * <p>替代 {@code JSON.parseObject(bytes, clazz)}</p>
     */
    public static <T> T parseObject(byte[] bytes, Class<T> clazz) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(bytes, clazz);
        } catch (Exception e) {
            log.warn("JSON字节数组反序列化失败: target={}", clazz.getSimpleName(), e);
            return null;
        }
    }

    /**
     * 解析 JSON 字符串（返回 Object，兼容 fastjson2 的 JSON.parse()）
     */
    public static Object parse(String json) {
        return parseObject(json, Object.class);
    }
}
