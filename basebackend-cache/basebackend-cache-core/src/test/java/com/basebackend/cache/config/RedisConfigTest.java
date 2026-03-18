package com.basebackend.cache.config;

import com.basebackend.cache.serializer.PlainJsonRedisSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class RedisConfigTest {

    private final RedisConfig redisConfig = new RedisConfig();

    @Test
    void configureRedisTemplateWithStringKeysAndPlainJsonValues() {
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);

        RedisTemplate<String, Object> redisTemplate = redisConfig.redisTemplate(connectionFactory);

        assertInstanceOf(StringRedisSerializer.class, redisTemplate.getKeySerializer());
        assertInstanceOf(StringRedisSerializer.class, redisTemplate.getHashKeySerializer());
        assertInstanceOf(PlainJsonRedisSerializer.class, redisTemplate.getValueSerializer());
        assertInstanceOf(PlainJsonRedisSerializer.class, redisTemplate.getHashValueSerializer());
    }

    @Test
    void valueSerializerKeepsExistingPlainJsonWireFormat() {
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        RedisTemplate<String, Object> redisTemplate = redisConfig.redisTemplate(connectionFactory);

        @SuppressWarnings("unchecked")
        RedisSerializer<Object> serializer = (RedisSerializer<Object>) redisTemplate.getValueSerializer();

        Map<String, Object> onlineUser = new LinkedHashMap<>();
        onlineUser.put("userId", 1L);
        onlineUser.put("username", "admin");

        byte[] tokenBytes = serializer.serialize("header.payload.signature");
        byte[] mapBytes = serializer.serialize(onlineUser);

        assertEquals("\"header.payload.signature\"", new String(tokenBytes, StandardCharsets.UTF_8));
        assertEquals("{\"userId\":1,\"username\":\"admin\"}", new String(mapBytes, StandardCharsets.UTF_8));
        assertFalse(new String(mapBytes, StandardCharsets.UTF_8).contains("@class"));
        assertEquals("header.payload.signature", serializer.deserialize(tokenBytes));
    }
}
