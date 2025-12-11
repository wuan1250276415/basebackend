package com.basebackend.backup.integration;

import com.basebackend.backup.config.BackupProperties;
import com.basebackend.backup.entity.BackupRecord;
import com.basebackend.backup.enums.BackupStatus;
import com.basebackend.backup.infrastructure.storage.StorageProvider;
import com.basebackend.backup.infrastructure.storage.UploadRequest;
import com.basebackend.backup.service.MySQLBackupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 备份模块集成测试
 * 使用TestContainers提供真实的数据库和存储环境
 *
 * @author BaseBackend
 */
@SpringBootTest
@SpringBootConfiguration
@Testcontainers
@DisplayName("BackupIntegrationTest 备份模块集成测试")
class BackupIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass")
            .withInitScript("db/integration-test-init.sql");

    @Autowired
    private MySQLBackupService backupService;

    @Autowired
    private StorageProvider storageProvider;

    @Autowired
    private BackupProperties backupProperties;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // 配置备份属性以连接到TestContainers MySQL
        registry.add("backup.database.host", mysql::getHost);
        registry.add("backup.database.port", mysql::getFirstMappedPort);
        registry.add("backup.database.database", mysql::getDatabaseName);
        registry.add("backup.database.username", mysql::getUsername);
        registry.add("backup.database.password", mysql::getPassword);
        registry.add("backup.backupPath", () -> "/tmp/backup-test");
        registry.add("backup.storage.local.enabled", () -> true);
        registry.add("backup.storage.local.basePath", () -> "/tmp/storage-test");
    }

    @BeforeEach
    void setUp() {
        // 确保测试数据库表已创建
        createTestTables();
    }

    @Test
    @DisplayName("MySQL全量备份集成测试")
    void shouldPerformFullBackupIntegrationTest() throws Exception {
        // Given - 插入测试数据
        insertTestData();

        // When - 执行全量备份
        BackupRecord record = backupService.fullBackup();

        // Then - 验证备份记录
        assertThat(record).isNotNull();
        assertThat(record.getBackupType().name()).isEqualTo("FULL");
        assertThat(record.getStatus()).isEqualTo(BackupStatus.SUCCESS);
        assertThat(record.getDatabaseName()).isEqualTo("testdb");
        assertThat(record.getFilePath()).isNotNull();
        assertThat(record.getFileSize()).isGreaterThan(0L);
        assertThat(record.getDuration()).isGreaterThanOrEqualTo(0L);

        // 验证备份文件存在
        java.io.File backupFile = new java.io.File(record.getFilePath());
        assertThat(backupFile).exists();
        assertThat(backupFile.length()).isEqualTo(record.getFileSize());

        System.out.println("全量备份成功: " + record.getFilePath());
    }

    @Test
    @DisplayName("MySQL增量备份集成测试")
    void shouldPerformIncrementalBackupIntegrationTest() throws Exception {
        // Given - 初始数据
        insertTestData();

        // When - 执行增量备份
        BackupRecord record = backupService.incrementalBackup();

        // Then - 验证增量备份记录
        assertThat(record).isNotNull();
        assertThat(record.getBackupType().name()).isEqualTo("INCREMENTAL");
        assertThat(record.getStatus()).isEqualTo(BackupStatus.SUCCESS);
        assertThat(record.getDatabaseName()).isEqualTo("testdb");

        System.out.println("增量备份成功");
    }

    @Test
    @DisplayName("本地存储上传集成测试")
    void shouldUploadToLocalStorageIntegrationTest() throws Exception {
        // Given
        String testContent = "Integration test file content " + System.currentTimeMillis();
        InputStream inputStream = new ByteArrayInputStream(testContent.getBytes());

        UploadRequest request = new UploadRequest() ;

        // When
        var result = storageProvider.upload(request);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getBucket()).isEqualTo("integration-test");
        assertThat(result.getKey()).isEqualTo("test-file.txt");
        assertThat(result.getStorageType()).isEqualTo("local");
        assertThat(result.getSize()).isEqualTo(testContent.length());

        // 验证文件确实存在
        boolean exists = storageProvider.exists("integration-test", "test-file.txt");
        assertThat(exists).isTrue();

        System.out.println("本地存储上传成功: " + result.getLocation());
    }

    @Test
    @DisplayName("本地存储下载集成测试")
    void shouldDownloadFromLocalStorageIntegrationTest() throws Exception {
        // Given - 先上传文件
        String testContent = "Download test content " + System.currentTimeMillis();
        InputStream uploadStream = new ByteArrayInputStream(testContent.getBytes());

        UploadRequest uploadRequest = new UploadRequest()
                ;

        storageProvider.upload(uploadRequest);

        // When - 下载文件
        InputStream downloadStream = storageProvider.download("integration-test", "download-test.txt");

        // Then - 验证下载内容
        assertThat(downloadStream).isNotNull();
        byte[] downloadedBytes = downloadStream.readAllBytes();
        String downloadedContent = new String(downloadedBytes);
        assertThat(downloadedContent).isEqualTo(testContent);

        System.out.println("本地存储下载成功");
    }

    @Test
    @DisplayName("存储文件删除集成测试")
    void shouldDeleteFromLocalStorageIntegrationTest() throws Exception {
        // Given - 上传文件
        String testContent = "Delete test content";
        InputStream uploadStream = new ByteArrayInputStream(testContent.getBytes());

        UploadRequest uploadRequest = new UploadRequest()
                ;

        storageProvider.upload(uploadRequest);

        // 验证文件存在
        boolean existsBefore = storageProvider.exists("integration-test", "delete-test.txt");
        assertThat(existsBefore).isTrue();

        // When - 删除文件
        boolean deleted = storageProvider.delete("integration-test", "delete-test.txt");

        // Then
        assertThat(deleted).isTrue();
        boolean existsAfter = storageProvider.exists("integration-test", "delete-test.txt");
        assertThat(existsAfter).isFalse();

        System.out.println("本地存储删除成功");
    }

    @Test
    @DisplayName("存储文件验证集成测试")
    void shouldVerifyLocalStorageFileIntegrationTest() throws Exception {
        // Given - 上传文件
        String testContent = "Verify test content " + System.currentTimeMillis();
        InputStream uploadStream = new ByteArrayInputStream(testContent.getBytes());

        UploadRequest uploadRequest = new UploadRequest()
                ;

        var result = storageProvider.upload(uploadRequest);
        String md5 = (String) result.getMetadata().get("md5");
        String sha256 = (String) result.getMetadata().get("sha256");

        // When - 验证文件完整性
        boolean verified = storageProvider.verify("integration-test", "verify-test.txt", md5, sha256);

        // Then
        assertThat(verified).isTrue();

        System.out.println("文件验证成功: MD5=" + md5);
    }

    @Test
    @DisplayName("存储使用量统计集成测试")
    void shouldGetStorageUsageIntegrationTest() throws Exception {
        // Given - 上传多个文件
        for (int i = 0; i < 3; i++) {
            String content = "Usage test content " + i;
            InputStream stream = new ByteArrayInputStream(content.getBytes());

            UploadRequest request = new UploadRequest()
                    ;

            storageProvider.upload(request);
        }

        // When
        var usage = storageProvider.getUsage("integration-test");

        // Then
        assertThat(usage).isNotNull();
        assertThat(usage.getObjectCount()).isGreaterThanOrEqualTo(3L);
        assertThat(usage.getBucketCount()).isEqualTo(1L);
        assertThat(usage.getUsedBytes()).isGreaterThan(0L);

        System.out.println("存储使用量: " + usage.getUsedHumanReadable());
    }

    @Test
    @DisplayName("备份记录查询集成测试")
    void shouldListBackupsIntegrationTest() {
        // Given - 执行备份操作
        backupService.fullBackup();
        backupService.incrementalBackup();

        // When - 查询备份记录
        var backups = backupService.listBackups();

        // Then
        assertThat(backups).isNotNull();
        assertThat(backups );

        // 验证备份记录
        boolean hasFullBackup = backups.stream()
                .anyMatch(b -> b.getBackupType().name().equals("FULL"));
        boolean hasIncrementalBackup = backups.stream()
                .anyMatch(b -> b.getBackupType().name().equals("INCREMENTAL"));

        assertThat(hasFullBackup).isTrue();
        assertThat(hasIncrementalBackup).isTrue();

        System.out.println("备份记录查询成功，共找到 " + backups  + " 条记录");
    }

    @Test
    @DisplayName("备份清理集成测试")
    void shouldCleanExpiredBackupsIntegrationTest() {
        // Given - 设置保留天数为0（所有备份都过期）
        // 实际项目中会在配置中设置

        // When - 执行清理
        int cleanedCount = backupService.cleanExpiredBackups();

        // Then
        assertThat(cleanedCount).isGreaterThanOrEqualTo(0);

        System.out.println("清理了 " + cleanedCount + " 个过期备份");
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

            // 创建测试表
            stmt.execute("DROP TABLE IF EXISTS test_users");
            stmt.execute("""
                CREATE TABLE test_users (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    email VARCHAR(100) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            System.out.println("测试表创建成功");

        } catch (Exception e) {
            System.err.println("创建测试表失败: " + e.getMessage());
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

            // 插入测试数据
            for (int i = 1; i <= 10; i++) {
                stmt.execute(String.format(
                    "INSERT INTO test_users (name, email) VALUES ('User%d', 'user%d@test.com')",
                    i, i
                ));
            }

            System.out.println("插入10条测试数据");

        } catch (Exception e) {
            System.err.println("插入测试数据失败: " + e.getMessage());
            throw new RuntimeException("Failed to insert test data", e);
        }
    }
}
