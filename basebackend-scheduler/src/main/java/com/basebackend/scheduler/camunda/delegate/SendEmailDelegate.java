package com.basebackend.scheduler.camunda.delegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * 发送邮件通知任务委托
 */
@Slf4j
@Component("sendEmailDelegate")
public class SendEmailDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String recipient = (String) execution.getVariable("email");
        String subject = (String) execution.getVariable("emailSubject");
        String content = (String) execution.getVariable("emailContent");

        log.info("发送邮件通知: recipient={}, subject={}", recipient, subject);

        // TODO: 集成邮件发送服务
        // emailService.sendEmail(recipient, subject, content);

        // 模拟邮件发送
        Thread.sleep(1000);

        // 设置执行结果
        execution.setVariable("emailSent", true);
        execution.setVariable("emailSentTime", System.currentTimeMillis());

        log.info("邮件发送成功: recipient={}", recipient);
    }
}
