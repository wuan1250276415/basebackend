package com.basebackend.messaging.management.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.common.context.UserContextHolder;
import com.basebackend.common.dto.PageResult;
import com.basebackend.messaging.entity.WebhookEndpointEntity;
import com.basebackend.messaging.management.dto.WebhookUpsertRequest;
import com.basebackend.messaging.mapper.WebhookEndpointMapper;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

public class WebhookManagementService {

    private final WebhookEndpointMapper webhookEndpointMapper;

    public WebhookManagementService(WebhookEndpointMapper webhookEndpointMapper) {
        this.webhookEndpointMapper = webhookEndpointMapper;
    }

    public PageResult<WebhookEndpointEntity> getPage(long current, long size, String name) {
        LambdaQueryWrapper<WebhookEndpointEntity> wrapper = new LambdaQueryWrapper<WebhookEndpointEntity>()
                .orderByDesc(WebhookEndpointEntity::getId);
        if (StringUtils.hasText(name)) {
            wrapper.like(WebhookEndpointEntity::getName, name.trim());
        }

        Page<WebhookEndpointEntity> page = webhookEndpointMapper.selectPage(new Page<>(current, size), wrapper);
        List<WebhookEndpointEntity> records = page.getRecords().stream()
                .map(this::sanitizeForDisplay)
                .toList();
        return PageResult.of(records, page.getTotal(), current, size);
    }

    public WebhookEndpointEntity getById(Long id) {
        WebhookEndpointEntity entity = webhookEndpointMapper.selectById(id);
        return entity == null ? null : sanitizeForDisplay(entity);
    }

    public void create(WebhookUpsertRequest request) {
        WebhookEndpointEntity entity = new WebhookEndpointEntity();
        applyRequest(entity, request, false);
        entity.setCreateBy(UserContextHolder.getUserId());
        entity.setUpdateBy(UserContextHolder.getUserId());
        entity.setDeleted(Boolean.FALSE);
        webhookEndpointMapper.insert(entity);
    }

    public void update(Long id, WebhookUpsertRequest request) {
        WebhookEndpointEntity entity = webhookEndpointMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("Webhook配置不存在");
        }

        applyRequest(entity, request, true);
        entity.setUpdateBy(UserContextHolder.getUserId());
        webhookEndpointMapper.updateById(entity);
    }

    public void delete(Long id) {
        if (webhookEndpointMapper.deleteById(id) == 0) {
            throw new IllegalArgumentException("Webhook配置不存在");
        }
    }

    public void toggle(Long id, boolean enabled) {
        WebhookEndpointEntity entity = webhookEndpointMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("Webhook配置不存在");
        }
        entity.setEnabled(enabled);
        entity.setUpdateBy(UserContextHolder.getUserId());
        webhookEndpointMapper.updateById(entity);
    }

    public List<WebhookEndpointEntity> listEnabled() {
        LambdaQueryWrapper<WebhookEndpointEntity> wrapper = new LambdaQueryWrapper<WebhookEndpointEntity>()
                .eq(WebhookEndpointEntity::getEnabled, Boolean.TRUE)
                .orderByAsc(WebhookEndpointEntity::getName);
        return webhookEndpointMapper.selectList(wrapper).stream()
                .map(this::sanitizeForDisplay)
                .toList();
    }

    private void applyRequest(WebhookEndpointEntity entity, WebhookUpsertRequest request, boolean preserveSecretWhenBlank) {
        entity.setName(request.name().trim());
        entity.setUrl(request.url().trim());
        entity.setEventTypes(request.eventTypes().trim());
        if (!preserveSecretWhenBlank || StringUtils.hasText(request.secret())) {
            entity.setSecret(StringUtils.hasText(request.secret()) ? request.secret().trim() : null);
        }
        entity.setSignatureEnabled(request.signatureEnabled());
        entity.setMethod(request.method().trim().toUpperCase());
        entity.setHeaders(StringUtils.hasText(request.headers()) ? request.headers().trim() : null);
        entity.setTimeout(request.timeout() != null ? request.timeout() : 30);
        entity.setMaxRetries(request.maxRetries() != null ? request.maxRetries() : 3);
        entity.setRetryInterval(request.retryInterval() != null ? request.retryInterval() : 60);
        entity.setEnabled(request.enabled());
        entity.setRemark(StringUtils.hasText(request.remark()) ? request.remark().trim() : null);
        entity.setUpdateTime(LocalDateTime.now());
    }

    private WebhookEndpointEntity sanitizeForDisplay(WebhookEndpointEntity source) {
        WebhookEndpointEntity target = new WebhookEndpointEntity();
        target.setId(source.getId());
        target.setName(source.getName());
        target.setUrl(source.getUrl());
        target.setEventTypes(source.getEventTypes());
        target.setSignatureEnabled(source.getSignatureEnabled());
        target.setMethod(source.getMethod());
        target.setHeaders(source.getHeaders());
        target.setTimeout(source.getTimeout());
        target.setMaxRetries(source.getMaxRetries());
        target.setRetryInterval(source.getRetryInterval());
        target.setEnabled(source.getEnabled());
        target.setCreateTime(source.getCreateTime());
        target.setUpdateTime(source.getUpdateTime());
        target.setCreateBy(source.getCreateBy());
        target.setUpdateBy(source.getUpdateBy());
        target.setRemark(source.getRemark());
        return target;
    }
}
