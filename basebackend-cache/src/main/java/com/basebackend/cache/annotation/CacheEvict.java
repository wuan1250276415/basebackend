package com.basebackend.cache.annotation;

import java.lang.annotation.*;

/**
 * 缓存清除注解
 * 标记在方法上，自动清除指定的缓存
 * 
 * 使用示例：
 * <pre>
 * {@code
 * @CacheEvict(key = "#userId")
 * public void deleteUser(Long userId) {
 *     userRepository.deleteById(userId);
 * }
 * 
 * @CacheEvict(allEntries = true)
 * public void clearAllUsers() {
 *     userRepository.deleteAll();
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheEvict {
    
    /**
     * 缓存名称，用于区分不同的缓存空间
     * 默认使用类名
     */
    String cacheName() default "";
    
    /**
     * 缓存键，支持 SpEL 表达式
     * 例如：#userId, #user.id, #p0, #a0
     */
    String key() default "";
    
    /**
     * 缓存键前缀
     * 最终的键格式为：prefix:cacheName:key
     */
    String keyPrefix() default "cache";
    
    /**
     * 是否清除所有缓存条目
     * 如果为 true，将清除该缓存名称下的所有条目
     */
    boolean allEntries() default false;
    
    /**
     * 条件表达式，支持 SpEL
     * 只有当条件为 true 时才执行清除操作
     * 例如：#result == true, #userId > 0
     */
    String condition() default "";
    
    /**
     * 是否在方法执行前清除缓存
     * 默认为 false（方法执行后清除）
     */
    boolean beforeInvocation() default false;
    
    /**
     * 是否使用多级缓存
     * 如果为 true 且多级缓存已启用，将使用 MultiLevelCacheManager
     * 否则直接使用 Redis
     */
    boolean useMultiLevel() default true;
}
