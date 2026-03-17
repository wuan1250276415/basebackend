package com.basebackend.messaging.event;

import cn.hutool.core.util.IdUtil;
import com.basebackend.messaging.entity.WebhookEndpointEntity;
import com.basebackend.messaging.mapper.WebhookEndpointMapper;
import com.basebackend.messaging.webhook.WebhookEvent;
import com.basebackend.messaging.webhook.WebhookInvoker;
import com.basebackend.messaging.webhook.WebhookProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 事件发布服务
 *
 * <p>查询已订阅指定事件类型的 Webhook 端点，并异步触发调用。
 * 使用 {@link WebhookEndpointMapper#selectSubscribed} 查询，自动应用逻辑删除过滤。</p>
 */
@Slf4j
@Service
public class EventPublisher {

    private final WebhookEndpointMapper webhookEndpointMapper;
    private final WebhookInvoker webhookInvoker;

    public EventPublisher(WebhookEndpointMapper webhookEndpointMapper, WebhookInvoker webhookInvoker) {
        this.webhookEndpointMapper = webhookEndpointMapper;
        this.webhookInvoker = webhookInvoker;
    }

    /**
     * 发布事件
     *
     * @param eventType 事件类型
     * @param data      事件数据
     * @param source    事件来源
     */
    public void publishEvent(String eventType, Object data, String source) {
        publishEvent(eventType, data, source, null);
    }

    /**
     * 发布事件
     *
     * @param eventType 事件类型
     * @param data      事件数据
     * @param source    事件来源
     * @param metadata  元数据
     */
    public void publishEvent(String eventType, Object data, String source, Map<String, Object> metadata) {
        WebhookEvent event = WebhookEvent.builder()
                .eventId(IdUtil.fastSimpleUUID())
                .eventType(eventType)
                .data(data)
                .timestamp(LocalDateTime.now())
                .source(source)
                .metadata(metadata)
                .build();

        List<WebhookEndpointEntity> endpoints = webhookEndpointMapper.selectSubscribed(eventType);

        if (endpoints.isEmpty()) {
            log.debug("No webhooks subscribed to event type: {}", eventType);
            return;
        }

        for (WebhookEndpointEntity endpoint : endpoints) {
            try {
                webhookInvoker.invokeAsync(toWebhookProperties(endpoint), event);
            } catch (Exception e) {
                log.error("Failed to invoke webhook: webhookId={}, eventType={}",
                        endpoint.getId(), eventType, e);
            }
        }

        log.info("Event published: eventId={}, eventType={}, webhookCount={}",
                event.getEventId(), eventType, endpoints.size());
    }

    /**
     * 将 Entity 转换为 WebhookProperties 模型
     */
    private WebhookProperties toWebhookProperties(WebhookEndpointEntity entity) {
        return WebhookProperties.builder()
                .id(entity.getId())
                .name(entity.getName())
                .url(entity.getUrl())
                .eventTypes(entity.getEventTypes())
                .secret(entity.getSecret())
                .signatureEnabled(entity.getSignatureEnabled())
                .method(entity.getMethod())
                .headers(entity.getHeaders())
                .timeout(entity.getTimeout())
                .maxRetries(entity.getMaxRetries())
                .retryInterval(entity.getRetryInterval())
                .enabled(entity.getEnabled())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .createBy(entity.getCreateBy())
                .remark(entity.getRemark())
                .build();
    }
}
