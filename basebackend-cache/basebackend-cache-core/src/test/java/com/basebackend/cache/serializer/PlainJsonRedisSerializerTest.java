package com.basebackend.cache.serializer;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlainJsonRedisSerializerTest {

    private final PlainJsonRedisSerializer serializer = new PlainJsonRedisSerializer();

    @Test
    void serializeAndDeserializeStringUsingJsonStringFormat() {
        byte[] bytes = serializer.serialize("header.payload.signature");

        assertEquals("\"header.payload.signature\"", new String(bytes, StandardCharsets.UTF_8));
        assertEquals("header.payload.signature", serializer.deserialize(bytes));
    }

    @Test
    void serializeAndDeserializeMapUsingPlainJsonObject() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("userId", 1L);
        value.put("username", "admin");

        byte[] bytes = serializer.serialize(value);
        Object deserialized = serializer.deserialize(bytes);

        assertEquals("{\"userId\":1,\"username\":\"admin\"}", new String(bytes, StandardCharsets.UTF_8));
        assertInstanceOf(LinkedHashMap.class, deserialized);

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) deserialized;
        assertEquals("admin", result.get("username"));
        assertInstanceOf(Number.class, result.get("userId"));
        assertEquals(1L, ((Number) result.get("userId")).longValue());
        assertFalse(new String(bytes, StandardCharsets.UTF_8).contains("@class"));
    }

    @Test
    void serializeAndDeserializeListUsingPlainJsonArray() {
        List<String> roles = new ArrayList<>();
        roles.add("ADMIN");
        roles.add("USER");

        byte[] bytes = serializer.serialize(roles);
        Object deserialized = serializer.deserialize(bytes);

        assertEquals("[\"ADMIN\",\"USER\"]", new String(bytes, StandardCharsets.UTF_8));
        assertInstanceOf(ArrayList.class, deserialized);
        assertEquals(List.of("ADMIN", "USER"), deserialized);
    }

    @Test
    void handleNullAndEmptyValuesGracefully() {
        assertNull(serializer.serialize(null));
        assertNull(serializer.deserialize(null));
        assertNull(serializer.deserialize(new byte[0]));
    }

    @Test
    void returnNullForInvalidJsonPayload() {
        assertNull(serializer.deserialize("not-json".getBytes(StandardCharsets.UTF_8)));
    }
}
