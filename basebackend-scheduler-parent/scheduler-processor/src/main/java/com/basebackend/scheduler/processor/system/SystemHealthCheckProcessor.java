package com.basebackend.scheduler.processor.system;

import com.basebackend.scheduler.core.TaskContext;
import com.basebackend.scheduler.core.TaskProcessor;
import com.basebackend.scheduler.core.TaskResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.RedisTemplate;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 系统健康检查处理器，检查CPU/内存/磁盘、数据库与Redis连接状态。
 *
 * <p>支持阈值配置：
 * <ul>
 *     <li>cpuThreshold：CPU使用率上限（0-1），默认0.85</li>
 *     <li>memoryThreshold：内存占用上限（0-1），默认0.85</li>
 *     <li>diskThreshold：磁盘剩余比例下限（0-1），默认0.15</li>
 * </ul>
 */
@Slf4j
@Component
public class SystemHealthCheckProcessor implements TaskProcessor {

    private final DataSource dataSource;
    private final RedisTemplate<String, Object> redisTemplate;

    public SystemHealthCheckProcessor(DataSource dataSource, RedisTemplate<String, Object> redisTemplate) {
        this.dataSource = dataSource;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String name() {
        return "system-health-check";
    }

    @Override
    public TaskResult process(TaskContext context) {
        Instant start = Instant.now();
        Map<String, Object> params = context.getParameters();
        double cpuThreshold = toDouble(params.get("cpuThreshold"), 0.85d);
        double memoryThreshold = toDouble(params.get("memoryThreshold"), 0.85d);
        double diskThreshold = toDouble(params.get("diskThreshold"), 0.15d);

        // 允许测试覆盖系统指标
        double cpuUsage = toDouble(params.get("cpuUsageOverride"), readCpuUsage());
        double memoryUsage = toDouble(params.get("memoryUsageOverride"), readMemoryUsage());
        double diskFreeRatio = toDouble(params.get("diskFreeRatioOverride"), readDiskFreeRatio());
        boolean dbHealthy = checkDatabase();
        boolean redisHealthy = checkRedis();

        boolean healthy = cpuUsage <= cpuThreshold
                && memoryUsage <= memoryThreshold
                && diskFreeRatio >= diskThreshold
                && dbHealthy
                && redisHealthy;

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("cpuUsage", cpuUsage);
        output.put("memoryUsage", memoryUsage);
        output.put("diskFreeRatio", diskFreeRatio);
        output.put("dbHealthy", dbHealthy);
        output.put("redisHealthy", redisHealthy);
        output.put("cpuThreshold", cpuThreshold);
        output.put("memoryThreshold", memoryThreshold);
        output.put("diskThreshold", diskThreshold);

        TaskResult.Status status = healthy ? TaskResult.Status.SUCCESS : TaskResult.Status.FAILED;
        String error = healthy ? null : "System health threshold violated";

        log.info("[SystemHealth] cpu={}% mem={}% diskFree={}% db={} redis={} healthy={}",
                Math.round(cpuUsage * 100), Math.round(memoryUsage * 100),
                Math.round(diskFreeRatio * 100), dbHealthy, redisHealthy, healthy);

        return TaskResult.builder(status)
                .startTime(start)
                .duration(Duration.between(start, Instant.now()))
                .errorMessage(error)
                .output(output)
                .idempotentKey(context.getIdempotentKey())
                .idempotentHit(context.getIdempotentKey() != null)
                .build();
    }

    private double readCpuUsage() {
        java.lang.management.OperatingSystemMXBean osBean =
                ManagementFactory.getOperatingSystemMXBean();
        double load = 0;

        // 尝试使用HotSpot扩展指标
        if (osBean instanceof com.sun.management.OperatingSystemMXBean os) {
            load = os.getSystemCpuLoad();
            if (load < 0) {
                double avg = os.getSystemLoadAverage();
                int cores = Math.max(1, os.getAvailableProcessors());
                load = avg > 0 ? avg / cores : 0;
            }
        } else {
            // 降级：使用标准JMX指标
            double avg = osBean.getSystemLoadAverage();
            int cores = Math.max(1, osBean.getAvailableProcessors());
            load = avg > 0 ? (avg / cores) : 0;
        }
        return Math.min(1.0d, Math.max(0d, load));
    }

    private double readMemoryUsage() {
        java.lang.management.OperatingSystemMXBean osBean =
                ManagementFactory.getOperatingSystemMXBean();

        // 尝试使用HotSpot扩展指标
        if (osBean instanceof com.sun.management.OperatingSystemMXBean os) {
            long total = os.getTotalPhysicalMemorySize();
            long free = os.getFreePhysicalMemorySize();
            if (total <= 0) {
                return 0d;
            }
            return 1d - (double) free / total;
        } else {
            // 降级：无法获取内存指标
            log.warn("[SystemHealth] Unable to read memory metrics: OperatingSystemMXBean not supported");
            return 0d;
        }
    }

    private double readDiskFreeRatio() {
        long total = 0;
        long free = 0;
        try {
            for (FileStore store : FileSystems.getDefault().getFileStores()) {
                total += store.getTotalSpace();
                free += store.getUsableSpace();
            }
        } catch (Exception ex) {
            log.warn("[SystemHealth] Failed to read disk metrics", ex);
            return 1d;
        }
        if (total == 0) {
            return 1d;
        }
        return (double) free / total;
    }

    private boolean checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2);
        } catch (Exception ex) {
            log.error("[SystemHealth] Database connection check failed", ex);
            return false;
        }
    }

    private boolean checkRedis() {
        try {
            return redisTemplate != null && redisTemplate.hasKey("health:check");
        } catch (Exception ex) {
            log.error("[SystemHealth] Redis connection check failed", ex);
            return false;
        }
    }

    private double toDouble(Object value, double defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        double parsed;
        try {
            parsed = (value instanceof Number) ? ((Number) value).doubleValue()
                    : Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
        if (Double.isNaN(parsed) || parsed < 0d || parsed > 1d) {
            return defaultValue;
        }
        return parsed;
    }
}
