package com.basebackend.cache.serializer;

import com.basebackend.cache.exception.CacheSerializationException;

/**
 * 缓存序列化器接口
 */
public interface CacheSerializer {
    
    /**
     * 序列化对象为字节数组
     *
     * @param obj 要序列化的对象
     * @return 序列化后的字节数组
     * @throws CacheSerializationException 序列化失败时抛出
     */
    byte[] serialize(Object obj) throws CacheSerializationException;
    
    /**
     * 反序列化字节数组为对象
     *
     * @param data 序列化的字节数组
     * @param type 目标类型
     * @param <T>  目标类型泛型
     * @return 反序列化后的对象
     * @throws CacheSerializationException 反序列化失败时抛出
     */
    <T> T deserialize(byte[] data, Class<T> type) throws CacheSerializationException;
    
    /**
     * 获取序列化器类型
     *
     * @return 序列化器类型标识
     */
    String getType();
}
