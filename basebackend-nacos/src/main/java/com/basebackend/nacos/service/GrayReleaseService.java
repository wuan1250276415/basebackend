/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.alibaba.nacos.api.naming.NamingService
 *  com.alibaba.nacos.api.naming.pojo.Instance
 *  com.fasterxml.jackson.core.type.TypeReference
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.context.ApplicationEvent
 *  org.springframework.context.ApplicationEventPublisher
 *  org.springframework.stereotype.Service
 *  org.springframework.util.CollectionUtils
 *  org.springframework.util.StringUtils
 */
package com.basebackend.nacos.service;

import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.basebackend.nacos.enums.GrayStrategyType;
import com.basebackend.nacos.event.GrayReleaseHistoryEvent;
import com.basebackend.nacos.model.ConfigInfo;
import com.basebackend.nacos.model.GrayReleaseConfig;
import com.basebackend.nacos.model.GrayReleaseHistory;
import com.basebackend.nacos.repository.GrayReleaseHistoryRepository;
import com.basebackend.nacos.service.NacosConfigService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class GrayReleaseService {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(GrayReleaseService.class);
    private final NacosConfigService nacosConfigService;
    private final NamingService namingService;
    private final ObjectMapper objectMapper;
    private final GrayReleaseHistoryRepository historyRepository;
    private final ApplicationEventPublisher eventPublisher;

    public GrayReleaseResult startGrayRelease(ConfigInfo configInfo, GrayReleaseConfig grayConfig) {
        try {
            this.validateGrayConfig(grayConfig);
            List<Instance> allInstances = this.getServiceInstances(configInfo);
            if (CollectionUtils.isEmpty(allInstances)) {
                return GrayReleaseResult.failed("\u672a\u627e\u5230\u53ef\u7528\u7684\u670d\u52a1\u5b9e\u4f8b");
            }
            List<String> targetInstances = this.selectTargetInstances(allInstances, grayConfig);
            if (CollectionUtils.isEmpty(targetInstances)) {
                return GrayReleaseResult.failed("\u672a\u627e\u5230\u7b26\u5408\u7070\u5ea6\u7b56\u7565\u7684\u5b9e\u4f8b");
            }
            log.info("\u7070\u5ea6\u53d1\u5e03\u76ee\u6807\u5b9e\u4f8b: {}", targetInstances);
            boolean applyConfigSuccess = this.applyGrayConfig(configInfo, grayConfig, targetInstances);
            if (!applyConfigSuccess) {
                return GrayReleaseResult.failed("\u7070\u5ea6\u914d\u7f6e\u53d1\u5e03\u5931\u8d25");
            }
            this.recordGrayHistory(grayConfig, targetInstances);
            grayConfig.setEffectiveInstances(targetInstances);
            grayConfig.setStatus("GRAYING");
            grayConfig.setStartTime(LocalDateTime.now());
            return GrayReleaseResult.success(String.format("\u7070\u5ea6\u53d1\u5e03\u542f\u52a8\u6210\u529f\uff0c\u76ee\u6807\u5b9e\u4f8b\u6570\uff1a%d", targetInstances.size()), targetInstances);
        }
        catch (Exception e) {
            log.error("\u7070\u5ea6\u53d1\u5e03\u542f\u52a8\u5931\u8d25", (Throwable)e);
            return GrayReleaseResult.failed("\u7070\u5ea6\u53d1\u5e03\u542f\u52a8\u5931\u8d25\uff1a" + e.getMessage());
        }
    }

    public GrayReleaseResult promoteToFull(ConfigInfo configInfo, GrayReleaseConfig grayConfig) {
        try {
            if (grayConfig == null || !StringUtils.hasText((String)grayConfig.getDataId())) {
                return GrayReleaseResult.failed("\u7070\u5ea6\u914d\u7f6e\u4fe1\u606f\u65e0\u6548");
            }
            boolean success = this.nacosConfigService.publishConfig(configInfo);
            if (success) {
                grayConfig.setStatus("PROMOTED");
                grayConfig.setPromoteTime(LocalDateTime.now());
                List<String> effectiveInstances = grayConfig.getEffectiveInstances();
                if (effectiveInstances == null) {
                    effectiveInstances = Collections.emptyList();
                }
                this.recordGrayHistory(grayConfig, effectiveInstances, GrayReleaseHistory.OperationType.PROMOTE.getCode(), GrayReleaseHistory.OperationResult.SUCCESS.getCode(), null, "\u7070\u5ea6\u5168\u91cf\u53d1\u5e03\u6210\u529f");
                log.info("\u7070\u5ea6\u5168\u91cf\u53d1\u5e03\u6210\u529f\uff1a{}", (Object)grayConfig.getDataId());
                return GrayReleaseResult.success("\u7070\u5ea6\u5168\u91cf\u53d1\u5e03\u6210\u529f", Collections.emptyList());
            }
            this.recordGrayHistoryFailed(grayConfig, GrayReleaseHistory.OperationType.PROMOTE.getCode(), "\u914d\u7f6e\u53d1\u5e03\u5931\u8d25");
            return GrayReleaseResult.failed("\u7070\u5ea6\u5168\u91cf\u53d1\u5e03\u5931\u8d25");
        }
        catch (Exception e) {
            log.error("\u7070\u5ea6\u5168\u91cf\u53d1\u5e03\u5931\u8d25", (Throwable)e);
            this.recordGrayHistoryFailed(grayConfig, GrayReleaseHistory.OperationType.PROMOTE.getCode(), e.getMessage());
            return GrayReleaseResult.failed("\u7070\u5ea6\u5168\u91cf\u53d1\u5e03\u5931\u8d25\uff1a" + e.getMessage());
        }
    }

    public GrayReleaseResult rollbackGrayRelease(ConfigInfo originalConfig, GrayReleaseConfig grayConfig) {
        try {
            if (originalConfig == null) {
                return GrayReleaseResult.failed("\u539f\u59cb\u914d\u7f6e\u4fe1\u606f\u65e0\u6548");
            }
            boolean success = this.nacosConfigService.publishConfig(originalConfig);
            if (success) {
                grayConfig.setStatus("ROLLED_BACK");
                grayConfig.setRollbackTime(LocalDateTime.now());
                List<String> effectiveInstances = grayConfig.getEffectiveInstances();
                if (effectiveInstances == null) {
                    effectiveInstances = Collections.emptyList();
                }
                this.recordGrayHistory(grayConfig, effectiveInstances, GrayReleaseHistory.OperationType.ROLLBACK.getCode(), GrayReleaseHistory.OperationResult.SUCCESS.getCode(), null, "\u7070\u5ea6\u56de\u6eda\u6210\u529f\uff0c\u5df2\u6062\u590d\u539f\u59cb\u914d\u7f6e");
                log.info("\u7070\u5ea6\u56de\u6eda\u6210\u529f\uff1a{}", (Object)grayConfig.getDataId());
                return GrayReleaseResult.success("\u7070\u5ea6\u56de\u6eda\u6210\u529f", Collections.emptyList());
            }
            this.recordGrayHistoryFailed(grayConfig, GrayReleaseHistory.OperationType.ROLLBACK.getCode(), "\u914d\u7f6e\u56de\u6eda\u53d1\u5e03\u5931\u8d25");
            return GrayReleaseResult.failed("\u7070\u5ea6\u56de\u6eda\u5931\u8d25");
        }
        catch (Exception e) {
            log.error("\u7070\u5ea6\u56de\u6eda\u5931\u8d25", (Throwable)e);
            this.recordGrayHistoryFailed(grayConfig, GrayReleaseHistory.OperationType.ROLLBACK.getCode(), e.getMessage());
            return GrayReleaseResult.failed("\u7070\u5ea6\u56de\u6eda\u5931\u8d25\uff1a" + e.getMessage());
        }
    }

    private void validateGrayConfig(GrayReleaseConfig grayConfig) {
        if (grayConfig == null) {
            throw new IllegalArgumentException("\u7070\u5ea6\u914d\u7f6e\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (!StringUtils.hasText((String)grayConfig.getDataId())) {
            throw new IllegalArgumentException("\u914d\u7f6eData ID\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (!StringUtils.hasText((String)grayConfig.getStrategyType())) {
            throw new IllegalArgumentException("\u7070\u5ea6\u7b56\u7565\u7c7b\u578b\u4e0d\u80fd\u4e3a\u7a7a");
        }
        GrayStrategyType strategy = GrayStrategyType.fromCode(grayConfig.getStrategyType());
        if (strategy == null) {
            throw new IllegalArgumentException("\u4e0d\u652f\u6301\u7684\u7070\u5ea6\u7b56\u7565\uff1a" + grayConfig.getStrategyType());
        }
        switch (strategy) {
            case IP: {
                if (StringUtils.hasText((String)grayConfig.getTargetInstances())) break;
                throw new IllegalArgumentException("IP\u7070\u5ea6\u7b56\u7565\u5fc5\u987b\u6307\u5b9a\u76ee\u6807\u5b9e\u4f8b");
            }
            case PERCENTAGE: {
                if (grayConfig.getPercentage() != null && grayConfig.getPercentage() > 0 && grayConfig.getPercentage() <= 100) break;
                throw new IllegalArgumentException("\u767e\u5206\u6bd4\u7070\u5ea6\u7b56\u7565\u7684\u767e\u5206\u6bd4\u5fc5\u987b\u57281-100\u4e4b\u95f4");
            }
            case LABEL: {
                if (StringUtils.hasText((String)grayConfig.getLabels())) break;
                throw new IllegalArgumentException("\u6807\u7b7e\u7070\u5ea6\u7b56\u7565\u5fc5\u987b\u6307\u5b9a\u6807\u7b7e");
            }
        }
    }

    private boolean applyGrayConfig(ConfigInfo configInfo, GrayReleaseConfig grayConfig, List<String> targetInstances) {
        try {
            String serviceName = configInfo.getServiceName();
            if (!StringUtils.hasText((String)serviceName)) {
                serviceName = this.extractServiceName(configInfo.getDataId());
            }
            String namespace = this.resolveNamespace(configInfo);
            String group = this.resolveGroup(configInfo);
            List<Instance> instances = this.namingService.getAllInstances(serviceName, group);
            for (String instanceAddress : targetInstances) {
                String[] parts = instanceAddress.split(":");
                if (parts.length != 2) {
                    log.warn("\u5b9e\u4f8b\u5730\u5740\u683c\u5f0f\u9519\u8bef\uff1a{}", (Object)instanceAddress);
                    continue;
                }
                String ip = parts[0];
                int port = Integer.parseInt(parts[1]);
                for (Instance instance : instances) {
                    if (!instance.getIp().equals(ip) || instance.getPort() != port) continue;
                    Map<String, String> metadata = instance.getMetadata();
                    if (metadata == null) {
                        metadata = new HashMap<String, String>();
                    }
                    metadata.put("gray-release", "true");
                    metadata.put("gray-data-id", configInfo.getDataId());
                    metadata.put("gray-start-time", String.valueOf(System.currentTimeMillis()));
                    instance.setMetadata(metadata);
                    this.namingService.registerInstance(serviceName, group, instance);
                    log.info("\u4e3a\u5b9e\u4f8b {}:{} \u6dfb\u52a0\u7070\u5ea6\u5143\u6570\u636e\u6210\u529f\uff08namespace={}, group={}\uff09", new Object[]{ip, port, namespace, group});
                }
            }
            String grayGroup = "GRAY_" + grayConfig.getDataId();
            ConfigInfo grayConfigInfo = ConfigInfo.builder().dataId(configInfo.getDataId()).content(configInfo.getContent()).group(grayGroup).namespace(namespace).environment(configInfo.getEnvironment()).tenantId(configInfo.getTenantId()).appId(configInfo.getAppId()).build();
            boolean publishSuccess = this.nacosConfigService.publishConfig(grayConfigInfo);
            if (publishSuccess) {
                log.info("\u7070\u5ea6\u914d\u7f6e\u53d1\u5e03\u5230\u5206\u7ec4 {} \u6210\u529f", (Object)grayGroup);
            }
            return publishSuccess;
        }
        catch (Exception e) {
            log.error("\u5e94\u7528\u7070\u5ea6\u914d\u7f6e\u5931\u8d25", (Throwable)e);
            return false;
        }
    }

    private void recordGrayHistory(GrayReleaseConfig grayConfig, List<String> targetInstances) {
        this.recordGrayHistory(grayConfig, targetInstances, GrayReleaseHistory.OperationType.START.getCode(), GrayReleaseHistory.OperationResult.SUCCESS.getCode(), null, null);
    }

    private void recordGrayHistory(GrayReleaseConfig grayConfig, List<String> targetInstances, String operationType, String result, String failureReason, String remark) {
        try {
            GrayReleaseHistory history = GrayReleaseHistory.builder().grayConfigId(grayConfig.getId()).dataId(grayConfig.getDataId()).strategyType(grayConfig.getStrategyType()).percentage(grayConfig.getPercentage()).labels(grayConfig.getLabels()).targetInstances(String.join((CharSequence)",", targetInstances)).effectiveInstanceCount(targetInstances.size()).operationType(operationType).grayContent(grayConfig.getGrayContent()).result(result).failureReason(failureReason).remark(remark).operationTime(LocalDateTime.now()).createTime(LocalDateTime.now()).build();
            GrayReleaseHistory savedHistory = this.historyRepository.save(history);
            log.info("\u7070\u5ea6\u53d1\u5e03\u5386\u53f2\u8bb0\u5f55\u5df2\u4fdd\u5b58: id={}, dataId={}, operationType={}, result={}, \u76ee\u6807\u5b9e\u4f8b\u6570={}", new Object[]{savedHistory.getId(), savedHistory.getDataId(), savedHistory.getOperationType(), savedHistory.getResult(), savedHistory.getEffectiveInstanceCount()});
            this.eventPublisher.publishEvent((ApplicationEvent)new GrayReleaseHistoryEvent(this, savedHistory));
            log.debug("\u7070\u5ea6\u53d1\u5e03\u5386\u53f2\u4e8b\u4ef6\u5df2\u53d1\u5e03: {}", (Object)savedHistory.getDataId());
        }
        catch (Exception e) {
            log.error("\u8bb0\u5f55\u7070\u5ea6\u53d1\u5e03\u5386\u53f2\u5931\u8d25: dataId={}, operationType={}", new Object[]{grayConfig.getDataId(), operationType, e});
        }
    }

    private void recordGrayHistoryFailed(GrayReleaseConfig grayConfig, String operationType, String failureReason) {
        this.recordGrayHistory(grayConfig, Collections.emptyList(), operationType, GrayReleaseHistory.OperationResult.FAILED.getCode(), failureReason, null);
    }

    private List<String> selectTargetInstances(List<Instance> allInstances, GrayReleaseConfig grayConfig) {
        GrayStrategyType strategy = GrayStrategyType.fromCode(grayConfig.getStrategyType());
        if (strategy == null) {
            log.warn("\u672a\u77e5\u7684\u7070\u5ea6\u7b56\u7565\u7c7b\u578b: {}", (Object)grayConfig.getStrategyType());
            return Collections.emptyList();
        }
        switch (strategy) {
            case IP: {
                return this.selectByIp(allInstances, grayConfig.getTargetInstances());
            }
            case PERCENTAGE: {
                return this.selectByPercentage(allInstances, grayConfig.getPercentage());
            }
            case LABEL: {
                return this.selectByLabel(allInstances, grayConfig.getLabels());
            }
        }
        return Collections.emptyList();
    }

    private List<String> selectByIp(List<Instance> allInstances, String targetIps) {
        if (!StringUtils.hasText((String)targetIps)) {
            return Collections.emptyList();
        }
        Set<String> ipSet = Arrays.stream(targetIps.split(",")).map(String::trim).filter(StringUtils::hasText).collect(Collectors.toSet());
        return allInstances.stream().filter(instance -> ipSet.contains(instance.getIp())).map(instance -> instance.getIp() + ":" + instance.getPort()).collect(Collectors.toList());
    }

    private List<String> selectByPercentage(List<Instance> allInstances, Integer percentage) {
        if (percentage == null || percentage <= 0 || percentage > 100) {
            log.warn("\u767e\u5206\u6bd4\u5fc5\u987b\u57281-100\u4e4b\u95f4\uff0c\u5f53\u524d\u503c: {}", (Object)percentage);
            return Collections.emptyList();
        }
        int count = (int)Math.ceil((double)(allInstances.size() * percentage) / 100.0);
        ArrayList<Instance> shuffled = new ArrayList<Instance>(allInstances);
        Collections.shuffle(shuffled);
        return shuffled.stream().limit(count).map(instance -> instance.getIp() + ":" + instance.getPort()).collect(Collectors.toList());
    }

    private List<String> selectByLabel(List<Instance> allInstances, String labelsJson) {
        if (!StringUtils.hasText((String)labelsJson)) {
            return Collections.emptyList();
        }
        try {
            Map<String, String> requiredLabels = this.objectMapper.readValue(labelsJson, new TypeReference<Map<String, String>>(){});
            return allInstances.stream().filter(instance -> this.matchesLabels(instance.getMetadata(), requiredLabels)).map(instance -> instance.getIp() + ":" + instance.getPort()).collect(Collectors.toList());
        }
        catch (Exception e) {
            log.error("\u89e3\u6790\u6807\u7b7e\u914d\u7f6e\u5931\u8d25", (Throwable)e);
            return Collections.emptyList();
        }
    }

    private boolean matchesLabels(Map<String, String> instanceMetadata, Map<String, String> requiredLabels) {
        if (CollectionUtils.isEmpty(requiredLabels)) {
            return false;
        }
        if (CollectionUtils.isEmpty(instanceMetadata)) {
            return false;
        }
        return requiredLabels.entrySet().stream().allMatch(entry -> {
            String value = (String)instanceMetadata.get(entry.getKey());
            return value != null && value.equals(entry.getValue());
        });
    }

    private List<Instance> getServiceInstances(ConfigInfo configInfo) {
        try {
            String serviceName = configInfo.getServiceName();
            if (!StringUtils.hasText((String)serviceName) && !StringUtils.hasText((String)(serviceName = this.extractServiceName(configInfo.getDataId())))) {
                log.warn("\u65e0\u6cd5\u4eceData ID\u4e2d\u63d0\u53d6\u670d\u52a1\u540d: {}", (Object)configInfo.getDataId());
                return Collections.emptyList();
            }
            String group = this.resolveGroup(configInfo);
            return this.namingService.getAllInstances(serviceName, group);
        }
        catch (Exception e) {
            log.error("\u83b7\u53d6\u670d\u52a1\u5b9e\u4f8b\u5931\u8d25", (Throwable)e);
            return Collections.emptyList();
        }
    }

    private String resolveNamespace(ConfigInfo configInfo) {
        String namespace = configInfo.getNamespace();
        if (!StringUtils.hasText((String)namespace)) {
            return "public";
        }
        return namespace;
    }

    private String resolveGroup(ConfigInfo configInfo) {
        String group = configInfo.getGroup();
        if (!StringUtils.hasText((String)group)) {
            return "DEFAULT_GROUP";
        }
        return group;
    }

    private String extractServiceName(String dataId) {
        if (dataId.contains("-")) {
            return dataId.split("-")[0];
        }
        return dataId;
    }

    @Generated
    public GrayReleaseService(NacosConfigService nacosConfigService, NamingService namingService, ObjectMapper objectMapper, GrayReleaseHistoryRepository historyRepository, ApplicationEventPublisher eventPublisher) {
        this.nacosConfigService = nacosConfigService;
        this.namingService = namingService;
        this.objectMapper = objectMapper;
        this.historyRepository = historyRepository;
        this.eventPublisher = eventPublisher;
    }

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

        @Generated
        public boolean isSuccess() {
            return this.success;
        }

        @Generated
        public String getMessage() {
            return this.message;
        }

        @Generated
        public List<String> getTargetInstances() {
            return this.targetInstances;
        }

        @Generated
        public void setSuccess(boolean success) {
            this.success = success;
        }

        @Generated
        public void setMessage(String message) {
            this.message = message;
        }

        @Generated
        public void setTargetInstances(List<String> targetInstances) {
            this.targetInstances = targetInstances;
        }

        @Generated
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof GrayReleaseResult)) {
                return false;
            }
            GrayReleaseResult other = (GrayReleaseResult)o;
            if (!other.canEqual(this)) {
                return false;
            }
            if (this.isSuccess() != other.isSuccess()) {
                return false;
            }
            String this$message = this.getMessage();
            String other$message = other.getMessage();
            if (this$message == null ? other$message != null : !this$message.equals(other$message)) {
                return false;
            }
            List<String> this$targetInstances = this.getTargetInstances();
            List<String> other$targetInstances = other.getTargetInstances();
            return !(this$targetInstances == null ? other$targetInstances != null : !((Object)this$targetInstances).equals(other$targetInstances));
        }

        @Generated
        protected boolean canEqual(Object other) {
            return other instanceof GrayReleaseResult;
        }

        @Generated
        public int hashCode() {
            int PRIME = 59;
            int result = 1;
            result = result * 59 + (this.isSuccess() ? 79 : 97);
            String $message = this.getMessage();
            result = result * 59 + ($message == null ? 43 : $message.hashCode());
            List<String> $targetInstances = this.getTargetInstances();
            result = result * 59 + ($targetInstances == null ? 43 : ((Object)$targetInstances).hashCode());
            return result;
        }

        @Generated
        public String toString() {
            return "GrayReleaseService.GrayReleaseResult(success=" + this.isSuccess() + ", message=" + this.getMessage() + ", targetInstances=" + String.valueOf(this.getTargetInstances()) + ")";
        }

        @Generated
        public GrayReleaseResult(boolean success, String message, List<String> targetInstances) {
            this.success = success;
            this.message = message;
            this.targetInstances = targetInstances;
        }

        @Generated
        public GrayReleaseResult() {
        }
    }
}
