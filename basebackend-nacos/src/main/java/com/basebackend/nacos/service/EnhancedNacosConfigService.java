/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.alibaba.nacos.api.config.ConfigService
 *  com.alibaba.nacos.api.config.listener.Listener
 *  com.alibaba.nacos.api.exception.NacosException
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.stereotype.Service
 *  org.springframework.util.DigestUtils
 */
package com.basebackend.nacos.service;

import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.basebackend.nacos.exception.NacosConfigException;
import com.basebackend.nacos.isolation.ConfigIsolationContext;
import com.basebackend.nacos.isolation.ConfigIsolationManager;
import com.basebackend.nacos.metrics.NacosMetrics;
import com.basebackend.nacos.model.ConfigInfo;
import com.basebackend.nacos.retry.RetryExecutor;
import java.nio.charset.StandardCharsets;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

@Service
public class EnhancedNacosConfigService {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(EnhancedNacosConfigService.class);
    private final ConfigService nacosConfigService;
    private final ConfigIsolationManager isolationManager;
    private final RetryExecutor retryExecutor;
    private final NacosMetrics metrics;

    @Autowired
    public EnhancedNacosConfigService(ConfigService nacosConfigService, ConfigIsolationManager isolationManager, @Autowired(required=false) NacosMetrics metrics) {
        this.nacosConfigService = nacosConfigService;
        this.isolationManager = isolationManager;
        this.metrics = metrics;
        this.retryExecutor = RetryExecutor.builder().maxRetries(3).initialDelayMs(1000L).maxDelayMs(10000L).build();
        log.info("EnhancedNacosConfigService initialized with retry support");
    }

