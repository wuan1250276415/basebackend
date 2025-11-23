package com.basebackend.cache.serializer;

import com.basebackend.cache.exception.CacheSerializationException;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import net.jqwik.api.lifecycle.BeforeTry;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for serializer round-trip consistency
 * 
 * **Feature: cache-enhancement, Property 17: 序列化往返一致性**
 * **Validates: Requirements 10.1, 10.2, 10.3**
 * 
 * For any serializable object and serializer type (JSON, Kryo),
 * serializing then deserializing should produce an equivalent object.
 */
class SerializerRoundTripPropertyTest {
    
    private JsonCacheSerializer jsonSerializer;
    private KryoCacheSerializer kryoSerializer;
    
    @BeforeTry
    void setUp() {
        jsonSerializer = new JsonCacheSerializer();
        kryoSerializer = new KryoCacheSerializer();
    }
    
    /**
     * Property: JSON serializer round-trip for primitive types
     */
    @Property(tries = 100)
    void jsonSerializerRoundTripForPrimitives(
        @ForAll String stringValue,
        @ForAll Integer intValue,
        @ForAll Long longValue,
        @ForAll Double doubleValue,
        @ForAll Boolean boolValue
    ) throws CacheSerializationException {
        // Test String
        assertRoundTrip(jsonSerializer, stringValue, String.class);
        
        // Test Integer
        assertRoundTrip(jsonSerializer, intValue, Integer.class);
        
        // Test Long
        assertRoundTrip(jsonSerializer, longValue, Long.class);
        
        // Test Double
        assertRoundTrip(jsonSerializer, doubleValue, Double.class);
        
        // Test Boolean
        assertRoundTrip(jsonSerializer, boolValue, Boolean.class);
    }
    
