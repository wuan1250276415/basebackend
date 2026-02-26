package com.basebackend.logging.audit.export;

import com.basebackend.logging.audit.model.AuditLogEntry;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 审计日志导出门面服务
 *
 * 统一管理 CEF、OCSF、CSV 三种导出格式。
 *
 * @author basebackend team
 * @since 2025-12-10
 */
@Slf4j
public class AuditExportService {

    private final CefExporter cefExporter;
    private final OcsfExporter ocsfExporter;
    private final CsvExporter csvExporter;
    private final LeefExporter leefExporter;

    public AuditExportService(CefExporter cefExporter,
                              OcsfExporter ocsfExporter,
                              CsvExporter csvExporter,
                              LeefExporter leefExporter) {
        this.cefExporter = cefExporter;
        this.ocsfExporter = ocsfExporter;
        this.csvExporter = csvExporter;
        this.leefExporter = leefExporter;
    }

    /**
     * 导出为 CEF 格式
     */
    public String exportToCef(List<AuditLogEntry> entries) {
        log.debug("导出 {} 条审计日志为 CEF 格式", entries != null ? entries.size() : 0);
        return cefExporter.export(entries);
    }

    /**
     * 导出为 OCSF JSON 格式
     */
    public String exportToOcsf(List<AuditLogEntry> entries) {
        log.debug("导出 {} 条审计日志为 OCSF 格式", entries != null ? entries.size() : 0);
        return ocsfExporter.export(entries);
    }

    /**
     * 导出为 CSV 格式
     */
    public String exportToCsv(List<AuditLogEntry> entries) {
        log.debug("导出 {} 条审计日志为 CSV 格式", entries != null ? entries.size() : 0);
        return csvExporter.export(entries);
    }

    /**
     * 导出为 LEEF 格式
     */
    public String exportToLeef(List<AuditLogEntry> entries) {
        log.debug("导出 {} 条审计日志为 LEEF 格式", entries != null ? entries.size() : 0);
        return leefExporter.export(entries);
    }
}
