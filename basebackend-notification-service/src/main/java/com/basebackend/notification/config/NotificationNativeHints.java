package com.basebackend.notification.config;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * Notification Service 原生镜像运行时提示
 */
public class NotificationNativeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.resources()
                .registerPattern("mapper/*.xml")
                .registerPattern("application*.yml")
                .registerPattern("logback*.xml")
                .registerPattern("nacos-logback.xml")
                .registerPattern("META-INF/spring/*");
    }
}
