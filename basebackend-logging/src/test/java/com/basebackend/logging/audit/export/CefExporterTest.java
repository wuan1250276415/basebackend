package com.basebackend.logging.audit.export;

import com.basebackend.logging.audit.AuditEventType;
import com.basebackend.logging.audit.model.AuditLogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CefExporter 单元测试
 */
class CefExporterTest {

    private CefExporter exporter;

    @BeforeEach
    void setUp() {
        exporter = new CefExporter("TestVendor", "TestProduct", "1.0");
    }

    @Test
    void convertToCef_shouldProduceCefFormat() {
        AuditLogEntry entry = AuditLogEntry.builder()
                .id("test-id-001")
                .timestamp(Instant.parse("2025-12-10T08:30:00Z"))
                .userId("user-123")
                .eventType(AuditEventType.LOGIN)
                .resource("/api/auth/login")
                .result("SUCCESS")
                .clientIp("192.168.1.100")
                .traceId("trace-abc")
                .build();

        String cef = exporter.convertToCef(entry);

        assertThat(cef).startsWith("CEF:0|TestVendor|TestProduct|1.0|LOGIN|");
        assertThat(cef).contains("suid=user-123");
        assertThat(cef).contains("src=192.168.1.100");
        assertThat(cef).contains("outcome=SUCCESS");
        assertThat(cef).contains("externalId=test-id-001");
        assertThat(cef).contains("cs2=trace-abc");
    }

    @Test
    void convertToCef_shouldMapSeverityCorrectly() {
        AuditLogEntry lowEntry = AuditLogEntry.builder()
                .eventType(AuditEventType.LOGOUT).build();
        AuditLogEntry medEntry = AuditLogEntry.builder()
                .eventType(AuditEventType.LOGIN).build();
        AuditLogEntry highEntry = AuditLogEntry.builder()
                .eventType(AuditEventType.DELETE).build();
        AuditLogEntry critEntry = AuditLogEntry.builder()
                .eventType(AuditEventType.ACCESS_DENIED).build();

        assertThat(exporter.mapSeverity(lowEntry)).isEqualTo(3);
        assertThat(exporter.mapSeverity(medEntry)).isEqualTo(5);
        assertThat(exporter.mapSeverity(highEntry)).isEqualTo(7);
        assertThat(exporter.mapSeverity(critEntry)).isEqualTo(10);
    }

    @Test
    void convertToCef_nullEventType_shouldUseDefaults() {
        AuditLogEntry entry = AuditLogEntry.builder()
                .id("test-id")
                .userId("user-1")
                .build();

        String cef = exporter.convertToCef(entry);

        assertThat(cef).contains("UNKNOWN");
        assertThat(cef).contains("|5|"); // default severity
    }

    @Test
    void escapePipe_shouldEscapeCorrectly() {
        assertThat(CefExporter.escapePipe("no|pipe")).isEqualTo("no\\|pipe");
        assertThat(CefExporter.escapePipe("back\\slash")).isEqualTo("back\\\\slash");
        assertThat(CefExporter.escapePipe(null)).isEqualTo("");
    }

    @Test
    void escapeExtensionValue_shouldEscapeCorrectly() {
        assertThat(CefExporter.escapeExtensionValue("key=value")).isEqualTo("key\\=value");
        assertThat(CefExporter.escapeExtensionValue("line\nbreak")).isEqualTo("line\\nbreak");
        assertThat(CefExporter.escapeExtensionValue(null)).isEqualTo("");
    }

    @Test
    void export_emptyList_shouldReturnEmpty() {
        assertThat(exporter.export(Collections.emptyList())).isEqualTo("");
        assertThat(exporter.export(null)).isEqualTo("");
    }

    @Test
    void export_multipleEntries_shouldJoinWithNewlines() {
        List<AuditLogEntry> entries = Arrays.asList(
                AuditLogEntry.builder().id("1").eventType(AuditEventType.LOGIN).build(),
                AuditLogEntry.builder().id("2").eventType(AuditEventType.LOGOUT).build()
        );

        String result = exporter.export(entries);
        String[] lines = result.split("\n");

        assertThat(lines).hasSize(2);
        assertThat(lines[0]).startsWith("CEF:0|");
        assertThat(lines[1]).startsWith("CEF:0|");
    }
}
