package com.basebackend.observability.alert.notifier;

import com.alibaba.fastjson2.JSON;
import com.basebackend.observability.alert.AlertEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 钉钉告警通知器
 * 通过钉钉机器人 Webhook 发送告警通知
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "observability.alert.dingtalk.enabled", havingValue = "true")
public class DingTalkAlertNotifier implements AlertNotifier {

    @Value("${observability.alert.dingtalk.webhook:}")
    private String webhookUrl;

    @Value("${observability.alert.dingtalk.secret:}")
    private String secret;

    @Value("${spring.application.name:BaseBackend}")
    private String applicationName;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public boolean sendAlert(AlertEvent event) {
        try {
            if (webhookUrl == null || webhookUrl.isEmpty()) {
                log.warn("DingTalk webhook URL is not configured");
                return false;
            }

            Map<String, Object> message = buildDingTalkMessage(event);
            String jsonMessage = JSON.toJSONString(message);

            String url = webhookUrl;
            if (secret != null && !secret.isEmpty()) {
                url = buildSecureUrl();
            }

            String response = restTemplate.postForObject(url, message, String.class);

            log.info("DingTalk alert sent successfully - ruleId: {}, ruleName: {}, response: {}",
                    event.getRuleId(), event.getRuleName(), response);
            return true;

        } catch (Exception e) {
            log.error("Failed to send DingTalk alert - ruleId: {}, ruleName: {}, error: {}",
                    event.getRuleId(), event.getRuleName(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getNotifierType() {
        return "dingtalk";
    }

    @Override
    public boolean isAvailable() {
        return webhookUrl != null && !webhookUrl.isEmpty();
    }

    /**
     * 构建钉钉消息体
     */
    private Map<String, Object> buildDingTalkMessage(AlertEvent event) {
        Map<String, Object> message = new HashMap<>();
        message.put("msgtype", "markdown");

        Map<String, String> markdown = new HashMap<>();
        markdown.put("title", buildTitle(event));
        markdown.put("text", buildMarkdownText(event));
        message.put("markdown", markdown);

        // @ 所有人（仅在 CRITICAL 级别时）
        if (event.getSeverity() == com.basebackend.observability.alert.AlertRule.AlertSeverity.CRITICAL) {
            Map<String, Object> at = new HashMap<>();
            at.put("isAtAll", true);
            message.put("at", at);
        }

        return message;
    }

    /**
     * 构建消息标题
     */
    private String buildTitle(AlertEvent event) {
        return String.format("[%s] %s 告警", applicationName, event.getSeverity());
    }

    /**
     * 构建 Markdown 格式的消息正文
     */
    private String buildMarkdownText(AlertEvent event) {
        StringBuilder text = new StringBuilder();

        text.append("## ").append(getSeverityEmoji(event.getSeverity()))
            .append(" 告警通知\n\n");

        text.append("**应用名称:** ").append(applicationName).append("\n\n");
        text.append("**告警规则:** ").append(event.getRuleName()).append("\n\n");
        text.append("**告警级别:** ").append(getSeverityText(event.getSeverity())).append("\n\n");
        text.append("**告警消息:** ").append(event.getMessage()).append("\n\n");
        text.append("**触发值:** ").append(event.getTriggerValue()).append("\n\n");
        text.append("**阈值:** ").append(event.getThresholdValue()).append("\n\n");
        text.append("**告警时间:** ").append(event.getAlertTime().format(DATE_TIME_FORMATTER)).append("\n\n");

        if (event.getMetadata() != null && !event.getMetadata().isEmpty()) {
            text.append("---\n\n**附加信息:**\n\n");
            event.getMetadata().forEach((key, value) ->
                text.append("- ").append(key).append(": ").append(value).append("\n"));
        }

        return text.toString();
    }

    /**
     * 构建带签名的安全 URL（如果配置了 secret）
     */
    private String buildSecureUrl() {
        try {
            long timestamp = System.currentTimeMillis();
            String stringToSign = timestamp + "\n" + secret;

            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256"));
            byte[] signData = mac.doFinal(stringToSign.getBytes("UTF-8"));
            String sign = java.net.URLEncoder.encode(
                    java.util.Base64.getEncoder().encodeToString(signData), "UTF-8");

            return webhookUrl + "&timestamp=" + timestamp + "&sign=" + sign;
        } catch (Exception e) {
            log.error("Failed to build secure DingTalk URL: {}", e.getMessage());
            return webhookUrl;
        }
    }

    /**
     * 获取告警级别的 Emoji
     */
    private String getSeverityEmoji(com.basebackend.observability.alert.AlertRule.AlertSeverity severity) {
        switch (severity) {
            case CRITICAL: return "🔴";
            case ERROR: return "🟠";
            case WARNING: return "🟡";
            case INFO: return "🔵";
            default: return "⚪";
        }
    }

    /**
     * 获取告警级别的文本（带颜色标记）
     */
    private String getSeverityText(com.basebackend.observability.alert.AlertRule.AlertSeverity severity) {
        switch (severity) {
            case CRITICAL: return "<font color=#FF0000>严重</font>";
            case ERROR: return "<font color=#FF6600>错误</font>";
            case WARNING: return "<font color=#FFCC00>警告</font>";
            case INFO: return "<font color=#0066FF>信息</font>";
            default: return severity.toString();
        }
    }
}
