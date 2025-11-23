package com.basebackend.logging.audit.storage;

import com.basebackend.logging.audit.crypto.AesEncryptor;
import com.basebackend.logging.audit.model.AuditLogEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 文件审计存储实现
 *
 * 基于文件的审计日志存储，支持：
 * - 加密存储（AES-256-GCM）
 * - 压缩存储（GZIP）
 * - 文件滚动（按大小或时间）
 * - 保留策略
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@Slf4j
public class FileAuditStorage implements AuditStorage {

    private static final String FILE_SUFFIX = ".log.gz";
    private static final String FILE_PREFIX = "audit-";
    private static final String TEMP_FILE_SUFFIX = ".tmp";

    private final Path baseDir;
    private final ObjectMapper objectMapper;
    private final AesEncryptor encryptor;
    private final boolean enableCompression;
    private final long rollSizeBytes;
    private final Duration rollInterval;

    private final Map<String, Path> fileIndex = new ConcurrentHashMap<>();
    private volatile Path currentFile;
    private volatile BufferedWriter currentWriter;
    private volatile long currentFileSize;
    private volatile long lastRollTime;

    private final Object lock = new Object();

    public FileAuditStorage(Path baseDir, ObjectMapper objectMapper,
                            AesEncryptor encryptor, boolean enableCompression,
                            long rollSizeBytes, Duration rollInterval) {
        this.baseDir = baseDir;
        this.objectMapper = objectMapper;
        this.encryptor = encryptor;
        this.enableCompression = enableCompression;
        this.rollSizeBytes = rollSizeBytes;
        this.rollInterval = rollInterval;

        try {
            Files.createDirectories(baseDir);
            loadExistingFiles();
            initCurrentWriter();
        } catch (IOException e) {
            log.error("初始化文件审计存储失败", e);
            throw new RuntimeException("初始化文件审计存储失败", e);
        }
    }

    @Override
    public void save(AuditLogEntry entry) throws StorageException {
        try {
            batchSave(Collections.singletonList(entry));
        } catch (Exception e) {
            throw new StorageException("保存审计日志失败", e);
        }
    }

    @Override
    public synchronized void batchSave(List<AuditLogEntry> entries) throws StorageException {
        if (entries == null || entries.isEmpty()) {
            return;
        }

        synchronized (lock) {
            try {
                for (AuditLogEntry entry : entries) {
                    rollIfNeeded();

                    String json = objectMapper.writeValueAsString(entry);

                    // 加密（如果启用）
                    if (encryptor != null) {
                        json = encryptor.encrypt(json);
                    }

                    // 写入文件
                    currentWriter.write(json);
                    currentWriter.newLine();
                    currentFileSize += json.getBytes(StandardCharsets.UTF_8).length + 1;

                    // 强制刷盘（高性能场景下可优化）
                    if (entries.size() < 10) {
                        currentWriter.flush();
                    }
                }

                // 批量刷盘
                currentWriter.flush();
                log.debug("批量保存审计日志完成，数量: {}", entries.size());
            } catch (Exception e) {
                log.error("批量保存审计日志失败", e);
                throw new StorageException("批量保存审计日志失败", e);
            }
        }
    }

    @Override
    public AuditLogEntry findById(String id) throws StorageException {
        try {
            List<AuditLogEntry> entries = scanAllFiles(entry -> id.equals(entry.getId()));
            return entries.isEmpty() ? null : entries.get(0);
        } catch (Exception e) {
            throw new StorageException("查询审计日志失败", e);
        }
    }

    @Override
    public List<AuditLogEntry> findByTimeRange(long startTime, long endTime, int limit) throws StorageException {
        try {
            List<AuditLogEntry> result = new ArrayList<>();
            scanAllFiles(entry -> {
                long timestamp = entry.getTimestamp().toEpochMilli();
                return timestamp >= startTime && timestamp <= endTime;
            }).forEach(entry -> {
                if (result.size() < limit) {
                    result.add(entry);
                }
            });
            return result;
        } catch (Exception e) {
            throw new StorageException("按时间范围查询失败", e);
        }
    }

    @Override
    public List<AuditLogEntry> findByUserId(String userId, int limit) throws StorageException {
        try {
            List<AuditLogEntry> result = new ArrayList<>();
            scanAllFiles(entry -> userId.equals(entry.getUserId()))
                .forEach(entry -> {
                    if (result.size() < limit) {
                        result.add(entry);
                    }
                });
            return result;
        } catch (Exception e) {
            throw new StorageException("按用户查询失败", e);
        }
    }

    @Override
    public List<AuditLogEntry> findByEventType(String eventType, int limit) throws StorageException {
        try {
            List<AuditLogEntry> result = new ArrayList<>();
            scanAllFiles(entry -> {
                if (entry.getEventType() == null) {
                    return false;
                }
                return entry.getEventType().name().equals(eventType);
            }).forEach(entry -> {
                if (result.size() < limit) {
                    result.add(entry);
                }
            });
            return result;
        } catch (Exception e) {
            throw new StorageException("按事件类型查询失败", e);
        }
    }

    @Override
    public boolean verify() throws StorageException {
        try {
            return Files.exists(baseDir) && Files.isDirectory(baseDir);
        } catch (Exception e) {
            throw new StorageException("验证存储失败", e);
        }
    }

