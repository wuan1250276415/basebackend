package com.basebackend.scheduler.camunda.delegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 发送邮件服务任务委托
 *
 * <p>功能说明：
 * <ul>
 *   <li>从流程变量中读取邮件参数（收件人、主题、内容）</li>
 *   <li>支持简单模板变量替换</li>
 *   <li>将发送结果写回流程变量</li>
 *   <li>发送失败抛出业务异常（BpmnError）</li>
 * </ul>
 *
 * <p>输入变量：
 * <ul>
 *   <li>emailTo (String, required): 收件人邮箱地址</li>
 *   <li>emailSubject (String, required): 邮件主题</li>
 *   <li>emailContent (String, required): 邮件内容</li>
 *   <li>emailTemplateParams (Map, optional): 模板参数，用于变量替换</li>
 * </ul>
 *
 * <p>输出变量：
 * <ul>
 *   <li>emailSendStatus (String): 发送状态 SUCCESS/FAILED</li>
 *   <li>emailSendMessage (String): 发送结果消息</li>
 *   <li>emailSendTime (String): 发送时间</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Slf4j
@Component("sendEmailDelegate")
public class SendEmailDelegate implements JavaDelegate {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public SendEmailDelegate(
            JavaMailSender mailSender,
            @Value("${spring.mail.from:noreply@basebackend.com}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String processInstanceId = execution.getProcessInstanceId();
        String activityId = execution.getCurrentActivityId();

        log.info("SendEmailDelegate started, processInstanceId={}, activityId={}",
                processInstanceId, activityId);

        try {
            // 获取并验证必需的流程变量
            String to = (String) execution.getVariable("emailTo");
            String subject = (String) execution.getVariable("emailSubject");
            String content = (String) execution.getVariable("emailContent");

            validateRequiredVariables(to, subject, content);

            // 获取可选的模板参数
            @SuppressWarnings("unchecked")
            Map<String, Object> templateParams = (Map<String, Object>)
                    execution.getVariable("emailTemplateParams");

            // 应用模板参数替换
            String finalContent = applyTemplateParams(content, templateParams);
            String finalSubject = applyTemplateParams(subject, templateParams);

            // 发送邮件
            sendEmail(to, finalSubject, finalContent);

            // 记录成功状态
            String sendTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            execution.setVariable("emailSendStatus", "SUCCESS");
            execution.setVariable("emailSendMessage", "邮件发送成功");
            execution.setVariable("emailSendTime", sendTime);

            log.info("Email sent successfully, processInstanceId={}, to={}, subject={}",
                    processInstanceId, to, finalSubject);

        } catch (IllegalArgumentException ex) {
            // 参数验证失败，抛出业务异常
            log.error("Email validation failed, processInstanceId={}, error={}",
                    processInstanceId, ex.getMessage());

            execution.setVariable("emailSendStatus", "FAILED");
            execution.setVariable("emailSendMessage", ex.getMessage());

            throw new BpmnError("EMAIL_VALIDATION_ERROR", ex.getMessage());

        } catch (Exception ex) {
            // 发送失败，记录错误并抛出业务异常
            log.error("Email sending failed, processInstanceId={}", processInstanceId, ex);

            execution.setVariable("emailSendStatus", "FAILED");
            execution.setVariable("emailSendMessage", "邮件发送失败: " + ex.getMessage());

            throw new BpmnError("EMAIL_SEND_ERROR", "邮件发送失败: " + ex.getMessage());
        }
    }

    /**
     * 验证必需的流程变量
     */
    private void validateRequiredVariables(String to, String subject, String content) {
        if (!StringUtils.hasText(to)) {
            throw new IllegalArgumentException("收件人地址不能为空（emailTo）");
        }
        if (!StringUtils.hasText(subject)) {
            throw new IllegalArgumentException("邮件主题不能为空（emailSubject）");
        }
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("邮件内容不能为空（emailContent）");
        }

        // 简单的邮箱格式验证
        if (!to.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new IllegalArgumentException("收件人邮箱地址格式不正确: " + to);
        }
    }

    /**
     * 应用模板参数替换
     *
     * @param template 模板字符串，使用 ${key} 格式的占位符
     * @param params 参数Map
     * @return 替换后的字符串
     */
    private String applyTemplateParams(String template, Map<String, Object> params) {
        if (template == null || params == null || params.isEmpty()) {
            return template;
        }

        String result = template;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace(placeholder, value);
        }

        return result;
    }

    /**
     * 发送邮件
     */
    private void sendEmail(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);

        mailSender.send(message);
    }
}
