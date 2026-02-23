package com.basebackend.cache.admin;

import com.basebackend.cache.admin.dto.CacheDetailDTO;
import com.basebackend.cache.admin.dto.CacheInfoDTO;
import com.basebackend.cache.metrics.CacheStatistics;
import com.basebackend.cache.service.CacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 缓存管理 Actuator 端点
 * 提供缓存列表、详情查询和清除操作
 *
 * 访问路径: /actuator/cacheAdmin
 */
@Slf4j
@Endpoint(id = "cacheAdmin")
public class CacheAdminEndpoint {

    private final CacheService cacheService;

    public CacheAdminEndpoint(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    /**
     * GET /actuator/cacheAdmin
     * 列出所有缓存及摘要统计
     */
    @ReadOperation
    public List<CacheInfoDTO> listCaches() {
        Set<String> cacheNames = cacheService.getAllCacheNames();
        List<CacheInfoDTO> result = new ArrayList<>();

        for (String name : cacheNames) {
            try {
                CacheStatistics stats = cacheService.getStatistics(name);
                long size = cacheService.getCacheSize(name);
                result.add(CacheInfoDTO.from(name, size, stats));
            } catch (Exception e) {
                log.warn("Failed to get stats for cache: {}", name, e);
                result.add(CacheInfoDTO.unavailable(name));
            }
        }

        return result;
    }

    /**
     * GET /actuator/cacheAdmin/{name}
     * 获取指定缓存的详细统计
     */
    @ReadOperation
    public CacheDetailDTO getCacheDetail(@Selector String name) {
        CacheStatistics stats = cacheService.getStatistics(name);
        long size = cacheService.getCacheSize(name);
        Set<String> sampleKeys = cacheService.keys(name + ":*");

        // 限制返回的 key 数量
        int limit = 100;
        Set<String> limitedKeys = sampleKeys.size() > limit
                ? sampleKeys.stream().limit(limit).collect(java.util.stream.Collectors.toSet())
                : sampleKeys;

        return CacheDetailDTO.from(name, size, stats, limitedKeys);
    }

    /**
     * DELETE /actuator/cacheAdmin/{name}
     * 清除指定缓存
     */
    @DeleteOperation
    public long clearCache(@Selector String name) {
        log.info("Actuator: clearing cache '{}'", name);
        return cacheService.clearCache(name);
    }
}
