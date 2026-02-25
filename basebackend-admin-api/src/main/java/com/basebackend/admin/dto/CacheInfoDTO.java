package com.basebackend.admin.dto;

/**
 * 缓存信息DTO
 */
public record CacheInfoDTO(
    /** 缓存名称 */
    String cacheName,
    /** 缓存类型 */
    String cacheType,
    /** 缓存大小 */
    Long cacheSize,
    /** 命中次数 */
    Long hitCount,
    /** 未命中次数 */
    Long missCount,
    /** 命中率 */
    String hitRate,
    /** 最大容量 */
    Long maxCapacity,
    /** 使用率 */
    String usageRate,
    /** 过期时间 */
    Long expireTime,
    /** 最后访问时间 */
    String lastAccessTime
) {}
