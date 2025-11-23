package com.basebackend.nacos.service;

import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.basebackend.nacos.enums.GrayStrategyType;
import com.basebackend.nacos.event.GrayReleaseHistoryEvent;
import com.basebackend.nacos.model.ConfigInfo;
import com.basebackend.nacos.model.GrayReleaseConfig;
import com.basebackend.nacos.model.GrayReleaseHistory;
import com.basebackend.nacos.repository.GrayReleaseHistoryRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 灰度发布服务
 * <p>
 * 实现三种灰度策略：按IP、按百分比、按标签
 * 支持灰度配置发布、全量发布和回滚功能。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GrayReleaseService {

    private final NacosConfigService nacosConfigService;
    private final NamingService namingService;
    private final ObjectMapper objectMapper;
    private final GrayReleaseHistoryRepository historyRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 开始灰度发布
     * <p>
     * 完整实现灰度发布流程：
     * 1. 验证灰度配置
     * 2. 查询目标实例
     * 3. 应用灰度策略
     * 4. 发布配置到目标实例
     * 5. 记录灰度发布历史
     * </p>
     */
    public GrayReleaseResult startGrayRelease(ConfigInfo configInfo, GrayReleaseConfig grayConfig) {
        try {
            // 1. 验证灰度配置
            validateGrayConfig(grayConfig);

            // 2. 获取服务实例列表
            List<Instance> allInstances = getServiceInstances(configInfo);
            if (CollectionUtils.isEmpty(allInstances)) {
                return GrayReleaseResult.failed("未找到可用的服务实例");
            }

            // 3. 根据灰度策略筛选目标实例
            List<String> targetInstances = selectTargetInstances(allInstances, grayConfig);
            if (CollectionUtils.isEmpty(targetInstances)) {
                return GrayReleaseResult.failed("未找到符合灰度策略的实例");
            }

            log.info("灰度发布目标实例: {}", targetInstances);

            // 4. 应用灰度配置（使用Nacos元数据机制）
            boolean applyConfigSuccess = applyGrayConfig(configInfo, grayConfig, targetInstances);
            if (!applyConfigSuccess) {
                return GrayReleaseResult.failed("灰度配置发布失败");
            }

            // 5. 记录灰度发布历史
            recordGrayHistory(grayConfig, targetInstances);

            // 6. 设置灰度实例
            grayConfig.setEffectiveInstances(targetInstances);
            grayConfig.setStatus("GRAYING");
            grayConfig.setStartTime(LocalDateTime.now());

            return GrayReleaseResult.success(
                String.format("灰度发布启动成功，目标实例数：%d", targetInstances.size()),
                targetInstances
            );

        } catch (Exception e) {
            log.error("灰度发布启动失败", e);
            return GrayReleaseResult.failed("灰度发布启动失败：" + e.getMessage());
        }
    }

    /**
     * 灰度全量发布
     * <p>
     * 将灰度配置推广到所有实例
     * </p>
     */
    public GrayReleaseResult promoteToFull(ConfigInfo configInfo, GrayReleaseConfig grayConfig) {
        try {
            if (grayConfig == null || !StringUtils.hasText(grayConfig.getDataId())) {
                return GrayReleaseResult.failed("灰度配置信息无效");
            }

            // 将灰度配置发布为正式配置
            boolean success = nacosConfigService.publishConfig(configInfo);
            if (success) {
                // 更新灰度状态
                grayConfig.setStatus("PROMOTED");
                grayConfig.setPromoteTime(LocalDateTime.now());

                // 记录全量发布历史
                List<String> effectiveInstances = grayConfig.getEffectiveInstances();
                if (effectiveInstances == null) {
                    effectiveInstances = Collections.emptyList();
                }
                recordGrayHistory(grayConfig, effectiveInstances,
                        GrayReleaseHistory.OperationType.PROMOTE.getCode(),
                        GrayReleaseHistory.OperationResult.SUCCESS.getCode(),
                        null, "灰度全量发布成功");

                log.info("灰度全量发布成功：{}", grayConfig.getDataId());
                return GrayReleaseResult.success("灰度全量发布成功", Collections.emptyList());
            } else {
                // 记录失败历史
                recordGrayHistoryFailed(grayConfig, 
                        GrayReleaseHistory.OperationType.PROMOTE.getCode(), 
                        "配置发布失败");
                return GrayReleaseResult.failed("灰度全量发布失败");
            }

        } catch (Exception e) {
            log.error("灰度全量发布失败", e);
            // 记录异常历史
            recordGrayHistoryFailed(grayConfig, 
                    GrayReleaseHistory.OperationType.PROMOTE.getCode(), 
                    e.getMessage());
            return GrayReleaseResult.failed("灰度全量发布失败：" + e.getMessage());
        }
    }

    /**
     * 回滚灰度发布
     * <p>
     * 将配置回滚到灰度前的版本
     * </p>
     */
    public GrayReleaseResult rollbackGrayRelease(ConfigInfo originalConfig, GrayReleaseConfig grayConfig) {
        try {
            if (originalConfig == null) {
                return GrayReleaseResult.failed("原始配置信息无效");
            }

            // 将配置回滚到灰度前的版本
            boolean success = nacosConfigService.publishConfig(originalConfig);
            if (success) {
                // 更新灰度状态
                grayConfig.setStatus("ROLLED_BACK");
                grayConfig.setRollbackTime(LocalDateTime.now());

                // 记录回滚历史
                List<String> effectiveInstances = grayConfig.getEffectiveInstances();
                if (effectiveInstances == null) {
                    effectiveInstances = Collections.emptyList();
                }
                recordGrayHistory(grayConfig, effectiveInstances,
                        GrayReleaseHistory.OperationType.ROLLBACK.getCode(),
                        GrayReleaseHistory.OperationResult.SUCCESS.getCode(),
                        null, "灰度回滚成功，已恢复原始配置");

                log.info("灰度回滚成功：{}", grayConfig.getDataId());
                return GrayReleaseResult.success("灰度回滚成功", Collections.emptyList());
            } else {
                // 记录失败历史
                recordGrayHistoryFailed(grayConfig, 
                        GrayReleaseHistory.OperationType.ROLLBACK.getCode(), 
                        "配置回滚发布失败");
                return GrayReleaseResult.failed("灰度回滚失败");
            }

        } catch (Exception e) {
            log.error("灰度回滚失败", e);
            // 记录异常历史
            recordGrayHistoryFailed(grayConfig, 
                    GrayReleaseHistory.OperationType.ROLLBACK.getCode(), 
                    e.getMessage());
            return GrayReleaseResult.failed("灰度回滚失败：" + e.getMessage());
        }
    }

    /**
     * 验证灰度配置
     */
    private void validateGrayConfig(GrayReleaseConfig grayConfig) {
        if (grayConfig == null) {
            throw new IllegalArgumentException("灰度配置不能为空");
        }

        if (!StringUtils.hasText(grayConfig.getDataId())) {
            throw new IllegalArgumentException("配置Data ID不能为空");
        }

        if (!StringUtils.hasText(grayConfig.getStrategyType())) {
            throw new IllegalArgumentException("灰度策略类型不能为空");
        }

        GrayStrategyType strategy = GrayStrategyType.fromCode(grayConfig.getStrategyType());
        if (strategy == null) {
            throw new IllegalArgumentException("不支持的灰度策略：" + grayConfig.getStrategyType());
        }

        // 根据策略类型验证参数
        switch (strategy) {
            case IP:
                if (!StringUtils.hasText(grayConfig.getTargetInstances())) {
                    throw new IllegalArgumentException("IP灰度策略必须指定目标实例");
                }
                break;
            case PERCENTAGE:
                if (grayConfig.getPercentage() == null || grayConfig.getPercentage() <= 0 || grayConfig.getPercentage() > 100) {
                    throw new IllegalArgumentException("百分比灰度策略的百分比必须在1-100之间");
                }
                break;
            case LABEL:
                if (!StringUtils.hasText(grayConfig.getLabels())) {
                    throw new IllegalArgumentException("标签灰度策略必须指定标签");
                }
                break;
        }
    }

    /**
     * 应用灰度配置到目标实例
     */
    private boolean applyGrayConfig(ConfigInfo configInfo, GrayReleaseConfig grayConfig, List<String> targetInstances) {
        try {
            // 获取服务名（优先使用ConfigInfo中的，备用从dataId解析）
            String serviceName = configInfo.getServiceName();
            if (!StringUtils.hasText(serviceName)) {
                serviceName = extractServiceName(configInfo.getDataId());
            }

            // 获取namespace和group
            String namespace = configInfo.getNamespace();
            if (!StringUtils.hasText(namespace)) {
                namespace = "public";
            }

            String group = configInfo.getGroup();
            if (!StringUtils.hasText(group)) {
                group = "DEFAULT_GROUP";
            }

            // 方法1：通过Nacos元数据实现实例级配置隔离
            // 为目标实例添加灰度标记元数据
            for (String instanceAddress : targetInstances) {
                String[] parts = instanceAddress.split(":");
                if (parts.length != 2) {
                    log.warn("实例地址格式错误：{}", instanceAddress);
                    continue;
                }

                String ip = parts[0];
                int port = Integer.parseInt(parts[1]);

                // 获取实例列表
                List<Instance> instances = namingService.getAllInstances(serviceName);
                for (Instance instance : instances) {
                    if (instance.getIp().equals(ip) && instance.getPort() == port) {
                        // 添加灰度元数据
                        Map<String, String> metadata = instance.getMetadata();
                        if (metadata == null) {
                            metadata = new HashMap<>();
                        }
                        metadata.put("gray-release", "true");
                        metadata.put("gray-data-id", configInfo.getDataId());
                        metadata.put("gray-start-time", String.valueOf(System.currentTimeMillis()));
                        instance.setMetadata(metadata);

                        // 注册实例以更新元数据（保持cluster不变）
                        namingService.registerInstance(serviceName, group, instance);
                        log.info("为实例 {}:{} 添加灰度元数据成功（namespace={}, group={}）", ip, port, namespace, group);
                    }
                }
            }

            // 方法2：发布配置到指定分组
            // 为灰度实例创建专门的分组
            String grayGroup = "GRAY_" + grayConfig.getDataId();
            ConfigInfo grayConfigInfo = ConfigInfo.builder()
                    .dataId(configInfo.getDataId())
                    .content(configInfo.getContent())
                    .group(grayGroup)
                    .namespace(namespace)
                    .environment(configInfo.getEnvironment())
                    .tenantId(configInfo.getTenantId())
                    .appId(configInfo.getAppId())
                    .build();

            boolean publishSuccess = nacosConfigService.publishConfig(grayConfigInfo);
            if (publishSuccess) {
                log.info("灰度配置发布到分组 {} 成功", grayGroup);
            }

            return publishSuccess;

        } catch (Exception e) {
            log.error("应用灰度配置失败", e);
            return false;
        }
    }

    /**
     * 记录灰度发布历史
     * <p>
     * 完整实现持久化逻辑：
     * 1. 构建历史记录对象
     * 2. 保存到仓储（支持内存/数据库等多种实现）
     * 3. 发布事件通知外部系统
     * </p>
     *
     * @param grayConfig      灰度配置
     * @param targetInstances 目标实例列表
     */
    private void recordGrayHistory(GrayReleaseConfig grayConfig, List<String> targetInstances) {
        recordGrayHistory(grayConfig, targetInstances, 
                GrayReleaseHistory.OperationType.START.getCode(),
                GrayReleaseHistory.OperationResult.SUCCESS.getCode(),
                null, null);
    }

    /**
     * 记录灰度发布历史（完整版本）
     *
     * @param grayConfig      灰度配置
     * @param targetInstances 目标实例列表
     * @param operationType   操作类型
     * @param result          操作结果
     * @param failureReason   失败原因（可选）
     * @param remark          备注信息（可选）
     */
    private void recordGrayHistory(GrayReleaseConfig grayConfig, List<String> targetInstances,
                                   String operationType, String result,
                                   String failureReason, String remark) {
        try {
            // 1. 构建历史记录对象
            GrayReleaseHistory history = GrayReleaseHistory.builder()
                    .grayConfigId(grayConfig.getId())
                    .dataId(grayConfig.getDataId())
                    .strategyType(grayConfig.getStrategyType())
                    .percentage(grayConfig.getPercentage())
                    .labels(grayConfig.getLabels())
                    .targetInstances(String.join(",", targetInstances))
                    .effectiveInstanceCount(targetInstances.size())
                    .operationType(operationType)
                    .grayContent(grayConfig.getGrayContent())
                    .result(result)
                    .failureReason(failureReason)
                    .remark(remark)
                    .operationTime(LocalDateTime.now())
                    .createTime(LocalDateTime.now())
                    .build();

            // 2. 保存到仓储
            GrayReleaseHistory savedHistory = historyRepository.save(history);
            
            log.info("灰度发布历史记录已保存: id={}, dataId={}, operationType={}, result={}, 目标实例数={}",
                    savedHistory.getId(),
                    savedHistory.getDataId(),
                    savedHistory.getOperationType(),
                    savedHistory.getResult(),
                    savedHistory.getEffectiveInstanceCount());

            // 3. 发布事件通知外部系统（支持异步处理、数据库持久化等扩展）
            eventPublisher.publishEvent(new GrayReleaseHistoryEvent(this, savedHistory));
            log.debug("灰度发布历史事件已发布: {}", savedHistory.getDataId());

        } catch (Exception e) {
            // 历史记录失败不应影响主流程，仅记录错误日志
            log.error("记录灰度发布历史失败: dataId={}, operationType={}", 
                    grayConfig.getDataId(), operationType, e);
        }
    }

    /**
     * 记录灰度发布失败历史
     *
     * @param grayConfig    灰度配置
     * @param operationType 操作类型
     * @param failureReason 失败原因
     */
    private void recordGrayHistoryFailed(GrayReleaseConfig grayConfig, String operationType, String failureReason) {
        recordGrayHistory(grayConfig, Collections.emptyList(), operationType,
                GrayReleaseHistory.OperationResult.FAILED.getCode(), failureReason, null);
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
        if (!StringUtils.hasText(targetIps)) {
            return Collections.emptyList();
        }

        Set<String> ipSet = Arrays.stream(targetIps.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
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
            log.warn("百分比必须在1-100之间，当前值: {}", percentage);
            return Collections.emptyList();
        }

        int count = (int) Math.ceil(allInstances.size() * percentage / 100.0);
        List<Instance> shuffled = new ArrayList<>(allInstances);
        Collections.shuffle(shuffled);

        return shuffled.stream()
                .limit(count)
                .map(instance -> instance.getIp() + ":" + instance.getPort())
                .collect(Collectors.toList());
    }

    /**
     * 按标签筛选实例
     */
    private List<String> selectByLabel(List<Instance> allInstances, String labelsJson) {
        if (!StringUtils.hasText(labelsJson)) {
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
            log.error("解析标签配置失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 检查实例标签是否匹配
     */
    private boolean matchesLabels(Map<String, String> instanceMetadata, Map<String, String> requiredLabels) {
        if (CollectionUtils.isEmpty(requiredLabels)) {
            return false;
        }

        if (CollectionUtils.isEmpty(instanceMetadata)) {
            return false;
        }

        return requiredLabels.entrySet().stream()
                .allMatch(entry -> {
                    String value = instanceMetadata.get(entry.getKey());
                    return value != null && value.equals(entry.getValue());
                });
    }

    /**
     * 获取服务实例列表
     */
    private List<Instance> getServiceInstances(ConfigInfo configInfo) {
        try {
            // 优先使用ConfigInfo中的serviceName
            String serviceName = configInfo.getServiceName();
            if (!StringUtils.hasText(serviceName)) {
                // 备用从dataId解析
                serviceName = extractServiceName(configInfo.getDataId());
                if (!StringUtils.hasText(serviceName)) {
                    log.warn("无法从Data ID中提取服务名: {}", configInfo.getDataId());
                    return Collections.emptyList();
                }
            }

            // 获取namespace和group
            String namespace = configInfo.getNamespace();
            if (!StringUtils.hasText(namespace)) {
                namespace = "public";
            }

            String group = configInfo.getGroup();
            if (!StringUtils.hasText(group)) {
                group = "DEFAULT_GROUP";
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
        return dataId; // 如果没有分隔符，直接返回
    }

    /**
     * 灰度发布结果
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
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
