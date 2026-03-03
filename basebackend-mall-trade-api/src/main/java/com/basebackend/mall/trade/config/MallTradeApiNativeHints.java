package com.basebackend.mall.trade.config;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * 交易服务原生镜像运行时提示
 */
public class MallTradeApiNativeHints implements RuntimeHintsRegistrar {

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
