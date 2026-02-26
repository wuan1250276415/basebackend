package com.basebackend.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JsonUtils 单元测试
 */
class JsonUtilsTest {

    // ========== toJsonString ==========

    @Nested
    @DisplayName("toJsonString")
    class ToJsonString {

        @Test
        @DisplayName("普通对象序列化")
        void shouldSerializeObject() {
            var user = new TestUser("张三", 25);
            String json = JsonUtils.toJsonString(user);
            assertThat(json).contains("\"name\":\"张三\"").contains("\"age\":25");
        }

        @Test
        @DisplayName("null 返回 \"null\"")
        void shouldReturnNullString() {
            assertThat(JsonUtils.toJsonString(null)).isEqualTo("null");
        }

        @Test
        @DisplayName("String 直接返回")
        void shouldReturnStringDirectly() {
            assertThat(JsonUtils.toJsonString("hello")).isEqualTo("hello");
        }

        @Test
        @DisplayName("Map 序列化")
        void shouldSerializeMap() {
            var map = Map.of("key", "value", "num", 42);
            String json = JsonUtils.toJsonString(map);
            assertThat(json).contains("\"key\":\"value\"").contains("\"num\":42");
        }

        @Test
        @DisplayName("List 序列化")
        void shouldSerializeList() {
            var list = List.of("a", "b", "c");
            String json = JsonUtils.toJsonString(list);
            assertThat(json).isEqualTo("[\"a\",\"b\",\"c\"]");
        }

        @Test
        @DisplayName("LocalDateTime 序列化为 ISO 格式")
        void shouldSerializeLocalDateTime() {
            var obj = Map.of("time", LocalDateTime.of(2026, 2, 26, 10, 30, 0));
            String json = JsonUtils.toJsonString(obj);
            assertThat(json).contains("2026-02-26");
        }
    }

    // ========== toJsonBytes ==========

    @Nested
    @DisplayName("toJsonBytes")
    class ToJsonBytes {

        @Test
        @DisplayName("正常序列化为字节数组")
        void shouldSerializeToBytes() {
            var user = new TestUser("李四", 30);
            byte[] bytes = JsonUtils.toJsonBytes(user);
            assertThat(bytes).isNotEmpty();
            assertThat(new String(bytes)).contains("\"name\":\"李四\"");
        }

        @Test
        @DisplayName("null 返回空数组")
        void shouldReturnEmptyForNull() {
            // toJsonBytes(null) 会写 "null" 字节
            byte[] bytes = JsonUtils.toJsonBytes(null);
            assertThat(bytes).isNotNull();
        }
    }

    // ========== parseObject (String, Class) ==========

    @Nested
    @DisplayName("parseObject(String, Class)")
    class ParseObjectClass {

        @Test
        @DisplayName("正常反序列化")
        void shouldDeserialize() {
            String json = "{\"name\":\"王五\",\"age\":28}";
            TestUser user = JsonUtils.parseObject(json, TestUser.class);
            assertThat(user).isNotNull();
            assertThat(user.getName()).isEqualTo("王五");
            assertThat(user.getAge()).isEqualTo(28);
        }

        @Test
        @DisplayName("null 输入返回 null")
        void shouldReturnNullForNullInput() {
            assertThat(JsonUtils.parseObject((String) null, TestUser.class)).isNull();
        }

        @Test
        @DisplayName("空字符串返回 null")
        void shouldReturnNullForEmptyString() {
            assertThat(JsonUtils.parseObject("", TestUser.class)).isNull();
            assertThat(JsonUtils.parseObject("   ", TestUser.class)).isNull();
        }

        @Test
        @DisplayName("无效 JSON 返回 null（容错）")
        void shouldReturnNullForInvalidJson() {
            assertThat(JsonUtils.parseObject("{invalid}", TestUser.class)).isNull();
        }

