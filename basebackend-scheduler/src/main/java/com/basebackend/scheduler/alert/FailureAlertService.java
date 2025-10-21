package com.basebackend.scheduler.alert;

import com.basebackend.scheduler.config.SchedulerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 失败告警服务
 * 支持钉钉、企业微信、邮件等多渠道告警
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FailureAlertService {

    private final SchedulerProperties schedulerProperties;

    /**
     * 发送任务失败告警
     *
     * @param jobName 任务名称
     * @param errorMsg 错误信息
     * @param retryTimes 重试次数
     */
    public void sendFailureAlert(String jobName, String errorMsg, Integer retryTimes) {
        if (!schedulerProperties.getAlert().getEnabled()) {
            log.debug("告警功能未启用");
            return;
        }

        String alertMessage = buildAlertMessage(jobName, errorMsg, retryTimes);

        // 发送钉钉告警
        if (schedulerProperties.getAlert().getDingTalkWebhook() != null) {
            sendDingTalkAlert(alertMessage);
        }

        // 发送企业微信告警
        if (schedulerProperties.getAlert().getWechatWebhook() != null) {
            sendWechatAlert(alertMessage);
        }

        // 发送邮件告警
        if (schedulerProperties.getAlert().getEmailTo() != null) {
            sendEmailAlert(alertMessage);
        }
    }

    /**
     * 构建告警消息
     */
    private String buildAlertMessage(String jobName, String errorMsg, Integer retryTimes) {
        return String.format(
                "【任务执行失败告警】\n" +
                "任务名称: %s\n" +
                "错误信息: %s\n" +
                "重试次数: %d\n" +
                "告警时间: %s",
                jobName, errorMsg, retryTimes,
                java.time.LocalDateTime.now().format(
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                )
        );
    }

    /**
     * 发送钉钉告警
     */
    private void sendDingTalkAlert(String message) {
        try {
            String webhook = schedulerProperties.getAlert().getDingTalkWebhook();
            Map<String, Object> body = Map.of(
                    "msgtype", "text",
                    "text", Map.of("content", message)
            );

            // TODO: 调用钉钉Webhook API
            log.info("发送钉钉告警: {}", message);
        } catch (Exception e) {
            log.error("发送钉钉告警失败", e);
        }
    }

    /**
     * 发送企业微信告警
     */
    private void sendWechatAlert(String message) {
        try {
            String webhook = schedulerProperties.getAlert().getWechatWebhook();
            Map<String, Object> body = Map.of(
                    "msgtype", "text",
                    "text", Map.of("content", message)
            );

            // TODO: 调用企业微信Webhook API
            log.info("发送企业微信告警: {}", message);
        } catch (Exception e) {
            log.error("发送企业微信告警失败", e);
        }
    }

    /**
     * 发送邮件告警
     */
    private void sendEmailAlert(String message) {
        try {
            // TODO: 调用邮件服务发送告警邮件
            log.info("发送邮件告警: {}", message);
        } catch (Exception e) {
            log.error("发送邮件告警失败", e);
        }
    }
}
