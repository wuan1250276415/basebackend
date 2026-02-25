package com.basebackend.cache.serializer;

import com.basebackend.cache.exception.CacheSerializationException;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.Pool;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;

/**
 * Kryo 序列化器实现（可选依赖）
 * 使用对象池来提高性能和线程安全性
 */
@Slf4j
public class KryoCacheSerializer implements CacheSerializer {
    
    private static final String TYPE = "kryo";
    private static final int DEFAULT_BUFFER_SIZE = 4096;
    
    // Kryo 对象池，用于线程安全
    private final Pool<Kryo> kryoPool;
    
    public KryoCacheSerializer() {
        this.kryoPool = new Pool<Kryo>(true, false, 8) {
            @Override
            protected Kryo create() {
                Kryo kryo = new Kryo();
                // 设置为不需要注册类
                kryo.setRegistrationRequired(false);
                // 支持循环引用
                kryo.setReferences(true);
                return kryo;
            }
        };
    }
    
    @Override
    public byte[] serialize(Object obj) throws CacheSerializationException {
        if (obj == null) {
            return null;
        }
        
        Kryo kryo = kryoPool.obtain();
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             Output output = new Output(byteArrayOutputStream, DEFAULT_BUFFER_SIZE)) {
            
            kryo.writeClassAndObject(output, obj);
            output.flush();
            return byteArrayOutputStream.toByteArray();
            
        } catch (Exception e) {
            log.error("Failed to serialize object with Kryo: {}", obj.getClass().getName(), e);
            throw new CacheSerializationException("Failed to serialize object with Kryo", e);
        } finally {
            kryoPool.free(kryo);
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] data, Class<T> type) throws CacheSerializationException {
        if (data == null || data.length == 0) {
            return null;
        }
        
        Kryo kryo = kryoPool.obtain();
        try (Input input = new Input(data)) {
            Object obj = kryo.readClassAndObject(input);
            
            if (obj != null && !type.isInstance(obj)) {
                throw new CacheSerializationException(
                    "Deserialized object type mismatch. Expected: " + type.getName() + 
                    ", but got: " + obj.getClass().getName()
                );
            }
            
            return (T) obj;
            
        } catch (Exception e) {
            log.error("Failed to deserialize object with Kryo to type: {}", type.getName(), e);
            throw new CacheSerializationException("Failed to deserialize object with Kryo", e);
        } finally {
            kryoPool.free(kryo);
        }
    }
    
    @Override
    public String getType() {
        return TYPE;
    }
}
