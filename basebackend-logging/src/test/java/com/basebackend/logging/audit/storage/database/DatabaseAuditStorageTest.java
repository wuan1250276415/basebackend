package com.basebackend.logging.audit.storage.database;

import com.basebackend.logging.audit.AuditEventType;
import com.basebackend.logging.audit.model.AuditLogEntry;
import com.basebackend.logging.audit.storage.AuditStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DatabaseAuditStorage 单元测试
 */
@ExtendWith(MockitoExtension.class)
class DatabaseAuditStorageTest {

    @Mock
    private SysAuditLogMapper mapper;

    private DatabaseAuditStorage storage;

    @BeforeEach
    void setUp() {
        storage = new DatabaseAuditStorage(mapper);
    }

    @Test
    void save_shouldConvertAndInsert() throws AuditStorage.StorageException {
        AuditLogEntry entry = buildTestEntry();

        when(mapper.insert(any(SysAuditLog.class))).thenReturn(1);

        storage.save(entry);

        ArgumentCaptor<SysAuditLog> captor = ArgumentCaptor.forClass(SysAuditLog.class);
        verify(mapper).insert(captor.capture());

        SysAuditLog saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo("test-id");
        assertThat(saved.getUserId()).isEqualTo("user-1");
        assertThat(saved.getEventType()).isEqualTo("LOGIN");
        assertThat(saved.getResult()).isEqualTo("SUCCESS");
        assertThat(saved.getClientIp()).isEqualTo("10.0.0.1");
        assertThat(saved.getTraceId()).isEqualTo("trace-123");
    }

    @Test
    void save_mapperThrows_shouldWrapInStorageException() {
        AuditLogEntry entry = buildTestEntry();
        when(mapper.insert(any(SysAuditLog.class))).thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() -> storage.save(entry))
                .isInstanceOf(AuditStorage.StorageException.class)
                .hasMessageContaining("DB error");
    }

    @Test
    void batchSave_shouldInsertAll() throws AuditStorage.StorageException {
        List<AuditLogEntry> entries = Arrays.asList(
                buildEntry("id-1", AuditEventType.LOGIN),
                buildEntry("id-2", AuditEventType.LOGOUT),
                buildEntry("id-3", AuditEventType.CREATE)
        );

        when(mapper.insert(any(SysAuditLog.class))).thenReturn(1);

        storage.batchSave(entries);

        verify(mapper, times(3)).insert(any(SysAuditLog.class));
    }

    @Test
    void batchSave_emptyList_shouldDoNothing() throws AuditStorage.StorageException {
        storage.batchSave(Collections.emptyList());
        storage.batchSave(null);

        verifyNoInteractions(mapper);
    }

    @Test
    void findById_found_shouldConvertToModel() throws AuditStorage.StorageException {
        SysAuditLog entity = buildTestEntity();
        when(mapper.selectById("test-id")).thenReturn(entity);

        AuditLogEntry result = storage.findById("test-id");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("test-id");
        assertThat(result.getEventType()).isEqualTo(AuditEventType.LOGIN);
        assertThat(result.getUserId()).isEqualTo("user-1");
    }

    @Test
    void findById_notFound_shouldReturnNull() throws AuditStorage.StorageException {
        when(mapper.selectById("nonexistent")).thenReturn(null);

        AuditLogEntry result = storage.findById("nonexistent");

        assertThat(result).isNull();
    }

    @Test
    void findByUserId_shouldQueryAndConvert() throws AuditStorage.StorageException {
        List<SysAuditLog> entities = Arrays.asList(buildTestEntity(), buildTestEntity());
        when(mapper.selectList(any())).thenReturn(entities);

        List<AuditLogEntry> results = storage.findByUserId("user-1", 10);

        assertThat(results).hasSize(2);
        verify(mapper).selectList(any());
    }

    @Test
    void findByEventType_shouldQueryAndConvert() throws AuditStorage.StorageException {
        List<SysAuditLog> entities = Collections.singletonList(buildTestEntity());
        when(mapper.selectList(any())).thenReturn(entities);

        List<AuditLogEntry> results = storage.findByEventType("LOGIN", 5);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getEventType()).isEqualTo(AuditEventType.LOGIN);
    }

    @Test
    void cleanup_shouldDeleteExpiredRecords() throws AuditStorage.StorageException {
        when(mapper.deleteExpired(any(Instant.class))).thenReturn(42);

        int deleted = storage.cleanup(30);

        assertThat(deleted).isEqualTo(42);
        verify(mapper).deleteExpired(any(Instant.class));
    }

    @Test
    void verify_shouldReturnTrueWhenStatsSucceed() throws AuditStorage.StorageException {
        when(mapper.selectStats()).thenReturn(new HashMap<>());

        boolean result = storage.verify();

        assertThat(result).isTrue();
    }

    @Test
    void getStats_shouldReturnStorageStats() throws AuditStorage.StorageException {
        Map<String, Object> statsMap = new HashMap<>();
        statsMap.put("total_entries", 100L);
        statsMap.put("oldest_entry_time", null);
        statsMap.put("newest_entry_time", null);
        when(mapper.selectStats()).thenReturn(statsMap);

        AuditStorage.StorageStats stats = storage.getStats();

        assertThat(stats.getTotalEntries()).isEqualTo(100);
    }

    @Test
    void toModel_unknownEventType_shouldSetNull() throws AuditStorage.StorageException {
        SysAuditLog entity = SysAuditLog.builder()
                .id("unknown-type")
                .eventType("NONEXISTENT_TYPE")
                .build();
        when(mapper.selectById("unknown-type")).thenReturn(entity);

        AuditLogEntry result = storage.findById("unknown-type");

        assertThat(result).isNotNull();
        assertThat(result.getEventType()).isNull();
    }

    @Test
    void close_shouldNotThrow() {
        storage.close(); // no-op, should not throw
    }

    // ========== Helpers ==========

    private AuditLogEntry buildTestEntry() {
        return buildEntry("test-id", AuditEventType.LOGIN);
    }

    private AuditLogEntry buildEntry(String id, AuditEventType eventType) {
        return AuditLogEntry.builder()
                .id(id)
                .timestamp(Instant.parse("2025-12-10T10:00:00Z"))
                .userId("user-1")
                .sessionId("session-1")
                .eventType(eventType)
                .resource("/api/test")
                .result("SUCCESS")
                .clientIp("10.0.0.1")
                .traceId("trace-123")
                .durationMs(150L)
                .build();
    }

    private SysAuditLog buildTestEntity() {
        return SysAuditLog.builder()
                .id("test-id")
                .timestamp(Instant.parse("2025-12-10T10:00:00Z"))
                .userId("user-1")
                .sessionId("session-1")
                .eventType("LOGIN")
                .resource("/api/test")
                .result("SUCCESS")
                .clientIp("10.0.0.1")
                .traceId("trace-123")
                .durationMs(150L)
                .build();
    }
}
