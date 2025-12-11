package com.basebackend.security.zerotrust;

import com.basebackend.security.zerotrust.policy.ZeroTrustPolicyEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 零信任健康指示器
 * <p>
 * 提供多维度的健康检查，包括：
 * - 策略引擎状态
 * - 内存使用情况
 * - 组件可用性
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ZeroTrustHealthIndicator implements HealthIndicator {

    private final ZeroTrustPolicyEngine policyEngine;

    /** 内存使用率警告阈值 */
    private static final double MEMORY_WARNING_THRESHOLD = 0.85;

    /** 启动时间 */
    private final Instant startTime = Instant.now();

    @Override
    public Health health() {
        try {
            Map<String, Object> details = new HashMap<>();
            Health.Builder builder;

            // 1. 检查策略引擎状态
            boolean policyEngineHealthy = checkPolicyEngine(details);

            // 2. 检查内存状态
            boolean memoryHealthy = checkMemoryUsage(details);

            // 3. 添加运行时信息
            addRuntimeInfo(details);

            // 4. 判断整体健康状态
            if (policyEngineHealthy && memoryHealthy) {
                builder = Health.up();
                details.put("status", "HEALTHY");
            } else if (policyEngineHealthy) {
                builder = Health.status("WARNING");
                details.put("status", "WARNING");
            } else {
                builder = Health.down();
                details.put("status", "UNHEALTHY");
            }

            details.put("component", "ZeroTrust");
            details.put("checkTime", Instant.now().toString());

            return builder.withDetails(details).build();

        } catch (Exception e) {
            log.error("零信任健康检查失败", e);
            return Health.down()
                    .withDetail("status", "ERROR")
                    .withDetail("component", "ZeroTrust")
                    .withDetail("error", maskSensitiveInfo(e.getMessage()))
                    .build();
        }
    }

    /**
     * 检查策略引擎状态
     */
    private boolean checkPolicyEngine(Map<String, Object> details) {
        try {
            boolean isAvailable = policyEngine != null;

            Map<String, Object> engineDetails = new HashMap<>();
            engineDetails.put("available", isAvailable);

            if (isAvailable) {
                engineDetails.put("enforceMode", policyEngine.isEnforceMode());
                engineDetails.put("auditEnabled", policyEngine.isAuditEnabled());
                engineDetails.put("cacheEnabled", policyEngine.isPolicyCacheEnabled());
            }

            details.put("policyEngine", engineDetails);
            return isAvailable;
        } catch (Exception e) {
            log.warn("策略引擎检查失败", e);
            details.put("policyEngine", Map.of("error", "检查失败"));
            return false;
        }
    }

    /**
     * 检查内存使用情况
     */
    private boolean checkMemoryUsage(Map<String, Object> details) {
        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();

            double usedRatio = (double) heapUsage.getUsed() / heapUsage.getMax();

            Map<String, Object> memoryDetails = new HashMap<>();
            memoryDetails.put("heapUsed", formatBytes(heapUsage.getUsed()));
            memoryDetails.put("heapMax", formatBytes(heapUsage.getMax()));
            memoryDetails.put("usedRatio", String.format("%.2f%%", usedRatio * 100));

            details.put("memory", memoryDetails);

            return usedRatio < MEMORY_WARNING_THRESHOLD;
        } catch (Exception e) {
            log.warn("内存检查失败", e);
            return true; // 内存检查失败不影响整体健康
        }
    }

    /**
     * 添加运行时信息
     */
    private void addRuntimeInfo(Map<String, Object> details) {
        Map<String, Object> runtime = new HashMap<>();
        runtime.put("startTime", startTime.toString());
        runtime.put("uptime", calculateUptime());
        details.put("runtime", runtime);
    }

    /**
     * 计算运行时间
     */
    private String calculateUptime() {
        long seconds = Instant.now().getEpochSecond() - startTime.getEpochSecond();
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        return String.format("%dh %dm", hours, minutes);
    }

    /**
     * 格式化字节数
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        if (bytes < 1024 * 1024)
            return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024)
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * 脱敏敏感信息
     */
    private String maskSensitiveInfo(String message) {
        if (message == null)
            return null;
        // 脱敏路径信息
        return message.replaceAll("(/[^\\s]+)+", "[PATH]")
                .replaceAll("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}", "[IP]");
    }
}
