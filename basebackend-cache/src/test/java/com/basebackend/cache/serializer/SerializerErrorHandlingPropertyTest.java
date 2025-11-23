package com.basebackend.cache.serializer;

import com.basebackend.cache.exception.CacheSerializationException;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import net.jqwik.api.lifecycle.BeforeTry;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Property-based tests for serializer error handling safety
 * 
 * **Feature: cache-enhancement, Property 18: 序列化错误处理安全性**
 * **Validates: Requirements 10.5**
 * 
 * For any corrupted serialization data, deserialization operations should catch exceptions,
 * log errors, and return null rather than throwing uncaught exceptions.
 */
class SerializerErrorHandlingPropertyTest {
    
    private JsonCacheSerializer jsonSerializer;
    private KryoCacheSerializer kryoSerializer;
    
    @BeforeTry
    void setUp() {
        jsonSerializer = new JsonCacheSerializer();
        kryoSerializer = new KryoCacheSerializer();
    }
    
    /**
     * Property: JSON deserializer handles corrupted data safely
     * 
     * For any corrupted byte array, the JSON deserializer should either:
     * 1) Return null (for data that can't be parsed), or
     * 2) Throw CacheSerializationException (for data that causes parsing errors)
     * 
     * The key is that it should NOT throw uncaught exceptions.
     */
    @Property(tries = 100)
    void jsonDeserializerHandlesCorruptedDataSafely(
        @ForAll("corruptedByteArrays") byte[] corruptedData
    ) {
        try {
            // The deserializer should either return null or throw CacheSerializationException
            String result = jsonSerializer.deserialize(corruptedData, String.class);
            // If it returns without exception, result should be null
            assertThat(result).isNull();
        } catch (CacheSerializationException e) {
            // This is also acceptable - the deserializer threw a proper exception
            assertThat(e.getMessage()).contains("Failed to deserialize");
        } catch (Exception e) {
            // Any other exception type is a failure
            throw new AssertionError("Expected CacheSerializationException or null, but got: " + e.getClass().getName(), e);
        }
    }
    
    /**
     * Property: JSON deserializer handles invalid JSON safely
     * 
     * For any invalid JSON string, the deserializer should either:
     * 1) Return null (for data that can't be parsed), or
     * 2) Throw CacheSerializationException (for data that causes parsing errors)
     * 
     * The key is that it should NOT throw uncaught exceptions.
     */
    @Property(tries = 100)
    void jsonDeserializerHandlesInvalidJsonSafely(
        @ForAll("invalidJsonStrings") String invalidJson
    ) {
        byte[] invalidData = invalidJson.getBytes(StandardCharsets.UTF_8);
        
        try {
            // The deserializer should either return null or throw CacheSerializationException
            String result = jsonSerializer.deserialize(invalidData, String.class);
            // If it returns without exception, result should be null
            assertThat(result).isNull();
        } catch (CacheSerializationException e) {
            // This is also acceptable - the deserializer threw a proper exception
            assertThat(e.getMessage()).contains("Failed to deserialize");
        } catch (Exception e) {
            // Any other exception type is a failure
            throw new AssertionError("Expected CacheSerializationException or null, but got: " + e.getClass().getName(), e);
        }
    }
    
    /**
     * Property: JSON deserializer handles type mismatch safely
     * 
     * When deserializing data to an incompatible type, the deserializer
     * should throw CacheSerializationException.
     */
    @Property(tries = 100)
    void jsonDeserializerHandlesTypeMismatchSafely(
        @ForAll @StringLength(min = 1, max = 100) String stringValue
    ) throws CacheSerializationException {
        // Serialize a string
        byte[] serializedString = jsonSerializer.serialize(stringValue);
        
        // Try to deserialize as Integer (type mismatch)
        assertThatThrownBy(() -> jsonSerializer.deserialize(serializedString, Integer.class))
            .isInstanceOf(CacheSerializationException.class)
            .hasMessageContaining("Failed to deserialize");
    }
    
    /**
     * Property: Kryo deserializer handles corrupted data safely
     * 
     * For any corrupted byte array, the Kryo deserializer should either:
     * 1) Return null (for data that can't be parsed), or
     * 2) Throw CacheSerializationException (for data that causes parsing errors)
     * 
     * The key is that it should NOT throw uncaught exceptions.
     */
    @Property(tries = 100)
    void kryoDeserializerHandlesCorruptedDataSafely(
        @ForAll("corruptedByteArrays") byte[] corruptedData
    ) {
        try {
            // The deserializer should either return null or throw CacheSerializationException
            String result = kryoSerializer.deserialize(corruptedData, String.class);
            // If it returns without exception, result should be null
            assertThat(result).isNull();
        } catch (CacheSerializationException e) {
            // This is also acceptable - the deserializer threw a proper exception
            assertThat(e.getMessage()).contains("Failed to deserialize");
        } catch (Exception e) {
            // Any other exception type is a failure
            throw new AssertionError("Expected CacheSerializationException or null, but got: " + e.getClass().getName(), e);
        }
    }
    
