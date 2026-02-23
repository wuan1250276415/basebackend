package com.basebackend.cache.pipeline;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Duration;

/**
 * Pipeline 单个操作描述
 */
@Data
@AllArgsConstructor
public class PipelineOperation {

    public enum Type {
        GET, SET, DELETE, EXPIRE, EXISTS, INCR
    }

    private final Type type;
    private final String key;
    private final Object value;
    private final Duration ttl;

    public static PipelineOperation get(String key) {
        return new PipelineOperation(Type.GET, key, null, null);
    }

    public static PipelineOperation set(String key, Object value) {
        return new PipelineOperation(Type.SET, key, value, null);
    }

    public static PipelineOperation set(String key, Object value, Duration ttl) {
        return new PipelineOperation(Type.SET, key, value, ttl);
    }

    public static PipelineOperation delete(String key) {
        return new PipelineOperation(Type.DELETE, key, null, null);
    }

    public static PipelineOperation expire(String key, Duration ttl) {
        return new PipelineOperation(Type.EXPIRE, key, null, ttl);
    }

    public static PipelineOperation exists(String key) {
        return new PipelineOperation(Type.EXISTS, key, null, null);
    }

    public static PipelineOperation incr(String key) {
        return new PipelineOperation(Type.INCR, key, null, null);
    }
}
