package com.basebackend.messaging.event;

import cn.hutool.core.util.IdUtil;
import com.basebackend.messaging.webhook.WebhookConfig;
import com.basebackend.messaging.webhook.WebhookEvent;
import com.basebackend.messaging.webhook.WebhookInvoker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 事件发布服务
 */
@Slf4j
@Service
public class EventPublisher {

    private final JdbcTemplate jdbcTemplate;
    private final WebhookInvoker webhookInvoker;

    public EventPublisher(JdbcTemplate jdbcTemplate, WebhookInvoker webhookInvoker) {
        this.jdbcTemplate = jdbcTemplate;
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
        // 构建事件
        WebhookEvent event = WebhookEvent.builder()
                .eventId(IdUtil.fastSimpleUUID())
                .eventType(eventType)
                .data(data)
                .timestamp(LocalDateTime.now())
                .source(source)
                .metadata(metadata)
                .build();

        // 查询订阅了该事件类型的Webhook配置
        List<WebhookConfig> configs = getSubscribedWebhooks(eventType);

        if (configs.isEmpty()) {
            log.debug("No webhooks subscribed to event type: {}", eventType);
            return;
        }

        // 异步调用所有订阅的Webhook
        for (WebhookConfig config : configs) {
            try {
                webhookInvoker.invokeAsync(config, event);
            } catch (Exception e) {
                log.error("Failed to invoke webhook: webhookId={}, eventType={}",
                        config.getId(), eventType, e);
            }
        }

        log.info("Event published: eventId={}, eventType={}, webhookCount={}",
                event.getEventId(), eventType, configs.size());
    }

    /**
     * 获取订阅了指定事件类型的Webhook配置
     *
     * @param eventType 事件类型
     * @return Webhook配置列表
     */
    private List<WebhookConfig> getSubscribedWebhooks(String eventType) {
        String sql = """
                SELECT id, name, url, event_types, secret, signature_enabled,
                       method, headers, timeout, max_retries, retry_interval,
                       enabled, create_time, update_time, create_by, remark
                FROM sys_webhook_config
                WHERE enabled = 1
                  AND (event_types = '*' OR FIND_IN_SET(?, event_types))
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            WebhookConfig config = new WebhookConfig();
            config.setId(rs.getLong("id"));
            config.setName(rs.getString("name"));
            config.setUrl(rs.getString("url"));
            config.setEventTypes(rs.getString("event_types"));
            config.setSecret(rs.getString("secret"));
            config.setSignatureEnabled(rs.getBoolean("signature_enabled"));
            config.setMethod(rs.getString("method"));
            config.setHeaders(rs.getString("headers"));
            config.setTimeout(rs.getInt("timeout"));
            config.setMaxRetries(rs.getInt("max_retries"));
            config.setRetryInterval(rs.getInt("retry_interval"));
            config.setEnabled(rs.getBoolean("enabled"));
            config.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
            if (rs.getTimestamp("update_time") != null) {
                config.setUpdateTime(rs.getTimestamp("update_time").toLocalDateTime());
            }
            config.setCreateBy(rs.getLong("create_by"));
            config.setRemark(rs.getString("remark"));
            return config;
        }, eventType);
    }
}