    /**
     * Property: Kryo deserializer handles truncated data safely
     * 
     * For any valid serialized data that is truncated, the deserializer should either:
     * 1) Return null (for data that can't be parsed), or
     * 2) Throw CacheSerializationException (for data that causes parsing errors)
     * 
     * The key is that it should NOT throw uncaught exceptions.
     */
    @Property(tries = 100)
    void kryoDeserializerHandlesTruncatedDataSafely(
        @ForAll @StringLength(min = 10, max = 100) String originalValue
    ) throws CacheSerializationException {
        // Serialize a valid string
        byte[] validData = kryoSerializer.serialize(originalValue);
        
        // Truncate the data (remove last few bytes)
        if (validData != null && validData.length > 5) {
            byte[] truncatedData = new byte[validData.length / 2];
            System.arraycopy(validData, 0, truncatedData, 0, truncatedData.length);
            
            try {
                // The deserializer should either return null or throw CacheSerializationException
                String result = kryoSerializer.deserialize(truncatedData, String.class);
                // If it returns without exception, result should be null
                assertThat(result).isNull();
            } catch (CacheSerializationException e) {
                // This is also acceptable - the deserializer threw a proper exception
                assertThat(e.getMessage()).contains("Failed to deserialize");
            } catch (Exception e) {
                // Any other exception type is a failure
                throw new AssertionError("Expected CacheSerializationException or null, but got: " + e.getClass().getName(), e);
            }
        }
    }
    
    /**
     * Property: Kryo deserializer handles type mismatch safely
     * 
     * When deserializing data to an incompatible type, the deserializer
     * should throw CacheSerializationException.
     */
    @Property(tries = 100)
    void kryoDeserializerHandlesTypeMismatchSafely(
        @ForAll @StringLength(min = 1, max = 100) String stringValue
    ) throws CacheSerializationException {
        // Serialize a string
        byte[] serializedString = kryoSerializer.serialize(stringValue);
        
        // Try to deserialize as Integer (type mismatch)
        assertThatThrownBy(() -> kryoSerializer.deserialize(serializedString, Integer.class))
            .isInstanceOf(CacheSerializationException.class)
            .hasMessageContaining("Failed to deserialize");
    }
    
    /**
     * Property: Serializers handle null and empty data gracefully
     * 
     * Null and empty byte arrays should be handled without throwing exceptions.
     */
    @Property(tries = 10)
    void serializersHandleNullAndEmptyDataGracefully() throws CacheSerializationException {
        // JSON serializer
        String jsonNullResult = jsonSerializer.deserialize(null, String.class);
        assertThat(jsonNullResult).isNull();
        
        String jsonEmptyResult = jsonSerializer.deserialize(new byte[0], String.class);
        assertThat(jsonEmptyResult).isNull();
        
        // Kryo serializer
        String kryoNullResult = kryoSerializer.deserialize(null, String.class);
        assertThat(kryoNullResult).isNull();
        
        String kryoEmptyResult = kryoSerializer.deserialize(new byte[0], String.class);
        assertThat(kryoEmptyResult).isNull();
    }
    
    /**
     * Property: Serializers handle null serialization result gracefully
     * 
     * When serializing null, the serializer should return null without throwing exceptions.
     */
    @Property(tries = 10)
    void serializersHandleNullSerializationGracefully() throws CacheSerializationException {
        // JSON serializer
        byte[] jsonResult = jsonSerializer.serialize(null);
        assertThat(jsonResult).isNull();
        
        // Kryo serializer
        byte[] kryoResult = kryoSerializer.serialize(null);
        assertThat(kryoResult).isNull();
    }
    
    // Arbitrary provider for corrupted byte arrays
    @Provide
    Arbitrary<byte[]> corruptedByteArrays() {
        return Arbitraries.integers().between(1, 100).flatMap(size -> {
            return Arbitraries.bytes().array(byte[].class).ofSize(size);
        });
    }
    
    // Arbitrary provider for invalid JSON strings
    @Provide
    Arbitrary<String> invalidJsonStrings() {
        return Arbitraries.oneOf(
            // Incomplete JSON objects
            Arbitraries.just("{\"key\":"),
            Arbitraries.just("{\"key\": \"value\""),
            Arbitraries.just("[1, 2, 3"),
            // Invalid JSON syntax
            Arbitraries.just("{key: value}"),
            Arbitraries.just("{'key': 'value'}"),
            Arbitraries.just("{\"key\": undefined}"),
            // Random non-JSON strings
            Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(50),
            // Binary garbage that looks like text
            Arbitraries.just("\u0000\u0001\u0002\u0003"),
            // Malformed escape sequences
            Arbitraries.just("{\"key\": \"\\u00\"}")
        );
    }
}
