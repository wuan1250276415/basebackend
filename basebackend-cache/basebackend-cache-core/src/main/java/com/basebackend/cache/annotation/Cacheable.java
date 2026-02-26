package com.basebackend.cache.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 缓存注解
 * 标记在方法上，自动缓存方法返回值
 * 
 * 使用示例：
 * <pre>
 * {@code
 * @Cacheable(key = "#userId", ttl = 300)
 * public User getUserById(Long userId) {
 *     return userRepository.findById(userId);
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Cacheable {
    
    /**
     * 缓存名称，用于区分不同的缓存空间
     * 默认使用类名
     */
    String cacheName() default "";
    
    /**
     * 缓存键，支持 SpEL 表达式
     * 例如：#userId, #user.id, #p0, #a0
     * 如果不指定，将使用所有参数生成键
     */
    String key() default "";
    
    /**
     * 缓存键前缀
     * 最终的键格式为：prefix:cacheName:key
     */
    String keyPrefix() default "cache";
    
    /**
     * 缓存过期时间（秒）
     * 默认 300 秒（5 分钟）
     */
    long ttl() default 300;
    
    /**
     * 时间单位
     * 默认为秒
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    
    /**
     * 条件表达式，支持 SpEL
     * 只有当条件为 true 时才执行缓存操作
     * 例如：#result != null, #userId > 0
     */
    String condition() default "";
    
    /**
     * 排除条件表达式，支持 SpEL
     * 当条件为 true 时不缓存结果
     * 例如：#result == null, #result.isEmpty()
     */
    String unless() default "";
    
    /**
     * 是否使用多级缓存
     * 如果为 true 且多级缓存已启用，将使用 MultiLevelCacheManager
     * 否则直接使用 Redis
     */
    boolean useMultiLevel() default true;
}
