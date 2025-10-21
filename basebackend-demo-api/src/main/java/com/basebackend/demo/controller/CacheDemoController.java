package com.basebackend.demo.controller;

import com.basebackend.common.model.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 缓存演示控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
@Validated
public class CacheDemoController {

    private final RedissonClient redissonClient;

    /**
     * 设置缓存
     */
    @PostMapping("/set")
    public Result<String> setCache(
            @RequestParam String key,
            @RequestParam String value,
            @RequestParam(required = false, defaultValue = "0") long ttl) {
        log.info("设置缓存 - Key: {}, Value: {}, TTL: {}秒", key, value, ttl);

        try {
            RBucket<String> bucket = redissonClient.getBucket(key);
            if (ttl > 0) {
                bucket.set(value, ttl, TimeUnit.SECONDS);
            } else {
                bucket.set(value);
            }
            return Result.success("缓存设置成功");
        } catch (Exception e) {
            log.error("设置缓存失败", e);
            return Result.error(500, "设置缓存失败: " + e.getMessage());
        }
    }

    /**
     * 获取缓存
     */
    @GetMapping("/get")
    public Result<String> getCache(@RequestParam String key) {
        log.info("获取缓存 - Key: {}", key);

        try {
            RBucket<String> bucket = redissonClient.getBucket(key);
            String value = bucket.get();
            if (value != null) {
                return Result.success(value);
            } else {
                return Result.error(404, "缓存不存在");
            }
        } catch (Exception e) {
            log.error("获取缓存失败", e);
            return Result.error(500, "获取缓存失败: " + e.getMessage());
        }
    }

    /**
     * 删除缓存
     */
    @DeleteMapping("/delete")
    public Result<String> deleteCache(@RequestParam String key) {
        log.info("删除缓存 - Key: {}", key);

        try {
            RBucket<String> bucket = redissonClient.getBucket(key);
            boolean deleted = bucket.delete();
            if (deleted) {
                return Result.success("缓存删除成功");
            } else {
                return Result.error(404, "缓存不存在");
            }
        } catch (Exception e) {
            log.error("删除缓存失败", e);
            return Result.error(500, "删除缓存失败: " + e.getMessage());
        }
    }

    /**
     * 设置Hash缓存
     */
    @PostMapping("/hash/set")
    public Result<String> setHashCache(
            @RequestParam String key,
            @RequestBody Map<String, Object> data) {
        log.info("设置Hash缓存 - Key: {}, Data: {}", key, data);

        try {
            RMap<String, Object> map = redissonClient.getMap(key);
            map.putAll(data);
            return Result.success("Hash缓存设置成功");
        } catch (Exception e) {
            log.error("设置Hash缓存失败", e);
            return Result.error(500, "设置Hash缓存失败: " + e.getMessage());
        }
    }

    /**
     * 获取Hash缓存
     */
    @GetMapping("/hash/get")
    public Result<Map<String, Object>> getHashCache(@RequestParam String key) {
        log.info("获取Hash缓存 - Key: {}", key);

        try {
            RMap<String, Object> map = redissonClient.getMap(key);
            if (!map.isEmpty()) {
                return Result.success(new HashMap<>(map));
            } else {
                return Result.error(404, "Hash缓存不存在");
            }
        } catch (Exception e) {
            log.error("获取Hash缓存失败", e);
            return Result.error(500, "获取Hash缓存失败: " + e.getMessage());
        }
    }

    /**
     * 检查缓存是否存在
     */
    @GetMapping("/exists")
    public Result<Map<String, Object>> exists(@RequestParam String key) {
        log.info("检查缓存是否存在 - Key: {}", key);

        try {
            RBucket<String> bucket = redissonClient.getBucket(key);
            boolean exists = bucket.isExists();

            Map<String, Object> result = new HashMap<>();
            result.put("key", key);
            result.put("exists", exists);

            if (exists) {
                long ttl = bucket.remainTimeToLive();
                result.put("ttl", ttl);
            }

            return Result.success(result);
        } catch (Exception e) {
            log.error("检查缓存失败", e);
            return Result.error(500, "检查缓存失败: " + e.getMessage());
        }
    }
}
