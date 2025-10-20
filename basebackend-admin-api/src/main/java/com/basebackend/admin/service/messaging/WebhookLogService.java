package com.basebackend.admin.service.messaging;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.admin.dto.messaging.WebhookLogQueryDTO;
import com.basebackend.admin.entity.messaging.SysWebhookLog;
import com.basebackend.admin.mapper.messaging.SysWebhookLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Webhook调用日志服务
 */
@Slf4j
@Service
public class WebhookLogService {

    private final SysWebhookLogMapper webhookLogMapper;

    public WebhookLogService(SysWebhookLogMapper webhookLogMapper) {
        this.webhookLogMapper = webhookLogMapper;
    }

    /**
     * 分页查询Webhook调用日志
     */
    public Page<SysWebhookLog> getWebhookLogPage(WebhookLogQueryDTO dto) {
        Page<SysWebhookLog> pageParam = new Page<>(dto.getPage(), dto.getSize());
        LambdaQueryWrapper<SysWebhookLog> queryWrapper = new LambdaQueryWrapper<>();

        if (dto.getWebhookId() != null) {
            queryWrapper.eq(SysWebhookLog::getWebhookId, dto.getWebhookId());
        }
        if (dto.getEventType() != null && !dto.getEventType().isEmpty()) {
            queryWrapper.eq(SysWebhookLog::getEventType, dto.getEventType());
        }
        if (dto.getSuccess() != null) {
            queryWrapper.eq(SysWebhookLog::getSuccess, dto.getSuccess());
        }
        if (dto.getStartTime() != null) {
            queryWrapper.ge(SysWebhookLog::getCallTime, dto.getStartTime());
        }
        if (dto.getEndTime() != null) {
            queryWrapper.le(SysWebhookLog::getCallTime, dto.getEndTime());
        }

        queryWrapper.orderByDesc(SysWebhookLog::getCallTime);
        return webhookLogMapper.selectPage(pageParam, queryWrapper);
    }

    /**
     * 根据ID获取Webhook调用日志详情
     */
    public SysWebhookLog getWebhookLogById(Long id) {
        return webhookLogMapper.selectById(id);
    }

    /**
     * 保存Webhook调用日志
     */
    public void saveWebhookLog(SysWebhookLog log) {
        webhookLogMapper.insert(log);
    }
}
