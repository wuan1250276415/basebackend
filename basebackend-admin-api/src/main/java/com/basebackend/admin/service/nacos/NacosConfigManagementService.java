package com.basebackend.admin.service.nacos;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.admin.dto.nacos.*;
import com.basebackend.admin.entity.nacos.SysNacosConfig;
import com.basebackend.admin.entity.nacos.SysNacosConfigHistory;
import com.basebackend.admin.mapper.nacos.SysNacosConfigHistoryMapper;
import com.basebackend.admin.mapper.nacos.SysNacosConfigMapper;
import com.basebackend.common.exception.BusinessException;
import com.basebackend.nacos.model.ConfigInfo;
import com.basebackend.nacos.service.ConfigPublisher;
import com.basebackend.nacos.service.NacosConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Nacos配置管理Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NacosConfigManagementService {

    private final SysNacosConfigMapper nacosConfigMapper;
    private final SysNacosConfigHistoryMapper configHistoryMapper;
    private final NacosConfigService nacosConfigService;
    private final ConfigPublisher configPublisher;

    /**
     * 分页查询配置
     */
    public IPage<SysNacosConfig> queryConfigPage(NacosConfigQueryDTO queryDTO) {
        Page<SysNacosConfig> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());

        LambdaQueryWrapper<SysNacosConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(queryDTO.getDataId()), SysNacosConfig::getDataId, queryDTO.getDataId())
                .eq(StringUtils.hasText(queryDTO.getGroupName()), SysNacosConfig::getGroupName, queryDTO.getGroupName())
                .eq(StringUtils.hasText(queryDTO.getNamespace()), SysNacosConfig::getNamespace, queryDTO.getNamespace())
                .eq(StringUtils.hasText(queryDTO.getEnvironment()), SysNacosConfig::getEnvironment, queryDTO.getEnvironment())
                .eq(StringUtils.hasText(queryDTO.getTenantId()), SysNacosConfig::getTenantId, queryDTO.getTenantId())
                .eq(queryDTO.getAppId() != null, SysNacosConfig::getAppId, queryDTO.getAppId())
                .eq(StringUtils.hasText(queryDTO.getStatus()), SysNacosConfig::getStatus, queryDTO.getStatus())
                .eq(StringUtils.hasText(queryDTO.getType()), SysNacosConfig::getType, queryDTO.getType())
                .eq(queryDTO.getIsCritical() != null, SysNacosConfig::getIsCritical, queryDTO.getIsCritical())
                .orderByDesc(SysNacosConfig::getUpdateTime);

        return nacosConfigMapper.selectPage(page, wrapper);
    }

    /**
     * 获取配置详情
     */
    public SysNacosConfig getConfigDetail(Long id) {
        SysNacosConfig config = nacosConfigMapper.selectById(id);
        if (config == null) {
            throw new BusinessException("配置不存在");
        }
        return config;
    }

    /**
     * 创建配置
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createConfig(NacosConfigDTO configDTO) {
        // 检查配置是否已存在
        LambdaQueryWrapper<SysNacosConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNacosConfig::getDataId, configDTO.getDataId())
                .eq(SysNacosConfig::getGroupName, configDTO.getGroupName())
                .eq(SysNacosConfig::getNamespace, configDTO.getNamespace())
                .eq(StringUtils.hasText(configDTO.getEnvironment()), SysNacosConfig::getEnvironment, configDTO.getEnvironment())
                .eq(StringUtils.hasText(configDTO.getTenantId()), SysNacosConfig::getTenantId, configDTO.getTenantId())
                .eq(configDTO.getAppId() != null, SysNacosConfig::getAppId, configDTO.getAppId());

        if (nacosConfigMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("配置已存在");
        }

        // 创建配置
        SysNacosConfig config = new SysNacosConfig();
        BeanUtils.copyProperties(configDTO, config);
        config.setVersion(1);
        config.setStatus("draft");
        config.setMd5(nacosConfigService.calculateMd5(config.getContent()));

        nacosConfigMapper.insert(config);

        // 记录历史
        recordHistory(config, "create", null);

        return config.getId();
    }

    /**
     * 更新配置
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(Long id, NacosConfigDTO configDTO) {
        SysNacosConfig existingConfig = nacosConfigMapper.selectById(id);
        if (existingConfig == null) {
            throw new BusinessException("配置不存在");
        }

        // 更新配置
        SysNacosConfig config = new SysNacosConfig();
        BeanUtils.copyProperties(configDTO, config);
        config.setId(id);
        config.setVersion(existingConfig.getVersion() + 1);
        config.setMd5(nacosConfigService.calculateMd5(config.getContent()));

        nacosConfigMapper.updateById(config);

        // 记录历史
        recordHistory(config, "update", null);
    }

    /**
     * 删除配置
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfig(Long id) {
        SysNacosConfig config = nacosConfigMapper.selectById(id);
        if (config == null) {
            throw new BusinessException("配置不存在");
        }

        // 从Nacos删除配置
        try {
            ConfigInfo configInfo = convertToConfigInfo(config);
            nacosConfigService.removeConfig(configInfo);
        } catch (Exception e) {
            log.error("从Nacos删除配置失败", e);
        }

        // 软删除
        nacosConfigMapper.deleteById(id);

        // 记录历史
        recordHistory(config, "delete", null);
    }

    /**
     * 发布配置
     */
    @Transactional(rollbackFor = Exception.class)
    public void publishConfig(ConfigPublishDTO publishDTO) {
        SysNacosConfig config = nacosConfigMapper.selectById(publishDTO.getConfigId());
        if (config == null) {
            throw new BusinessException("配置不存在");
        }

        ConfigInfo configInfo = convertToConfigInfo(config);

        // 发布配置
        ConfigPublisher.PublishResult result = configPublisher.publishConfig(
                configInfo,
                Boolean.TRUE.equals(publishDTO.getForce())
        );

        if ("success".equals(result.getStatus())) {
            // 更新状态为已发布
            config.setStatus("published");
            nacosConfigMapper.updateById(config);

            // 记录历史
            recordHistory(config, "publish", null);
        } else if ("failed".equals(result.getStatus())) {
            throw new BusinessException("配置发布失败：" + result.getMessage());
        }
    }

    /**
     * 回滚配置
     */
    @Transactional(rollbackFor = Exception.class)
    public void rollbackConfig(ConfigRollbackDTO rollbackDTO) {
        SysNacosConfig config = nacosConfigMapper.selectById(rollbackDTO.getConfigId());
        if (config == null) {
            throw new BusinessException("配置不存在");
        }

        SysNacosConfigHistory history = configHistoryMapper.selectById(rollbackDTO.getHistoryId());
        if (history == null) {
            throw new BusinessException("历史版本不存在");
        }

        // 回滚配置内容
        config.setContent(history.getContent());
        config.setVersion(config.getVersion() + 1);
        config.setMd5(history.getMd5());
        nacosConfigMapper.updateById(config);

        // 发布到Nacos
        ConfigInfo configInfo = convertToConfigInfo(config);
        configPublisher.manualPublish(configInfo);

        // 记录历史
        recordHistory(config, "rollback", history.getVersion());
    }

    /**
     * 记录配置历史
     */
    private void recordHistory(SysNacosConfig config, String operationType, Integer rollbackFrom) {
        SysNacosConfigHistory history = new SysNacosConfigHistory();
        history.setConfigId(config.getId());
        history.setDataId(config.getDataId());
        history.setGroupName(config.getGroupName());
        history.setNamespace(config.getNamespace());
        history.setContent(config.getContent());
        history.setVersion(config.getVersion());
        history.setOperationType(operationType);
        history.setRollbackFrom(rollbackFrom);
        history.setMd5(config.getMd5());

        configHistoryMapper.insert(history);
    }

    /**
     * 转换为ConfigInfo
     */
    private ConfigInfo convertToConfigInfo(SysNacosConfig config) {
        return ConfigInfo.builder()
                .id(config.getId())
                .dataId(config.getDataId())
                .group(config.getGroupName())
                .namespace(config.getNamespace())
                .content(config.getContent())
                .type(config.getType())
                .environment(config.getEnvironment())
                .tenantId(config.getTenantId())
                .appId(config.getAppId())
                .version(config.getVersion())
                .status(config.getStatus())
                .isCritical(config.getIsCritical())
                .publishType(config.getPublishType())
                .description(config.getDescription())
                .md5(config.getMd5())
                .build();
    }
}
