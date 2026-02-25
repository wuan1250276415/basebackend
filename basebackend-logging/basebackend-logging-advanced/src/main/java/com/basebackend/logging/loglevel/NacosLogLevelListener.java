package com.basebackend.logging.loglevel;

import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.logging.LogLevel;

import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Nacos 日志级别配置监听器
 *
 * 监听 Nacos 配置变更，自动调整日志级别。
 * 配置格式 (properties):
 * <pre>
 * com.basebackend.admin=DEBUG
 * com.basebackend.cache=WARN
 * ROOT=INFO
 * </pre>
 *
 * @author basebackend team
 * @since 2025-12-10
 */
@Slf4j
public class NacosLogLevelListener {

    private final ConfigService configService;
    private final LogLevelManager logLevelManager;
    private final String dataId;
    private final String group;
    private final Executor executor;

    public NacosLogLevelListener(ConfigService configService,
                                  LogLevelManager logLevelManager,
                                  String dataId,
                                  String group) {
        this.configService = configService;
        this.logLevelManager = logLevelManager;
        this.dataId = dataId;
        this.group = group;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @PostConstruct
    public void init() {
        try {
            // 加载初始配置
            String initialConfig = configService.getConfig(dataId, group, 5000);
            if (initialConfig != null && !initialConfig.isBlank()) {
                applyConfig(initialConfig);
            }

            // 注册配置变更监听
            configService.addListener(dataId, group, new Listener() {
                @Override
                public Executor getExecutor() {
                    return executor;
                }

                @Override
                public void receiveConfigInfo(String configInfo) {
                    log.info("收到 Nacos 日志级别配置变更");
                    applyConfig(configInfo);
                }
            });

            log.info("Nacos 日志级别监听已注册: dataId={}, group={}", dataId, group);
        } catch (NacosException e) {
            log.warn("注册 Nacos 日志级别监听失败: {}", e.getMessage());
        }
    }

    private void applyConfig(String configContent) {
        try {
            Properties props = new Properties();
            props.load(new StringReader(configContent));

            Map<String, LogLevel> levels = new LinkedHashMap<>();
            for (String name : props.stringPropertyNames()) {
                String value = props.getProperty(name).trim().toUpperCase();
                try {
                    levels.put(name.trim(), LogLevel.valueOf(value));
                } catch (IllegalArgumentException e) {
                    log.warn("忽略无效的日志级别配置: {}={}", name, value);
                }
            }

            if (!levels.isEmpty()) {
                logLevelManager.applyBulkLevels(levels);
            }
        } catch (Exception e) {
            log.error("解析 Nacos 日志级别配置失败", e);
        }
    }
}
