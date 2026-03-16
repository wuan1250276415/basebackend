package com.basebackend.mall.product.config;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * 商品服务原生镜像运行时提示
 */
public class MallProductApiNativeHints implements RuntimeHintsRegistrar {

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
