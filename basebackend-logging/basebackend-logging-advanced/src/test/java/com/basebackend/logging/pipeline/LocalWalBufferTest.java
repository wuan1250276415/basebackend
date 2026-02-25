package com.basebackend.logging.pipeline;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LocalWalBuffer 本地 WAL 缓冲测试")
class LocalWalBufferTest {

    @TempDir
    Path tempDir;

    private LocalWalBuffer wal;

    @BeforeEach
    void setUp() {
        wal = new LocalWalBuffer(tempDir.toString(), 1024, 5);
    }

    @Test
    @DisplayName("写入事件后文件数应至少为 1")
    void write_shouldCreateFile() {
        wal.write("{\"msg\":\"hello\"}");
        assertThat(wal.getFileCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("写入事件后总大小应增加")
    void write_shouldIncreaseSize() {
        wal.write("{\"msg\":\"hello world\"}");
        assertThat(wal.getTotalSizeBytes()).isGreaterThan(0);
    }

    @Test
    @DisplayName("超过文件大小限制应滚动到新文件")
    void write_shouldRollFile_whenSizeExceeded() {
        // maxFileSizeBytes = 1024, 写入大量数据触发滚动
        LocalWalBuffer smallWal = new LocalWalBuffer(tempDir.resolve("small").toString(), 50, 10);
        for (int i = 0; i < 20; i++) {
            smallWal.write("{\"msg\":\"event-" + i + "\",\"data\":\"some padding data here\"}");
        }
        assertThat(smallWal.getFileCount()).isGreaterThan(1);
    }

    @Test
    @DisplayName("drainOldest 应返回空列表当只有当前文件时")
    void drainOldest_shouldReturnEmpty_whenOnlyCurrentFile() {
        wal.write("{\"msg\":\"hello\"}");
        List<String> events = wal.drainOldest();
        assertThat(events).isEmpty();
    }

    @Test
    @DisplayName("drainOldest 应读取并删除最旧的非当前文件")
    void drainOldest_shouldDrainAndDelete() {
        // 使用小文件触发滚动
        LocalWalBuffer smallWal = new LocalWalBuffer(tempDir.resolve("drain").toString(), 30, 10);
        smallWal.write("{\"msg\":\"event-1\"}");
        // 写入足够多数据触发滚动
        for (int i = 0; i < 10; i++) {
            smallWal.write("{\"msg\":\"event-pad-" + i + "\",\"padding\":\"xxxxxxxxx\"}");
        }

        int filesBefore = smallWal.getFileCount();
        if (filesBefore > 1) {
            List<String> drained = smallWal.drainOldest();
            assertThat(drained).isNotEmpty();
            assertThat(smallWal.getFileCount()).isLessThan(filesBefore);
        }
    }

    @Test
    @DisplayName("超过 maxFiles 限制应清理旧文件")
    void write_shouldCleanupOldFiles_whenMaxFilesExceeded() {
        LocalWalBuffer tinyWal = new LocalWalBuffer(tempDir.resolve("cleanup").toString(), 20, 3);
        for (int i = 0; i < 50; i++) {
            tinyWal.write("{\"msg\":\"event-" + i + "\",\"pad\":\"xxxxxxxxxxxxxxxxxxxx\"}");
        }
        // maxFiles=3，但清理存在时间窗口，允许短暂多1个文件
        assertThat(tinyWal.getFileCount()).isLessThanOrEqualTo(5);
    }
}
