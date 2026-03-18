package com.basebackend.gateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.SerializationException;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@DisplayName("RedisConfig 序列化配置测试")
class RedisConfigTest {

    private final RedisConfig redisConfig = new RedisConfig();

    @Test
    @DisplayName("value 序列化应使用 JSON 字符串格式")
    void shouldRoundTripJsonSerializedTokenValue() {
        ReactiveRedisTemplate<String, Object> template = redisConfig
                .reactiveRedisTemplate(mock(ReactiveRedisConnectionFactory.class));
        RedisSerializationContext.SerializationPair<Object> valuePair = template.getSerializationContext()
                .getValueSerializationPair();

        String token = "header.payload.signature";
        ByteBuffer serialized = valuePair.write(token);

        assertThat(readBytes(serialized)).isEqualTo("\"header.payload.signature\"");
        assertThat(valuePair.read(valuePair.write(token))).isEqualTo(token);
    }

    @Test
    @DisplayName("裸字符串值不匹配当前 value 反序列化格式")
    void shouldRejectPlainStringRedisValue() {
        ReactiveRedisTemplate<String, Object> template = redisConfig
                .reactiveRedisTemplate(mock(ReactiveRedisConnectionFactory.class));
        RedisSerializationContext.SerializationPair<Object> valuePair = template.getSerializationContext()
                .getValueSerializationPair();

        ByteBuffer plainTokenBytes = ByteBuffer.wrap("header.payload.signature".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> valuePair.read(plainTokenBytes))
                .isInstanceOf(SerializationException.class);
    }

    private String readBytes(ByteBuffer byteBuffer) {
        ByteBuffer copy = byteBuffer.duplicate();
        byte[] bytes = new byte[copy.remaining()];
        copy.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
