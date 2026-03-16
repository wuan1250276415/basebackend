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
 * <p><b>I/O 优化</b>：使用持久的 {@link BufferedOutputStream} 替代每次写入调用
 * {@code Files.write()}（后者每次打开-写入-关闭文件，高吞吐时开销极大）。
 * 滚动时先 flush+close 旧的 writer，再打开新的 writer。
 *
 * @author basebackend team
 * @since 2025-12-10
 */
@Slf4j
public class LocalWalBuffer implements Closeable {

    private final Path walDirectory;
    private final long maxFileSizeBytes;
    private final int maxFiles;
    private final AtomicLong currentFileSize = new AtomicLong(0);
    private volatile Path currentFile;
    private final Object writeLock = new Object();

    /** 持久打开的带缓冲写入器，避免每次写入都触发 open/close syscall */
    private BufferedOutputStream currentWriter;

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
                if (currentWriter == null) {
                    return;
                }
                byte[] data = (serializedEvent + "\n").getBytes(StandardCharsets.UTF_8);
                currentWriter.write(data);
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
            // flush 当前 writer 保证数据落盘后再读取其他文件
            synchronized (writeLock) {
                if (currentWriter != null) {
                    currentWriter.flush();
                }
            }
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
     * 获取 WAL 目录总大小（字节），包含缓冲区中尚未刷盘的字节
     */
    public long getTotalSizeBytes() {
        long diskSize = 0;
        try (java.util.stream.Stream<Path> files = java.nio.file.Files.list(walDirectory)) {
            diskSize = files.filter(p -> p.toString().endsWith(".wal"))
                    .filter(p -> !p.equals(currentFile))  // 已完成的文件读磁盘大小
                    .mapToLong(p -> {
                        try {
                            return java.nio.file.Files.size(p);
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .sum();
        } catch (IOException e) {
            // ignore
        }
        // 当前文件用 AtomicLong 跟踪（包含缓冲区中未刷盘的字节）
        return diskSize + currentFileSize.get();
    }

    /** 关闭当前 writer（实现 Closeable 以支持 try-with-resources） */
    @Override
    public void close() {
        synchronized (writeLock) {
            closeCurrentWriter();
        }
    }

    private void rollFile() throws IOException {
        // 关闭旧 writer
        closeCurrentWriter();

        currentFile = walDirectory.resolve("wal-" + System.currentTimeMillis() + ".wal");
        currentFileSize.set(0);

        // 打开新的带缓冲 writer（8KB buffer，大幅减少系统调用）
        currentWriter = new BufferedOutputStream(
                Files.newOutputStream(currentFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND),
                8192);

        cleanupOldFiles();
    }

    private void closeCurrentWriter() {
        if (currentWriter != null) {
            try {
                currentWriter.flush();
                currentWriter.close();
            } catch (IOException e) {
                log.warn("关闭 WAL writer 失败", e);
            } finally {
                currentWriter = null;
            }
        }
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
