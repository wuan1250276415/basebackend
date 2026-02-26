package com.basebackend.logging.audit.export;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 审计导出自动配置
 *
 * @author basebackend team
 * @since 2025-12-10
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(AuditExportProperties.class)
@ConditionalOnProperty(value = "basebackend.logging.audit.export.enabled", matchIfMissing = true)
public class AuditExportAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CefExporter cefExporter(AuditExportProperties properties) {
        AuditExportProperties.CefConfig cef = properties.getCef();
        log.info("初始化 CEF 导出器: vendor={}, product={}", cef.getVendor(), cef.getProduct());
        return new CefExporter(cef.getVendor(), cef.getProduct(), cef.getVersion());
    }

    @Bean
    @ConditionalOnMissingBean
    public OcsfExporter ocsfExporter(AuditExportProperties properties) {
        AuditExportProperties.OcsfConfig ocsf = properties.getOcsf();
        log.info("初始化 OCSF 导出器: schema={}", ocsf.getSchemaVersion());
        return new OcsfExporter(ocsf.getSchemaVersion(), ocsf.getProductName(), ocsf.getVendor());
    }

    @Bean
    @ConditionalOnMissingBean
    public CsvExporter csvExporter() {
        log.info("初始化 CSV 导出器");
        return new CsvExporter();
    }

    @Bean
    @ConditionalOnMissingBean
    public LeefExporter leefExporter(AuditExportProperties properties) {
        AuditExportProperties.CefConfig cef = properties.getCef();
        log.info("初始化 LEEF 导出器");
        return new LeefExporter(cef.getVendor(), cef.getProduct(), cef.getVersion());
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditExportService auditExportService(CefExporter cefExporter,
                                                  OcsfExporter ocsfExporter,
                                                  CsvExporter csvExporter,
                                                  LeefExporter leefExporter) {
        log.info("初始化审计导出服务");
        return new AuditExportService(cefExporter, ocsfExporter, csvExporter, leefExporter);
    }
}
