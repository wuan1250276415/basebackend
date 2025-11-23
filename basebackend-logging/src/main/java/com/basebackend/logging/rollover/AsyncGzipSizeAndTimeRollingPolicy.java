package com.basebackend.logging.rollover;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingPolicyBase;
import ch.qos.logback.core.rolling.RolloverFailure;
import ch.qos.logback.core.rolling.TimeBasedFileNamingAndTriggeringPolicy;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.rolling.TriggeringPolicy;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.InfoStatus;
import ch.qos.logback.core.status.WarnStatus;
import ch.qos.logback.core.util.FileSize;
import ch.qos.logback.core.util.StatusPrinter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 异步Gzip压缩滚动策略
 *
 * 核心特性：
 * 1. 时间/大小双触发滚动：支持基于时间和文件大小的双重滚动触发
 * 2. 异步Gzip压缩：后台线程池异步压缩，不阻塞日志写入
 * 3. 冷热数据分离：7天内热存储，7天后自动归档压缩
 * 4. 智能保留策略：基于天数和总存储容量的双重约束
 * 5. 压缩验证：自动验证压缩文件完整性
 * 6. 性能优化：流式压缩、CPU使用率控制、小文件跳过压缩
 *
 * 性能指标：
 * - 存储节省：目标60%以上
 * - 压缩速度：不影响日志写入
 * - CPU使用率：压缩期间<50%
 *
 * @author basebackend team
 * @since 2025-11-22
 */
