package com.basebackend.gateway.config;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * Gateway 原生镜像运行时提示
 * <p>
 * 注册 GraalVM 原生编译所需的反射、资源、代理等元数据。
 * Spring AOT 会自动发现 META-INF/spring/aot.factories 中注册的 Registrar。
 */
public class GatewayNativeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        // 网关路由 DTO — Jackson 反序列化需要反射
        hints.reflection()
                .registerType(com.basebackend.gateway.route.RouteDefinitionDTO.class,
                        MemberCategory.values());

        // 灰度路由配置
        hints.reflection()
                .registerType(com.basebackend.gateway.gray.GrayRouteProperties.class,
                        MemberCategory.values());

        // 网关配置属性
        hints.reflection()
                .registerType(com.basebackend.gateway.config.GatewaySecurityProperties.class,
                        MemberCategory.values());

        // 资源文件
        hints.resources()
                .registerPattern("application*.yml")
                .registerPattern("logback*.xml")
                .registerPattern("META-INF/spring/*");
    }
}
