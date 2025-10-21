package com.basebackend.nacos.service;

import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.basebackend.nacos.isolation.ConfigIsolationContext;
import com.basebackend.nacos.isolation.ConfigIsolationManager;
import com.basebackend.nacos.model.ConfigInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * Nacos配置服务
 * 提供配置的增删改查操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NacosConfigService {

    private final ConfigService nacosConfigService;
    private final ConfigIsolationManager isolationManager;

    /**
     * 获取配置
     */
    public String getConfig(ConfigInfo configInfo) throws NacosException {
        ConfigIsolationContext context = buildContext(configInfo);
        String dataId = context.buildDataId(configInfo.getDataId());
        String group = context.buildGroup();
        String namespace = context.buildNamespace();

        log.info("获取配置: dataId={}, group={}, namespace={}", dataId, group, namespace);

        return nacosConfigService.getConfig(dataId, group, 5000);
    }

    /**
     * 发布配置
     */
    public boolean publishConfig(ConfigInfo configInfo) throws NacosException {
        ConfigIsolationContext context = buildContext(configInfo);
        String dataId = context.buildDataId(configInfo.getDataId());
        String group = context.buildGroup();
        String content = configInfo.getContent();
        String type = configInfo.getType() != null ? configInfo.getType() : "yaml";

        log.info("发布配置: dataId={}, group={}, type={}", dataId, group, type);

        return nacosConfigService.publishConfig(dataId, group, content, type);
    }

    /**
     * 删除配置
     */
    public boolean removeConfig(ConfigInfo configInfo) throws NacosException {
        ConfigIsolationContext context = buildContext(configInfo);
        String dataId = context.buildDataId(configInfo.getDataId());
        String group = context.buildGroup();

        log.info("删除配置: dataId={}, group={}", dataId, group);

        return nacosConfigService.removeConfig(dataId, group);
    }

    /**
     * 添加配置监听器
     */
    public void addListener(ConfigInfo configInfo, com.alibaba.nacos.api.config.listener.Listener listener) throws NacosException {
        ConfigIsolationContext context = buildContext(configInfo);
        String dataId = context.buildDataId(configInfo.getDataId());
        String group = context.buildGroup();

        log.info("添加配置监听器: dataId={}, group={}", dataId, group);

        nacosConfigService.addListener(dataId, group, listener);
    }

    /**
     * 移除配置监听器
     */
    public void removeListener(ConfigInfo configInfo, com.alibaba.nacos.api.config.listener.Listener listener) {
        ConfigIsolationContext context = buildContext(configInfo);
        String dataId = context.buildDataId(configInfo.getDataId());
        String group = context.buildGroup();

        log.info("移除配置监听器: dataId={}, group={}", dataId, group);

        nacosConfigService.removeListener(dataId, group, listener);
    }

    /**
     * 计算配置内容的MD5
     */
    public String calculateMd5(String content) {
        if (content == null) {
            return null;
        }
        return DigestUtils.md5DigestAsHex(content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 构建配置隔离上下文
     */
    private ConfigIsolationContext buildContext(ConfigInfo configInfo) {
        return isolationManager.createContext(
                configInfo.getEnvironment(),
                configInfo.getTenantId(),
                configInfo.getAppId()
        );
    }
}
