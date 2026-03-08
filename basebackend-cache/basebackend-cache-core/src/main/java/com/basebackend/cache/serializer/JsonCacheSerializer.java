package com.basebackend.cache.serializer;

import com.basebackend.common.util.JsonUtils;
import com.basebackend.cache.exception.CacheSerializationException;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * JSON 序列化器实现（使用 Fastjson2）
 * <p>
 * 安全说明：
 * <ul>
 *   <li>禁用 WriteClassName 防止序列化时写入类型信息</li>
 *   <li>禁用 SupportAutoType 防止反序列化时自动加载类</li>
 *   <li>通过显式指定目标类型进行反序列化，避免 RCE 漏洞</li>
 * </ul>
 *
 * @author BaseBackend
 */
@Slf4j
public class JsonCacheSerializer implements CacheSerializer {

    private static final String TYPE = "json";

    @Override
    public byte[] serialize(Object obj) throws CacheSerializationException {
        if (obj == null) {
            return null;
        }

        try {
            // 兼容独立模块构建：即使上游 JsonUtils 未升级，也保证字符串按 JSON 字符串编码
            if (obj instanceof String stringValue) {
                String encodedString = JsonUtils.getObjectMapper().writeValueAsString(stringValue);
                return encodedString.getBytes(StandardCharsets.UTF_8);
            }

            // 安全修复：禁用 WriteClassName，不在 JSON 中写入类型信息
            // 这样可以防止反序列化时被恶意类型利用
            String jsonString = JsonUtils.toJsonString(obj);
            return jsonString.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("序列化对象到JSON失败: {}", obj.getClass().getName(), e);
            throw new CacheSerializationException("Failed to serialize object to JSON", e);
        }
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> type) throws CacheSerializationException {
        if (data == null || data.length == 0) {
            return null;
        }

        try {
            String jsonString = new String(data, StandardCharsets.UTF_8);
            String trimmed = jsonString.trim();

            // 验证是否为有效的 JSON
            Object parsedCheck = JsonUtils.parse(jsonString);
            if (parsedCheck == null) {
                return null;
            }

            // 简单类型的早期检查
            if (isSimpleType(type)) {
                // 对基础类型做严格匹配，避免 "123" -> Integer / 123 -> String 这类隐式转换
                if (hasSimpleTypeMismatch(type, parsedCheck)) {
                    throw new CacheSerializationException("Failed to deserialize: type mismatch");
                }
            }

            // 安全修复：禁用 SupportAutoType，只使用显式指定的目标类型进行反序列化
            // 这是防止 Fastjson 反序列化漏洞的关键措施
            T result = JsonUtils.parseObject(jsonString, type);

            // 字符串类型特殊检查
            if (type == String.class && result != null) {
                if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                    throw new CacheSerializationException("Failed to deserialize: unexpected JSON structure for String type");
                }
            }

            // 类型安全检查
            if (result != null) {
                Class<?> resultClass = result.getClass();

                // 允许数字类型之间的转换
                if (Number.class.isAssignableFrom(type) && Number.class.isAssignableFrom(resultClass)) {
                    return result;
                }

                // 其他类型检查精确匹配
                if (!type.isInstance(result)) {
                    throw new CacheSerializationException("Failed to deserialize: type incompatible");
                }
            }

            return result;
        } catch (CacheSerializationException e) {
            throw e;
        } catch (Exception e) {
            log.debug("反序列化JSON到类型 {} 失败，返回null", type.getName(), e);
            return null;
        }
    }

    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * 检查是否为简单/基本类型
     */
    private boolean isSimpleType(Class<?> type) {
        return type == String.class ||
               type == Integer.class ||
               type == Long.class ||
               type == Double.class ||
               type == Float.class ||
               type == Boolean.class ||
               type == Byte.class ||
               type == Short.class ||
               type == Character.class ||
               type.isPrimitive();
    }

    /**
     * 检查简单类型是否发生不匹配
     */
    private boolean hasSimpleTypeMismatch(Class<?> expectedType, Object parsedValue) {
        if (parsedValue == null) {
            return false;
        }

        Class<?> normalizedType = normalizePrimitiveType(expectedType);

        if (Number.class.isAssignableFrom(normalizedType)) {
            return !(parsedValue instanceof Number);
        }
        if (normalizedType == String.class) {
            return !(parsedValue instanceof String);
        }
        if (normalizedType == Boolean.class) {
            return !(parsedValue instanceof Boolean);
        }
        if (normalizedType == Character.class) {
            return !(parsedValue instanceof String str && str.length() == 1);
        }

        return !normalizedType.isInstance(parsedValue);
    }

    /**
     * 将基本类型归一化为包装类型，便于统一判定
     */
    private Class<?> normalizePrimitiveType(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        return type;
    }
}
