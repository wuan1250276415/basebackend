package com.basebackend.gateway.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

/**
 * 网关组件扫描配置
 * 只扫描需要的common模块中的类，排除Servlet API相关的类
 */
@Configuration
@ComponentScan(
        basePackages = {
                "com.basebackend.gateway",
                "com.basebackend.jwt",
                "com.basebackend.common.model",
                "com.basebackend.common.constant"
        },
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "com\\.basebackend\\.common\\.web\\..*"
                ),
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "com\\.basebackend\\.common\\.exception\\..*"
                ),
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "com\\.basebackend\\.common\\.security\\..*"
                )
        }
)
public class GatewayComponentScanConfig {
}
