package com.basebackend.observability.alert.notifier;

import com.basebackend.observability.alert.AlertEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/**
 * 邮件告警通知器
 * 通过邮件发送告警通知
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "observability.alert.email.enabled", havingValue = "true")
public class EmailAlertNotifier implements AlertNotifier {

    private final JavaMailSender mailSender;

    @Value("${observability.alert.email.from:noreply@basebackend.com}")
    private String fromEmail;

    @Value("${observability.alert.email.to:admin@basebackend.com}")
    private String toEmail;

    @Value("${spring.application.name:BaseBackend}")
    private String applicationName;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public boolean sendAlert(AlertEvent event) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail.split(","));
            message.setSubject(buildSubject(event));
            message.setText(buildEmailBody(event));

            mailSender.send(message);

            log.info("Email alert sent successfully - ruleId: {}, ruleName: {}, severity: {}",
                    event.getRuleId(), event.getRuleName(), event.getSeverity());
            return true;

        } catch (Exception e) {
            log.error("Failed to send email alert - ruleId: {}, ruleName: {}, error: {}",
                    event.getRuleId(), event.getRuleName(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getNotifierType() {
        return "email";
    }

    @Override
    public boolean isAvailable() {
        try {
            return mailSender != null;
        } catch (Exception e) {
            log.warn("Email notifier is not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 构建邮件主题
     */
    private String buildSubject(AlertEvent event) {
        return String.format("[%s] %s 告警 - %s",
                applicationName,
                event.getSeverity(),
                event.getRuleName());
    }

    /**
     * 构建邮件正文
     */
    private String buildEmailBody(AlertEvent event) {
        StringBuilder body = new StringBuilder();
        body.append("========================================\n");
        body.append("告警通知\n");
        body.append("========================================\n\n");

        body.append("应用名称: ").append(applicationName).append("\n");
        body.append("告警规则: ").append(event.getRuleName()).append("\n");
        body.append("告警级别: ").append(event.getSeverity()).append("\n");
        body.append("告警消息: ").append(event.getMessage()).append("\n");
        body.append("触发值: ").append(event.getTriggerValue()).append("\n");
        body.append("阈值: ").append(event.getThresholdValue()).append("\n");
        body.append("告警时间: ").append(event.getAlertTime().format(DATE_TIME_FORMATTER)).append("\n");

        if (event.getMetadata() != null && !event.getMetadata().isEmpty()) {
            body.append("\n附加信息:\n");
            event.getMetadata().forEach((key, value) ->
                body.append("  ").append(key).append(": ").append(value).append("\n"));
        }

        body.append("\n========================================\n");
        body.append("此邮件由监控系统自动发送，请勿回复\n");

        return body.toString();
    }
}
