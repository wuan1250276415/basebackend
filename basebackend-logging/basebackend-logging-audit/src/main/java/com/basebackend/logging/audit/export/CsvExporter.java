package com.basebackend.logging.audit.export;

import com.basebackend.logging.audit.model.AuditLogEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * RFC 4180 CSV 格式导出器
 *
 * 将审计日志导出为标准 CSV 格式，便于 Excel 分析和数据导入。
 *
 * @author basebackend team
 * @since 2025-12-10
 */
@Slf4j
public class CsvExporter {

    private static final DateTimeFormatter ISO_FORMAT =
            DateTimeFormatter.ISO_INSTANT;

    private static final String[] HEADERS = {
            "id", "timestamp", "userId", "sessionId", "eventType", "resource",
            "result", "clientIp", "userAgent", "deviceInfo", "location",
            "entityId", "operation", "details", "durationMs",
            "errorCode", "errorMessage", "traceId", "spanId"
    };

    private static final String HEADER_LINE = String.join(",", HEADERS);
    private static final String LINE_SEPARATOR = "\r\n";

    private final ObjectMapper objectMapper;

    public CsvExporter() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 将审计日志条目列表导出为 CSV 字符串 (含表头)
     */
    public String export(List<AuditLogEntry> entries) {
        StringBuilder sb = new StringBuilder();
        sb.append(HEADER_LINE).append(LINE_SEPARATOR);

        if (entries != null) {
            for (AuditLogEntry entry : entries) {
                sb.append(convertToCsvRow(entry)).append(LINE_SEPARATOR);
            }
        }
        return sb.toString();
    }

    /**
     * 将单条审计日志转换为 CSV 行
     */
    public String convertToCsvRow(AuditLogEntry entry) {
        StringBuilder sb = new StringBuilder();
        sb.append(quote(entry.getId()));
        sb.append(',').append(quote(formatTimestamp(entry.getTimestamp())));
        sb.append(',').append(quote(entry.getUserId()));
        sb.append(',').append(quote(entry.getSessionId()));
        sb.append(',').append(quote(entry.getEventType() != null ? entry.getEventType().name() : null));
        sb.append(',').append(quote(entry.getResource()));
        sb.append(',').append(quote(entry.getResult()));
        sb.append(',').append(quote(entry.getClientIp()));
        sb.append(',').append(quote(entry.getUserAgent()));
        sb.append(',').append(quote(entry.getDeviceInfo()));
        sb.append(',').append(quote(entry.getLocation()));
        sb.append(',').append(quote(entry.getEntityId()));
        sb.append(',').append(quote(entry.getOperation()));
        sb.append(',').append(quote(serializeDetails(entry.getDetails())));
        sb.append(',').append(quote(entry.getDurationMs() != null ? entry.getDurationMs().toString() : null));
        sb.append(',').append(quote(entry.getErrorCode()));
        sb.append(',').append(quote(entry.getErrorMessage()));
        sb.append(',').append(quote(entry.getTraceId()));
        sb.append(',').append(quote(entry.getSpanId()));
        return sb.toString();
    }

    /**
     * RFC 4180 CSV 引用规则:
     * - 字段包含逗号、双引号或换行时必须用双引号包裹
     * - 双引号本身用两个双引号转义
     */
    static String quote(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String formatTimestamp(Instant timestamp) {
        if (timestamp == null) {
            return null;
        }
        return ISO_FORMAT.format(timestamp);
    }

    private String serializeDetails(Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException e) {
            log.warn("序列化 details 字段失败", e);
            return details.toString();
        }
    }
}
