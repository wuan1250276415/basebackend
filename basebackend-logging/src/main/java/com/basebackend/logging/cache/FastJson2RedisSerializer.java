package com.basebackend.logging.cache;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

/**
 * FastJSON2序列化器
 *
 * 基于FastJSON2的Redis序列化器，支持对象的序列化和反序列化。
 * 使用WriteClassName特性确保类型安全。
 *
 * @author basebackend team
 * @since 2025-11-22
 */
public class FastJson2RedisSerializer implements RedisSerializer<Object> {

    /**
     * 序列化对象为字节数组
     *
     * @param object 要序列化的对象
     * @return 序列化后的字节数组
     * @throws SerializationException 序列化失败
     */
    @Override
    public byte[] serialize(Object object) throws SerializationException {
        if (object == null) {
            return new byte[0];
        }
        try {
            return JSON.toJSONBytes(object, JSONWriter.Feature.WriteClassName);
        } catch (Exception e) {
            throw new SerializationException("Failed to serialize object", e);
        }
    }

    /**
     * 反序列化字节数组为对象
     *
     * @param bytes 字节数组
     * @return 反序列化后的对象
     * @throws SerializationException 反序列化失败
     */
    @Override
    public Object deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            return JSON.parseObject(bytes, Object.class, JSONReader.Feature.SupportClassForName);
        } catch (Exception e) {
            throw new SerializationException("Failed to deserialize bytes", e);
        }
    }
}
