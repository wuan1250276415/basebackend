package com.basebackend.database.migration.service.impl;

import com.basebackend.database.exception.MigrationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MigrationBackupServiceImpl 测试")
class MigrationBackupServiceImplTest {

    @TempDir
    Path tempDir;

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private Statement statement;

    @Mock
    private DatabaseMetaData databaseMetaData;

    private MigrationBackupServiceImpl backupService;

    @BeforeEach
    void setUp() {
        backupService = new MigrationBackupServiceImpl(dataSource);
        ReflectionTestUtils.setField(backupService, "backupDir", tempDir.toString());
    }

    @Test
    @DisplayName("恢复执行失败时应回滚事务")
    void shouldRollbackTransactionWhenRestoreFailed() throws Exception {
        mockConnectionForRestore();

        Path backupFile = tempDir.resolve("backup_restore_fail.sql");
        Files.writeString(backupFile,
                "DELETE FROM `orders`;\nINSERT INTO `orders` (`id`) VALUES (1);\n",
                StandardCharsets.UTF_8);

        doAnswer(invocation -> {
            String sql = invocation.getArgument(0, String.class);
            if (sql.startsWith("INSERT INTO `orders`")) {
                throw new SQLException("mock restore failure");
            }
            return true;
        }).when(statement).execute(anyString());

        assertThatThrownBy(() -> backupService.restoreBackup("backup_restore_fail"))
                .isInstanceOf(MigrationException.class)
                .hasMessageContaining("恢复备份失败");

        verify(connection).setAutoCommit(false);
        verify(connection).rollback();
        verify(connection, never()).commit();
        verify(statement).execute("SET FOREIGN_KEY_CHECKS=0");
        verify(statement).execute("SET FOREIGN_KEY_CHECKS=1");
    }

    @Test
    @DisplayName("SQL 字符串中的分号不应被误切分")
    void shouldKeepSemicolonInsideStringLiteral() throws Exception {
        mockConnectionForRestore();

        Path backupFile = tempDir.resolve("backup_semicolon.sql");
        Files.writeString(backupFile,
                "-- comment\n" +
                        "DELETE FROM `orders`;\n" +
                        "INSERT INTO `orders` (`note`) VALUES ('a;b;c');\n" +
                        "DELETE FROM `audit_log`;\n",
                StandardCharsets.UTF_8);
        when(statement.execute(anyString())).thenReturn(true);

        String result = backupService.restoreBackup("backup_semicolon");

        assertThat(result).contains("备份恢复成功");
        verify(connection).commit();

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(statement, atLeast(1)).execute(sqlCaptor.capture());
        List<String> executedSql = sqlCaptor.getAllValues().stream()
                .filter(sql -> !sql.startsWith("SET FOREIGN_KEY_CHECKS"))
                .toList();

        assertThat(executedSql).containsExactly(
                "DELETE FROM `orders`",
                "INSERT INTO `orders` (`note`) VALUES ('a;b;c')",
                "DELETE FROM `audit_log`"
        );
    }

    @Test
    @DisplayName("非法备份ID应被拒绝")
    void shouldRejectInvalidBackupId() {
        assertThatThrownBy(() -> backupService.restoreBackup("../hack"))
                .isInstanceOf(MigrationException.class)
                .hasMessageContaining("备份ID格式非法");

        verifyNoInteractions(dataSource);
    }

    @Test
    @DisplayName("旧版脚本恢复时自动清表并跳过 *_backup 临时表语句")
    void shouldAutoCleanLegacyInsertAndSkipLegacyBackupStatements() throws Exception {
        mockConnectionForRestore();

        Path backupFile = tempDir.resolve("backup_legacy.sql");
        Files.writeString(
                backupFile,
                "DROP TABLE IF EXISTS `orders_backup`;\n" +
                        "CREATE TABLE `orders_backup` AS SELECT * FROM `orders`;\n" +
                        "INSERT INTO `orders` (`id`) VALUES (1);\n",
                StandardCharsets.UTF_8
        );
        when(statement.execute(anyString())).thenReturn(true);

        backupService.restoreBackup("backup_legacy");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(statement, atLeast(1)).execute(sqlCaptor.capture());
        List<String> executedSql = sqlCaptor.getAllValues().stream()
                .filter(sql -> !sql.startsWith("SET FOREIGN_KEY_CHECKS"))
                .toList();

        assertThat(executedSql).contains("DELETE FROM `orders`");
        assertThat(executedSql).contains("INSERT INTO `orders` (`id`) VALUES (1)");
        assertThat(executedSql.stream().noneMatch(sql -> sql.contains("orders_backup"))).isTrue();

        Path restoreMarker = tempDir.resolve("backup_legacy.restored");
        assertThat(Files.exists(restoreMarker)).isTrue();
        assertThat(backupService.getBackup("backup_legacy").getRestored()).isTrue();
        assertThat(backupService.getBackup("backup_legacy").getRestoreTime()).isNotNull();
    }

    private void mockConnectionForRestore() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(connection.getAutoCommit()).thenReturn(true);
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        when(databaseMetaData.getDatabaseProductName()).thenReturn("MySQL");
    }
}
