package com.basebackend.cache.admin;

import com.basebackend.cache.admin.dto.CacheDetailDTO;
import com.basebackend.cache.admin.dto.CacheInfoDTO;
import com.basebackend.cache.metrics.CacheStatistics;
import com.basebackend.cache.service.CacheService;
import com.basebackend.cache.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 缓存管理 REST 控制器
 * 提供缓存管理 API，供管理后台调用
 *
 * 安全由消费端应用自行配置（如 admin-api 的 @RequiresPermission）
 */
@Slf4j
@RestController
@RequestMapping("/api/cache/admin")
@ConditionalOnProperty(prefix = "basebackend.cache.admin", name = "rest-enabled")
public class CacheAdminController {

    private final CacheService cacheService;
    private final RedisService redisService;

    public CacheAdminController(CacheService cacheService, RedisService redisService) {
        this.cacheService = cacheService;
        this.redisService = redisService;
    }

    /**
     * 列出所有缓存及摘要统计
     */
    @GetMapping("/caches")
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
     * 获取指定缓存的详细统计
     */
    @GetMapping("/caches/{name}")
    public CacheDetailDTO getCacheDetail(@PathVariable String name) {
        CacheStatistics stats = cacheService.getStatistics(name);
        long size = cacheService.getCacheSize(name);
        Set<String> sampleKeys = cacheService.keys(name + ":*");

        int limit = 100;
        Set<String> limitedKeys = sampleKeys.size() > limit
                ? sampleKeys.stream().limit(limit).collect(Collectors.toSet())
                : sampleKeys;

        return CacheDetailDTO.from(name, size, stats, limitedKeys);
    }

    /**
     * 获取匹配模式的缓存键列表
     */
    @GetMapping("/caches/{name}/keys")
    public Set<String> getCacheKeys(
            @PathVariable String name,
            @RequestParam(defaultValue = "*") String pattern) {
        String fullPattern = name + ":" + pattern;
        Set<String> keys = cacheService.keys(fullPattern);

        int limit = 100;
        if (keys.size() > limit) {
            return keys.stream().limit(limit).collect(Collectors.toSet());
        }
        return keys;
    }

    /**
     * 清除指定缓存
     */
    @DeleteMapping("/caches/{name}")
    public Map<String, Object> clearCache(@PathVariable String name) {
        log.info("REST: clearing cache '{}'", name);
        long evicted = cacheService.clearCache(name);
        return Map.of("cacheName", name, "evicted", evicted);
    }

    /**
     * 清除所有缓存（需要确认参数）
     */
    @PostMapping("/caches/clear-all")
    public Map<String, Object> clearAllCaches(@RequestParam boolean confirmed) {
        if (!confirmed) {
            return Map.of("error", "Must set confirmed=true to clear all caches");
        }
        log.warn("REST: clearing ALL caches");
        long evicted = cacheService.clearAllCaches();
        return Map.of("evicted", evicted);
    }

    /**
     * 重置指定缓存的统计数据
     */
    @PostMapping("/caches/{name}/reset-stats")
    public Map<String, Object> resetStats(@PathVariable String name) {
        cacheService.resetStatistics(name);
        return Map.of("cacheName", name, "statsReset", true);
    }

    /**
     * 缓存健康状态
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("redisAvailable", redisService.isRedisAvailable());
        result.put("circuitBreakerState", redisService.getCircuitBreakerState());
        return result;
    }
}
