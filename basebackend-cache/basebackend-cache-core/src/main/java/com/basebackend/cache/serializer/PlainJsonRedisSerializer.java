package com.basebackend.cache.serializer;

import com.basebackend.cache.exception.CacheSerializationException;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

/**
 * Redis value 序列化适配器。
 * 复用现有 JsonCacheSerializer，保持共享缓存层的纯 JSON 格式不变。
 */
public class PlainJsonRedisSerializer implements RedisSerializer<Object> {

    private final JsonCacheSerializer delegate = new JsonCacheSerializer();

    @Override
    public byte[] serialize(Object value) throws SerializationException {
        try {
            return delegate.serialize(value);
        } catch (CacheSerializationException e) {
            throw new SerializationException("Failed to serialize Redis value as plain JSON", e);
        }
    }

    @Override
    public Object deserialize(byte[] bytes) throws SerializationException {
        try {
            return delegate.deserialize(bytes, Object.class);
        } catch (CacheSerializationException e) {
            throw new SerializationException("Failed to deserialize Redis value from plain JSON", e);
        }
    }
}