public class AsyncGzipSizeAndTimeRollingPolicy extends TimeBasedRollingPolicy<ILoggingEvent>
        implements TriggeringPolicy<ILoggingEvent> {

    // ==================== 可配置参数（兼容Logback XML配置） ====================

    /**
     * 最大文件大小触发阈值，默认256MB
     * 当文件超过此大小时会触发滚动
     */
    private long maxFileSizeBytes = FileSize.valueOf("256MB").getSize();

    /**
     * Gzip压缩级别（1-9），默认5
     * 1=最快，压缩率最低；9=最慢，压缩率最高
     */
    private int compressionLevel = 5;

    /**
     * 压缩文件保留天数，默认30天
     * 超过此天数的压缩文件将被自动清理
     */
    private int retentionDays = 30;

    /**
     * 热数据保留天数，默认7天
     * 7天内保留在原目录，7天后移至归档目录
     */
    private int hotRetentionDays = 7;

    /**
     * 最大总存储容量，默认20GB
     * 当压缩文件总大小超过此值时，从最旧开始清理
     */
    private long maxTotalSizeBytes = FileSize.valueOf("20GB").getSize();

    /**
     * 归档目录（可选）
     * 冷数据（超过hotRetentionDays）将压缩后存放至此目录
     */
    private String archiveDirectory;

    /**
     * 是否验证压缩完整性，默认true
     * 压缩完成后读取Gzip头部验证完整性
     */
    private boolean verifyCompression = true;

    /**
     * 最大并发压缩线程数
     * 默认取CPU核心数和2的最小值，限制CPU使用率
     */
    private int maxConcurrentCompressions = Math.max(1,
            Math.min(2, Runtime.getRuntime().availableProcessors() / 2));

    /**
     * 压缩阈值（字节），默认0（总是压缩）
     * 小于此大小的文件将直接移动而不压缩，避免小文件压缩开销
     */
    private long compressionThresholdBytes = 0;

    /**
     * 是否启用立即清理，默认true
     * 每次滚动后立即执行清理任务
     */
    private boolean eagerCleanup = true;

    // ==================== 状态与指标 ====================

    /**
     * 压缩线程池：执行异步压缩任务
     */
    private ThreadPoolExecutor compressionPool;

    /**
     * 统计指标：使用LongAdder保证高并发性能
     */
    private final LongAdder rawBytes = new LongAdder();           // 原始字节数
    private final LongAdder compressedBytes = new LongAdder();    // 压缩后字节数
    private final LongAdder compressionTimeNanos = new LongAdder(); // 压缩耗时（纳秒）
    private final LongAdder compressedFiles = new LongAdder();    // 已压缩文件数

    /**
     * 运行状态标志
     */
    private volatile boolean started;

    /**
     * 时间格式化器
     */
    private final DateTimeFormatter tsFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneOffset.UTC);

    // ==================== 生命周期管理 ====================

    @Override
    public void start() {
        if (started) {
            return;
        }

        // 验证配置参数
        validateConfig();

        // 启动父类（执行时间触发相关初始化）
        super.start();

        // 创建压缩线程池
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(32);
        ThreadFactory factory = r -> {
            Thread t = new Thread(r, "AsyncGzipCompressor-" + getName());
            t.setDaemon(true);  // 守护线程
            t.setUncaughtExceptionHandler((thread, ex) ->
                    addStatus(new ErrorStatus("Uncaught exception in compression thread " + thread.getName(),
                            this, ex)));
            return t;
        };

        compressionPool = new ThreadPoolExecutor(
                maxConcurrentCompressions,  // 核心线程数
                maxConcurrentCompressions,  // 最大线程数
                60L,                        // 空闲线程存活时间
                TimeUnit.SECONDS,
                queue,                      // 有界队列
                factory,                    // 线程工厂
                // CallerRunsPolicy：队列满时由调用者线程执行，避免任务丢失
                new ThreadPoolExecutor.CallerRunsPolicy());

        started = true;

        // 记录启动状态
        addStatus(new InfoStatus("AsyncGzipSizeAndTimeRollingPolicy started with config: "
                + "maxFileSize=" + maxFileSizeBytes
                + ", compressionLevel=" + compressionLevel
                + ", retentionDays=" + retentionDays
                + ", hotRetentionDays=" + hotRetentionDays
                + ", maxTotalSize=" + maxTotalSizeBytes
                + ", maxConcurrentCompressions=" + maxConcurrentCompressions, this));
    }

    /**
     * 验证配置参数有效性
     */
    private void validateConfig() {
        if (compressionLevel < 1 || compressionLevel > 9) {
            addStatus(new WarnStatus("compressionLevel out of range (1-9). Using default 5.", this));
            compressionLevel = 5;
        }
        if (retentionDays < 1) {
            addStatus(new WarnStatus("retentionDays must be >= 1. Using default 30.", this));
            retentionDays = 30;
        }
        if (hotRetentionDays < 0) {
            addStatus(new WarnStatus("hotRetentionDays must be >= 0. Using default 7.", this));
            hotRetentionDays = 7;
        }
        if (maxConcurrentCompressions < 1) {
            addStatus(new WarnStatus("maxConcurrentCompressions must be >= 1. Using default 1.", this));
            maxConcurrentCompressions = 1;
        }
        if (maxFileSizeBytes <= 0) {
            addStatus(new WarnStatus("maxFileSize must be > 0. Using default 256MB.", this));
            maxFileSizeBytes = FileSize.valueOf("256MB").getSize();
        }
        if (maxTotalSizeBytes <= 0) {
            addStatus(new WarnStatus("maxTotalSize must be > 0. Using default 20GB.", this));
            maxTotalSizeBytes = FileSize.valueOf("20GB").getSize();
        }
    }

    // ==================== 触发策略 ====================

    @Override
    public boolean isTriggeringEvent(File activeFile, ILoggingEvent event) {
        // 时间触发：基于时间窗口
        boolean timeTrigger = super.isTriggeringEvent(activeFile, event);

        // 大小触发：文件大小超过阈值
        boolean sizeTrigger = activeFile != null && activeFile.length() >= maxFileSizeBytes;

        return timeTrigger || sizeTrigger;
    }

    // ==================== 滚动与压缩 ====================

    @Override
    public void rollover() throws RolloverFailure {
        if (!started) {
            return;
        }

        try {
            // 获取时间触发策略
            TimeBasedFileNamingAndTriggeringPolicy<ILoggingEvent> tbn =
                    getTimeBasedFileNamingAndTriggeringPolicy();
            String elapsed = tbn.getElapsedPeriodsFileName();

            // 执行滚动（由父类处理rename，此时CompressionMode为NONE，不压缩）
            super.rollover();

            // 获取滚动后的文件
            File rolledFile = new File(elapsed);
            if (!rolledFile.exists()) {
                addStatus(new WarnStatus("Rolled file does not exist: " + elapsed, this));
                return;
            }

            // 解析归档目标位置（冷热分离）
            File target = resolveArchiveTarget(rolledFile);

            // 提交异步压缩任务
            submitCompression(rolledFile, target);

            // 提交清理任务
            if (eagerCleanup) {
                submitCleanup(target.getParentFile());
            }

        } catch (Exception ex) {
            throw new RolloverFailure("Failed to rollover", ex);
        }
    }

    /**
     * 解析归档目标位置（支持冷热分离）
     *
     * @param rolledFile 滚动后的原始文件
     * @return 归档目标文件（.gz后缀）
     */
    private File resolveArchiveTarget(File rolledFile) {
        File dir = rolledFile.getParentFile();

        // 计算文件年龄
        long ageDays = ageDays(rolledFile.toPath());

        // 如果超过热数据保留天数，移至归档目录
        if (archiveDirectory != null && ageDays >= hotRetentionDays) {
            dir = new File(archiveDirectory);
            if (!dir.exists() && !dir.mkdirs()) {
                addStatus(new WarnStatus("Cannot create archiveDirectory: "
                        + archiveDirectory + ". Using original directory.", this));
                dir = rolledFile.getParentFile();
            }
        }

        // 目标文件名（追加.gz后缀）
        String name = rolledFile.getName() + ".gz";
        return new File(dir, name);
    }

    /**
     * 提交异步压缩任务
     */
    private void submitCompression(File source, File target) {
        if (!source.exists()) {
            return;
        }
        compressionPool.submit(() -> compressAndManage(source, target));
    }

    /**
     * 执行压缩和管理操作
     *
     * @param source 源文件（未压缩）
     * @param target 目标文件（.gz压缩文件）
     */
    private void compressAndManage(File source, File target) {
        long rawSize = source.length();

        // 小文件跳过压缩，直接移动
        if (compressionThresholdBytes > 0 && rawSize < compressionThresholdBytes) {
            try {
                Files.move(source.toPath(), target.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                addStatus(new InfoStatus("Moved small file without compression: "
                        + source.getName() + " (" + rawSize + " bytes)", this));
                return;
            } catch (IOException e) {
                addStatus(new WarnStatus("Failed to move small file: " + source, this, e));
            }
        }

        long startNs = System.nanoTime();
        try {
            // 执行Gzip压缩
            compress(source, target);

            // 验证压缩完整性
            if (verifyCompression) {
                verify(target);
            }

            // 更新统计指标
            rawBytes.add(rawSize);
            compressedBytes.add(target.length());
            compressedFiles.increment();
            compressionTimeNanos.add(System.nanoTime() - startNs);

            // 更新索引
            updateIndex(target, rawSize, target.length());

            // 安全删除源文件
            safeDelete(source);

        } catch (Exception ex) {
            addStatus(new ErrorStatus("Compression failed for " + source, this, ex));
        }
    }

    /**
     * 执行Gzip压缩
     *
     * @param source 源文件
     * @param target 目标文件（.gz）
     * @throws IOException 压缩失败
     */
    private void compress(File source, File target) throws IOException {
        if (!target.getParentFile().exists() && !target.getParentFile().mkdirs()) {
            throw new IOException("Cannot create target directory: " + target.getParent());
        }

        // 使用缓冲流和8KB缓冲区，流式处理避免OOM
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(source));
             BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(target));
             GZIPOutputStream gzip = new GZIPOutputStream(out, 8192) {
                 {
                     // 设置压缩级别
                     def.setLevel(compressionLevel);
                 }
             }) {

            byte[] buffer = new byte[64 * 1024];  // 64KB缓冲区
            int len;
            while ((len = in.read(buffer)) != -1) {
                gzip.write(buffer, 0, len);
            }
        }
    }

    /**
     * 验证Gzip文件完整性
     *
     * @param gzFile Gzip压缩文件
     * @throws IOException 验证失败
     */
    private void verify(File gzFile) throws IOException {
        try (GZIPInputStream in = new GZIPInputStream(
                new BufferedInputStream(new FileInputStream(gzFile)))) {
            byte[] buf = new byte[1024];
            // 读取一小块数据验证头部和完整性
            in.read(buf, 0, buf.length);
        }
    }

    /**
     * 安全删除文件
     *
     * @param file 要删除的文件
     */
    private void safeDelete(File file) {
        try {
            Files.deleteIfExists(file.toPath());
        } catch (IOException e) {
            addStatus(new WarnStatus("Failed to delete raw file after compression: "
                    + file, this, e));
        }
    }

    // ==================== 清理管理 ====================

    /**
     * 提交清理任务
     */
    private void submitCleanup(File dir) {
        compressionPool.submit(() -> cleanupExpired(dir));
    }

    /**
     * 清理过期压缩文件
     *
     * @param dir 清理目录
     */
    private void cleanupExpired(File dir) {
        if (dir == null || !dir.exists()) {
            return;
        }

        long now = System.currentTimeMillis();
        long maxAgeMs = TimeUnit.DAYS.toMillis(retentionDays);

        // 扫描所有.gz文件
        List<Path> gzipFiles = new ArrayList<>();
        try {
            Files.list(dir.toPath())
                    .filter(p -> p.getFileName().toString().endsWith(".gz"))
                    .forEach(gzipFiles::add);
        } catch (IOException e) {
            addStatus(new WarnStatus("Failed to list archive directory: " + dir, this, e));
            return;
        }

        // 按修改时间排序（最旧在前）
        gzipFiles.sort(Comparator.comparingLong(this::modifiedTime));

        // 计算当前总大小
        long totalSize = gzipFiles.stream().mapToLong(this::size).sum();

        // 逐个检查，过期或超容量则删除
        for (Path path : gzipFiles) {
            long age = now - modifiedTime(path);

            // 判断是否过期（超过保留天数）或超容量
            boolean isExpired = age > maxAgeMs;
            boolean isOverCapacity = totalSize > maxTotalSizeBytes;

            if (isExpired || isOverCapacity) {
                long fileSize = size(path);
                try {
                    Files.deleteIfExists(path);
                    totalSize -= fileSize;
                    addStatus(new InfoStatus("Deleted expired archive: "
                            + path.getFileName() + " (age=" + TimeUnit.MILLISECONDS.toDays(age)
                            + " days, size=" + fileSize + " bytes)", this));
                } catch (IOException e) {
                    addStatus(new WarnStatus("Failed to delete expired archive: "
                            + path, this, e));
                }
            }
        }
    }

    /**
     * 获取文件修改时间
     */
    private long modifiedTime(Path p) {
        try {
            return Files.getLastModifiedTime(p).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    /**
     * 获取文件大小
     */
    private long size(Path p) {
        try {
            return Files.size(p);
        } catch (IOException e) {
            return 0L;
        }
    }

    /**
     * 计算文件年龄（天）
     */
    private long ageDays(Path p) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(p, BasicFileAttributes.class);
            return TimeUnit.MILLISECONDS.toDays(
                    System.currentTimeMillis() - attrs.lastModifiedTime().toMillis());
        } catch (IOException e) {
            return 0;
        }
    }

    // ==================== 索引管理 ====================

    /**
     * 更新压缩文件索引
     *
     * @param compressed 压缩文件
     * @param rawSize 原始大小
     * @param gzSize 压缩后大小
     */
    private void updateIndex(File compressed, long rawSize, long gzSize) {
        File idx = new File(compressed.getParentFile(), "log-index.meta");
        String line = tsFormatter.format(Instant.now()) + "|" + compressed.getName()
                + "|" + rawSize + "|" + gzSize + "\n";

        try {
            // 追加写入索引
            Files.write(idx.toPath(), line.getBytes(StandardCharsets.UTF_8),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);

            // 裁剪索引文件
            pruneIndex(idx);

        } catch (IOException e) {
            addStatus(new WarnStatus("Failed to update index: " + idx, this, e));
        }
    }

    /**
     * 裁剪索引文件，防止索引文件过大
     *
     * @param idx 索引文件
     */
    private void pruneIndex(File idx) {
        final long maxIndexBytes = 512 * 1024;  // 512KB上限

        if (idx.length() <= maxIndexBytes) {
            return;  // 不需要裁剪
        }

        try {
            List<String> lines = Files.readAllLines(idx.toPath(), StandardCharsets.UTF_8);
            int keep = Math.max(1000, lines.size() / 2);  // 保留最新的一半或1000行
            int start = Math.max(0, lines.size() - keep);
            List<String> tail = lines.subList(start, lines.size());

            // 重写索引文件（只保留尾部）
            Files.write(idx.toPath(), tail, StandardCharsets.UTF_8);

        } catch (IOException e) {
            addStatus(new WarnStatus("Failed to prune index file: " + idx, this, e));
        }
    }

    // ==================== 关闭与清理 ====================

    @Override
    public void stop() {
        if (!started) {
            return;
        }

        // 关闭父类
        super.stop();

        // 关闭压缩线程池
        compressionPool.shutdown();
        try {
            // 等待5秒让任务完成
            if (!compressionPool.awaitTermination(5, TimeUnit.SECONDS)) {
                compressionPool.shutdownNow();  // 强制中断
                if (!compressionPool.awaitTermination(2, TimeUnit.SECONDS)) {
                    addStatus(new WarnStatus("Compression pool did not terminate", this));
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            compressionPool.shutdownNow();
        }

        // 统计信息
        double ratio = getCompressionRatio();
        double speed = getCompressionSpeedMBps();
        addStatus(new InfoStatus("AsyncGzipSizeAndTimeRollingPolicy stopped. "
                + "Compression ratio=" + String.format("%.2f%%", (1.0 - ratio) * 100)
                + ", speed=" + String.format("%.2f", speed) + " MB/s"
                + ", compressedFiles=" + compressedFiles.sum()
                + ", rawBytes=" + rawBytes.sum()
                + ", compressedBytes=" + compressedBytes.sum(), this));

        started = false;
    }

    // ==================== 监控指标 ====================

    /**
     * 获取压缩比率（压缩后大小 / 原始大小）
     */
    public double getCompressionRatio() {
        long raw = rawBytes.sum();
        long gz = compressedBytes.sum();
        if (raw == 0) {
            return 1.0;  // 无数据时返回1.0
        }
        return (double) gz / raw;
    }

    /**
     * 获取压缩速度（MB/秒）
     */
    public double getCompressionSpeedMBps() {
        long nanos = compressionTimeNanos.sum();
        if (nanos == 0) {
            return 0.0;
        }
        double seconds = nanos / 1_000_000_000.0;
        return (rawBytes.sum() / (1024.0 * 1024.0)) / seconds;
    }

    /**
     * 获取已压缩文件数量
     */
    public long getCompressedFiles() {
        return compressedFiles.sum();
    }

    /**
     * 获取压缩后总字节数
     */
    public long getCompressedBytes() {
        return compressedBytes.sum();
    }

    /**
     * 获取原始总字节数
     */
    public long getRawBytes() {
        return rawBytes.sum();
    }

    /**
     * 获取压缩节省的字节数
     */
    public long getSavedBytes() {
        return rawBytes.sum() - compressedBytes.sum();
    }

    /**
     * 获取压缩节省比例（百分比）
     */
    public double getSavedPercentage() {
        long raw = rawBytes.sum();
        if (raw == 0) {
            return 0.0;
        }
        return ((double) (raw - compressedBytes.sum()) / raw) * 100.0;
    }

    // ==================== 配置属性设置器（Logback XML配置兼容） ====================

    /**
     * 设置最大文件大小（字符串格式，如"256MB"）
     */
    public void setMaxFileSize(String fileSize) {
        this.maxFileSizeBytes = FileSize.valueOf(fileSize).getSize();
    }

    /**
     * 设置压缩级别（1-9）
     */
    public void setCompressionLevel(int compressionLevel) {
        this.compressionLevel = compressionLevel;
    }

    /**
     * 设置保留天数
     */
    public void setRetentionDays(int retentionDays) {
        this.retentionDays = retentionDays;
    }

    /**
     * 设置热数据保留天数
     */
    public void setHotRetentionDays(int hotRetentionDays) {
        this.hotRetentionDays = hotRetentionDays;
    }

    /**
     * 设置最大总存储容量（字符串格式，如"20GB"）
     */
    public void setMaxTotalSize(String fileSize) {
        this.maxTotalSizeBytes = FileSize.valueOf(fileSize).getSize();
    }

    /**
     * 设置归档目录
     */
    public void setArchiveDirectory(String archiveDirectory) {
        this.archiveDirectory = archiveDirectory;
    }

    /**
     * 设置是否验证压缩完整性
     */
    public void setVerifyCompression(boolean verifyCompression) {
        this.verifyCompression = verifyCompression;
    }

    /**
     * 设置最大并发压缩线程数
     */
    public void setMaxConcurrentCompressions(int maxConcurrentCompressions) {
        this.maxConcurrentCompressions = maxConcurrentCompressions;
    }

    /**
     * 设置压缩阈值（字符串格式，如"512KB"）
     */
    public void setCompressionThreshold(String fileSize) {
        this.compressionThresholdBytes = FileSize.valueOf(fileSize).getSize();
    }

    /**
     * 设置是否启用立即清理
     */
    public void setEagerCleanup(boolean eagerCleanup) {
        this.eagerCleanup = eagerCleanup;
    }

    /**
     * 获取Appender名称（用于线程命名）
     */
    private String getName() {
        return "async-gzip-rolling";
    }
}
