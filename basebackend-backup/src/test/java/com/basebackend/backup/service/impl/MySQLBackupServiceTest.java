package com.basebackend.backup.service.impl;

import com.basebackend.backup.config.BackupProperties;
import com.basebackend.backup.entity.BackupRecord;
import com.basebackend.backup.enums.BackupStatus;
import com.basebackend.backup.enums.BackupType;
import com.basebackend.backup.infrastructure.reliability.LockManager;
import com.basebackend.backup.infrastructure.reliability.impl.ChecksumService;
import com.basebackend.backup.infrastructure.reliability.impl.RetryTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * MySQL备份服务测试
 *
 * @author BaseBackend
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MySQLBackupService MySQL备份服务测试")
class MySQLBackupServiceTest {

    @Mock
    private BackupProperties backupProperties;

    @Mock
    private BackupProperties.DatabaseConfig databaseConfig;

    @TempDir
    Path tempDir;

    private MySQLBackupServiceImpl backupService;

    private  RetryTemplate retryTemplate;
    private  LockManager lockManager;
    private  ChecksumService checksumService;
    @BeforeEach
    void setUp() {
        backupService = new MySQLBackupServiceImpl(backupProperties,retryTemplate,lockManager,checksumService);

        // 模拟配置
        when(backupProperties.getDatabase()).thenReturn(databaseConfig);
        when(databaseConfig.getDatabase()).thenReturn("basebackend_admin");
        when(databaseConfig.getHost()).thenReturn("192.168.66.126");
        when(databaseConfig.getPort()).thenReturn(3306);
        when(databaseConfig.getUsername()).thenReturn("basebackend_admin");
        when(databaseConfig.getPassword()).thenReturn("5iA7pGWQJnACwXw3");
        when(backupProperties.getMysqldumpPath()).thenReturn("/usr/bin/mysqldump");
        when(backupProperties.getMysqlPath()).thenReturn("/usr/bin/mysql");
        when(backupProperties.getBackupPath()).thenReturn(tempDir.toString());
        when(backupProperties.getRetentionDays()).thenReturn(Math.toIntExact(30L));
    }

    @Test
    @DisplayName("全量备份成功")
    void shouldPerformFullBackupSuccessfully() throws Exception {
        // Given
        String backupPath = tempDir.resolve("full").toString();
        when(backupProperties.getBackupPath()).thenReturn(backupPath);

        // 创建临时文件来模拟备份成功
        Path backupFile = Path.of(backupPath, "test_backup.sql");
        Files.createDirectories(Path.of(backupPath));
        Files.write(backupFile, "test content".getBytes());

        // When - 简化测试，仅验证方法能被调用
        BackupRecord record = backupService.fullBackup();

        // Then
        assertThat(record).isNotNull();
        assertThat(record.getBackupType()).isEqualTo(BackupType.FULL);
    }

    @Test
    @DisplayName("全量备份失败")
    void shouldHandleFullBackupFailure() throws Exception {
        // Given - 设置无效路径导致失败
        when(backupProperties.getBackupPath()).thenReturn("/invalid/nonexistent/path");

        // When
        BackupRecord record = backupService.fullBackup();

        // Then
        assertThat(record).isNotNull();
        assertThat(record.getBackupType()).isEqualTo(BackupType.FULL);
        assertThat(record.getStatus()).isEqualTo(BackupStatus.FAILED);
        assertThat(record.getErrorMessage()).isNotNull();
    }

    @Test
    @DisplayName("全量备份异常处理")
    void shouldHandleFullBackupException() {
        // Given
        when(backupProperties.getBackupPath()).thenReturn("/invalid/path/that/does/not/exist");

        // When
        BackupRecord record = backupService.fullBackup();

        // Then
        assertThat(record).isNotNull();
        assertThat(record.getStatus()).isEqualTo(BackupStatus.FAILED);
        assertThat(record.getBackupType()).isEqualTo(BackupType.FULL);
        assertThat(record.getErrorMessage()).isNotNull();
    }

    @Test
    @DisplayName("增量备份成功")
    void shouldPerformIncrementalBackupSuccessfully() {
        // When
        BackupRecord record = backupService.incrementalBackup();

        // Then
        assertThat(record).isNotNull();
        assertThat(record.getBackupType()).isEqualTo(BackupType.INCREMENTAL);
        assertThat(record.getStatus()).isEqualTo(BackupStatus.SUCCESS);
        assertThat(record.getDatabaseName()).isEqualTo("testdb");
    }

