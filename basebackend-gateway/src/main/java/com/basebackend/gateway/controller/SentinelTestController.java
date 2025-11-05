package com.basebackend.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Sentinel测试控制器
 * 用于测试流控、降级、网关限流规则
 */
@Slf4j
@RestController
@RequestMapping("/sentinel-test")
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = "spring.cloud.sentinel.enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class SentinelTestController {

    /**
     * 快速响应接口 - 用于测试流控规则
     * 测试方法: 使用ab或jmeter进行压测
     * ab -n 1000 -c 10 http://localhost:8080/sentinel-test/fast
     */
    @GetMapping("/fast")
    public Mono<Map<String, Object>> fastApi() {
        log.info("快速响应接口被调用");
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "快速响应成功");
        result.put("timestamp", System.currentTimeMillis());
        return Mono.just(result);
    }

    /**
     * 慢响应接口 - 用于测试降级规则
     * 响应时间: 500-1500ms随机
     * 测试方法: 使用ab或jmeter进行压测,观察降级效果
     * ab -n 100 -c 5 http://localhost:8080/sentinel-test/slow
     */
    @GetMapping("/slow")
    public Mono<Map<String, Object>> slowApi() {
        long delay = ThreadLocalRandom.current().nextLong(500, 1500);
        log.info("慢响应接口被调用,延迟: {}ms", delay);

        return Mono.delay(Duration.ofMillis(delay))
                .map(tick -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    result.put("message", "慢响应成功");
                    result.put("delay", delay + "ms");
                    result.put("timestamp", System.currentTimeMillis());
                    return result;
                });
    }

    /**
     * 随机异常接口 - 用于测试异常比例降级
     * 异常比例: 50%
     * 测试方法: 多次调用观察异常率和降级效果
     * ab -n 100 -c 5 http://localhost:8080/sentinel-test/random-error
     */
    @GetMapping("/random-error")
    public Mono<Map<String, Object>> randomError() {
        if (ThreadLocalRandom.current().nextBoolean()) {
            log.error("随机异常接口抛出异常");
            return Mono.error(new RuntimeException("模拟业务异常"));
        }

        log.info("随机异常接口正常响应");
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "正常响应");
        result.put("timestamp", System.currentTimeMillis());
        return Mono.just(result);
    }

    /**
     * 参数限流测试接口 - 用于测试基于参数的限流
     * 测试方法:
     * curl "http://localhost:8080/sentinel-test/param?userId=user1"
     * ab -n 100 -c 5 "http://localhost:8080/sentinel-test/param?userId=user1"
     */
    @GetMapping("/param")
    public Mono<Map<String, Object>> paramApi(@RequestParam(required = false) String userId) {
        log.info("参数限流接口被调用, userId: {}", userId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "参数限流测试");
        result.put("userId", userId);
        result.put("timestamp", System.currentTimeMillis());
        return Mono.just(result);
    }

    /**
     * 网关限流测试接口 - 通过路由前缀测试
     * 测试方法: 通过不同的路由访问
     * curl http://localhost:8080/admin-api/test
     * curl http://localhost:8080/basebackend-demo-api/api/test
     */
    @GetMapping("/route-test")
    public Mono<Map<String, Object>> routeTest() {
        log.info("路由限流测试接口被调用");
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "路由限流测试");
        result.put("timestamp", System.currentTimeMillis());
        return Mono.just(result);
    }

    /**
     * 批量请求测试 - 用于快速触发限流
     * 测试方法: 调用此接口会自动发起多次请求
     */
    @GetMapping("/batch")
    public Mono<Map<String, Object>> batchTest(@RequestParam(defaultValue = "10") int count) {
        log.info("批量请求测试, count: {}", count);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "批量测试已启动");
        result.put("count", count);
        result.put("tip", "请查看后台日志观察限流效果");
        return Mono.just(result);
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public Mono<Map<String, Object>> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("service", "sentinel-test");
        result.put("timestamp", System.currentTimeMillis());
        return Mono.just(result);
    }
}