    /**
     * Property: JSON serializer round-trip for collections
     */
    @Property(tries = 100)
    void jsonSerializerRoundTripForCollections(
        @ForAll @Size(max = 20) List<@StringLength(max = 50) String> stringList,
        @ForAll @Size(max = 20) Set<Integer> intSet,
        @ForAll @Size(max = 20) Map<@StringLength(max = 30) String, Integer> stringIntMap
    ) throws CacheSerializationException {
        // Test List
        byte[] listBytes = jsonSerializer.serialize(stringList);
        @SuppressWarnings("unchecked")
        List<String> deserializedStringList = jsonSerializer.deserialize(listBytes, List.class);
        assertThat(deserializedStringList).isEqualTo(stringList);
        
        // Test Set - JSON may deserialize large integers as Long, so we convert for comparison
        byte[] setBytes = jsonSerializer.serialize(intSet);
        @SuppressWarnings("unchecked")
        Set<Object> deserializedSetRaw = jsonSerializer.deserialize(setBytes, Set.class);
        // Convert any Long values back to Integer for comparison (JSON limitation)
        Set<Integer> deserializedSet = deserializedSetRaw.stream()
            .map(obj -> obj instanceof Long ? ((Long) obj).intValue() : (Integer) obj)
            .collect(java.util.stream.Collectors.toSet());
        assertThat(deserializedSet).isEqualTo(intSet);
        
        // Test Map - Fastjson2 adds type metadata (@type key) and may deserialize integers as Long
        byte[] mapBytes = jsonSerializer.serialize(stringIntMap);
        @SuppressWarnings("unchecked")
        Map<String, Object> deserializedMapRaw = jsonSerializer.deserialize(mapBytes, Map.class);
        // Remove @type metadata and convert Long values to Integer
        deserializedMapRaw.remove("@type");
        Map<String, Integer> deserializedMap = deserializedMapRaw.entrySet().stream()
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue() instanceof Long ? ((Long) e.getValue()).intValue() : (Integer) e.getValue()
            ));
        assertThat(deserializedMap).isEqualTo(stringIntMap);
    }
    
    /**
     * Property: JSON serializer round-trip for nested objects
     */
    @Property(tries = 100)
    void jsonSerializerRoundTripForNestedObjects(
        @ForAll("testDataObjects") TestData testData
    ) throws CacheSerializationException {
        assertRoundTrip(jsonSerializer, testData, TestData.class);
    }
    
    /**
     * Property: Kryo serializer round-trip for primitive types
     */
    @Property(tries = 100)
    void kryoSerializerRoundTripForPrimitives(
        @ForAll String stringValue,
        @ForAll Integer intValue,
        @ForAll Long longValue,
        @ForAll Double doubleValue,
        @ForAll Boolean boolValue
    ) throws CacheSerializationException {
        // Test String
        assertRoundTrip(kryoSerializer, stringValue, String.class);
        
        // Test Integer
        assertRoundTrip(kryoSerializer, intValue, Integer.class);
        
        // Test Long
        assertRoundTrip(kryoSerializer, longValue, Long.class);
        
        // Test Double
        assertRoundTrip(kryoSerializer, doubleValue, Double.class);
        
        // Test Boolean
        assertRoundTrip(kryoSerializer, boolValue, Boolean.class);
    }
    
    /**
     * Property: Kryo serializer round-trip for collections
     */
    @Property(tries = 100)
    void kryoSerializerRoundTripForCollections(
        @ForAll @Size(max = 20) List<@StringLength(max = 50) String> stringList,
        @ForAll @Size(max = 20) Set<Integer> intSet,
        @ForAll @Size(max = 20) Map<@StringLength(max = 30) String, Integer> stringIntMap
    ) throws CacheSerializationException {
        // Test List
        assertRoundTrip(kryoSerializer, stringList, List.class);
        
        // Test Set
        assertRoundTrip(kryoSerializer, intSet, Set.class);
        
        // Test Map
        assertRoundTrip(kryoSerializer, stringIntMap, Map.class);
    }
    
    /**
     * Property: Kryo serializer round-trip for nested objects
     */
    @Property(tries = 100)
    void kryoSerializerRoundTripForNestedObjects(
        @ForAll("testDataObjects") TestData testData
    ) throws CacheSerializationException {
        assertRoundTrip(kryoSerializer, testData, TestData.class);
    }
    
    /**
     * Property: Null handling for all serializers
     */
    @Property(tries = 10)
    void serializersHandleNullCorrectly() throws CacheSerializationException {
        // JSON serializer
        byte[] jsonNull = jsonSerializer.serialize(null);
        assertThat(jsonNull).isNull();
        
        String jsonDeserialized = jsonSerializer.deserialize(null, String.class);
        assertThat(jsonDeserialized).isNull();
        
        String jsonEmptyDeserialized = jsonSerializer.deserialize(new byte[0], String.class);
        assertThat(jsonEmptyDeserialized).isNull();
        
        // Kryo serializer
        byte[] kryoNull = kryoSerializer.serialize(null);
        assertThat(kryoNull).isNull();
        
        String kryoDeserialized = kryoSerializer.deserialize(null, String.class);
        assertThat(kryoDeserialized).isNull();
        
        String kryoEmptyDeserialized = kryoSerializer.deserialize(new byte[0], String.class);
        assertThat(kryoEmptyDeserialized).isNull();
    }
    
    // Helper method for round-trip assertion
    private <T> void assertRoundTrip(CacheSerializer serializer, T original, Class<T> type) 
            throws CacheSerializationException {
        byte[] serialized = serializer.serialize(original);
        T deserialized = serializer.deserialize(serialized, type);
        assertThat(deserialized).isEqualTo(original);
    }
    
    // Arbitrary provider for test data objects
    @Provide
    Arbitrary<TestData> testDataObjects() {
        return Combinators.combine(
            Arbitraries.strings().withCharRange('a', 'z').ofMaxLength(50),
            Arbitraries.integers().between(0, 1000),
            Arbitraries.doubles().between(0.0, 1000.0),
            Arbitraries.defaultFor(List.class, String.class).map(list -> 
                list.stream().limit(10).toList()
            )
        ).as(TestData::new);
    }
    
    /**
     * Test data class for nested object testing
     */
    public static class TestData {
        private String name;
        private Integer age;
        private Double score;
        private List<String> tags;
        
        // Default constructor for serialization
        public TestData() {
        }
        
        public TestData(String name, Integer age, Double score, List<String> tags) {
            this.name = name;
            this.age = age;
            this.score = score;
            this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public Integer getAge() {
            return age;
        }
        
        public void setAge(Integer age) {
            this.age = age;
        }
        
        public Double getScore() {
            return score;
        }
        
        public void setScore(Double score) {
            this.score = score;
        }
        
        public List<String> getTags() {
            return tags;
        }
        
        public void setTags(List<String> tags) {
            this.tags = tags;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestData testData = (TestData) o;
            return Objects.equals(name, testData.name) &&
                   Objects.equals(age, testData.age) &&
                   Objects.equals(score, testData.score) &&
                   Objects.equals(tags, testData.tags);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(name, age, score, tags);
        }
        
        @Override
        public String toString() {
            return "TestData{" +
                   "name='" + name + '\'' +
                   ", age=" + age +
                   ", score=" + score +
                   ", tags=" + tags +
                   '}';
        }
    }
}