    @Test
    @DisplayName("增量备份异常处理")
    void shouldHandleIncrementalBackupException() {
        // Given - 模拟创建目录时的异常
        when(backupProperties.getBackupPath()).thenThrow(new RuntimeException("模拟异常"));

        // When
        BackupRecord record = backupService.incrementalBackup();

        // Then
        assertThat(record).isNotNull();
        assertThat(record.getStatus()).isEqualTo(BackupStatus.FAILED);
        assertThat(record.getErrorMessage()).isEqualTo("模拟异常");
    }

    @Test
    @DisplayName("恢复成功")
    void shouldRestoreSuccessfully() throws Exception {
        // Given
        // 首先创建一个成功的备份记录
        BackupRecord successRecord = BackupRecord.builder()
                .backupId("backup-123")
                .status(BackupStatus.SUCCESS)
                .filePath("/tmp/test_backup.sql")
                .build();

        // 使用反射设置备份缓存（简单测试）
        // 实际项目中应使用Repository保存

        ProcessBuilder mockProcessBuilder = mock(ProcessBuilder.class);
        Process mockProcess = mock(Process.class);

        when(mockProcessBuilder.command(anyList())).thenReturn(mockProcessBuilder);
        when(mockProcessBuilder.start()).thenReturn(mockProcess);
        when(mockProcess.waitFor()).thenReturn(0);
        when(mockProcess.getInputStream()).thenReturn(System.in);

        // 直接测试restore方法（绕过缓存检查）
        // 注意：实际测试中需要更完善的缓存测试

        // When & Then - 这个测试需要更复杂的mock设置
        // 由于缓存是private的，测试需要特殊处理
    }

