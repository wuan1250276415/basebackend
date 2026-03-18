package com.basebackend.messaging.webhook;

import com.basebackend.common.util.JsonUtils;
import com.basebackend.messaging.management.service.WebhookLogStore;
import com.basebackend.messaging.producer.MessageProducer;
import com.basebackend.messaging.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.*;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Webhook 调用服务
 *
 * <p>支持同步调用 {@link #invoke} 和异步调用 {@link #invokeAsync}。
 * 异步调用在 webhookExecutor（虚拟线程）中执行，调用失败时通过指数退避延迟消息触发重试。</p>
 */
@Slf4j
public class WebhookInvoker {

    private final RestClient restClient;
    private final WebhookSignatureService signatureService;
    private final MessageProducer messageProducer;
    private final TaskExecutor webhookExecutor;
    private final WebhookLogStore webhookLogStore;

    public WebhookInvoker(RestClient restClient,
            WebhookSignatureService signatureService,
            MessageProducer messageProducer,
            TaskExecutor webhookExecutor) {
        this(restClient, signatureService, messageProducer, webhookExecutor, null);
    }

    public WebhookInvoker(RestClient restClient,
            WebhookSignatureService signatureService,
            MessageProducer messageProducer,
            TaskExecutor webhookExecutor,
            WebhookLogStore webhookLogStore) {
        this.restClient = restClient;
        this.signatureService = signatureService;
        this.messageProducer = messageProducer;
        this.webhookExecutor = webhookExecutor;
        this.webhookLogStore = webhookLogStore;
    }

    /**
     * 同步调用 Webhook
     *
     * @param config Webhook 配置
     * @param event  事件数据
     * @return 调用日志
     */
    public WebhookLog invoke(WebhookProperties config, WebhookEvent event) {
        WebhookLog webhookLog = new WebhookLog();
        webhookLog.setWebhookId(config.getId());
        webhookLog.setEventId(event.getEventId());
        webhookLog.setEventType(event.getEventType());
        webhookLog.setRequestUrl(config.getUrl());
        webhookLog.setRequestMethod(config.getMethod());
        webhookLog.setCallTime(LocalDateTime.now());
        webhookLog.setRetryCount(0);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            if (config.getHeaders() != null) {
                @SuppressWarnings("unchecked")
                Map<String, String> customHeaders = JsonUtils.parseObject(config.getHeaders(), Map.class);
                customHeaders.forEach(headers::set);
            }

            String requestBody = JsonUtils.toJsonString(event);
            webhookLog.setRequestBody(requestBody);

            if (config.getSignatureEnabled() && config.getSecret() != null) {
                signatureService.addSignatureHeaders(headers, requestBody, config.getSecret());
            }

            webhookLog.setRequestHeaders(JsonUtils.toJsonString(headers.toSingleValueMap()));

            long startTime = System.currentTimeMillis();

            ResponseEntity<String> response;
            if ("POST".equalsIgnoreCase(config.getMethod())) {
                response = restClient.post().uri(config.getUrl()).headers(h -> h.addAll(headers)).body(requestBody).retrieve().toEntity(String.class);
            } else if ("PUT".equalsIgnoreCase(config.getMethod())) {
                response = restClient.put().uri(config.getUrl()).headers(h -> h.addAll(headers)).body(requestBody).retrieve().toEntity(String.class);
            } else {
                throw new UnsupportedOperationException("Unsupported HTTP method: " + config.getMethod());
            }

            long responseTime = System.currentTimeMillis() - startTime;

            webhookLog.setResponseStatus(response.getStatusCode().value());
            webhookLog.setResponseBody(response.getBody());
            webhookLog.setResponseTime(responseTime);
            webhookLog.setSuccess(response.getStatusCode().is2xxSuccessful());

            if (!response.getStatusCode().is2xxSuccessful()) {
                webhookLog.setErrorMessage("HTTP " + response.getStatusCode().value());
            }

            log.info("Webhook invoked successfully: webhookId={}, eventId={}, status={}",
                    config.getId(), event.getEventId(), response.getStatusCode());

        } catch (Exception e) {
            log.error("Failed to invoke webhook: webhookId={}, eventId={}",
                    config.getId(), event.getEventId(), e);

            webhookLog.setSuccess(false);
            webhookLog.setErrorMessage(e.getMessage());
            webhookLog.setResponseTime(0L);

            if (webhookLog.getRetryCount() < config.getMaxRetries()) {
                scheduleRetry(config, event, webhookLog.getRetryCount() + 1);
            }
        }

        webhookLog.setCreateTime(LocalDateTime.now());
        persistLog(webhookLog);
        return webhookLog;
    }

    /**
     * 异步调用 Webhook（在 webhookExecutor 虚拟线程中执行）
     *
     * @param config Webhook 配置
     * @param event  事件数据
     */
    public void invokeAsync(WebhookProperties config, WebhookEvent event) {
        webhookExecutor.execute(() -> {
            try {
                invoke(config, event);
            } catch (Exception e) {
                log.error("Async webhook invocation failed: webhookId={}, eventId={}",
                        config.getId(), event.getEventId(), e);
            }
        });

        log.info("Webhook async invocation queued: webhookId={}, eventId={}",
                config.getId(), event.getEventId());
    }

    private void scheduleRetry(WebhookProperties config, WebhookEvent event, int retryCount) {
        if (messageProducer == null) {
            log.warn("MessageProducer not available, cannot schedule webhook retry: webhookId={}, eventId={}",
                    config.getId(), event.getEventId());
            return;
        }

        long delaySeconds = (long) (config.getRetryInterval() * Math.pow(2, retryCount - 1));
        long delayMillis = delaySeconds * 1000;

        Message<Map<String, Object>> retryMessage = Message.<Map<String, Object>>builder()
                .topic("webhook.retry")
                .routingKey("webhook.retry." + config.getId())
                .payload(Map.of(
                        "config", config,
                        "event", event,
                        "retryCount", retryCount))
                .build();

        messageProducer.sendDelay(retryMessage, delayMillis);

        log.info("Webhook retry scheduled: webhookId={}, eventId={}, retryCount={}, delaySeconds={}",
                config.getId(), event.getEventId(), retryCount, delaySeconds);
    }

    private void persistLog(WebhookLog webhookLog) {
        if (webhookLogStore == null) {
            return;
        }

        try {
            webhookLogStore.save(webhookLog);
        } catch (Exception ex) {
            log.warn("Failed to persist webhook log: webhookId={}, eventId={}",
                    webhookLog.getWebhookId(), webhookLog.getEventId(), ex);
        }
    }
}
