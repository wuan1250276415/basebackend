package com.basebackend.logging.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

/**
 * Jackson Redis 序列化器
 *
 * 基于 Jackson 的 Redis 序列化器，支持对象的序列化和反序列化。
 * 使用 DefaultTyping 确保类型安全（替代原 FastJson2 的 WriteClassName）。
 *
 * @author basebackend team
 * @since 2025-11-22
 */
public class FastJson2RedisSerializer implements RedisSerializer<Object> {

    private final GenericJackson2JsonRedisSerializer delegate;

    public FastJson2RedisSerializer() {
        // 限制反序列化到项目包前缀，防止反序列化 Gadget 攻击
        ObjectMapper mapper = new ObjectMapper();
        mapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType("com.basebackend.")
                        .allowIfSubType("java.util.")
                        .allowIfSubType("java.lang.")
                        .build(),
                DefaultTyping.NON_FINAL
        );
        this.delegate = new GenericJackson2JsonRedisSerializer(mapper);
    }

    @Override
    public byte[] serialize(Object object) throws SerializationException {
        if (object == null) {
            return new byte[0];
        }
        return delegate.serialize(object);
    }

    @Override
    public Object deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        return delegate.deserialize(bytes);
    }
}
