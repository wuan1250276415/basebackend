package com.basebackend.web.example;

import com.basebackend.web.annotation.Idempotent;
import com.basebackend.web.annotation.RateLimit;
import com.basebackend.web.annotation.XssClean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 示例控制器
 * 展示如何使用各种 Web 模块功能
 *
 * @author basebackend
 * @since 2025-11-23
 */
@Slf4j
@RestController
@RequestMapping("/api/example")
public class ExampleController {

    /**
     * 使用 Sentinel 限流
     * QPS: 10 次/秒
     */
    @RateLimit(
            resource = "example-api",
            threshold = 10.0,
            message = "请求过于频繁，请稍后重试"
    )
    @GetMapping("/limited")
    public Map<String, Object> rateLimited() {
        log.info("处理限流请求");
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "请求成功");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    /**
     * 使用 XSS 防护
     * 自动清理输入内容中的恶意脚本
     */
    @XssClean(
            strategy = XssClean.CleanStrategy.ESCAPE,
            enabled = true
    )
    @PostMapping("/xss-clean")
    public Map<String, Object> xssClean(@RequestBody Map<String, Object> request) {
        log.info("处理XSS清理请求: {}", request);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "内容已清理");
        result.put("originalData", request);
        return result;
    }

    /**
     * 使用幂等性控制
     * 防止重复提交
     */
    @Idempotent(
            keyPrefix = "example-create",
            expireTime = 300L,
            strategy = Idempotent.Strategy.REJECT,
            message = "请勿重复提交"
    )
    @PostMapping("/idempotent")
    public Map<String, Object> idempotent(@RequestBody Map<String, Object> request) {
        log.info("处理幂等性请求: {}", request);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "操作成功");
        result.put("data", request);
        return result;
    }

    /**
     * 组合使用多种功能
     * 限流 + XSS清理 + 幂等性
     */
    @RateLimit(resource = "example-combined", threshold = 50.0)
    @XssClean(strategy = XssClean.CleanStrategy.ESCAPE)
    @Idempotent(keyPrefix = "example-combined", expireTime = 300L)
    @PostMapping("/combined")
    public Map<String, Object> combined(@RequestBody Map<String, Object> request) {
        log.info("处理组合功能请求: {}", request);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "所有功能正常");
        result.put("data", request);
        return result;
    }

    /**
     * 跨域示例
     * 自动应用 CORS 配置
     */
    @GetMapping("/cors")
    public Map<String, Object> cors() {
        log.info("处理跨域请求");
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "跨域请求成功");
        return result;
    }

    /**
     * 性能监控示例
     * 自动收集响应时间等指标
     */
    @GetMapping("/performance")
    public Map<String, Object> performance() throws InterruptedException {
        // 模拟耗时操作
        Thread.sleep(100);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "性能测试完成");
        result.put("duration", "100ms");
        return result;
    }
}
