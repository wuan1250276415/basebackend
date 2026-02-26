package com.basebackend.logging.audit.export;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 审计导出配置属性
 *
 * @author basebackend team
 * @since 2025-12-10
 */
@Data
@ConfigurationProperties(prefix = "basebackend.logging.audit.export")
public class AuditExportProperties {

    /**
     * 是否启用审计导出
     */
    private boolean enabled = true;

    /**
     * CEF 格式配置
     */
    private CefConfig cef = new CefConfig();

    /**
     * OCSF 格式配置
     */
    private OcsfConfig ocsf = new OcsfConfig();

    @Data
    public static class CefConfig {
        private String vendor = "BaseBackend";
        private String product = "AuditSystem";
        private String version = "1.0";
    }

    @Data
    public static class OcsfConfig {
        private String schemaVersion = "1.1.0";
        private String productName = "BaseBackend Audit";
        private String vendor = "BaseBackend";
    }
}
