package com.basebackend.nacos.service;

import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.basebackend.nacos.constants.NacosConstants;
import com.basebackend.nacos.exception.NacosConfigException;
import com.basebackend.nacos.isolation.ConfigIsolationContext;
import com.basebackend.nacos.isolation.ConfigIsolationManager;
import com.basebackend.nacos.metrics.NacosMetrics;
import com.basebackend.nacos.model.ConfigInfo;
import com.basebackend.nacos.retry.RetryExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * 增强版Nacos配置服务
 * <p>
 * 提供带重试机制的配置操作，包括：
 * - 自动重试（指数退避）
 * - 详细的异常处理
 * - 监控指标记录
 * - 规范的日志记录
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class EnhancedNacosConfigService {

    private final ConfigService nacosConfigService;
    private final ConfigIsolationManager isolationManager;
    private final RetryExecutor retryExecutor;
    private final NacosMetrics metrics;

    @Autowired
    public EnhancedNacosConfigService(ConfigService nacosConfigService,
            ConfigIsolationManager isolationManager,
            @Autowired(required = false) NacosMetrics metrics) {
        this.nacosConfigService = nacosConfigService;
        this.isolationManager = isolationManager;
        this.metrics = metrics;
        this.retryExecutor = RetryExecutor.builder()
                .maxRetries(NacosConstants.DEFAULT_MAX_RETRIES)
                .initialDelayMs(NacosConstants.DEFAULT_RETRY_INITIAL_DELAY_MS)
                .maxDelayMs(NacosConstants.DEFAULT_RETRY_MAX_DELAY_MS)
                .build();

        log.info("EnhancedNacosConfigService initialized with retry support");
    }

    /**
     * 获取配置（带重试）
     *
     * @param configInfo 配置信息
     * @return 配置内容
     */
    public String getConfig(ConfigInfo configInfo) {
        ConfigIsolationContext context = buildContext(configInfo);
        String dataId = context.buildDataId(configInfo.getDataId());
        String group = context.buildGroup();

        log.debug("Fetching config: dataId={}, group={}", dataId, group);
        long startTime = System.currentTimeMillis();

        try {
            String config = retryExecutor.execute(
                    () -> nacosConfigService.getConfig(dataId, group, NacosConstants.DEFAULT_TIMEOUT_MS),
                    this::isRetryable);

            long latency = System.currentTimeMillis() - startTime;

            if (config != null) {
                log.info("Config fetched successfully: dataId={}, group={}, latency={}ms",
                        dataId, group, latency);
                recordMetrics(dataId, group, true, latency);
            } else {
                log.warn("Config not found: dataId={}, group={}", dataId, group);
            }

            return config;
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            log.error("Failed to fetch config after retries: dataId={}, group={}, latency={}ms, error={}",
                    dataId, group, latency, e.getMessage());
            recordMetrics(dataId, group, false, latency);
            throw handleException("获取配置失败", dataId, group, e);
        }
    }

    /**
     * 发布配置（带重试）
     *
     * @param configInfo 配置信息
     * @return 是否成功
     */
    public boolean publishConfig(ConfigInfo configInfo) {
        ConfigIsolationContext context = buildContext(configInfo);
        String dataId = context.buildDataId(configInfo.getDataId());
        String group = context.buildGroup();
        String content = configInfo.getContent();
        String type = configInfo.getType() != null ? configInfo.getType() : NacosConstants.DEFAULT_FILE_EXTENSION;

        log.info("Publishing config: dataId={}, group={}, type={}, contentLength={}",
                dataId, group, type, content != null ? content.length() : 0);

        try {
            Boolean result = retryExecutor.execute(
                    () -> nacosConfigService.publishConfig(dataId, group, content, type),
                    this::isRetryable);

            if (Boolean.TRUE.equals(result)) {
                log.info("Config published successfully: dataId={}, group={}", dataId, group);
                if (metrics != null) {
                    metrics.recordConfigPublishSuccess(dataId, group);
                }
            } else {
                log.warn("Config publish returned false: dataId={}, group={}", dataId, group);
            }

            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Failed to publish config: dataId={}, group={}, error={}",
                    dataId, group, e.getMessage());
            if (metrics != null) {
                metrics.recordConfigPublishFailure(dataId, group, getErrorType(e));
            }
            throw NacosConfigException.publishFailed(dataId, group, e);
        }
    }

    /**
     * 删除配置（带重试）
     *
     * @param configInfo 配置信息
     * @return 是否成功
     */
    public boolean removeConfig(ConfigInfo configInfo) {
        ConfigIsolationContext context = buildContext(configInfo);
        String dataId = context.buildDataId(configInfo.getDataId());
        String group = context.buildGroup();

        log.info("Removing config: dataId={}, group={}", dataId, group);

        try {
            Boolean result = retryExecutor.execute(
                    () -> nacosConfigService.removeConfig(dataId, group),
                    this::isRetryable);

            if (Boolean.TRUE.equals(result)) {
                log.info("Config removed successfully: dataId={}, group={}", dataId, group);
            } else {
                log.warn("Config remove returned false: dataId={}, group={}", dataId, group);
            }

            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Failed to remove config: dataId={}, group={}, error={}",
                    dataId, group, e.getMessage());
            throw NacosConfigException.deleteFailed(dataId, group, e);
        }
    }

    /**
     * 添加配置监听器
     *
     * @param configInfo 配置信息
     * @param listener   监听器
     */
    public void addListener(ConfigInfo configInfo,
            com.alibaba.nacos.api.config.listener.Listener listener) {
        ConfigIsolationContext context = buildContext(configInfo);
        String dataId = context.buildDataId(configInfo.getDataId());
        String group = context.buildGroup();

        log.info("Adding config listener: dataId={}, group={}", dataId, group);

        try {
            nacosConfigService.addListener(dataId, group, listener);
            log.info("Config listener added successfully: dataId={}, group={}", dataId, group);
        } catch (NacosException e) {
            log.error("Failed to add config listener: dataId={}, group={}, error={}",
                    dataId, group, e.getMessage());
            throw new NacosConfigException("添加配置监听器失败", dataId, group,
                    NacosConfigException.ErrorCode.LISTENER_ADD_FAILED, e);
        }
    }

    /**
     * 移除配置监听器
     *
     * @param configInfo 配置信息
     * @param listener   监听器
     */
    public void removeListener(ConfigInfo configInfo,
            com.alibaba.nacos.api.config.listener.Listener listener) {
        ConfigIsolationContext context = buildContext(configInfo);
        String dataId = context.buildDataId(configInfo.getDataId());
        String group = context.buildGroup();

        log.info("Removing config listener: dataId={}, group={}", dataId, group);
        nacosConfigService.removeListener(dataId, group, listener);
        log.debug("Config listener removed: dataId={}, group={}", dataId, group);
    }

    /**
     * 计算配置内容的MD5
     *
     * @param content 配置内容
     * @return MD5值
     */
    public String calculateMd5(String content) {
        if (content == null) {
            return null;
        }
        return DigestUtils.md5DigestAsHex(content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 判断是否需要重试
     */
    private boolean isRetryable(Exception e) {
        if (e instanceof NacosException nacosException) {
            // 网络相关错误可以重试
            int errorCode = nacosException.getErrCode();
            return errorCode == NacosException.SERVER_ERROR ||
                    errorCode == NacosException.HTTP_CLIENT_ERROR_CODE ||
                    e.getMessage() != null && e.getMessage().contains("timeout");
        }
        return false;
    }

    /**
     * 获取错误类型
     */
    private String getErrorType(Exception e) {
        if (e instanceof NacosException nacosException) {
            return "NACOS_" + nacosException.getErrCode();
        }
        return e.getClass().getSimpleName();
    }

    /**
     * 处理异常，转换为统一的NacosConfigException
     */
    private NacosConfigException handleException(String message, String dataId, String group, Exception e) {
        if (e instanceof NacosException nacosException) {
            int errorCode = nacosException.getErrCode();
            if (errorCode == NacosException.SERVER_ERROR) {
                return new NacosConfigException(message, dataId, group,
                        NacosConfigException.ErrorCode.SERVICE_UNAVAILABLE, e);
            }
        }
        return new NacosConfigException(message, dataId, group,
                NacosConfigException.ErrorCode.UNKNOWN, e);
    }

    /**
     * 记录监控指标
     */
    private void recordMetrics(String dataId, String group, boolean success, long latencyMs) {
        if (metrics != null) {
            if (success) {
                metrics.recordConfigGetSuccess(dataId, group);
            } else {
                metrics.recordConfigGetFailure(dataId, group, "FETCH_FAILED");
            }
            metrics.recordConfigGetLatency(dataId, group, latencyMs);
        }
    }

    /**
     * 构建配置隔离上下文
     */
    private ConfigIsolationContext buildContext(ConfigInfo configInfo) {
        return isolationManager.createContext(
                configInfo.getEnvironment(),
                configInfo.getTenantId(),
                configInfo.getAppId());
    }
}
