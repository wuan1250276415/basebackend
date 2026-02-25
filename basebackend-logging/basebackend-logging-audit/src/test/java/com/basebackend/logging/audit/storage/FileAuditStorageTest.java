package com.basebackend.logging.audit.storage;

import com.basebackend.logging.audit.AuditEventType;
import com.basebackend.logging.audit.model.AuditLogEntry;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class FileAuditStorageTest {

    @TempDir
    Path tempDir;

    private FileAuditStorage storage;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        // AuditLogEntry has computed getters (getSummary, isSuccess, etc.) that Jackson
        // serializes but can't deserialize — ignore unknown properties on read-back.
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        storage = createStorage();
    }

    @AfterEach
    void tearDown() {
        if (storage != null) {
            storage.close();
        }
    }

    private FileAuditStorage createStorage() {
        return new FileAuditStorage(
                tempDir,
                objectMapper,
                null,              // no encryption
                false,             // no compression
                10 * 1024 * 1024,  // 10 MB roll size
                Duration.ofHours(1)
        );
    }

    /**
     * Close the current storage and re-open to ensure all buffered data is
     * flushed to disk and the new instance loads the existing files for reading.
     */
    private void reopenStorage() {
        storage.close();
        storage = createStorage();
    }

    // --- save / batchSave ---

    @Test
    void save_singleEntry_writesToFile() throws Exception {
        AuditLogEntry entry = buildEntry("user-1", AuditEventType.LOGIN);

        storage.save(entry);

        long fileCount = Files.list(tempDir)
                .filter(p -> p.toString().endsWith(".log.gz"))
                .count();
        assertThat(fileCount).isPositive();
    }

    @Test
    void batchSave_multipleEntries_allWritten() throws Exception {
        List<AuditLogEntry> entries = List.of(
                buildEntry("user-1", AuditEventType.LOGIN),
                buildEntry("user-2", AuditEventType.LOGOUT),
                buildEntry("user-3", AuditEventType.CREATE)
        );

        storage.batchSave(entries);
        reopenStorage();

        AuditStorage.StorageStats stats = storage.getStats();
        assertThat(stats.getTotalEntries()).isEqualTo(3);
    }

    @Test
    void batchSave_nullList_doesNotThrow() {
        assertThatCode(() -> storage.batchSave(null)).doesNotThrowAnyException();
    }

    @Test
    void batchSave_emptyList_doesNotThrow() {
        assertThatCode(() -> storage.batchSave(List.of())).doesNotThrowAnyException();
    }

    // --- findById ---

    @Test
    void findById_existingEntry_returnsIt() throws Exception {
        AuditLogEntry entry = buildEntry("user-1", AuditEventType.LOGIN);
        storage.save(entry);
        reopenStorage();

        AuditLogEntry found = storage.findById(entry.getId());
        assertThat(found).isNotNull();
        assertThat(found.getUserId()).isEqualTo("user-1");
    }

    @Test
    void findById_nonExisting_returnsNull() throws Exception {
        AuditLogEntry found = storage.findById("nonexistent-id");
        assertThat(found).isNull();
    }

    // --- findByUserId ---

    @Test
    void findByUserId_returnsMatchingEntries() throws Exception {
        storage.batchSave(List.of(
                buildEntry("alice", AuditEventType.LOGIN),
                buildEntry("bob", AuditEventType.LOGIN),
                buildEntry("alice", AuditEventType.LOGOUT)
        ));
        reopenStorage();

        List<AuditLogEntry> results = storage.findByUserId("alice", 100);
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(e -> "alice".equals(e.getUserId()));
    }

    // --- findByEventType ---

    @Test
    void findByEventType_returnsMatchingEntries() throws Exception {
        storage.batchSave(List.of(
                buildEntry("user-1", AuditEventType.LOGIN),
                buildEntry("user-2", AuditEventType.LOGOUT),
                buildEntry("user-3", AuditEventType.LOGIN)
        ));
        reopenStorage();

        List<AuditLogEntry> results = storage.findByEventType("LOGIN", 100);
        assertThat(results).hasSize(2);
    }

    // --- verify ---

    @Test
    void verify_validDirectory_returnsTrue() throws Exception {
        assertThat(storage.verify()).isTrue();
    }

    // --- getStats ---

    @Test
    void getStats_emptyStorage_returnsZeroCounts() throws Exception {
        AuditStorage.StorageStats stats = storage.getStats();

        assertThat(stats.getTotalEntries()).isZero();
        assertThat(stats.getFileCount()).isPositive(); // At least the current file
    }

    @Test
    void getStats_afterWrites_reflectsData() throws Exception {
        storage.batchSave(List.of(
                buildEntry("user-1", AuditEventType.LOGIN),
                buildEntry("user-2", AuditEventType.CREATE)
        ));
        reopenStorage();

        AuditStorage.StorageStats stats = storage.getStats();
        assertThat(stats.getTotalEntries()).isEqualTo(2);
        assertThat(stats.getTotalSizeBytes()).isPositive();
    }

    @Test
    void storageStats_formatSize_returnsHumanReadable() {
        AuditStorage.StorageStats stats = new AuditStorage.StorageStats(
                100, 1536, 2, System.currentTimeMillis() - 3600_000,
                System.currentTimeMillis(), 30);

        assertThat(stats.formatSize()).contains("KB");
        assertThat(stats.getAverageEntrySize()).isEqualTo(15.36);
    }

    @Test
    void storageStats_toString_containsInfo() {
        AuditStorage.StorageStats stats = new AuditStorage.StorageStats(
                50, 2048, 1, System.currentTimeMillis(), System.currentTimeMillis(), 90);

        String str = stats.toString();
        assertThat(str).contains("存储统计").contains("50");
    }

    // --- close ---

    @Test
    void close_canBeCalledMultipleTimes() {
        assertThatCode(() -> {
            storage.close();
            storage.close();
        }).doesNotThrowAnyException();
        storage = null; // prevent tearDown from closing again
    }

    // --- cleanup ---

    @Test
    void cleanup_noExpiredFiles_returnsZero() throws Exception {
        storage.save(buildEntry("user-1", AuditEventType.LOGIN));
        reopenStorage();

        int deleted = storage.cleanup(365); // 1-year retention — nothing expired
        assertThat(deleted).isZero();
    }

    // --- Helpers ---

    private AuditLogEntry buildEntry(String userId, AuditEventType type) {
        return AuditLogEntry.builder()
                .id(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .userId(userId)
                .eventType(type)
                .resource("/api/test")
                .result("SUCCESS")
                .operation("test-op")
                .build();
    }
}
