package com.basebackend.cache.serializer;

import com.basebackend.cache.exception.CacheException;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 序列化器工厂
 * 根据配置选择合适的序列化器
 */
@Slf4j
public class SerializerFactory {
    
    private static final Map<String, CacheSerializer> SERIALIZER_CACHE = new ConcurrentHashMap<>();
    
    /**
     * 根据类型获取序列化器
     *
     * @param type 序列化器类型（json, protobuf, kryo）
     * @return 对应的序列化器实例
     * @throws CacheException 如果序列化器类型不支持或依赖不可用
     */
    public static CacheSerializer getSerializer(String type) {
        String serializerType = type;
        if (serializerType == null || serializerType.trim().isEmpty()) {
            serializerType = "json"; // 默认使用 JSON
        }
        
        final String normalizedType = serializerType.toLowerCase().trim();
        
        // 从缓存中获取
        return SERIALIZER_CACHE.computeIfAbsent(normalizedType, t -> {
            switch (t) {
                case "json":
                    log.info("Creating JSON serializer");
                    return new JsonCacheSerializer();
                    
                case "protobuf":
                    return createProtobufSerializer();
                    
                case "kryo":
                    return createKryoSerializer();
                    
                default:
                    throw new CacheException("Unsupported serializer type: " + normalizedType);
            }
        });
    }
    
    /**
     * 创建 Protobuf 序列化器
     * 检查依赖是否可用
     */
    private static CacheSerializer createProtobufSerializer() {
        try {
            Class.forName("com.google.protobuf.Message");
            log.info("Creating Protobuf serializer");
            return new ProtobufCacheSerializer();
        } catch (ClassNotFoundException e) {
            throw new CacheException(
                "Protobuf serializer requires protobuf-java dependency. " +
                "Please add it to your pom.xml", e
            );
        }
    }
    
    /**
     * 创建 Kryo 序列化器
     * 检查依赖是否可用
     */
    private static CacheSerializer createKryoSerializer() {
        try {
            Class.forName("com.esotericsoftware.kryo.Kryo");
            log.info("Creating Kryo serializer");
            return new KryoCacheSerializer();
        } catch (ClassNotFoundException e) {
            throw new CacheException(
                "Kryo serializer requires kryo dependency. " +
                "Please add it to your pom.xml", e
            );
        }
    }
    
    /**
     * 清除序列化器缓存
     * 主要用于测试
     */
    public static void clearCache() {
        SERIALIZER_CACHE.clear();
    }
}
