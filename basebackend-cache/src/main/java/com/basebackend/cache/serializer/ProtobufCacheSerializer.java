package com.basebackend.cache.serializer;

import com.basebackend.cache.exception.CacheSerializationException;
import com.google.protobuf.Message;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;

/**
 * Protobuf 序列化器实现（可选依赖）
 * 注意：此序列化器仅支持 Protobuf Message 类型
 */
@Slf4j
public class ProtobufCacheSerializer implements CacheSerializer {
    
    private static final String TYPE = "protobuf";
    
    @Override
    public byte[] serialize(Object obj) throws CacheSerializationException {
        if (obj == null) {
            return null;
        }
        
        if (!(obj instanceof Message)) {
            throw new CacheSerializationException(
                "Object must be a Protobuf Message, but got: " + obj.getClass().getName()
            );
        }
        
        try {
            Message message = (Message) obj;
            return message.toByteArray();
        } catch (Exception e) {
            log.error("Failed to serialize Protobuf message: {}", obj.getClass().getName(), e);
            throw new CacheSerializationException("Failed to serialize Protobuf message", e);
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] data, Class<T> type) throws CacheSerializationException {
        if (data == null || data.length == 0) {
            return null;
        }
        
        if (!Message.class.isAssignableFrom(type)) {
            throw new CacheSerializationException(
                "Type must be a Protobuf Message, but got: " + type.getName()
            );
        }
        
        try {
            // 使用反射调用 parseFrom 方法
            Method parseFromMethod = type.getMethod("parseFrom", byte[].class);
            return (T) parseFromMethod.invoke(null, data);
        } catch (Exception e) {
            log.error("Failed to deserialize Protobuf message of type: {}", type.getName(), e);
            throw new CacheSerializationException("Failed to deserialize Protobuf message", e);
        }
    }
    
    @Override
    public String getType() {
        return TYPE;
    }
}
