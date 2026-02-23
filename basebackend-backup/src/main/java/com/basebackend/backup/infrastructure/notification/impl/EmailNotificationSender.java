package com.basebackend.backup.infrastructure.notification.impl;

import com.basebackend.backup.config.BackupProperties;
import com.basebackend.backup.infrastructure.notification.BackupNotificationEvent;
import com.basebackend.backup.infrastructure.notification.BackupNotificationSender;
import lombok.extern.slf4j.Slf4j;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.Properties;

/**
 * 邮件通知发送器
 */
@Slf4j
public class EmailNotificationSender implements BackupNotificationSender {

    private final BackupProperties.Notify.Email config;

    public EmailNotificationSender(BackupProperties.Notify.Email config) {
        this.config = config;
    }

    @Override
    public void send(BackupNotificationEvent event) {
        List<String> recipients = config.getRecipients();
        if (recipients == null || recipients.isEmpty()) {
            log.warn("邮件通知未配置收件人，跳过发送");
            return;
        }

        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", config.getSmtpHost());
            props.put("mail.smtp.port", String.valueOf(config.getSmtpPort()));
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(config.getUsername(), config.getPassword());
                }
            });

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(config.getUsername()));
            for (String recipient : recipients) {
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            }

            String subject = buildSubject(event);
            message.setSubject(subject, "UTF-8");
            message.setText(event.toSummary(), "UTF-8");

            Transport.send(message);
            log.info("备份邮件通知发送成功, 收件人: {}", recipients);
        } catch (Exception e) {
            log.error("备份邮件通知发送失败", e);
        }
    }

    @Override
    public String getChannelType() {
        return "email";
    }

    private String buildSubject(BackupNotificationEvent event) {
        return "[BaseBackend] 备份通知 - " + event.getEventType().name();
    }
}
