package com.basebackend.messaging.webhook;

import com.alibaba.fastjson2.JSON;
import com.basebackend.messaging.producer.MessageProducer;
import com.basebackend.messaging.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Webhook调用服务
 */
@Slf4j
@Service
public class WebhookInvoker {

    private final RestTemplate restTemplate;
    private final WebhookSignatureService signatureService;
    private final MessageProducer messageProducer;

    public WebhookInvoker(RestTemplate restTemplate,
                          WebhookSignatureService signatureService,
                          MessageProducer messageProducer) {
        this.restTemplate = restTemplate;
        this.signatureService = signatureService;
        this.messageProducer = messageProducer;
    }

    /**
     * 调用Webhook
     *
     * @param config Webhook配置
     * @param event  事件数据
     * @return 调用日志
     */
    public WebhookLog invoke(WebhookConfig config, WebhookEvent event) {
        WebhookLog webhookLog = new WebhookLog();
        webhookLog.setWebhookId(config.getId());
        webhookLog.setEventId(event.getEventId());
        webhookLog.setEventType(event.getEventType());
        webhookLog.setRequestUrl(config.getUrl());
        webhookLog.setRequestMethod(config.getMethod());
        webhookLog.setCallTime(LocalDateTime.now());
        webhookLog.setRetryCount(0);

        try {
            // 构建请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 添加自定义请求头
            if (config.getHeaders() != null) {
                Map<String, String> customHeaders = JSON.parseObject(config.getHeaders(), Map.class);
                customHeaders.forEach(headers::set);
            }

            // 构建请求体
            String requestBody = JSON.toJSONString(event);
            webhookLog.setRequestBody(requestBody);

            // 添加签名
            if (config.getSignatureEnabled() && config.getSecret() != null) {
                signatureService.addSignatureHeaders(headers, requestBody, config.getSecret());
            }

            webhookLog.setRequestHeaders(JSON.toJSONString(headers.toSingleValueMap()));

            // 发送请求
            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);
            long startTime = System.currentTimeMillis();

            ResponseEntity<String> response;
            if ("POST".equalsIgnoreCase(config.getMethod())) {
                response = restTemplate.postForEntity(config.getUrl(), requestEntity, String.class);
            } else if ("PUT".equalsIgnoreCase(config.getMethod())) {
                response = restTemplate.exchange(config.getUrl(), HttpMethod.PUT, requestEntity, String.class);
            } else {
                throw new UnsupportedOperationException("Unsupported HTTP method: " + config.getMethod());
            }

            long responseTime = System.currentTimeMillis() - startTime;

            // 记录响应
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

            // 如果需要重试，发送到重试队列
            if (webhookLog.getRetryCount() < config.getMaxRetries()) {
                scheduleRetry(config, event, webhookLog.getRetryCount() + 1);
            }
        }

        webhookLog.setCreateTime(LocalDateTime.now());
        return webhookLog;
    }

    /**
     * 安排重试
     *
     * @param config     Webhook配置
     * @param event      事件数据
     * @param retryCount 重试次数
     */
    private void scheduleRetry(WebhookConfig config, WebhookEvent event, int retryCount) {
        // 计算重试延迟（指数退避）
        long delaySeconds = (long) (config.getRetryInterval() * Math.pow(2, retryCount - 1));
        long delayMillis = delaySeconds * 1000;

        // 构建重试消息
        Message<Map<String, Object>> retryMessage = Message.<Map<String, Object>>builder()
                .topic("webhook.retry")
                .routingKey("webhook.retry." + config.getId())
                .payload(Map.of(
                        "config", config,
                        "event", event,
                        "retryCount", retryCount
                ))
                .build();

        // 发送延迟消息
        messageProducer.sendDelay(retryMessage, delayMillis);

        log.info("Webhook retry scheduled: webhookId={}, eventId={}, retryCount={}, delaySeconds={}",
                config.getId(), event.getEventId(), retryCount, delaySeconds);
    }

    /**
     * 异步调用Webhook（通过消息队列）
     *
     * @param config Webhook配置
     * @param event  事件数据
     */
    public void invokeAsync(WebhookConfig config, WebhookEvent event) {
        Message<Map<String, Object>> message = Message.<Map<String, Object>>builder()
                .topic("webhook.invoke")
                .routingKey("webhook.invoke." + config.getId())
                .payload(Map.of(
                        "config", config,
                        "event", event
                ))
                .build();

        messageProducer.send(message);

        log.info("Webhook async invocation queued: webhookId={}, eventId={}",
                config.getId(), event.getEventId());
    }
}
