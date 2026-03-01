package com.basebackend.ticket.config;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * Ticket API 原生镜像运行时提示
 */
public class TicketApiNativeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.resources()
                .registerPattern("mapper/*.xml")
                .registerPattern("application*.yml")
                .registerPattern("logback*.xml")
                .registerPattern("META-INF/spring/*");
    }
}
