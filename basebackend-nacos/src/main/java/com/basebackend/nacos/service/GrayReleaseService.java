package com.basebackend.nacos.service;

import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.basebackend.nacos.enums.GrayStrategyType;
import com.basebackend.nacos.model.ConfigInfo;
import com.basebackend.nacos.model.GrayReleaseConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 灰度发布服务
 * 实现三种灰度策略：按IP、按百分比、按标签
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GrayReleaseService {

    private final NacosConfigService nacosConfigService;
    private final NamingService namingService;
    private final ObjectMapper objectMapper;

    /**
     * 开始灰度发布
     */
    public GrayReleaseResult startGrayRelease(ConfigInfo configInfo, GrayReleaseConfig grayConfig) {
        try {
            // 1. 获取服务实例列表
            List<Instance> allInstances = getServiceInstances(configInfo);
            if (allInstances == null || allInstances.isEmpty()) {
                return GrayReleaseResult.failed("未找到可用的服务实例");
            }

            // 2. 根据灰度策略筛选目标实例
            List<String> targetInstances = selectTargetInstances(allInstances, grayConfig);
            if (targetInstances.isEmpty()) {
                return GrayReleaseResult.failed("未找到符合灰度策略的实例");
            }

            log.info("灰度发布目标实例: {}", targetInstances);
            grayConfig.setEffectiveInstances(targetInstances);

            // 3. 为目标实例发布灰度配置
            // TODO: 这里需要实现针对特定实例的配置推送
            // Nacos 2.x 支持通过metadata标签实现实例级配置隔离

            return GrayReleaseResult.success("灰度发布启动成功", targetInstances);

        } catch (Exception e) {
            log.error("灰度发布失败", e);
            return GrayReleaseResult.failed("灰度发布失败：" + e.getMessage());
        }
    }

    /**
     * 灰度全量发布
     */
    public boolean promoteToFull(ConfigInfo configInfo, GrayReleaseConfig grayConfig) {
        try {
            // 将灰度配置发布为正式配置
            return nacosConfigService.publishConfig(configInfo);
        } catch (Exception e) {
            log.error("灰度全量发布失败", e);
            return false;
        }
    }

    /**
     * 回滚灰度发布
     */
    public boolean rollbackGrayRelease(ConfigInfo originalConfig, GrayReleaseConfig grayConfig) {
        try {
            // 将配置回滚到灰度前的版本
            return nacosConfigService.publishConfig(originalConfig);
        } catch (Exception e) {
            log.error("灰度回滚失败", e);
            return false;
        }
    }

    /**
     * 根据灰度策略筛选目标实例
     */
    private List<String> selectTargetInstances(List<Instance> allInstances, GrayReleaseConfig grayConfig) {
        GrayStrategyType strategy = GrayStrategyType.fromCode(grayConfig.getStrategyType());
        if (strategy == null) {
            log.warn("未知的灰度策略类型: {}", grayConfig.getStrategyType());
            return Collections.emptyList();
        }

        switch (strategy) {
            case IP:
                return selectByIp(allInstances, grayConfig.getTargetInstances());
            case PERCENTAGE:
                return selectByPercentage(allInstances, grayConfig.getPercentage());
            case LABEL:
                return selectByLabel(allInstances, grayConfig.getLabels());
            default:
                return Collections.emptyList();
        }
    }

    /**
     * 按IP筛选实例
     */
    private List<String> selectByIp(List<Instance> allInstances, String targetIps) {
        if (targetIps == null || targetIps.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> ipSet = Arrays.stream(targetIps.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());

        return allInstances.stream()
                .filter(instance -> ipSet.contains(instance.getIp()))
                .map(instance -> instance.getIp() + ":" + instance.getPort())
                .collect(Collectors.toList());
    }

    /**
     * 按百分比筛选实例
     */
    private List<String> selectByPercentage(List<Instance> allInstances, Integer percentage) {
        if (percentage == null || percentage <= 0 || percentage > 100) {
            return Collections.emptyList();
        }

        int count = (int) Math.ceil(allInstances.size() * percentage / 100.0);

        // 随机选择指定数量的实例
        Collections.shuffle(allInstances);
        return allInstances.stream()
                .limit(count)
                .map(instance -> instance.getIp() + ":" + instance.getPort())
                .collect(Collectors.toList());
    }

    /**
     * 按标签筛选实例
     */
    private List<String> selectByLabel(List<Instance> allInstances, String labelsJson) {
        if (labelsJson == null || labelsJson.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            Map<String, String> requiredLabels = objectMapper.readValue(
                    labelsJson,
                    new TypeReference<Map<String, String>>() {}
            );

            return allInstances.stream()
                    .filter(instance -> matchesLabels(instance.getMetadata(), requiredLabels))
                    .map(instance -> instance.getIp() + ":" + instance.getPort())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("解析标签JSON失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 检查实例标签是否匹配
     */
    private boolean matchesLabels(Map<String, String> instanceMetadata, Map<String, String> requiredLabels) {
        if (instanceMetadata == null || requiredLabels == null) {
            return false;
        }

        for (Map.Entry<String, String> entry : requiredLabels.entrySet()) {
            String value = instanceMetadata.get(entry.getKey());
            if (!entry.getValue().equals(value)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 获取服务实例列表
     */
    private List<Instance> getServiceInstances(ConfigInfo configInfo) {
        try {
            // 从dataId中提取服务名（简化处理，实际需要更复杂的逻辑）
            String serviceName = extractServiceName(configInfo.getDataId());
            if (serviceName == null) {
                return Collections.emptyList();
            }

            return namingService.getAllInstances(serviceName);
        } catch (Exception e) {
            log.error("获取服务实例失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 从dataId中提取服务名
     */
    private String extractServiceName(String dataId) {
        // 简化处理：假设配置文件名格式为 {serviceName}-{env}.yml
        if (dataId.contains("-")) {
            return dataId.split("-")[0];
        }
        return null;
    }

    /**
     * 灰度发布结果
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class GrayReleaseResult {
        private boolean success;
        private String message;
        private List<String> targetInstances;

        public static GrayReleaseResult success(String message, List<String> targetInstances) {
            return new GrayReleaseResult(true, message, targetInstances);
        }

        public static GrayReleaseResult failed(String message) {
            return new GrayReleaseResult(false, message, Collections.emptyList());
        }
    }
}