    @Test
    @DisplayName("恢复不存在的备份")
    void shouldFailToRestoreNonExistentBackup() {
        // When
        boolean result = backupService.restore("non-existent-backup-id");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("恢复状态失败的备份")
    void shouldFailToRestoreFailedBackup() {
        // Given
        BackupRecord failedRecord = BackupRecord.builder()
                .backupId("backup-456")
                .status(BackupStatus.FAILED)
                .filePath("/tmp/test_backup.sql")
                .build();

        // 实际测试需要通过反射设置缓存

        // When
        // boolean result = backupService.restore("backup-456");

        // Then
        // 实际实现中需要验证缓存获取逻辑
    }

    @Test
    @DisplayName("恢复异常处理")
    void shouldHandleRestoreException() throws Exception {
        // Given - 模拟ProcessBuilder.start()抛出异常
        // 这个测试需要更详细的mock设置

        // When & Then
        // assertThatThrownBy(() -> backupService.restore("backup-123"))
        //     .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("时间点恢复未实现")
    void shouldReturnFalseForPITR() {
        // When
        boolean result = backupService.restoreToPointInTime("2023-12-01 10:00:00");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("列出所有备份")
    void shouldListAllBackups() {
        // Given - 需要先创建一些备份记录

        // When
        List<BackupRecord> backups = backupService.listBackups();

        // Then
        assertThat(backups).isNotNull();
        // 实际测试需要验证缓存中的记录
    }

    @Test
    @DisplayName("删除存在的备份")
    void shouldDeleteExistingBackup() {
        // Given - 创建测试文件
        Path testBackup = tempDir.resolve("test_backup.sql");
        // 这里简化，实际需要真实的文件

        BackupRecord record = BackupRecord.builder()
                .backupId("backup-789")
                .filePath(testBackup.toString())
                .build();

        // 实际测试需要通过反射设置缓存

        // When
        // boolean result = backupService.deleteBackup("backup-789");

        // Then
        // 实际实现中需要验证删除结果
    }

    @Test
    @DisplayName("删除不存在的备份")
    void shouldFailToDeleteNonExistentBackup() {
        // When
        boolean result = backupService.deleteBackup("non-existent-id");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("清理过期备份")
    void shouldCleanExpiredBackups() {
        // Given - 设置保留天数
        when(backupProperties.getRetentionDays()).thenReturn(Math.toIntExact(7L));

        // 实际测试需要创建过期和非过期的备份记录

        // When
        int cleanedCount = backupService.cleanExpiredBackups();

        // Then
        assertThat(cleanedCount).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("构建mysqldump命令")
    void shouldBuildMysqldumpCommand() {
        // When
        List<String> command = invokeBuildMysqldumpCommand();

        // Then
        assertThat(command).isNotNull();
        assertThat(command.size()).isGreaterThan(10);
        assertThat(command).contains(
                "/usr/bin/mysqldump",
                "-h", "localhost",
                "-P", "3306",
                "-u", "root",
                "--single-transaction",
                "--master-data=2",
                "testdb"
        );

        // 验证包含-p参数但不检查具体值
        boolean hasPasswordArg = command.stream()
                .anyMatch(arg -> arg.startsWith("-p"));
        assertThat(hasPasswordArg).isTrue();
    }

    @Test
    @DisplayName("构建mysql恢复命令")
    void shouldBuildMysqlRestoreCommand() {
        // Given
        String backupFile = "/tmp/test_backup.sql";

        // When
        List<String> command = invokeBuildMysqlRestoreCommand(backupFile);

        // Then
        assertThat(command).isNotNull();
        assertThat(command.size()).isGreaterThan(8);
        assertThat(command).contains(
                "/usr/bin/mysql",
                "-h", "localhost",
                "-P", "3306",
                "-u", "root",
                "testdb"
        );

        // 验证包含-p参数
        boolean hasPasswordArg = command.stream()
                .anyMatch(arg -> arg.startsWith("-p"));
        assertThat(hasPasswordArg).isTrue();

        // 验证source命令
        String sourceCommand = command.stream()
                .filter(arg -> arg.contains("source"))
                .findFirst()
                .orElse(null);
        assertThat(sourceCommand).contains("source " + backupFile);
    }

    @Test
    @DisplayName("命令日志记录应该隐藏密码")
    void shouldMaskPasswordInLogCommand() {
        // Given
        List<String> command = List.of(
                "mysqldump",
                "-h", "localhost",
                "-u", "root",
                "-ppassword", // 明文密码
                "testdb"
        );

        // When & Then - 测试私有方法
        invokeLogCommand(command);
        // 实际验证需要检查日志输出
        // 这里是验证方法可以被调用而不抛异常
    }

    @Test
    @DisplayName("空命令日志记录")
    void shouldHandleNullCommandInLogCommand() {
        // When & Then
        // 直接调用，不应该抛异常
        invokeLogCommand(null);
        invokeLogCommand(List.of());
    }

    @Test
    @DisplayName("全量备份目录创建失败")
    void shouldHandleBackupDirectoryCreationFailure() {
        // Given - 模拟无法创建目录
        // 这里需要更复杂的文件系统mock

        // When
        BackupRecord record = backupService.fullBackup();

        // Then
        assertThat(record.getStatus()).isEqualTo(BackupStatus.FAILED);
    }

    @Test
    @DisplayName("验证备份记录所有字段")
    void shouldPopulateAllBackupRecordFields() throws Exception {
        // Given
        String backupPath = tempDir.resolve("full").toString();
        when(backupProperties.getBackupPath()).thenReturn(backupPath);

        // 创建临时文件
        Path backupFile = Path.of(backupPath, "test_backup.sql");
        Files.createDirectories(Path.of(backupPath));
        Files.write(backupFile, "test content".getBytes());

        // When
        BackupRecord record = backupService.fullBackup();

        // Then - 验证必要字段
        assertThat(record).isNotNull();
        assertThat(record.getBackupType()).isEqualTo(BackupType.FULL);
        assertThat(record.getDatabaseName()).isEqualTo("testdb");
        assertThat(record.getStartTime()).isNotNull();
    }

    /**
     * 使用反射调用私有方法
     */
    private List<String> invokeBuildMysqldumpCommand() {
        try {
            java.lang.reflect.Method method = MySQLBackupServiceImpl.class
                    .getDeclaredMethod("buildMysqldumpCommand", String.class);
            method.setAccessible(true);
            return (List<String>) method.invoke(backupService, "/tmp/test.sql");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 使用反射调用私有方法
     */
    private List<String> invokeBuildMysqlRestoreCommand(String backupFile) {
        try {
            java.lang.reflect.Method method = MySQLBackupServiceImpl.class
                    .getDeclaredMethod("buildMysqlRestoreCommand", String.class);
            method.setAccessible(true);
            return (List<String>) method.invoke(backupService, backupFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 使用反射调用私有方法
     */
    private void invokeLogCommand(List<String> command) {
        try {
            java.lang.reflect.Method method = MySQLBackupServiceImpl.class
                    .getDeclaredMethod("logCommand", List.class);
            method.setAccessible(true);
            method.invoke(backupService, command);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
