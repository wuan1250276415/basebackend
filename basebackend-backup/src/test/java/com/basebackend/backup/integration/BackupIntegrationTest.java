package com.basebackend.backup.integration;

import com.basebackend.backup.config.BackupProperties;
import com.basebackend.backup.entity.BackupRecord;
import com.basebackend.backup.enums.BackupStatus;
import com.basebackend.backup.infrastructure.reliability.impl.ChecksumService;
import com.basebackend.backup.infrastructure.reliability.impl.LocalLockManager;
import com.basebackend.backup.infrastructure.reliability.impl.RetryTemplate;
import com.basebackend.backup.infrastructure.storage.StorageProvider;
import com.basebackend.backup.infrastructure.storage.UploadRequest;
import com.basebackend.backup.infrastructure.storage.impl.LocalStorageProvider;
import com.basebackend.backup.service.impl.MySQLBackupServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 备份模块集成测试
 * 使用 Testcontainers + 本地组件编排，避免依赖完整 Spring 上下文。
 */
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("BackupIntegrationTest 备份模块集成测试")
class BackupIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass")
            .withInitScript("db/integration-test-init.sql");

    @TempDir
    Path tempDir;

    private MySQLBackupServiceImpl backupService;
    private StorageProvider storageProvider;
    private BackupProperties backupProperties;

    @BeforeEach
    void setUp() {
        backupProperties = new BackupProperties();
        backupProperties.setEnabled(true);
        backupProperties.getRetry().setMaxAttempts(1);
        backupProperties.getDistributedLock().setKeyPrefix("backup:lock:");

        backupProperties.getDatabase().setHost(mysql.getHost());
        backupProperties.getDatabase().setPort(mysql.getMappedPort(3306));
        backupProperties.getDatabase().setDatabase(mysql.getDatabaseName());
        backupProperties.getDatabase().setUsername(mysql.getUsername());
        backupProperties.getDatabase().setPassword(mysql.getPassword());

        backupProperties.setBackupPath(tempDir.resolve("backup").toString());
        backupProperties.getStorage().getLocal().setEnabled(true);
        backupProperties.getStorage().getLocal().setBasePath(tempDir.resolve("storage").toString());

        RetryTemplate retryTemplate = new RetryTemplate(backupProperties);
        LocalLockManager lockManager = new LocalLockManager();
        ChecksumService checksumService = new ChecksumService(backupProperties);

        backupService = new MySQLBackupServiceImpl(
                backupProperties,
                retryTemplate,
                lockManager,
                checksumService
        );
        storageProvider = new LocalStorageProvider(backupProperties);

        createTestTables();
    }

    @Test
    @DisplayName("MySQL全量备份集成测试")
    void shouldPerformFullBackupIntegrationTest() {
        assumeTrue(isCommandAvailable(backupProperties.getMysqldumpPath()),
                "缺少mysqldump命令，跳过全量备份集成测试");

        insertTestData();

        BackupRecord record = backupService.fullBackup();

        assertThat(record).isNotNull();
        assertThat(record.getBackupType().name()).isEqualTo("FULL");
        assertThat(record.getStatus()).isEqualTo(BackupStatus.SUCCESS);
        assertThat(record.getDatabaseName()).isEqualTo(mysql.getDatabaseName());
        assertThat(record.getFilePath()).isNotNull();
        assertThat(record.getFileSize()).isGreaterThan(0L);

        File backupFile = new File(record.getFilePath());
        assertThat(backupFile).exists();
        assertThat(backupFile.length()).isEqualTo(record.getFileSize());
    }

    @Test
    @DisplayName("MySQL增量备份集成测试")
    void shouldPerformIncrementalBackupIntegrationTest() {
        insertTestData();

        BackupRecord record = backupService.incrementalBackup();

        assertThat(record).isNotNull();
        assertThat(record.getBackupType().name()).isEqualTo("INCREMENTAL");
        assertThat(record.getStatus()).isEqualTo(BackupStatus.FAILED);
        assertThat(record.getDatabaseName()).isEqualTo(mysql.getDatabaseName());
    }

    @Test
    @DisplayName("本地存储上传集成测试")
    void shouldUploadToLocalStorageIntegrationTest() throws Exception {
        String testContent = "Integration test file content " + System.currentTimeMillis();
        UploadRequest request = buildUploadRequest("integration-test", "test-file.txt", testContent);

        var result = storageProvider.upload(request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getBucket()).isEqualTo("integration-test");
        assertThat(result.getKey()).isEqualTo("test-file.txt");
        assertThat(result.getStorageType()).isEqualTo("local");
        assertThat(result.getSize()).isEqualTo(testContent.getBytes(StandardCharsets.UTF_8).length);

        assertThat(storageProvider.exists("integration-test", "test-file.txt")).isTrue();
    }

    @Test
    @DisplayName("本地存储下载集成测试")
    void shouldDownloadFromLocalStorageIntegrationTest() throws Exception {
        String testContent = "Download test content " + System.currentTimeMillis();
        UploadRequest uploadRequest = buildUploadRequest("integration-test", "download-test.txt", testContent);
        storageProvider.upload(uploadRequest);

        try (InputStream downloadStream = storageProvider.download("integration-test", "download-test.txt")) {
            assertThat(downloadStream).isNotNull();
            byte[] downloadedBytes = downloadStream.readAllBytes();
            String downloadedContent = new String(downloadedBytes, StandardCharsets.UTF_8);
            assertThat(downloadedContent).isEqualTo(testContent);
        }
    }

    @Test
    @DisplayName("存储文件删除集成测试")
    void shouldDeleteFromLocalStorageIntegrationTest() throws Exception {
        UploadRequest uploadRequest = buildUploadRequest("integration-test", "delete-test.txt", "Delete test content");
        storageProvider.upload(uploadRequest);

        assertThat(storageProvider.exists("integration-test", "delete-test.txt")).isTrue();

        boolean deleted = storageProvider.delete("integration-test", "delete-test.txt");

        assertThat(deleted).isTrue();
        assertThat(storageProvider.exists("integration-test", "delete-test.txt")).isFalse();
    }

    @Test
    @DisplayName("存储文件验证集成测试")
    void shouldVerifyLocalStorageFileIntegrationTest() throws Exception {
        String testContent = "Verify test content " + System.currentTimeMillis();
        UploadRequest uploadRequest = buildUploadRequest("integration-test", "verify-test.txt", testContent);

        var result = storageProvider.upload(uploadRequest);
        String md5 = (String) result.getMetadata().get("md5");
        String sha256 = (String) result.getMetadata().get("sha256");

        boolean verified = storageProvider.verify("integration-test", "verify-test.txt", md5, sha256);

        assertThat(verified).isTrue();
    }

    @Test
    @DisplayName("存储使用量统计集成测试")
    void shouldGetStorageUsageIntegrationTest() throws Exception {
        for (int i = 0; i < 3; i++) {
            String content = "Usage test content " + i;
            UploadRequest request = buildUploadRequest("integration-test", "usage-" + i + ".txt", content);
            storageProvider.upload(request);
        }

        var usage = storageProvider.getUsage("integration-test");

        assertThat(usage).isNotNull();
        assertThat(usage.getObjectCount()).isGreaterThanOrEqualTo(3L);
        assertThat(usage.getBucketCount()).isEqualTo(1L);
        assertThat(usage.getUsedBytes()).isGreaterThan(0L);
    }

    @Test
    @DisplayName("备份记录查询集成测试")
    void shouldListBackupsIntegrationTest() {
        backupService.fullBackup();
        backupService.incrementalBackup();

        List<BackupRecord> backups = backupService.listBackups();

        assertThat(backups).isNotNull();
        assertThat(backups).isNotEmpty();

        boolean hasFullBackup = backups.stream()
                .anyMatch(b -> b.getBackupType().name().equals("FULL"));
        boolean hasIncrementalBackup = backups.stream()
                .anyMatch(b -> b.getBackupType().name().equals("INCREMENTAL"));

        assertThat(hasFullBackup).isTrue();
        assertThat(hasIncrementalBackup).isTrue();
    }

    @Test
    @DisplayName("备份清理集成测试")
    void shouldCleanExpiredBackupsIntegrationTest() {
        int cleanedCount = backupService.cleanExpiredBackups();
        assertThat(cleanedCount).isGreaterThanOrEqualTo(0);
    }

    private UploadRequest buildUploadRequest(String bucket, String key, String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        return new UploadRequest(
                bucket,
                key,
                new ByteArrayInputStream(bytes),
                (long) bytes.length,
                "text/plain",
                null,
                null,
                false,
                null
        );
    }

    private boolean isCommandAvailable(String command) {
        if (command == null || command.isBlank()) {
            return false;
        }
        String path = System.getenv("PATH");
        if (path == null || path.isBlank()) {
            return false;
        }
        String[] dirs = path.split(File.pathSeparator);
        for (String dir : dirs) {
            Path candidate = Path.of(dir, command);
            if (Files.isExecutable(candidate)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 创建测试表
     */
    private void createTestTables() {
        try (Connection conn = DriverManager.getConnection(
                mysql.getJdbcUrl(),
                mysql.getUsername(),
                mysql.getPassword());
             Statement stmt = conn.createStatement()) {

            stmt.execute("DROP TABLE IF EXISTS test_users");
            stmt.execute("""
                        CREATE TABLE test_users (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            name VARCHAR(100) NOT NULL,
                            email VARCHAR(100) NOT NULL,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create test tables", e);
        }
    }

    /**
     * 插入测试数据
     */
    private void insertTestData() {
        try (Connection conn = DriverManager.getConnection(
                mysql.getJdbcUrl(),
                mysql.getUsername(),
                mysql.getPassword());
             Statement stmt = conn.createStatement()) {

            for (int i = 1; i <= 10; i++) {
                stmt.execute(String.format(
                        "INSERT INTO test_users (name, email) VALUES ('User%d', 'user%d@test.com')",
                        i, i));
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to insert test data", e);
        }
    }
}
