package com.basebackend.logging.masking;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Logback脱敏转换器
 *
 * 在日志输出阶段自动脱敏格式化后的消息。
 *
 * 使用方法：
 * 1. 在logback-spring.xml中声明转换规则：
 *    <conversionRule conversionWord="maskMsg" converterClass="com.basebackend.logging.masking.MaskingMessageConverter"/>
 *
 * 2. 在模式中使用：
 *    %d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger - %maskMsg%n
 *
 * @author basebackend team
 * @since 2025-11-22
 */
public class MaskingMessageConverter extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent event) {
        PiiMaskingService svc = MaskingServiceHolder.get();
        if (svc == null || event == null || event.getFormattedMessage() == null) {
            return event == null ? "" : event.getFormattedMessage();
        }
        return svc.mask(event.getFormattedMessage());
    }
}