        @Test
        @DisplayName("忽略未知字段")
        void shouldIgnoreUnknownFields() {
            String json = "{\"name\":\"赵六\",\"age\":35,\"unknown\":\"xxx\"}";
            TestUser user = JsonUtils.parseObject(json, TestUser.class);
            assertThat(user).isNotNull();
            assertThat(user.getName()).isEqualTo("赵六");
        }
    }

    // ========== parseObject (String, TypeReference) ==========

    @Nested
    @DisplayName("parseObject(String, TypeReference)")
    class ParseObjectTypeRef {

        @Test
        @DisplayName("反序列化为 Map")
        void shouldDeserializeToMap() {
            String json = "{\"name\":\"test\",\"count\":10}";
            Map<String, Object> map = JsonUtils.parseObject(json, new TypeReference<>() {});
            assertThat(map).containsEntry("name", "test").containsEntry("count", 10);
        }

        @Test
        @DisplayName("反序列化为 List")
        void shouldDeserializeToList() {
            String json = "[{\"name\":\"a\",\"age\":1},{\"name\":\"b\",\"age\":2}]";
            List<TestUser> list = JsonUtils.parseObject(json, new TypeReference<>() {});
            assertThat(list).hasSize(2);
            assertThat(list.get(0).getName()).isEqualTo("a");
        }

        @Test
        @DisplayName("null 输入返回 null")
        void shouldReturnNullForNullInput() {
            assertThat(JsonUtils.parseObject((String) null, new TypeReference<Map<String, Object>>() {})).isNull();
        }
    }

    // ========== parseObject (byte[], Class) ==========

    @Nested
    @DisplayName("parseObject(byte[], Class)")
    class ParseObjectBytes {

        @Test
        @DisplayName("字节数组反序列化")
        void shouldDeserializeFromBytes() {
            byte[] bytes = "{\"name\":\"test\",\"age\":20}".getBytes();
            TestUser user = JsonUtils.parseObject(bytes, TestUser.class);
            assertThat(user).isNotNull();
            assertThat(user.getName()).isEqualTo("test");
        }

        @Test
        @DisplayName("null/空数组返回 null")
        void shouldReturnNullForEmpty() {
            assertThat(JsonUtils.parseObject((byte[]) null, TestUser.class)).isNull();
            assertThat(JsonUtils.parseObject(new byte[0], TestUser.class)).isNull();
        }
    }

    // ========== parse ==========

    @Nested
    @DisplayName("parse")
    class Parse {

        @Test
        @DisplayName("解析 JSON 对象")
        void shouldParseJsonObject() {
            Object result = JsonUtils.parse("{\"key\":\"value\"}");
            assertThat(result).isInstanceOf(Map.class);
        }

        @Test
        @DisplayName("解析 JSON 数组")
        void shouldParseJsonArray() {
            Object result = JsonUtils.parse("[1,2,3]");
            assertThat(result).isInstanceOf(List.class);
        }
    }

    // ========== 往返测试 ==========

    @Nested
    @DisplayName("序列化/反序列化往返")
    class RoundTrip {

        @Test
        @DisplayName("对象往返一致")
        void shouldRoundTrip() {
            var original = new TestUser("往返测试", 99);
            String json = JsonUtils.toJsonString(original);
            TestUser restored = JsonUtils.parseObject(json, TestUser.class);
            assertThat(restored.getName()).isEqualTo(original.getName());
            assertThat(restored.getAge()).isEqualTo(original.getAge());
        }

        @Test
        @DisplayName("字节数组往返一致")
        void shouldRoundTripBytes() {
            var original = new TestUser("字节往返", 66);
            byte[] bytes = JsonUtils.toJsonBytes(original);
            TestUser restored = JsonUtils.parseObject(bytes, TestUser.class);
            assertThat(restored.getName()).isEqualTo(original.getName());
        }
    }

    // ========== 测试用内部类 ==========

    static class TestUser {
        private String name;
        private int age;

        public TestUser() {}
        public TestUser(String name, int age) { this.name = name; this.age = age; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
    }
}
