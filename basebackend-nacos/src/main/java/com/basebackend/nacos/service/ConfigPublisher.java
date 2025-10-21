package com.basebackend.nacos.service;

import com.basebackend.nacos.enums.PublishType;
import com.basebackend.nacos.model.ConfigInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 配置发布服务
 * 根据配置类型（关键/普通）决定发布策略
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigPublisher {

    private final NacosConfigService nacosConfigService;

    /**
     * 发布配置
     * @param configInfo 配置信息
     * @param force 是否强制发布（忽略关键配置检查）
     * @return 发布结果
     */
    public PublishResult publishConfig(ConfigInfo configInfo, boolean force) {
        try {
            // 检查是否为关键配置
            if (!force && Boolean.TRUE.equals(configInfo.getIsCritical())) {
                log.info("配置{}为关键配置，需要手动审核发布", configInfo.getDataId());
                return PublishResult.pending("配置为关键配置，需要手动审核发布");
            }

            // 计算MD5
            String md5 = nacosConfigService.calculateMd5(configInfo.getContent());
            configInfo.setMd5(md5);

            // 发布到Nacos
            boolean success = nacosConfigService.publishConfig(configInfo);

            if (success) {
                log.info("配置{}发布成功", configInfo.getDataId());
                return PublishResult.success("配置发布成功");
            } else {
                log.error("配置{}发布失败", configInfo.getDataId());
                return PublishResult.failed("配置发布失败");
            }

        } catch (Exception e) {
            log.error("配置{}发布异常", configInfo.getDataId(), e);
            return PublishResult.failed("配置发布异常：" + e.getMessage());
        }
    }

    /**
     * 自动发布（用于非关键配置）
     */
    public PublishResult autoPublish(ConfigInfo configInfo) {
        return publishConfig(configInfo, false);
    }

    /**
     * 手动发布（用于关键配置）
     */
    public PublishResult manualPublish(ConfigInfo configInfo) {
        return publishConfig(configInfo, true);
    }

    /**
     * 批量发布
     */
    public BatchPublishResult batchPublish(java.util.List<ConfigInfo> configInfoList, boolean force) {
        int successCount = 0;
        int failedCount = 0;
        java.util.List<String> errors = new java.util.ArrayList<>();

        for (ConfigInfo configInfo : configInfoList) {
            PublishResult result = publishConfig(configInfo, force);
            if ("success".equals(result.getStatus())) {
                successCount++;
            } else {
                failedCount++;
                errors.add(configInfo.getDataId() + ": " + result.getMessage());
            }
        }

        return new BatchPublishResult(successCount, failedCount, errors);
    }

    /**
     * 发布结果
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class PublishResult {
        private String status; // success/failed/pending
        private String message;

        public static PublishResult success(String message) {
            return new PublishResult("success", message);
        }

        public static PublishResult failed(String message) {
            return new PublishResult("failed", message);
        }

        public static PublishResult pending(String message) {
            return new PublishResult("pending", message);
        }
    }

    /**
     * 批量发布结果
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class BatchPublishResult {
        private int successCount;
        private int failedCount;
        private java.util.List<String> errors;
    }
}
