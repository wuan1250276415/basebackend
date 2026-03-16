package com.basebackend.backup.infrastructure.executor.impl;

import com.basebackend.backup.config.BackupProperties;
import com.basebackend.backup.domain.mapper.BackupHistoryMapper;
import com.basebackend.backup.infrastructure.executor.BinlogPosition;
import com.basebackend.backup.infrastructure.reliability.LockManager;
import com.basebackend.backup.infrastructure.reliability.impl.ChecksumService;
import com.basebackend.backup.infrastructure.reliability.impl.RetryTemplate;
import com.basebackend.backup.infrastructure.storage.StorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("MySqlBackupExecutor 增量区间解析测试")
class MySqlBackupExecutorTest {

    @Mock
    private LockManager lockManager;
    @Mock
    private RetryTemplate retryTemplate;
    @Mock
    private StorageProvider storageProvider;
    @Mock
    private ChecksumService checksumService;
    @Mock
    private BackupHistoryMapper backupHistoryMapper;

    private MySqlBackupExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new MySqlBackupExecutor(
                lockManager,
                retryTemplate,
                storageProvider,
                checksumService,
                backupHistoryMapper,
                null,
                new BackupProperties(),
                null
        );
    }

    @Test
    @DisplayName("同一binlog文件区间应仅返回单文件")
    void shouldResolveSingleBinlogFileRange() {
        List<String> files = List.of("mysql-bin.000010", "mysql-bin.000011");

        List<String> range = executor.resolveBinlogFilesInRange(
                files,
                BinlogPosition.of("mysql-bin.000010", 120),
                BinlogPosition.of("mysql-bin.000010", 560)
        );

        assertThat(range).containsExactly("mysql-bin.000010");
    }

    @Test
    @DisplayName("跨binlog文件区间应返回包含起止文件的连续列表")
    void shouldResolveCrossBinlogFileRange() {
        List<String> files = List.of(
                "mysql-bin.000010",
                "mysql-bin.000011",
                "mysql-bin.000012",
                "mysql-bin.000013"
        );

        List<String> range = executor.resolveBinlogFilesInRange(
                files,
                BinlogPosition.of("mysql-bin.000011", 4),
                BinlogPosition.of("mysql-bin.000013", 88)
        );

        assertThat(range).containsExactly(
                "mysql-bin.000011",
                "mysql-bin.000012",
                "mysql-bin.000013"
        );
    }

    @Test
    @DisplayName("起始binlog文件不存在时应抛异常")
    void shouldThrowWhenStartBinlogFileMissing() {
        List<String> files = List.of("mysql-bin.000020", "mysql-bin.000021");

        assertThatThrownBy(() -> executor.resolveBinlogFilesInRange(
                files,
                BinlogPosition.of("mysql-bin.000019", 100),
                BinlogPosition.of("mysql-bin.000021", 120)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("起始binlog文件不存在");
    }

    @Test
    @DisplayName("起始文件在结束文件之后时应抛异常")
    void shouldThrowWhenStartBinlogFileAfterEndBinlogFile() {
        List<String> files = List.of(
                "mysql-bin.000030",
                "mysql-bin.000031",
                "mysql-bin.000032"
        );

        assertThatThrownBy(() -> executor.resolveBinlogFilesInRange(
                files,
                BinlogPosition.of("mysql-bin.000032", 10),
                BinlogPosition.of("mysql-bin.000031", 99)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("增量区间非法");
    }
}

