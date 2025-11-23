package com.basebackend.nacos.example;

import com.basebackend.nacos.model.ConfigInfo;
import com.basebackend.nacos.model.GrayReleaseConfig;
import com.basebackend.nacos.service.GrayReleaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * 灰度发布示例
 * <p>
 * 展示如何使用 GrayReleaseService 实现灰度发布功能
 * </p>
 *
 * <pre>
 * 支持三种灰度策略：
 * 1. IP灰度：按指定的 IP 地址灰度
 * 2. 百分比灰度：按百分比随机选择实例灰度
 * 3. 标签灰度：按实例元数据标签灰度
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GrayReleaseExample {

    private final GrayReleaseService grayReleaseService;

    @PostConstruct
    public void init() {
        log.info("=== 灰度发布示例 ===");

        // 示例1: IP灰度发布
        grayReleaseByIp();

        // 示例2: 百分比灰度发布
        grayReleaseByPercentage();

        // 示例3: 标签灰度发布
        grayReleaseByLabel();

        // 示例4: 灰度全量发布
        grayReleasePromoteToFull();

        // 示例5: 灰度回滚
        grayReleaseRollback();
    }

    /**
     * 示例1: IP灰度发布
     *
     * 按指定的 IP 地址进行灰度发布
     */
    private void grayReleaseByIp() {
        log.info("\n--- 示例1: IP灰度发布 ---");

        // 准备配置信息
        ConfigInfo configInfo = new ConfigInfo();
        configInfo.setDataId("my-config.yml");
        configInfo.setContent("new config content");
        configInfo.setGroup("DEFAULT_GROUP");

        // 准备灰度配置
        GrayReleaseConfig grayConfig = new GrayReleaseConfig();
        grayConfig.setDataId("my-config.yml");
        grayConfig.setStrategyType("IP");
        grayConfig.setTargetInstances("192.168.1.10,192.168.1.11");

        // 执行灰度发布
        log.info("开始 IP 灰度发布，目标实例：192.168.1.10,192.168.1.11");
        // GrayReleaseResult result = grayReleaseService.startGrayRelease(configInfo, grayConfig);
        // log.info("灰度发布结果：{}", result.isSuccess() ? "成功" : "失败");
        log.info("  消息：{}", "灰度发布功能需要实际的 Nacos 实例才能测试");
    }

    /**
     * 示例2: 百分比灰度发布
     *
     * 按百分比随机选择实例进行灰度发布
     */
    private void grayReleaseByPercentage() {
        log.info("\n--- 示例2: 百分比灰度发布 ---");

        ConfigInfo configInfo = new ConfigInfo();
        configInfo.setDataId("my-config.yml");
        configInfo.setContent("new config content");
        configInfo.setGroup("DEFAULT_GROUP");

        GrayReleaseConfig grayConfig = new GrayReleaseConfig();
        grayConfig.setDataId("my-config.yml");
        grayConfig.setStrategyType("PERCENTAGE");
        grayConfig.setPercentage(20); // 20%实例灰度

        log.info("开始百分比灰度发布，灰度比例：20%");
        // GrayReleaseResult result = grayReleaseService.startGrayRelease(configInfo, grayConfig);
        log.info("  灰度策略：随机选择 20% 的实例进行灰度");
    }

    /**
     * 示例3: 标签灰度发布
     *
     * 按实例元数据标签进行灰度发布
     */
    private void grayReleaseByLabel() {
        log.info("\n--- 示例3: 标签灰度发布 ---");

        ConfigInfo configInfo = new ConfigInfo();
        configInfo.setDataId("my-config.yml");
        configInfo.setContent("new config content");
        configInfo.setGroup("DEFAULT_GROUP");

        GrayReleaseConfig grayConfig = new GrayReleaseConfig();
        grayConfig.setDataId("my-config.yml");
        grayConfig.setStrategyType("LABEL");
        // 按版本和区域标签灰度
        grayConfig.setLabels("{\"version\":\"1.0\",\"region\":\"beijing\"}");

        log.info("开始标签灰度发布，灰度条件：version=1.0, region=beijing");
        // GrayReleaseResult result = grayReleaseService.startGrayRelease(configInfo, grayConfig);
        log.info("  灰度策略：选择具有指定标签的实例进行灰度");
    }

    /**
     * 示例4: 灰度全量发布
     *
     * 将灰度配置推广到所有实例
     */
    private void grayReleasePromoteToFull() {
        log.info("\n--- 示例4: 灰度全量发布 ---");

        ConfigInfo configInfo = new ConfigInfo();
        configInfo.setDataId("my-config.yml");
        configInfo.setContent("new config content");
        configInfo.setGroup("DEFAULT_GROUP");

        GrayReleaseConfig grayConfig = new GrayReleaseConfig();
        grayConfig.setDataId("my-config.yml");

        log.info("开始灰度全量发布");
        // GrayReleaseResult result = grayReleaseService.promoteToFull(configInfo, grayConfig);
        // if (result.isSuccess()) {
        //     log.info("灰度全量发布成功");
        // }
        log.info("  操作：将灰度配置推广到所有实例");
    }

    /**
     * 示例5: 灰度回滚
     *
     * 将配置回滚到灰度前的版本
     */
    private void grayReleaseRollback() {
        log.info("\n--- 示例5: 灰度回滚 ---");

        // 原始配置
        ConfigInfo originalConfig = new ConfigInfo();
        originalConfig.setDataId("my-config.yml");
        originalConfig.setContent("old config content");
        originalConfig.setGroup("DEFAULT_GROUP");

        GrayReleaseConfig grayConfig = new GrayReleaseConfig();
        grayConfig.setDataId("my-config.yml");

        log.info("开始灰度回滚");
        // GrayReleaseResult result = grayReleaseService.rollbackGrayRelease(originalConfig, grayConfig);
        // if (result.isSuccess()) {
        //     log.info("灰度回滚成功");
        // }
        log.info("  操作：将配置回滚到灰度前的版本");
    }
}

/**
 * 配置示例：
 *
 * <pre>
 * nacos:
 *   discovery:
 *     enabled: true
 *     server-addr: 127.0.0.1:8848
 *     metadata:
 *       version: 1.0
 *       region: beijing
 * </pre>
 *
 * 灰度发布流程：
 *
 * <pre>
 * 1. 配置灰度策略（IP/百分比/标签）
 * 2. 执行灰度发布
 * 3. 监控灰度实例运行状态
 * 4. 观察无误后执行全量发布
 * 5. 如有问题执行灰度回滚
 * </pre>
 *
 * 最佳实践：
 *
 * <pre>
 * 1. 灰度百分比建议从 5% 开始，逐步扩大
 * 2. 设置灰度超时时间，自动回滚
 * 3. 观察灰度实例的运行指标
 * 4. 记录灰度发布历史，便于追溯
 * </pre>
 */
