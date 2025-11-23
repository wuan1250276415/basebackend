package com.basebackend.logging.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.spi.AppenderAttachable;
import ch.qos.logback.core.spi.AppenderAttachableImpl;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.InfoStatus;
import ch.qos.logback.core.status.WarnStatus;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * 高性能异步批量日志写入器
 *
 * 核心特性：
 * 1. 双触发策略：基于批量大小和时间窗口的混合触发机制
 * 2. 有界队列：防止内存溢出，队列满时自动丢弃最旧日志
 * 3. 动态批量调整：根据写入延迟和队列压力自动优化批量大小
 * 4. 指数退避重试：写入失败时使用指数退避算法进行重试
 * 5. 完整监控指标：提供队列深度、吞吐量、失败率等关键指标
 *
 * 性能提升：
 * - 吞吐量提升目标：80%（相比同步逐条写入）
 * - 内存使用可控：通过有界队列和批量处理限制内存占用
 * - 写入延迟优化：批量处理减少I/O次数
 *
 * @author basebackend team
 * @since 2025-11-22
 */
public class AsyncBatchAppender extends UnsynchronizedAppenderBase<ILoggingEvent>
        implements AppenderAttachable<ILoggingEvent> {

    /**
     * 内部委托的Appender引用器
     * 支持附加多个底层Appender（File、Console等）
     */
    private final AppenderAttachableImpl<ILoggingEvent> aai = new AppenderAttachableImpl<>();

    /**
     * 事件队列：使用有界阻塞队列防止OOM
     */
    private BlockingQueue<ILoggingEvent> queue;

    /**
     * 工作线程：专用于批量写入的守护线程
     */
    private Thread workerThread;

    /**
     * 运行状态标志
     */
    private volatile boolean running;

    // ==================== 可配置参数（兼容Logback XML配置） ====================

    /**
     * 队列容量，默认16K事件
     * 建议值：根据日志产生速率调整，通常16K-100K之间
     */
    private int queueSize = 16_384;

    /**
     * 最大批量大小，默认512事件
     * 批量越大，吞吐越高但延迟增加
     */
    private int maxBatchSize = 512;

    /**
     * 最小批量大小，默认32事件
     * 保证批量处理的最小效率
     */
    private int minBatchSize = 32;

    /**
     * 刷新间隔（毫秒），默认200ms
     * 时间窗口触发器，即使批量未满也会强制刷新
     */
    private long flushIntervalMillis = 200;

    /**
     * 最大重试次数，默认3次
     * 写入失败时的重试策略
     */
    private int maxRetries = 3;

    /**
     * 初始退避时间（毫秒），默认50ms
     * 指数退避算法的起始值
     */
    private long initialBackoffMillis = 50;

    /**
     * 最大退避时间（毫秒），默认2000ms
     * 防止退避时间过长
     */
    private long maxBackoffMillis = 2_000;

    /**
     * 同步模式开关，默认false（异步）
     * true时退化为同步直写模式，用于调试和对比测试
     */
    private boolean synchronous = false;

    /**
     * 动态批量调整开关，默认true
     * 根据系统压力自动调整批量大小
     */
    private boolean dynamicBatchSize = true;

    /**
     * 目标延迟（毫秒），默认150ms
     * 用于动态调整批量大小的基准指标
     */
    private long targetLatencyMillis = 150;

    // ==================== 状态指标 ====================

    /**
     * 当前批量大小（动态调整）
     */
    private int currentBatchSize;

    /**
     * 统计指标：使用LongAdder保证高并发场景下的性能
     */
    private final LongAdder dropped = new LongAdder();      // 丢弃事件数
    private final LongAdder delivered = new LongAdder();    // 成功交付事件数
    private final LongAdder failed = new LongAdder();       // 失败事件数
    private final LongAdder batches = new LongAdder();      // 已处理批量数
    private final LongAdder lastBatchSize = new LongAdder(); // 最后一批大小

    /**
     * 时间统计
     */
    private long startNanos;              // 启动时间（纳秒）
    private volatile long lastWriteNanos; // 最后一次写入时间（纳秒）

    // ==================== 生命周期管理 ====================

    @Override
    public void start() {
        // 防止重复启动
        if (isStarted()) {
            return;
        }

        // 验证配置参数
        if (queueSize <= 0) {
            addStatus(new ErrorStatus("Invalid queueSize: " + queueSize + ". Must be > 0", this));
            return;
        }
        if (maxBatchSize <= 0 || minBatchSize <= 0) {
            addStatus(new ErrorStatus("Invalid batch sizes. Must be > 0", this));
            return;
        }
        if (maxBatchSize < minBatchSize) {
            addStatus(new ErrorStatus("maxBatchSize (" + maxBatchSize
                    + ") must be >= minBatchSize (" + minBatchSize + ")", this));
            return;
        }
        if (flushIntervalMillis <= 0) {
            addStatus(new ErrorStatus("Invalid flushIntervalMillis: " + flushIntervalMillis
                    + ". Must be > 0", this));
            return;
        }

        // 初始化队列和状态
        queue = new ArrayBlockingQueue<>(queueSize);
        running = true;
        currentBatchSize = Math.max(minBatchSize, Math.min(maxBatchSize, maxBatchSize / 2));

        // 启动工作线程
        startWorker();

        // 记录启动时间
        startNanos = System.nanoTime();

        // 记录启动状态
        addStatus(new InfoStatus("AsyncBatchAppender started with queueSize=" + queueSize
                + ", batch=[" + minBatchSize + "," + maxBatchSize + "], flushIntervalMs="
                + flushIntervalMillis + ", synchronous=" + synchronous
                + ", dynamicBatchSize=" + dynamicBatchSize, this));

        // 调用父类启动
        super.start();
    }

    /**
     * 启动工作线程
     */
    private void startWorker() {
        workerThread = new Thread(this::runWorker, "AsyncBatchAppender-" + getName());
        workerThread.setDaemon(true);  // 守护线程，不阻止JVM退出
        workerThread.setUncaughtExceptionHandler((t, e) ->
                addStatus(new ErrorStatus("Uncaught exception in worker thread " + t.getName(), this, e)));
        workerThread.start();
    }

    // ==================== 核心写入逻辑 ====================

    @Override
    protected void append(ILoggingEvent event) {
        if (!isStarted()) {
            return;
        }

        // 同步模式：直接写入（用于调试或特殊场景）
        if (synchronous) {
            aai.appendLoopOnAppenders(event);
            delivered.increment();
            return;
        }

        // 异步模式：入队
        event.prepareForDeferredProcessing();
        boolean offered = queue.offer(event);

        if (!offered) {
            // 队列满时，丢弃最旧事件，避免阻塞
            queue.poll();
            if (!queue.offer(event)) {
                dropped.increment();
            }
        }
    }

    /**
     * 工作线程主循环
     * 负责从队列中拉取事件并批量写入
     */
    private void runWorker() {
        List<ILoggingEvent> batch = new ArrayList<>(maxBatchSize);
        long lastFlush = System.nanoTime();

        while (running || !queue.isEmpty()) {
            try {
                // 定期检查队列，使用超时避免空转
                ILoggingEvent event = queue.poll(flushIntervalMillis, TimeUnit.MILLISECONDS);

                if (event != null) {
                    batch.add(event);
                    // 批量拉取直到达到当前批量大小
                    queue.drainTo(batch, currentBatchSize - batch.size());
                }

                long now = System.nanoTime();
                boolean timeElapsed = TimeUnit.NANOSECONDS.toMillis(now - lastFlush) >= flushIntervalMillis;

                // 双触发：批量满或时间到
                if (!batch.isEmpty() && (batch.size() >= currentBatchSize || timeElapsed)) {
                    flushBatch(batch);
                    batch = new ArrayList<>(maxBatchSize);  // 避免保留引用
                    lastFlush = now;
                }
            } catch (InterruptedException ie) {
                // 收到中断信号，优雅退出
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ex) {
                addStatus(new ErrorStatus("Unexpected error in AsyncBatchAppender worker", this, ex));
            }
        }

        // 关闭前清理剩余事件
        if (!batch.isEmpty()) {
            flushBatch(batch);
        }

        // 清空队列
        List<ILoggingEvent> tail = new ArrayList<>();
        queue.drainTo(tail);
        if (!tail.isEmpty()) {
            flushBatch(tail);
        }
    }

    /**
     * 批量刷新：将一批事件写入到底层Appender
     *
     * @param batch 事件批次
     */
    private void flushBatch(List<ILoggingEvent> batch) {
        if (batch.isEmpty()) {
            return;
        }

        batches.increment();
        int size = batch.size();
        int attempt = 0;
        long backoff = initialBackoffMillis;

        // 指数退避重试
        while (attempt <= maxRetries) {
            try {
                // 逐个写入到底层Appender
                for (ILoggingEvent event : batch) {
                    aai.appendLoopOnAppenders(event);
                }

                // 写入成功，更新统计和状态
                delivered.add(size);
                lastBatchSize.reset();
                lastBatchSize.add(size);
                lastWriteNanos = System.nanoTime();

                // 动态调整批量大小
                adjustBatchSize(size);

                return;  // 成功退出

            } catch (Exception ex) {
                attempt++;
                failed.add(size);

                if (attempt > maxRetries) {
                    addStatus(new ErrorStatus("Failed to flush batch after " + attempt
                            + " attempts, batch size=" + size, this, ex));
                    return;
                }

                addStatus(new WarnStatus("Flush failed; backing off " + backoff
                        + "ms (attempt " + attempt + "/" + (maxRetries + 1)
                        + "), batch size=" + size, this, ex));
                sleepQuietly(backoff);
                backoff = Math.min(maxBackoffMillis, backoff * 2);
            }
        }
    }

    /**
     * 动态调整批量大小
     * 根据写入延迟和队列压力自动优化
     *
     * @param lastSize 最后一批的大小
     */
    private void adjustBatchSize(int lastSize) {
        if (!dynamicBatchSize) {
            return;
        }

        long now = System.nanoTime();
        long elapsedMs = lastWriteNanos == 0
                ? targetLatencyMillis
                : TimeUnit.NANOSECONDS.toMillis(now - lastWriteNanos);

        // 如果延迟超过目标，减小批量以降低延迟
        if (elapsedMs > targetLatencyMillis) {
            currentBatchSize = Math.max(minBatchSize, currentBatchSize / 2);
            return;
        }

        // 如果队列压力较大但延迟可接受，适当增大批量
        if (queue.size() > currentBatchSize && currentBatchSize < maxBatchSize) {
            currentBatchSize = Math.min(maxBatchSize,
                    currentBatchSize + Math.max(1, lastSize / 4));
        }
    }

    /**
     * 安静地睡眠，用于重试时的退避
     */
    private void sleepQuietly(long backoffMillis) {
        try {
            TimeUnit.MILLISECONDS.sleep(backoffMillis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    // ==================== 关闭与清理 ====================

    @Override
    public void stop() {
        if (!isStarted()) {
            return;
        }

        // 停止运行标志
        running = false;

        // 中断工作线程
        if (workerThread != null) {
            workerThread.interrupt();
            try {
                // 等待线程结束，给足够时间清理队列
                workerThread.join(flushIntervalMillis * 2);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                addStatus(new WarnStatus("Interrupted while waiting for worker thread to stop", this));
            }
        }

        addStatus(new InfoStatus("AsyncBatchAppender stopped. Stats: delivered="
                + delivered.sum() + ", dropped=" + dropped.sum()
                + ", failed=" + failed.sum() + ", batches=" + batches.sum(), this));

        super.stop();
    }

    // ==================== 监控指标获取 ====================

    /**
     * 获取当前队列深度
     */
    public long getQueueDepth() {
        return queue == null ? 0 : queue.size();
    }

    /**
     * 获取丢弃事件总数
     */
    public long getDropped() {
        return dropped.sum();
    }

    /**
     * 获取成功交付事件总数
     */
    public long getDelivered() {
        return delivered.sum();
    }

    /**
     * 获取失败事件总数
     */
    public long getFailed() {
        return failed.sum();
    }

    /**
     * 获取已处理批量总数
     */
    public long getBatches() {
        return batches.sum();
    }

    /**
     * 获取最后一批的大小
     */
    public long getLastBatchSize() {
        return lastBatchSize.sum();
    }

    /**
     * 获取每秒写入速率
     */
    public double getWritePerSecond() {
        long elapsed = Math.max(1, System.nanoTime() - startNanos);
        double seconds = elapsed / 1_000_000_000.0;
        return delivered.sum() / seconds;
    }

    /**
     * 获取失败率（0.0-1.0）
     */
    public double getFailureRate() {
        long total = delivered.sum() + failed.sum();
        if (total == 0) {
            return 0.0;
        }
        return (double) failed.sum() / total;
    }

    /**
     * 获取当前批量大小
     */
    public int getCurrentBatchSize() {
        return currentBatchSize;
    }

    /**
     * 获取队列使用率（0.0-1.0）
     */
    public double getQueueUtilization() {
        if (queue == null) {
            return 0.0;
        }
        return (double) queue.size() / queueSize;
    }

    // ==================== 配置属性设置器（Logback XML配置兼容） ====================

    /**
     * 设置队列大小
     */
    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    /**
     * 设置最大批量大小
     */
    public void setMaxBatchSize(int maxBatchSize) {
        this.maxBatchSize = maxBatchSize;
    }

    /**
     * 设置最小批量大小
     */
    public void setMinBatchSize(int minBatchSize) {
        this.minBatchSize = minBatchSize;
    }

    /**
     * 设置刷新间隔（毫秒）
     */
    public void setFlushIntervalMillis(long flushIntervalMillis) {
        this.flushIntervalMillis = flushIntervalMillis;
    }

    /**
     * 设置最大重试次数
     */
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    /**
     * 设置初始退避时间（毫秒）
     */
    public void setInitialBackoffMillis(long initialBackoffMillis) {
        this.initialBackoffMillis = initialBackoffMillis;
    }

    /**
     * 设置最大退避时间（毫秒）
     */
    public void setMaxBackoffMillis(long maxBackoffMillis) {
        this.maxBackoffMillis = maxBackoffMillis;
    }

    /**
     * 设置同步模式
     */
    public void setSynchronous(boolean synchronous) {
        this.synchronous = synchronous;
    }

    /**
     * 设置动态批量调整开关
     */
    public void setDynamicBatchSize(boolean dynamicBatchSize) {
        this.dynamicBatchSize = dynamicBatchSize;
    }

    /**
     * 设置目标延迟（毫秒）
     */
    public void setTargetLatencyMillis(long targetLatencyMillis) {
        this.targetLatencyMillis = targetLatencyMillis;
    }

    // ==================== AppenderAttachable接口实现 ====================

    @Override
    public void addAppender(Appender<ILoggingEvent> newAppender) {
        aai.addAppender(newAppender);
    }

    @Override
    public Iterator<Appender<ILoggingEvent>> iteratorForAppenders() {
        return aai.iteratorForAppenders();
    }

    @Override
    public Appender<ILoggingEvent> getAppender(String name) {
        return aai.getAppender(name);
    }

    @Override
    public boolean isAttached(Appender<ILoggingEvent> appender) {
        return aai.isAttached(appender);
    }

    @Override
    public boolean detachAppender(Appender<ILoggingEvent> appender) {
        return aai.detachAppender(appender);
    }

    @Override
    public boolean detachAppender(String name) {
        return aai.detachAppender(name);
    }

    @Override
    public void detachAndStopAllAppenders() {
        aai.detachAndStopAllAppenders();
    }

    /**
     * 便捷方法：支持XML中直接设置单个Appender
     */
    public void setAppender(Appender<ILoggingEvent> appender) {
        if (Objects.nonNull(appender)) {
            addAppender(appender);
        }
    }
}
