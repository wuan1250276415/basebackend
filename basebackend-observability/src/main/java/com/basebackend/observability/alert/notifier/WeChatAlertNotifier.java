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
 * 企业微信告警通知器
 * 通过企业微信机器人 Webhook 发送告警通知
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "observability.alert.wechat.enabled", havingValue = "true")
public class WeChatAlertNotifier implements AlertNotifier {

    @Value("${observability.alert.wechat.webhook:}")
    private String webhookUrl;

    @Value("${spring.application.name:BaseBackend}")
    private String applicationName;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public boolean sendAlert(AlertEvent event) {
        try {
            if (webhookUrl == null || webhookUrl.isEmpty()) {
                log.warn("WeChat webhook URL is not configured");
                return false;
            }

            Map<String, Object> message = buildWeChatMessage(event);
            String jsonMessage = JSON.toJSONString(message);

            String response = restTemplate.postForObject(webhookUrl, message, String.class);

            log.info("WeChat alert sent successfully - ruleId: {}, ruleName: {}, response: {}",
                    event.getRuleId(), event.getRuleName(), response);
            return true;

        } catch (Exception e) {
            log.error("Failed to send WeChat alert - ruleId: {}, ruleName: {}, error: {}",
                    event.getRuleId(), event.getRuleName(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getNotifierType() {
        return "wechat";
    }

    @Override
    public boolean isAvailable() {
        return webhookUrl != null && !webhookUrl.isEmpty();
    }

    /**
     * 构建企业微信消息体
     */
    private Map<String, Object> buildWeChatMessage(AlertEvent event) {
        Map<String, Object> message = new HashMap<>();
        message.put("msgtype", "markdown");

        Map<String, String> markdown = new HashMap<>();
        markdown.put("content", buildMarkdownContent(event));
        message.put("markdown", markdown);

        return message;
    }

    /**
     * 构建 Markdown 格式的消息内容
     */
    private String buildMarkdownContent(AlertEvent event) {
        StringBuilder content = new StringBuilder();

        // 标题
        content.append("## ").append(getSeverityEmoji(event.getSeverity()))
               .append(" 告警通知\n");

        // 基本信息
        content.append("> **应用名称:** ").append(applicationName).append("\n");
        content.append("> **告警规则:** <font color=\"info\">").append(event.getRuleName()).append("</font>\n");
        content.append("> **告警级别:** ").append(getSeverityTag(event.getSeverity())).append("\n");
        content.append("> **告警消息:** ").append(event.getMessage()).append("\n");
        content.append("> **触发值:** <font color=\"warning\">").append(event.getTriggerValue()).append("</font>\n");
        content.append("> **阈值:** ").append(event.getThresholdValue()).append("\n");
        content.append("> **告警时间:** ").append(event.getAlertTime().format(DATE_TIME_FORMATTER)).append("\n");

        // 附加信息
        if (event.getMetadata() != null && !event.getMetadata().isEmpty()) {
            content.append("\n**附加信息:**\n");
            event.getMetadata().forEach((key, value) ->
                content.append("> - ").append(key).append(": ").append(value).append("\n"));
        }

        return content.toString();
    }

    /**
     * 获取告警级别的 Emoji
     */
    private String getSeverityEmoji(com.basebackend.observability.alert.AlertRule.AlertSeverity severity) {
        switch (severity) {
            case CRITICAL: return "🚨";
            case ERROR: return "❌";
            case WARNING: return "⚠️";
            case INFO: return "ℹ️";
            default: return "📢";
        }
    }

    /**
     * 获取告警级别的标签（带颜色）
     */
    private String getSeverityTag(com.basebackend.observability.alert.AlertRule.AlertSeverity severity) {
        switch (severity) {
            case CRITICAL: return "<font color=\"warning\">严重</font>";
            case ERROR: return "<font color=\"warning\">错误</font>";
            case WARNING: return "<font color=\"comment\">警告</font>";
            case INFO: return "<font color=\"info\">信息</font>";
            default: return severity.toString();
        }
    }
}
