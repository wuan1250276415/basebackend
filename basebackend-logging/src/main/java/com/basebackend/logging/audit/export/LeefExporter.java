package com.basebackend.logging.audit.export;

import com.basebackend.logging.audit.AuditSeverity;
import com.basebackend.logging.audit.model.AuditLogEntry;
import lombok.extern.slf4j.Slf4j;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * LEEF (Log Event Extended Format) 导出器
 *
 * 将审计日志转换为 LEEF 2.0 格式，用于 IBM QRadar SIEM 集成。
 * 格式: LEEF:2.0|vendor|product|version|eventType|delimiter|extensions
 *
 * @author basebackend team
 * @since 2025-12-10
 */
@Slf4j
public class LeefExporter {

    private static final char LEEF_DELIMITER = '\t';
    private static final DateTimeFormatter LEEF_DATE_FORMAT =
            DateTimeFormatter.ofPattern("MMM dd yyyy HH:mm:ss").withZone(ZoneOffset.UTC);

    private final String vendor;
    private final String product;
    private final String version;

    public LeefExporter(String vendor, String product, String version) {
        this.vendor = vendor;
        this.product = product;
        this.version = version;
    }

    /**
     * 将审计日志条目列表导出为 LEEF 格式字符串
     */
    public String export(List<AuditLogEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return "";
        }
        return entries.stream()
                .map(this::convertToLeef)
                .collect(Collectors.joining("\n"));
    }

    /**
     * 将单条审计日志转换为 LEEF 2.0 格式
     */
    public String convertToLeef(AuditLogEntry entry) {
        String eventType = entry.getEventType() != null ? entry.getEventType().name() : "UNKNOWN";

        StringBuilder extensions = new StringBuilder();
        appendAttribute(extensions, "devTime", formatTimestamp(entry));
        appendAttribute(extensions, "usrName", entry.getUserId());
        appendAttribute(extensions, "src", entry.getClientIp());
        appendAttribute(extensions, "sessionId", entry.getSessionId());
        appendAttribute(extensions, "resource", entry.getResource());
        appendAttribute(extensions, "action", entry.getOperation());
        appendAttribute(extensions, "outcome", entry.getResult());
        appendAttribute(extensions, "sev", String.valueOf(mapSeverity(entry)));
        appendAttribute(extensions, "identSrc", entry.getTraceId());
        appendAttribute(extensions, "externalId", entry.getId());
        appendAttribute(extensions, "entityId", entry.getEntityId());
        appendAttribute(extensions, "msg", entry.getErrorMessage());
        if (entry.getDurationMs() != null) {
            appendAttribute(extensions, "responseTime", entry.getDurationMs().toString());
        }
        if (entry.getTenantId() != null) {
            appendAttribute(extensions, "tenantId", entry.getTenantId());
        }

        return String.format("LEEF:2.0|%s|%s|%s|%s|0x%02X|%s",
                escapePipe(vendor),
                escapePipe(product),
                escapePipe(version),
                escapePipe(eventType),
                (int) LEEF_DELIMITER,
                extensions.toString());
    }

    int mapSeverity(AuditLogEntry entry) {
        if (entry.getEventType() == null) {
            return 5;
        }
        AuditSeverity severity = entry.getEventType().getSeverity();
        return switch (severity) {
            case LOW -> 3;
            case MEDIUM -> 5;
            case HIGH -> 7;
            case CRITICAL -> 10;
        };
    }

    private String formatTimestamp(AuditLogEntry entry) {
        if (entry.getTimestamp() == null) {
            return null;
        }
        return LEEF_DATE_FORMAT.format(entry.getTimestamp());
    }

    private void appendAttribute(StringBuilder sb, String key, String value) {
        if (value != null && !value.isEmpty()) {
            if (!sb.isEmpty()) {
                sb.append(LEEF_DELIMITER);
            }
            sb.append(key).append('=').append(escapeValue(value));
        }
    }

    static String escapePipe(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("|", "\\|");
    }

    static String escapeValue(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("=", "\\=")
                .replace("\t", "\\t")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
