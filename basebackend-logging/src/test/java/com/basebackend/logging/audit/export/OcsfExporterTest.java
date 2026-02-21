package com.basebackend.logging.audit.export;

import com.basebackend.logging.audit.AuditEventType;
import com.basebackend.logging.audit.model.AuditLogEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OcsfExporter 单元测试
 */
class OcsfExporterTest {

    private OcsfExporter exporter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        exporter = new OcsfExporter("1.1.0", "TestAudit", "TestVendor");
    }

    @Test
    void convertToOcsf_authEvent_shouldMapToClass3002() {
        AuditLogEntry entry = AuditLogEntry.builder()
                .id("ocsf-001")
                .timestamp(Instant.parse("2025-12-10T10:00:00Z"))
                .userId("user-abc")
                .sessionId("session-xyz")
                .eventType(AuditEventType.LOGIN)
                .resource("/api/auth/login")
                .result("SUCCESS")
                .clientIp("10.0.0.1")
                .build();

        Map<String, Object> ocsf = exporter.convertToOcsf(entry);

        assertThat(ocsf.get("class_uid")).isEqualTo(3002);
        assertThat(ocsf.get("category_uid")).isEqualTo(3);
        assertThat(ocsf.get("status_id")).isEqualTo(1);
        assertThat(ocsf.get("status")).isEqualTo("SUCCESS");
        assertThat(ocsf.get("time")).isEqualTo(Instant.parse("2025-12-10T10:00:00Z").toEpochMilli());

        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) ocsf.get("metadata");
        assertThat(metadata.get("version")).isEqualTo("1.1.0");
        assertThat(metadata.get("uid")).isEqualTo("ocsf-001");

        @SuppressWarnings("unchecked")
        Map<String, Object> actor = (Map<String, Object>) ocsf.get("actor");
        @SuppressWarnings("unchecked")
        Map<String, Object> user = (Map<String, Object>) actor.get("user");
        assertThat(user.get("uid")).isEqualTo("user-abc");
    }

    @Test
    void convertToOcsf_securityEvent_shouldMapToClass3003() {
        AuditLogEntry entry = AuditLogEntry.builder()
                .id("ocsf-002")
                .eventType(AuditEventType.ACCESS_DENIED)
                .result("FAILURE")
                .build();

        Map<String, Object> ocsf = exporter.convertToOcsf(entry);

        assertThat(ocsf.get("class_uid")).isEqualTo(3003);
        assertThat(ocsf.get("category_uid")).isEqualTo(3);
        assertThat(ocsf.get("severity_id")).isEqualTo(4); // CRITICAL
        assertThat(ocsf.get("status_id")).isEqualTo(2); // not SUCCESS
    }

    @Test
    void convertToOcsf_dataEvent_shouldMapToClass6003() {
        AuditLogEntry entry = AuditLogEntry.builder()
                .id("ocsf-003")
                .eventType(AuditEventType.CREATE)
                .result("SUCCESS")
                .build();

        Map<String, Object> ocsf = exporter.convertToOcsf(entry);

        assertThat(ocsf.get("class_uid")).isEqualTo(6003);
        assertThat(ocsf.get("category_uid")).isEqualTo(6);
    }

    @Test
    void convertToOcsf_nullEventType_shouldUseDefaults() {
        AuditLogEntry entry = AuditLogEntry.builder()
                .id("ocsf-004")
                .result("SUCCESS")
                .build();

        Map<String, Object> ocsf = exporter.convertToOcsf(entry);

        assertThat(ocsf.get("class_uid")).isEqualTo(6003);
        assertThat(ocsf.get("severity_id")).isEqualTo(1);
    }

    @Test
    void convertToOcsf_withErrorInfo_shouldIncludeErrorBlock() {
        AuditLogEntry entry = AuditLogEntry.builder()
                .id("ocsf-005")
                .eventType(AuditEventType.API_ERROR)
                .result("FAILURE")
                .errorCode("ERR_500")
                .errorMessage("Internal Server Error")
                .build();

        Map<String, Object> ocsf = exporter.convertToOcsf(entry);

        @SuppressWarnings("unchecked")
        Map<String, Object> error = (Map<String, Object>) ocsf.get("error");
        assertThat(error).isNotNull();
        assertThat(error.get("code")).isEqualTo("ERR_500");
        assertThat(error.get("message")).isEqualTo("Internal Server Error");
    }

    @Test
    void export_emptyList_shouldReturnEmptyArray() {
        assertThat(exporter.export(null)).isEqualTo("[]");
        assertThat(exporter.export(Collections.emptyList())).isEqualTo("[]");
    }

    @Test
    void export_multipleEntries_shouldReturnJsonArray() throws Exception {
        List<AuditLogEntry> entries = Arrays.asList(
                AuditLogEntry.builder().id("1").eventType(AuditEventType.LOGIN).result("SUCCESS").build(),
                AuditLogEntry.builder().id("2").eventType(AuditEventType.LOGOUT).result("SUCCESS").build()
        );

        String json = exporter.export(entries);

        assertThat(json).startsWith("[");
        assertThat(json).endsWith("]");
        List<?> parsed = objectMapper.readValue(json, List.class);
        assertThat(parsed).hasSize(2);
    }

    @Test
    void mapClassUid_shouldMapCategoriesCorrectly() {
        assertThat(exporter.mapClassUid(AuditEventType.LOGIN)).isEqualTo(3002);
        assertThat(exporter.mapClassUid(AuditEventType.ACCESS_DENIED)).isEqualTo(3003);
        assertThat(exporter.mapClassUid(AuditEventType.CREATE)).isEqualTo(6003);
        assertThat(exporter.mapClassUid(AuditEventType.UPLOAD)).isEqualTo(6003);
        assertThat(exporter.mapClassUid(null)).isEqualTo(6003);
    }
}