    @Override
    public int cleanup(int retentionDays) throws StorageException {
        try {
            long cutoffTime = System.currentTimeMillis() - (retentionDays * 24L * 60 * 60 * 1000);
            int deletedCount = 0;

            try (Stream<Path> files = Files.list(baseDir)) {
                List<Path> filesToDelete = files
                    .filter(path -> path.toString().endsWith(FILE_SUFFIX))
                    .filter(path -> {
                        try {
                            long lastModified = Files.getLastModifiedTime(path).toMillis();
                            return lastModified < cutoffTime;
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .toList();

                for (Path path : filesToDelete) {
                    Files.delete(path);
                    fileIndex.remove(path.getFileName().toString());
                    deletedCount++;
                    log.info("删除过期审计日志文件: {}", path.getFileName());
                }
            }

            log.info("清理过期审计日志完成，删除文件数: {}", deletedCount);
            return deletedCount;
        } catch (Exception e) {
            throw new StorageException("清理过期数据失败", e);
        }
    }

    @Override
    public StorageStats getStats() throws StorageException {
        try {
            AtomicLong totalEntries = new AtomicLong();
            long totalSizeBytes = 0;
            int fileCount = 0;
            AtomicLong oldestEntryTime = new AtomicLong(Long.MAX_VALUE);
            AtomicLong newestEntryTime = new AtomicLong();

            try (Stream<Path> files = Files.list(baseDir)) {
                List<Path> auditFiles = files
                    .filter(path -> path.toString().endsWith(FILE_SUFFIX))
                    .toList();

                fileCount = auditFiles.size();

                for (Path file : auditFiles) {
                    totalSizeBytes += Files.size(file);

                    // 扫描文件获取条目数和时间范围
                    scanFile(file, entry -> {
                        totalEntries.getAndIncrement();
                        long timestamp = entry.getTimestamp().toEpochMilli();
                        oldestEntryTime.set(Math.min(oldestEntryTime.get(), timestamp));
                        newestEntryTime.set(Math.max(newestEntryTime.get(), timestamp));
                    });
                }
            }

            if (oldestEntryTime.get() == Long.MAX_VALUE) {
                oldestEntryTime.set(0);
            }

            return new StorageStats(
                    totalEntries.get(),
                totalSizeBytes,
                fileCount,
                    oldestEntryTime.get(),
                    newestEntryTime.get(),
                180 // 默认保留期
            );
        } catch (Exception e) {
            throw new StorageException("获取存储统计失败", e);
        }
    }

    @Override
    public void close() {
        synchronized (lock) {
            try {
                if (currentWriter != null) {
                    currentWriter.close();
                    currentWriter = null;
                }
            } catch (IOException e) {
                log.error("关闭写入器失败", e);
            }
        }
    }

    /**
     * 初始化当前写入器
     */
    private void initCurrentWriter() throws IOException {
        String fileName = FILE_PREFIX + Instant.now().toEpochMilli() + FILE_SUFFIX;
        currentFile = baseDir.resolve(fileName);
        fileIndex.put(fileName, currentFile);

        OutputStream outputStream = Files.newOutputStream(currentFile,
            StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        if (enableCompression) {
            outputStream = new GZIPOutputStream(outputStream);
        }

        currentWriter = new BufferedWriter(
            new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)
        );

        lastRollTime = System.currentTimeMillis();
        currentFileSize = 0;

        log.info("初始化审计日志文件: {}", fileName);
    }

    /**
     * 检查是否需要文件滚动
     */
    private void rollIfNeeded() throws IOException {
        long now = System.currentTimeMillis();
        boolean needSizeRoll = currentFileSize >= rollSizeBytes;
        boolean needTimeRoll = (now - lastRollTime) >= rollInterval.toMillis();

        if (needSizeRoll || needTimeRoll) {
            rollFile();
        }
    }

    /**
     * 滚动文件
     */
    private void rollFile() throws IOException {
        if (currentWriter != null) {
            currentWriter.close();
            currentWriter = null;
        }

        log.info("滚动审计日志文件，大小: {} bytes", currentFileSize);
        initCurrentWriter();
    }

    /**
     * 加载现有文件
     */
    private void loadExistingFiles() throws IOException {
        if (!Files.exists(baseDir)) {
            return;
        }

        try (Stream<Path> files = Files.list(baseDir)) {
            files.filter(path -> path.toString().endsWith(FILE_SUFFIX))
                .forEach(path -> {
                    String fileName = path.getFileName().toString();
                    fileIndex.put(fileName, path);
                });
        }

        log.info("加载现有审计日志文件: {} 个", fileIndex.size());
    }

    /**
     * 扫描所有文件
     */
    private List<AuditLogEntry> scanAllFiles(java.util.function.Predicate<AuditLogEntry> filter)
            throws IOException {
        List<AuditLogEntry> result = new ArrayList<>();

        for (Path file : fileIndex.values()) {
            scanFile(file, entry -> {
                if (filter.test(entry)) {
                    result.add(entry);
                }
            });
        }

        return result;
    }

    /**
     * 扫描单个文件
     */
    private void scanFile(Path file, java.util.function.Consumer<AuditLogEntry> consumer) {
        try (BufferedReader reader = createReader(file)) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    // 解密（如果启用）
                    String json = line;
                    if (encryptor != null) {
                        json = encryptor.decrypt(json);
                    }

                    AuditLogEntry entry = objectMapper.readValue(json, AuditLogEntry.class);
                    consumer.accept(entry);
                } catch (Exception e) {
                    log.warn("解析审计日志条目失败，跳过: {}", line.substring(0, Math.min(100, line.length())), e);
                }
            }
        } catch (IOException e) {
            log.error("扫描文件失败: {}", file, e);
        }
    }

    /**
     * 创建文件读取器
     */
    private BufferedReader createReader(Path file) throws IOException {
        InputStream inputStream = Files.newInputStream(file, StandardOpenOption.READ);

        if (enableCompression) {
            inputStream = new GZIPInputStream(inputStream);
        }

        return new BufferedReader(
            new InputStreamReader(inputStream, StandardCharsets.UTF_8)
        );
    }
}
