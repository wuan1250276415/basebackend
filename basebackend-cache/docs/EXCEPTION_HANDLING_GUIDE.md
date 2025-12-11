# Cache Exception Handling Guide

## Overview

The cache module provides a comprehensive exception handling framework to ensure robust error handling across all cache operations. This guide explains the exception hierarchy and how exceptions are integrated throughout the module.

## Exception Hierarchy

```
CacheException (基础异常)
├── CacheSerializationException (序列化异常)
├── CacheLockException (锁异常)
├── CacheConnectionException (连接异常)
└── CacheConfigurationException (配置异常)
```

## Exception Classes

### 1. CacheException

**Base exception class for all cache-related exceptions.**

```java
public class CacheException extends RuntimeException {
    public CacheException(String message);
    public CacheException(String message, Throwable cause);
    public CacheException(Throwable cause);
}
```

**Usage:** Base class for all cache exceptions. Can be used for general cache errors that don't fit into specific categories.

### 2. CacheSerializationException

**Thrown when serialization or deserialization operations fail.**

```java
public class CacheSerializationException extends CacheException {
    public CacheSerializationException(String message);
    public CacheSerializationException(String message, Throwable cause);
    public CacheSerializationException(Throwable cause);
}
```

**When thrown:**
- JSON serialization/deserialization fails
- Protobuf message conversion fails
- Kryo serialization/deserialization fails
- Type mismatch during deserialization
- Corrupted data encountered

**Example:**
```java
try {
    byte[] data = serializer.serialize(object);
} catch (CacheSerializationException e) {
    log.error("Failed to serialize object", e);
    // Handle serialization failure
}
```

### 3. CacheLockException

**Thrown when distributed lock operations fail.**

```java
public class CacheLockException extends CacheException {
    public CacheLockException(String message);
    public CacheLockException(String message, Throwable cause);
}
```

**When thrown:**
- Failed to acquire lock within wait time
- Lock acquisition interrupted
- Lock release fails
- Invalid lock configuration

**Example:**
```java
try {
    lockService.executeWithLock("myLock", () -> {
        // Critical section
        return result;
    }, 5, 10);
} catch (CacheLockException e) {
    log.error("Failed to acquire lock", e);
    // Handle lock failure
}
```

### 4. CacheConnectionException

**Thrown when Redis connection fails or times out.**

```java
public class CacheConnectionException extends CacheException {
    public CacheConnectionException(String message);
    public CacheConnectionException(String message, Throwable cause);
    public CacheConnectionException(Throwable cause);
}
```

**When thrown:**
- Redis connection fails
- Redis operation times out
- Network issues prevent Redis access
- Redis server is unavailable

**Example:**
```java
try {
    String value = redisService.get("key");
} catch (CacheConnectionException e) {
    log.error("Redis connection failed", e);
    // Fallback to local cache or data source
}
```

### 5. CacheConfigurationException

**Thrown when cache configuration is invalid or incomplete.**

```java
public class CacheConfigurationException extends CacheException {
    public CacheConfigurationException(String message);
    public CacheConfigurationException(String message, Throwable cause);
    public CacheConfigurationException(Throwable cause);
}
```

**When thrown:**
- Invalid serialization type specified
- Negative or zero cache size configured
- Invalid TTL values
- Circuit breaker threshold is invalid
- Hit rate threshold out of range (0.0-1.0)

**Example:**
```java
// This will throw CacheConfigurationException during startup
basebackend:
  cache:
    serialization:
      type: "invalid"  # Invalid type
```

## Exception Integration

### Configuration Validation

The `CacheAutoConfiguration` class validates all configuration properties during startup:

```java
@PostConstruct
public void init() {
    validateConfiguration();  // Throws CacheConfigurationException if invalid
    // ... rest of initialization
}
```

**Validated properties:**
- Serialization type (must be: json, protobuf, or kryo)
- Local cache max size (must be positive)
- Local cache TTL (must be positive)
- Hit rate threshold (must be 0.0-1.0)
- Resilience timeout (must be positive)
- Circuit breaker threshold (must be positive)

### Serialization Error Handling

All serializers catch exceptions and wrap them in `CacheSerializationException`:

```java
// JsonCacheSerializer
try {
    return JSON.toJSONString(obj).getBytes();
} catch (Exception e) {
    log.error("Failed to serialize object to JSON", e);
    throw new CacheSerializationException("Failed to serialize object to JSON", e);
}
```

**Behavior:**
- Logs detailed error information
- Wraps original exception for debugging
- Allows caller to handle serialization failures gracefully

### Lock Error Handling

Distributed lock operations throw `CacheLockException` when locks cannot be acquired:

```java
// DistributedLockServiceImpl
boolean locked = tryLock(lockKey, waitTime, leaseTime, TimeUnit.SECONDS);
if (!locked) {
    throw new CacheLockException("Failed to acquire lock within wait time: " + lockKey);
}
```