    public String getConfig(ConfigInfo configInfo) {
        ConfigIsolationContext context = this.buildContext(configInfo);
        String dataId = context.buildDataId(configInfo.getDataId());
        String group = context.buildGroup();
        log.debug("Fetching config: dataId={}, group={}", (Object)dataId, (Object)group);
        long startTime = System.currentTimeMillis();
        try {
            String config = this.retryExecutor.execute(() -> this.nacosConfigService.getConfig(dataId, group, 5000L), this::isRetryable);
            long latency = System.currentTimeMillis() - startTime;
            if (config != null) {
                log.info("Config fetched successfully: dataId={}, group={}, latency={}ms", new Object[]{dataId, group, latency});
                this.recordMetrics(dataId, group, true, latency);
            } else {
                log.warn("Config not found: dataId={}, group={}", (Object)dataId, (Object)group);
            }
            return config;
        }
        catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            log.error("Failed to fetch config after retries: dataId={}, group={}, latency={}ms, error={}", new Object[]{dataId, group, latency, e.getMessage()});
            this.recordMetrics(dataId, group, false, latency);
            throw this.handleException("\u83b7\u53d6\u914d\u7f6e\u5931\u8d25", dataId, group, e);
        }
    }

    public boolean publishConfig(ConfigInfo configInfo) {
        ConfigIsolationContext context = this.buildContext(configInfo);
        String dataId = context.buildDataId(configInfo.getDataId());
        String group = context.buildGroup();
        String content = configInfo.getContent();
        String type = configInfo.getType() != null ? configInfo.getType() : "yml";
        log.info("Publishing config: dataId={}, group={}, type={}, contentLength={}", new Object[]{dataId, group, type, content != null ? content.length() : 0});
        try {
            Boolean result = this.retryExecutor.execute(() -> this.nacosConfigService.publishConfig(dataId, group, content, type), this::isRetryable);
            if (Boolean.TRUE.equals(result)) {
                log.info("Config published successfully: dataId={}, group={}", (Object)dataId, (Object)group);
                if (this.metrics != null) {
                    this.metrics.recordConfigPublishSuccess(dataId, group);
                }
            } else {
                log.warn("Config publish returned false: dataId={}, group={}", (Object)dataId, (Object)group);
            }
            return Boolean.TRUE.equals(result);
        }
        catch (Exception e) {
            log.error("Failed to publish config: dataId={}, group={}, error={}", new Object[]{dataId, group, e.getMessage()});
            if (this.metrics != null) {
                this.metrics.recordConfigPublishFailure(dataId, group, this.getErrorType(e));
            }
            throw NacosConfigException.publishFailed(dataId, group, e);
        }
    }

    public boolean removeConfig(ConfigInfo configInfo) {
        ConfigIsolationContext context = this.buildContext(configInfo);
        String dataId = context.buildDataId(configInfo.getDataId());
        String group = context.buildGroup();
        log.info("Removing config: dataId={}, group={}", (Object)dataId, (Object)group);
        try {
            Boolean result = this.retryExecutor.execute(() -> this.nacosConfigService.removeConfig(dataId, group), this::isRetryable);
            if (Boolean.TRUE.equals(result)) {
                log.info("Config removed successfully: dataId={}, group={}", (Object)dataId, (Object)group);
            } else {
                log.warn("Config remove returned false: dataId={}, group={}", (Object)dataId, (Object)group);
            }
            return Boolean.TRUE.equals(result);
        }
        catch (Exception e) {
            log.error("Failed to remove config: dataId={}, group={}, error={}", new Object[]{dataId, group, e.getMessage()});
            throw NacosConfigException.deleteFailed(dataId, group, e);
        }
    }

    public void addListener(ConfigInfo configInfo, Listener listener) {
        ConfigIsolationContext context = this.buildContext(configInfo);
        String dataId = context.buildDataId(configInfo.getDataId());
        String group = context.buildGroup();
        log.info("Adding config listener: dataId={}, group={}", (Object)dataId, (Object)group);
        try {
            this.nacosConfigService.addListener(dataId, group, listener);
            log.info("Config listener added successfully: dataId={}, group={}", (Object)dataId, (Object)group);
        }
        catch (NacosException e) {
            log.error("Failed to add config listener: dataId={}, group={}, error={}", new Object[]{dataId, group, e.getMessage()});
            throw new NacosConfigException("\u6dfb\u52a0\u914d\u7f6e\u76d1\u542c\u5668\u5931\u8d25", dataId, group, NacosConfigException.ErrorCode.LISTENER_ADD_FAILED, e);
        }
    }

    public void removeListener(ConfigInfo configInfo, Listener listener) {
        ConfigIsolationContext context = this.buildContext(configInfo);
        String dataId = context.buildDataId(configInfo.getDataId());
        String group = context.buildGroup();
        log.info("Removing config listener: dataId={}, group={}", (Object)dataId, (Object)group);
        this.nacosConfigService.removeListener(dataId, group, listener);
        log.debug("Config listener removed: dataId={}, group={}", (Object)dataId, (Object)group);
    }

    public String calculateMd5(String content) {
        if (content == null) {
            return null;
        }
        return DigestUtils.md5DigestAsHex((byte[])content.getBytes(StandardCharsets.UTF_8));
    }

    private boolean isRetryable(Exception e) {
        if (e instanceof NacosException) {
            NacosException nacosException = (NacosException)((Object)e);
            int errorCode = nacosException.getErrCode();
            return errorCode == 500 || errorCode == -500 || e.getMessage() != null && e.getMessage().contains("timeout");
        }
        return false;
    }

    private String getErrorType(Exception e) {
        if (e instanceof NacosException) {
            NacosException nacosException = (NacosException)((Object)e);
            return "NACOS_" + nacosException.getErrCode();
        }
        return e.getClass().getSimpleName();
    }

    private NacosConfigException handleException(String message, String dataId, String group, Exception e) {
        NacosException nacosException;
        int errorCode;
        if (e instanceof NacosException && (errorCode = (nacosException = (NacosException)((Object)e)).getErrCode()) == 500) {
            return new NacosConfigException(message, dataId, group, NacosConfigException.ErrorCode.SERVICE_UNAVAILABLE, e);
        }
        return new NacosConfigException(message, dataId, group, NacosConfigException.ErrorCode.UNKNOWN, e);
    }

    private void recordMetrics(String dataId, String group, boolean success, long latencyMs) {
        if (this.metrics != null) {
            if (success) {
                this.metrics.recordConfigGetSuccess(dataId, group);
            } else {
                this.metrics.recordConfigGetFailure(dataId, group, "FETCH_FAILED");
            }
            this.metrics.recordConfigGetLatency(dataId, group, latencyMs);
        }
    }

    private ConfigIsolationContext buildContext(ConfigInfo configInfo) {
        return this.isolationManager.createContext(configInfo.getEnvironment(), configInfo.getTenantId(), configInfo.getAppId());
    }
}

