package com.basebackend.logging.monitoring.config;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Grafana 仪表板配置
 *
 * 提供 Grafana 仪表板 JSON 配置的访问接口。
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@Component
public class GrafanaDashboardConfig {

    /**
     * 获取仪表板 JSON 作为资源
     */
    public ByteArrayResource asResource() {
        return new ByteArrayResource(
                DashboardJson.JSON.getBytes(),
                "logging-dashboard.json"
        );
    }

    /**
     * 获取原始 JSON 字符串
     */
    public String rawJson() {
        return DashboardJson.JSON;
    }

    /**
     * 获取内容类型
     */
    public MediaType contentType() {
        return MediaType.APPLICATION_JSON;
    }
}
