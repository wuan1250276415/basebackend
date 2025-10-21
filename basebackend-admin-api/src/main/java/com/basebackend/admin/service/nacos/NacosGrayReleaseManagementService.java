package com.basebackend.admin.service.nacos;

import com.basebackend.admin.dto.nacos.GrayReleaseDTO;
import com.basebackend.admin.entity.nacos.SysNacosConfig;
import com.basebackend.admin.entity.nacos.SysNacosGrayConfig;
import com.basebackend.admin.mapper.nacos.SysNacosConfigMapper;
import com.basebackend.admin.mapper.nacos.SysNacosGrayConfigMapper;
import com.basebackend.common.exception.BusinessException;
import com.basebackend.nacos.model.ConfigInfo;
import com.basebackend.nacos.model.GrayReleaseConfig;
import com.basebackend.nacos.service.GrayReleaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Nacos灰度发布Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NacosGrayReleaseManagementService {

    private final SysNacosGrayConfigMapper grayConfigMapper;
    private final SysNacosConfigMapper nacosConfigMapper;
    private final GrayReleaseService grayReleaseService;

    /**
     * 创建灰度发布
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createGrayRelease(GrayReleaseDTO grayDTO) {
        SysNacosConfig config = nacosConfigMapper.selectById(grayDTO.getConfigId());
        if (config == null) {
            throw new BusinessException("配置不存在");
        }

        // 创建灰度配置
        SysNacosGrayConfig grayConfig = new SysNacosGrayConfig();
        BeanUtils.copyProperties(grayDTO, grayConfig);
        grayConfig.setStatus("preparing");
        grayConfig.setStartTime(LocalDateTime.now());

        grayConfigMapper.insert(grayConfig);

        // 启动灰度发布
        ConfigInfo configInfo = convertToConfigInfo(config);
        GrayReleaseConfig grayReleaseConfig = convertToGrayReleaseConfig(grayConfig);

        GrayReleaseService.GrayReleaseResult result = grayReleaseService.startGrayRelease(configInfo, grayReleaseConfig);

        if (result.isSuccess()) {
            grayConfig.setStatus("running");
            grayConfigMapper.updateById(grayConfig);
        } else {
            throw new BusinessException("灰度发布启动失败：" + result.getMessage());
        }

        return grayConfig.getId();
    }

    /**
     * 灰度全量发布
     */
    @Transactional(rollbackFor = Exception.class)
    public void promoteGrayRelease(Long grayId) {
        SysNacosGrayConfig grayConfig = grayConfigMapper.selectById(grayId);
        if (grayConfig == null) {
            throw new BusinessException("灰度配置不存在");
        }

        SysNacosConfig config = nacosConfigMapper.selectById(grayConfig.getConfigId());
        ConfigInfo configInfo = convertToConfigInfo(config);
        GrayReleaseConfig grayReleaseConfig = convertToGrayReleaseConfig(grayConfig);

        boolean success = grayReleaseService.promoteToFull(configInfo, grayReleaseConfig);

        if (success) {
            grayConfig.setStatus("completed");
            grayConfig.setEndTime(LocalDateTime.now());
            grayConfigMapper.updateById(grayConfig);
        } else {
            throw new BusinessException("灰度全量发布失败");
        }
    }

    /**
     * 回滚灰度发布
     */
    @Transactional(rollbackFor = Exception.class)
    public void rollbackGrayRelease(Long grayId) {
        SysNacosGrayConfig grayConfig = grayConfigMapper.selectById(grayId);
        if (grayConfig == null) {
            throw new BusinessException("灰度配置不存在");
        }

        SysNacosConfig config = nacosConfigMapper.selectById(grayConfig.getConfigId());
        ConfigInfo configInfo = convertToConfigInfo(config);
        GrayReleaseConfig grayReleaseConfig = convertToGrayReleaseConfig(grayConfig);

        boolean success = grayReleaseService.rollbackGrayRelease(configInfo, grayReleaseConfig);

        if (success) {
            grayConfig.setStatus("rollback");
            grayConfig.setEndTime(LocalDateTime.now());
            grayConfigMapper.updateById(grayConfig);
        } else {
            throw new BusinessException("灰度回滚失败");
        }
    }

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
                .build();
    }

    private GrayReleaseConfig convertToGrayReleaseConfig(SysNacosGrayConfig grayConfig) {
        return GrayReleaseConfig.builder()
                .id(grayConfig.getId())
                .configId(grayConfig.getConfigId())
                .strategyType(grayConfig.getStrategyType())
                .targetInstances(grayConfig.getTargetInstances())
                .percentage(grayConfig.getPercentage())
                .labels(grayConfig.getLabels())
                .status(grayConfig.getStatus())
                .grayContent(grayConfig.getGrayContent())
                .build();
    }
}
