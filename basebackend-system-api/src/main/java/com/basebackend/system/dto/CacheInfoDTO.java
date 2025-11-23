package com.basebackend.system.dto;

import lombok.Data;

/**
 * 缓存信息DTO
 */
@Data
public class CacheInfoDTO {

    /**
     * 缓存名称
     */
    private String cacheName;

    /**
     * 缓存类型
     */
    private String cacheType;

    /**
     * 缓存大小
     */
    private Long cacheSize;

    /**
     * 命中次数
     */
    private Long hitCount;

    /**
     * 未命中次数
     */
    private Long missCount;

    /**
     * 命中率
     */
    private String hitRate;

    /**
     * 最大容量
     */
    private Long maxCapacity;

    /**
     * 使用率
     */
    private String usageRate;

    /**
     * 过期时间
     */
    private Long expireTime;

    /**
     * 最后访问时间
     */
    private String lastAccessTime;
}
