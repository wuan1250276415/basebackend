package com.basebackend.logging.audit.export;

import com.basebackend.logging.audit.AuditEventType;
import com.basebackend.logging.audit.model.AuditLogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CsvExporter 单元测试
 */
class CsvExporterTest {

    private CsvExporter exporter;

    @BeforeEach
    void setUp() {
        exporter = new CsvExporter();
    }

    @Test
    void export_shouldIncludeHeaderRow() {
        String csv = exporter.export(Collections.emptyList());

        assertThat(csv).startsWith("id,timestamp,userId,sessionId,eventType,resource,result,");
        assertThat(csv).contains("clientIp,userAgent,deviceInfo,location,entityId,operation,details,durationMs,errorCode,errorMessage,traceId,spanId");
    }

    @Test
    void export_singleEntry_shouldProduceDataRow() {
        AuditLogEntry entry = AuditLogEntry.builder()
                .id("csv-001")
                .timestamp(Instant.parse("2025-12-10T12:00:00Z"))
                .userId("user-1")
                .eventType(AuditEventType.LOGIN)
                .resource("/api/login")
                .result("SUCCESS")
                .clientIp("10.0.0.1")
                .durationMs(150L)
                .build();

        String csv = exporter.export(Collections.singletonList(entry));
        String[] lines = csv.split("\r\n");

        assertThat(lines).hasSize(2); // header + 1 data row
        assertThat(lines[1]).contains("csv-001");
        assertThat(lines[1]).contains("user-1");
        assertThat(lines[1]).contains("LOGIN");
        assertThat(lines[1]).contains("SUCCESS");
        assertThat(lines[1]).contains("150");
    }

    @Test
    void quote_shouldEscapeCommas() {
        assertThat(CsvExporter.quote("hello,world")).isEqualTo("\"hello,world\"");
    }

    @Test
    void quote_shouldEscapeDoubleQuotes() {
        assertThat(CsvExporter.quote("say \"hello\"")).isEqualTo("\"say \"\"hello\"\"\"");
    }

    @Test
    void quote_shouldEscapeNewlines() {
        assertThat(CsvExporter.quote("line1\nline2")).isEqualTo("\"line1\nline2\"");
    }

    @Test
    void quote_nullValue_shouldReturnEmpty() {
        assertThat(CsvExporter.quote(null)).isEqualTo("");
    }

    @Test
    void quote_simpleValue_shouldReturnAsIs() {
        assertThat(CsvExporter.quote("simple")).isEqualTo("simple");
    }

    @Test
    void export_withDetailsMap_shouldSerializeAsJson() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("action", "create");
        details.put("count", 5);

        AuditLogEntry entry = AuditLogEntry.builder()
                .id("csv-002")
                .eventType(AuditEventType.CREATE)
                .details(details)
                .build();

        String csv = exporter.export(Collections.singletonList(entry));

        // Details should contain JSON with commas, so it should be quoted
        assertThat(csv).contains("action");
        assertThat(csv).contains("count");
    }

    @Test
    void export_nullList_shouldReturnHeaderOnly() {
        String csv = exporter.export(null);
        String[] lines = csv.split("\r\n");
        assertThat(lines).hasSize(1);
    }

    @Test
    void export_multipleEntries_shouldProduceCorrectRowCount() {
        List<AuditLogEntry> entries = Arrays.asList(
                AuditLogEntry.builder().id("1").build(),
                AuditLogEntry.builder().id("2").build(),
                AuditLogEntry.builder().id("3").build()
        );

        String csv = exporter.export(entries);
        String[] lines = csv.split("\r\n");

        assertThat(lines).hasSize(4); // header + 3 data rows
    }

    @Test
    void convertToCsvRow_allNullFields_shouldNotThrow() {
        AuditLogEntry entry = new AuditLogEntry();
        String row = exporter.convertToCsvRow(entry);
        assertThat(row).isNotNull();
        // Should have 18 commas (19 fields)
        long commaCount = row.chars().filter(c -> c == ',').count();
        assertThat(commaCount).isEqualTo(18);
    }
}