**Behavior:**
- Throws exception if lock cannot be acquired
- Logs error with lock key for debugging
- Preserves interrupt status for interrupted threads

### Connection Error Handling

Redis connection failures are handled with fallback mechanisms:

```java
// RedisService
try {
    return redisTemplate.opsForValue().get(key);
} catch (DataAccessException e) {
    return handleRedisFailure(e, fallbackValue, operationName);
} catch (Exception e) {
    return handleUnexpectedError(e, fallbackValue, operationName);
}
```

**Behavior:**
- Logs connection errors
- Falls back to local cache if enabled
- Triggers circuit breaker if configured
- Returns null or fallback value instead of propagating exception

### Template Error Handling

Cache templates catch and log exceptions without propagating them:

```java
// CacheAsideTemplate
try {
    return cacheManager.get(key, type);
} catch (Exception e) {
    log.error("Error in cache-aside get operation for key: {}", key, e);
    metricsService.recordLatency("cache-aside-error", latency);
    // Fallback to data loader
    return dataLoader.get();
}
```

**Behavior:**
- Logs errors with context
- Records error metrics
- Falls back to data source
- Ensures operation completes even if cache fails

## Best Practices

### 1. Catch Specific Exceptions

Catch specific exception types when you need different handling:

```java
try {
    cacheService.set("key", value);
} catch (CacheSerializationException e) {
    // Handle serialization failure
    log.error("Cannot serialize value", e);
} catch (CacheConnectionException e) {
    // Handle connection failure
    log.error("Redis unavailable, using local cache", e);
} catch (CacheException e) {
    // Handle other cache errors
    log.error("Cache operation failed", e);
}
```

### 2. Use Configuration Validation

Always validate configuration during startup to fail fast:

```java
@PostConstruct
public void init() {
    validateConfiguration();  // Throws CacheConfigurationException
}
```

### 3. Log with Context

Include relevant context in error logs:

```java
log.error("Failed to serialize object: type={}, size={}", 
    obj.getClass().getName(), 
    obj.toString().length(), 
    e);
```

### 4. Enable Fallback Mechanisms

Configure fallback to handle Redis failures gracefully:

```yaml
basebackend:
  cache:
    resilience:
      fallback-enabled: true
      timeout: 3s
      circuit-breaker:
        enabled: true
        failure-threshold: 5
```

### 5. Monitor Exception Metrics

Use the metrics service to track error rates:

```java
metricsService.recordError(cacheName, operation, errorMessage);
```

## Error Recovery

### Automatic Recovery

The cache module includes automatic recovery mechanisms:

1. **Circuit Breaker**: Automatically stops Redis calls after consecutive failures
2. **Auto-Recovery Check**: Periodically checks if Redis is available again
3. **Fallback to Local Cache**: Uses local cache when Redis is unavailable
4. **Graceful Degradation**: Returns null instead of throwing exceptions

### Manual Recovery

To manually recover from errors:

```java
// Reset circuit breaker
redisService.resetCircuitBreaker();

// Clear corrupted cache data
cacheService.evict(corruptedKey);

// Reload cache from data source
cacheService.getOrLoad(key, dataLoader, ttl);
```

## Testing Exception Handling

### Unit Tests

Test exception scenarios with mocks:

```java
@Test
void testSerializationException() {
    when(serializer.serialize(any()))
        .thenThrow(new CacheSerializationException("Test error"));
    
    assertThrows(CacheSerializationException.class, () -> {
        cacheService.set("key", value);
    });
}
```

### Integration Tests

Test real failure scenarios:

```java
@Test
void testRedisConnectionFailure() {
    // Stop Redis container
    redisContainer.stop();
    
    // Should fallback gracefully
    String value = cacheService.get("key");
    assertNull(value);
}
```

## Troubleshooting

### Common Issues

1. **CacheConfigurationException at startup**
   - Check configuration values in application.yml
   - Ensure all required properties are set
   - Verify value ranges (e.g., threshold 0.0-1.0)

2. **CacheSerializationException during runtime**
   - Check if object is serializable
   - Verify Protobuf/Kryo dependencies if using those serializers
   - Check for circular references in objects

3. **CacheLockException frequently**
   - Increase lock wait time
   - Check for deadlocks
   - Verify lock keys are unique

4. **CacheConnectionException**
   - Verify Redis is running
   - Check network connectivity
   - Review Redis connection pool settings
   - Enable fallback mechanisms

## Summary

The cache exception handling framework provides:

✅ **Comprehensive exception hierarchy** for different error types
✅ **Configuration validation** to fail fast on invalid settings
✅ **Graceful degradation** with fallback mechanisms
✅ **Detailed error logging** with context
✅ **Automatic recovery** through circuit breakers
✅ **Metrics integration** for monitoring error rates

All exceptions extend `CacheException` and are properly integrated throughout the cache module to ensure robust error handling.
