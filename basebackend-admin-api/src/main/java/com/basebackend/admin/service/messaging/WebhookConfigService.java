package com.basebackend.admin.service.messaging;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.admin.dto.messaging.WebhookConfigDTO;
import com.basebackend.admin.entity.messaging.SysWebhookConfig;
import com.basebackend.admin.mapper.messaging.SysWebhookConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Webhook配置服务
 */
@Slf4j
@Service
public class WebhookConfigService {

    private final SysWebhookConfigMapper webhookConfigMapper;

    public WebhookConfigService(SysWebhookConfigMapper webhookConfigMapper) {
        this.webhookConfigMapper = webhookConfigMapper;
    }

    /**
     * 分页查询Webhook配置
     */
    public Page<SysWebhookConfig> getWebhookConfigPage(Integer page, Integer size, String name) {
        Page<SysWebhookConfig> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysWebhookConfig> queryWrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            queryWrapper.like(SysWebhookConfig::getName, name);
        }
        queryWrapper.orderByDesc(SysWebhookConfig::getCreateTime);
        return webhookConfigMapper.selectPage(pageParam, queryWrapper);
    }

    /**
     * 根据ID获取Webhook配置
     */
    public SysWebhookConfig getWebhookConfigById(Long id) {
        return webhookConfigMapper.selectById(id);
    }

    /**
     * 创建Webhook配置
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createWebhookConfig(WebhookConfigDTO dto) {
        SysWebhookConfig config = new SysWebhookConfig();
        BeanUtils.copyProperties(dto, config);
        webhookConfigMapper.insert(config);
        return config.getId();
    }

    /**
     * 更新Webhook配置
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateWebhookConfig(Long id, WebhookConfigDTO dto) {
        SysWebhookConfig config = new SysWebhookConfig();
        config.setId(id);
        BeanUtils.copyProperties(dto, config);
        webhookConfigMapper.updateById(config);
    }

    /**
     * 删除Webhook配置
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteWebhookConfig(Long id) {
        webhookConfigMapper.deleteById(id);
    }

    /**
     * 启用/禁用Webhook配置
     */
    @Transactional(rollbackFor = Exception.class)
    public void toggleWebhookConfig(Long id, Boolean enabled) {
        SysWebhookConfig config = new SysWebhookConfig();
        config.setId(id);
        config.setEnabled(enabled);
        webhookConfigMapper.updateById(config);
    }

    /**
     * 获取所有启用的Webhook配置
     */
    public List<SysWebhookConfig> getEnabledWebhookConfigs() {
        LambdaQueryWrapper<SysWebhookConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysWebhookConfig::getEnabled, true);
        return webhookConfigMapper.selectList(queryWrapper);
    }
}
