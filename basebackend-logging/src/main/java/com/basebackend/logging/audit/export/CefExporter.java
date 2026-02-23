package com.basebackend.logging.audit.export;

import com.basebackend.logging.audit.AuditSeverity;
import com.basebackend.logging.audit.model.AuditLogEntry;
import lombok.extern.slf4j.Slf4j;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CEF (Common Event Format) 导出器
 *
 * 将审计日志转换为 CEF 格式，用于 SIEM 系统集成。
 * 格式: CEF:0|vendor|product|version|eventType|description|severity|extensions
 *
 * @author basebackend team
 * @since 2025-12-10
 */
@Slf4j
public class CefExporter {

    private static final DateTimeFormatter CEF_DATE_FORMAT =
            DateTimeFormatter.ofPattern("MMM dd yyyy HH:mm:ss.SSS zzz").withZone(ZoneOffset.UTC);

    private final String vendor;
    private final String product;
    private final String version;

    public CefExporter(String vendor, String product, String version) {
        this.vendor = vendor;
        this.product = product;
        this.version = version;
    }

    /**
     * 将审计日志条目列表导出为 CEF 格式字符串
     */
    public String export(List<AuditLogEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return "";
        }
        return entries.stream()
                .map(this::convertToCef)
                .collect(Collectors.joining("\n"));
    }

    /**
     * 将单条审计日志转换为 CEF 格式
     */
    public String convertToCef(AuditLogEntry entry) {
        String eventType = entry.getEventType() != null ? entry.getEventType().name() : "UNKNOWN";
        String description = entry.getEventType() != null
                ? entry.getEventType().getDescription()
                : "Unknown Event";
        int severity = mapSeverity(entry);

        StringBuilder extensions = new StringBuilder();
        appendExtension(extensions, "rt", formatTimestamp(entry));
        appendExtension(extensions, "suid", entry.getUserId());
        appendExtension(extensions, "src", entry.getClientIp());
        appendExtension(extensions, "requestClientApplication", entry.getUserAgent());
        appendExtension(extensions, "cs1", entry.getResource());
        appendExtension(extensions, "cs1Label", "Resource");
        appendExtension(extensions, "outcome", entry.getResult());
        appendExtension(extensions, "cs2", entry.getTraceId());
        appendExtension(extensions, "cs2Label", "TraceId");
        appendExtension(extensions, "cs3", entry.getEntityId());
        appendExtension(extensions, "cs3Label", "EntityId");
        appendExtension(extensions, "cs4", entry.getOperation());
        appendExtension(extensions, "cs4Label", "Operation");
        appendExtension(extensions, "cn1", entry.getDurationMs() != null ? entry.getDurationMs().toString() : null);
        appendExtension(extensions, "cn1Label", "DurationMs");
        appendExtension(extensions, "msg", entry.getErrorMessage());
        appendExtension(extensions, "externalId", entry.getId());

        return String.format("CEF:0|%s|%s|%s|%s|%s|%d|%s",
                escapePipe(vendor),
                escapePipe(product),
                escapePipe(version),
                escapePipe(eventType),
                escapePipe(description),
                severity,
                extensions.toString().trim());
    }

    /**
     * 将 AuditSeverity 映射为 CEF 严重级别 (0-10)
     */
    int mapSeverity(AuditLogEntry entry) {
        if (entry.getEventType() == null) {
            return 5;
        }
        AuditSeverity severity = entry.getEventType().getSeverity();
        switch (severity) {
            case LOW:
                return 3;
            case MEDIUM:
                return 5;
            case HIGH:
                return 7;
            case CRITICAL:
                return 10;
            default:
                return 5;
        }
    }

    private String formatTimestamp(AuditLogEntry entry) {
        if (entry.getTimestamp() == null) {
            return null;
        }
        return CEF_DATE_FORMAT.format(entry.getTimestamp());
    }

    private void appendExtension(StringBuilder sb, String key, String value) {
        if (value != null && !value.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(key).append('=').append(escapeExtensionValue(value));
        }
    }

    /**
     * CEF header 字段中的管道符和反斜杠必须转义
     */
    static String escapePipe(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("|", "\\|");
    }

    /**
     * CEF extension 值中的反斜杠、等号和换行符必须转义
     */
    static String escapeExtensionValue(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("=", "\\=")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
