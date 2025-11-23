package com.basebackend.nacos.example;

import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Properties;

/**
 * 基本使用示例
 * <p>
 * 展示如何注入和使用 Nacos ConfigService 和 NamingService
 * </p>
 *
 * <pre>
 * 配置示例：
 * nacos:
 *   config:
 *     enabled: true
 *     server-addr: 127.0.0.1:8848
 *   discovery:
 *     enabled: true
 *     server-addr: 127.0.0.1:8848
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BasicUsageExample {

    private final ConfigService configService;
    private final NamingService namingService;

    @PostConstruct
    public void init() {
        log.info("=== 基本使用示例 ===");

        // 示例1: 获取配置
        fetchConfig();

        // 示例2: 发布配置
        publishConfig();

        // 示例3: 注册服务实例
        registerServiceInstance();

        // 示例4: 查询服务实例
        queryServiceInstances();
    }

    /**
     * 示例1: 获取配置
     */
    private void fetchConfig() {
        try {
            log.info("\n--- 示例1: 获取配置 ---");
            String content = configService.getConfig(
                "my-config.yml",  // dataId
                "DEFAULT_GROUP",  // group
                5000L            // timeout
            );
            log.info("配置内容：{}", content);
        } catch (Exception e) {
            log.error("获取配置失败", e);
        }
    }

    /**
     * 示例2: 发布配置
     */
    private void publishConfig() {
        try {
            log.info("\n--- 示例2: 发布配置 ---");
            boolean success = configService.publishConfig(
                "my-config.yml",  // dataId
                "content: value", // content
                "DEFAULT_GROUP"   // group
            );
            log.info("配置发布结果：{}", success ? "成功" : "失败");
        } catch (Exception e) {
            log.error("发布配置失败", e);
        }
    }

    /**
     * 示例3: 注册服务实例
     */
    private void registerServiceInstance() {
        try {
            log.info("\n--- 示例3: 注册服务实例 ---");
            namingService.registerInstance(
                "my-service",     // serviceName
                "127.0.0.1",      // ip
                8080,             // port
                "DEFAULT"         // clusterName
            );
            log.info("服务实例注册成功：127.0.0.1:8080");
        } catch (Exception e) {
            log.error("注册服务实例失败", e);
        }
    }

    /**
     * 示例4: 查询服务实例
     */
    private void queryServiceInstances() {
        try {
            log.info("\n--- 示例4: 查询服务实例 ---");
            List<Instance> instances = namingService.getAllInstances("my-service");
            log.info("查询到 {} 个实例", instances.size());
            instances.forEach(instance -> {
                log.info("  - {}:{} (健康: {}, 权重: {})",
                    instance.getIp(),
                    instance.getPort(),
                    instance.isHealthy(),
                    instance.getWeight()
                );
            });
        } catch (Exception e) {
            log.error("查询服务实例失败", e);
        }
    }
}
