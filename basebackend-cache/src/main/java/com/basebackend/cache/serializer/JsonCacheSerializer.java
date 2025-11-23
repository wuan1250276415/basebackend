package com.basebackend.cache.serializer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.basebackend.cache.exception.CacheSerializationException;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * JSON 序列化器实现（使用 Fastjson2）
 */
@Slf4j
public class JsonCacheSerializer implements CacheSerializer {
    
    private static final String TYPE = "json";
    
    @Override
    public byte[] serialize(Object obj) throws CacheSerializationException {
        if (obj == null) {
            return null;
        }
        
        try {
            String jsonString = JSON.toJSONString(obj, JSONWriter.Feature.WriteClassName);
            return jsonString.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to serialize object to JSON: {}", obj.getClass().getName(), e);
            throw new CacheSerializationException("Failed to serialize object to JSON", e);
        }
    }
    
    @Override
    public <T> T deserialize(byte[] data, Class<T> type) throws CacheSerializationException {
        if (data == null || data.length == 0) {
            return null;
        }
        
        try {
            String jsonString = new String(data, StandardCharsets.UTF_8);
            return JSON.parseObject(jsonString, type, JSONReader.Feature.SupportAutoType);
        } catch (Exception e) {
            log.error("Failed to deserialize JSON to object of type: {}", type.getName(), e);
            throw new CacheSerializationException("Failed to deserialize JSON to object", e);
        }
    }
    
    @Override
    public String getType() {
        return TYPE;
    }
}
