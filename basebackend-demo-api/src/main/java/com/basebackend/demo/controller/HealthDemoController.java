package com.basebackend.demo.controller;

import com.basebackend.common.model.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查和基础演示控制器
 */
@Slf4j
@RestController
@RequestMapping("/api")
@Validated
public class HealthDemoController {

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "UP");
        data.put("timestamp", System.currentTimeMillis());
        data.put("service", "basebackend-demo-api");
        data.put("version", "1.0.0-SNAPSHOT");
        return Result.success(data);
    }

    /**
     * Hello World
     */
    @GetMapping("/hello")
    public Result<String> hello(@RequestParam(defaultValue = "World") String name) {
        log.info("Hello endpoint called with name: {}", name);
        return Result.success("Hello, " + name + "!");
    }

    /**
     * Echo 测试
     */
    @PostMapping("/echo")
    public Result<Map<String, Object>> echo(@RequestBody Map<String, Object> data) {
        log.info("Echo endpoint called with data: {}", data);
        Map<String, Object> result = new HashMap<>();
        result.put("received", data);
        result.put("timestamp", System.currentTimeMillis());
        return Result.success(result);
    }

    /**
     * 获取系统信息
     */
    @GetMapping("/system/info")
    public Result<Map<String, Object>> systemInfo() {
        Map<String, Object> info = new HashMap<>();

        // JVM 信息
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> jvmInfo = new HashMap<>();
        jvmInfo.put("totalMemory", runtime.totalMemory() / 1024 / 1024 + "MB");
        jvmInfo.put("freeMemory", runtime.freeMemory() / 1024 / 1024 + "MB");
        jvmInfo.put("maxMemory", runtime.maxMemory() / 1024 / 1024 + "MB");
        jvmInfo.put("processors", runtime.availableProcessors());

        // 系统属性
        Map<String, Object> systemProps = new HashMap<>();
        systemProps.put("javaVersion", System.getProperty("java.version"));
        systemProps.put("javaVendor", System.getProperty("java.vendor"));
        systemProps.put("osName", System.getProperty("os.name"));
        systemProps.put("osVersion", System.getProperty("os.version"));
        systemProps.put("osArch", System.getProperty("os.arch"));

        info.put("jvm", jvmInfo);
        info.put("system", systemProps);
        info.put("timestamp", System.currentTimeMillis());

        return Result.success(info);
    }

    /**
     * 测试异常处理
     */
    @GetMapping("/test/error")
    public Result<String> testError(@RequestParam(defaultValue = "false") boolean throwError) {
        if (throwError) {
            throw new RuntimeException("这是一个测试异常");
        }
        return Result.success("未抛出异常");
    }
}
