package com.basebackend.logging.pipeline;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/**
 * 本地 WAL（Write-Ahead Log）缓冲
 *
 * 在传输后端不可用时，将日志事件持久化到本地磁盘文件。
 * 传输恢复后可重放 WAL 中的事件，避免数据丢失。
 *
 * 文件格式：每行一条 JSON 序列化的 LogEvent，以换行符分隔。
 *
 * @author basebackend team
 * @since 2025-12-10
 */
@Slf4j
public class LocalWalBuffer {

    private final Path walDirectory;
    private final long maxFileSizeBytes;
    private final int maxFiles;
    private final AtomicLong currentFileSize = new AtomicLong(0);
    private volatile Path currentFile;
    private final Object writeLock = new Object();

    public LocalWalBuffer(String directory, long maxFileSizeBytes, int maxFiles) {
        this.walDirectory = Paths.get(directory);
        this.maxFileSizeBytes = maxFileSizeBytes;
        this.maxFiles = maxFiles;
        initialize();
    }

    private void initialize() {
        try {
            Files.createDirectories(walDirectory);
            rollFile();
            log.info("WAL 缓冲初始化完成，目录: {}", walDirectory);
        } catch (IOException e) {
            log.error("WAL 缓冲初始化失败", e);
        }
    }

    /**
     * 写入一条日志事件到 WAL
     *
     * @param serializedEvent JSON 序列化的日志事件
     */
    public void write(String serializedEvent) {
        synchronized (writeLock) {
            try {
                if (currentFileSize.get() >= maxFileSizeBytes) {
                    rollFile();
                }
                byte[] data = (serializedEvent + "\n").getBytes(StandardCharsets.UTF_8);
                Files.write(currentFile, data, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                currentFileSize.addAndGet(data.length);
            } catch (IOException e) {
                log.error("WAL 写入失败", e);
            }
        }
    }

    /**
     * 读取并移除最旧的 WAL 文件中的所有事件
     *
     * @return 日志事件行列表，如果无可用 WAL 文件则返回空列表
     */
    public List<String> drainOldest() {
        List<String> events = new ArrayList<>();
        try {
            Path oldest = findOldestNonCurrentFile();
            if (oldest == null) {
                return events;
            }
            events = Files.readAllLines(oldest, StandardCharsets.UTF_8);
            Files.delete(oldest);
            log.info("WAL 文件已重放并删除: {}, 事件数: {}", oldest.getFileName(), events.size());
        } catch (IOException e) {
            log.error("WAL 读取失败", e);
        }
        return events;
    }

    /**
     * 获取当前 WAL 文件数量
     */
    public int getFileCount() {
        try (Stream<Path> files = Files.list(walDirectory)) {
            return (int) files.filter(p -> p.toString().endsWith(".wal")).count();
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * 获取 WAL 目录总大小（字节）
     */
    public long getTotalSizeBytes() {
        try (Stream<Path> files = Files.list(walDirectory)) {
            return files.filter(p -> p.toString().endsWith(".wal"))
                    .mapToLong(p -> {
                        try {
                            return Files.size(p);
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .sum();
        } catch (IOException e) {
            return 0;
        }
    }

    private void rollFile() throws IOException {
        currentFile = walDirectory.resolve("wal-" + System.currentTimeMillis() + ".wal");
        currentFileSize.set(0);
        cleanupOldFiles();
    }

    private void cleanupOldFiles() {
        try (Stream<Path> files = Files.list(walDirectory)) {
            List<Path> walFiles = files
                    .filter(p -> p.toString().endsWith(".wal"))
                    .sorted(Comparator.comparingLong(p -> {
                        try {
                            return Files.getLastModifiedTime(p).toMillis();
                        } catch (IOException e) {
                            return 0;
                        }
                    }))
                    .toList();

            if (walFiles.size() > maxFiles) {
                int toDelete = walFiles.size() - maxFiles;
                for (int i = 0; i < toDelete; i++) {
                    Path oldFile = walFiles.get(i);
                    if (!oldFile.equals(currentFile)) {
                        Files.deleteIfExists(oldFile);
                        log.debug("WAL 旧文件已清理: {}", oldFile.getFileName());
                    }
                }
            }
        } catch (IOException e) {
            log.warn("WAL 文件清理失败", e);
        }
    }

    private Path findOldestNonCurrentFile() throws IOException {
        try (Stream<Path> files = Files.list(walDirectory)) {
            return files
                    .filter(p -> p.toString().endsWith(".wal"))
                    .filter(p -> !p.equals(currentFile))
                    .min(Comparator.comparingLong(p -> {
                        try {
                            return Files.getLastModifiedTime(p).toMillis();
                        } catch (IOException e) {
                            return Long.MAX_VALUE;
                        }
                    }))
                    .orElse(null);
        }
    }
}
