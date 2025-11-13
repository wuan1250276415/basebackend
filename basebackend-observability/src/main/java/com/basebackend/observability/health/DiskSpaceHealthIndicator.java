package com.basebackend.observability.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 磁盘空间健康检查器
 * 检查磁盘可用空间，避免磁盘空间不足导致的系统故障
 */
@Slf4j
@Component
public class DiskSpaceHealthIndicator implements HealthIndicator {

    // 磁盘空间阈值（字节）
    private static final long THRESHOLD_BYTES = 1024L * 1024 * 1024 * 10; // 10GB

    // 磁盘空间使用率阈值
    private static final double THRESHOLD_PERCENT = 0.9; // 90%

    @Override
    public Health health() {
        try {
            File root = new File("/");

            long totalSpace = root.getTotalSpace();
            long freeSpace = root.getFreeSpace();
            long usableSpace = root.getUsableSpace();
            long usedSpace = totalSpace - freeSpace;

            double usedPercent = (double) usedSpace / totalSpace;

            Map<String, Object> details = new HashMap<>();
            details.put("total", formatBytes(totalSpace));
            details.put("free", formatBytes(freeSpace));
            details.put("usable", formatBytes(usableSpace));
            details.put("used", formatBytes(usedSpace));
            details.put("usedPercent", String.format("%.2f%%", usedPercent * 100));
            details.put("threshold", formatBytes(THRESHOLD_BYTES));
            details.put("path", root.getAbsolutePath());

            // 检查可用空间是否低于阈值
            if (usableSpace < THRESHOLD_BYTES) {
                return Health.down()
                        .withDetails(details)
                        .withDetail("message",
                                String.format("Disk space below threshold (%s available, %s required)",
                                        formatBytes(usableSpace), formatBytes(THRESHOLD_BYTES)))
                        .build();
            }

            // 检查磁盘使用率是否超过阈值
            if (usedPercent > THRESHOLD_PERCENT) {
                return Health.down()
                        .withDetails(details)
                        .withDetail("message",
                                String.format("Disk usage too high (%.2f%% used, %.2f%% threshold)",
                                        usedPercent * 100, THRESHOLD_PERCENT * 100))
                        .build();
            }

            return Health.up()
                    .withDetails(details)
                    .build();

        } catch (Exception e) {
            log.error("Disk space health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("errorType", e.getClass().getSimpleName())
                    .build();
        }
    }

    /**
     * 格式化字节数为人类可读格式
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }

        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
