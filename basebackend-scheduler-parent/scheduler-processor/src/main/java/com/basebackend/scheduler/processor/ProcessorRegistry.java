package com.basebackend.scheduler.processor;

import com.basebackend.scheduler.core.TaskProcessor;
import com.basebackend.scheduler.core.TaskContext;
import com.basebackend.scheduler.core.TaskResult;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Data;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 线程安全的处理器注册表，统一管理 TaskProcessor 生命周期。
 * 支持动态注册/注销、版本管理与优雅关闭。
 *
 * <p>主要特性：
 * <ul>
 *   <li>参数验证 - 严格的输入参数校验</li>
 *   <li>重复注册检查 - 防止意外覆盖已注册的处理器</li>
 *   <li>版本管理 - 支持处理器版本控制和热升级</li>
 *   <li>统计信息 - 注册数量、版本分布等统计</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(prefix = "scheduler.processor", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ProcessorRegistry {

    private static final Logger log = LoggerFactory.getLogger(ProcessorRegistry.class);

    // 处理器注册表缓存 - 限制最大200个处理器，通常足够使用
    private final Cache<String, ProcessorInfo> processors;
    private final AtomicLong totalRegistered = new AtomicLong(0);
    private final AtomicLong totalUnregistered = new AtomicLong(0);

    public ProcessorRegistry() {
        this.processors = Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterAccess(1, TimeUnit.HOURS)
                .recordStats()
                .build();
    }

    /**
     * 注册处理器（默认版本）
     *
     * @param name     处理器名称
     * @param processor 处理器实例
     * @throws IllegalArgumentException 参数无效时抛出
     * @throws IllegalStateException   重复注册且不允许覆盖时抛出
     */
    public void register(String name, TaskProcessor processor) {
        register(name, processor, null, false);
    }

    /**
     * 注册处理器（指定版本）
     *
     * @param name     处理器名称
     * @param processor 处理器实例
     * @param version  版本号，null表示默认版本
     * @param allowOverwrite 是否允许覆盖已存在的处理器
     * @throws IllegalArgumentException 参数无效时抛出
     * @throws IllegalStateException   重复注册且不允许覆盖时抛出
     */
    public void register(String name, TaskProcessor processor, String version, boolean allowOverwrite) {
        // 参数验证
        validateProcessorRegistration(name, processor, version);

        String key = buildProcessorKey(name, version);
        ProcessorInfo existing = processors.asMap().get(key);

        // 检查重复注册
        if (existing != null && !allowOverwrite) {
            throw new IllegalStateException(
                String.format("Processor [%s] with version [%s] already registered. " +
                            "Current: %s, New: %s. Use allowOverwrite=true to replace.",
                    name, version != null ? version : "default",
                    existing.getProcessor().getClass().getSimpleName(),
                    processor.getClass().getSimpleName())
            );
        }

        // 注册或更新处理器
        ProcessorInfo info = new ProcessorInfo(
            processor,
            version != null ? version : "default",
            totalRegistered.incrementAndGet()
        );

        processors.put(key, info);

        log.info("Registered processor [{}] version [{}] -> {} (overwrite: {})",
                name, info.getVersion(), processor.getClass().getSimpleName(), allowOverwrite);
    }

    /**
     * 查询处理器（默认版本）
     */
    public Optional<TaskProcessor> find(String name) {
        return find(name, null);
    }

    /**
     * 查询处理器（指定版本）
     */
    public Optional<TaskProcessor> find(String name, String version) {
        if (!StringUtils.hasText(name)) {
            return Optional.empty();
        }

        String key = buildProcessorKey(name, version);
        ProcessorInfo info = processors.asMap().get(key);
        return info != null ? Optional.of(info.getProcessor()) : Optional.empty();
    }

    /**
     * 查询处理器信息（包含版本等元数据）
     */
    public Optional<ProcessorInfo> findInfo(String name, String version) {
        if (!StringUtils.hasText(name)) {
            return Optional.empty();
        }

        String key = buildProcessorKey(name, version);
        return Optional.ofNullable(processors.asMap().get(key));
    }

    /**
     * 查询所有版本的处理器
     */
    public Map<String, ProcessorInfo> findAllVersions(String name) {
        if (!StringUtils.hasText(name)) {
            return Collections.emptyMap();
        }

        Map<String, ProcessorInfo> result = new ConcurrentHashMap<>();
        String prefix = name.trim().toLowerCase() + ":";

        processors.asMap().forEach((key, info) -> {
            if (key.startsWith(prefix)) {
                result.put(key, info);
            }
        });

        return Collections.unmodifiableMap(result);
    }

    /**
     * 注销处理器（默认版本）
     */
    public boolean unregister(String name) {
        return unregister(name, null);
    }

    /**
     * 注销处理器（指定版本）
     */
    public boolean unregister(String name, String version) {
        if (!StringUtils.hasText(name)) {
            return false;
        }

        String key = buildProcessorKey(name, version);
        ProcessorInfo removed = processors.asMap().remove(key);

        if (removed != null) {
            totalUnregistered.incrementAndGet();
            log.info("Unregistered processor [{}] version [{}]",
                    name, version != null ? version : "default");
            return true;
        }

        return false;
    }

    /**
     * 获取所有处理器的只读视图
     */
    public Collection<ProcessorInfo> list() {
        return Collections.unmodifiableCollection(processors.asMap().values());
    }

    /**
     * 获取处理器统计信息
     */
    public ProcessorStats getStats() {
        return new ProcessorStats(
            processors.asMap().size(),
            totalRegistered.get(),
            totalUnregistered.get(),
            processors.stats().hitCount(),
            processors.stats().missCount(),
            processors.stats().evictionCount()
        );
    }

    /**
     * 清理所有处理器
     */
    @PreDestroy
    public void shutdown() {
        processors.asMap().clear();
        log.info("ProcessorRegistry cleared. Total registered: {}, Total unregistered: {}",
                totalRegistered.get(), totalUnregistered.get());
    }

    // ========== 私有方法 ==========

    private void validateProcessorRegistration(String name, TaskProcessor processor, String version) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Processor name cannot be null or empty");
        }

        if (processor == null) {
            throw new IllegalArgumentException("Processor cannot be null");
        }

        // 验证处理器名称格式
        String normalizedName = name.trim();
        if (!normalizedName.matches("^[a-zA-Z0-9_.-]+$")) {
            throw new IllegalArgumentException(
                "Processor name [" + name + "] contains invalid characters. " +
                "Only letters, numbers, underscores, hyphens and dots are allowed."
            );
        }

        // 验证版本格式（如果提供）
        if (version != null && !version.trim().isEmpty()) {
            String normalizedVersion = version.trim();
            if (!normalizedVersion.matches("^[a-zA-Z0-9_.-]+$")) {
                throw new IllegalArgumentException(
                    "Processor version [" + version + "] contains invalid characters"
                );
            }
        }
    }

    private String buildProcessorKey(String name, String version) {
        String normalizedName = normalize(name);
        String normalizedVersion = version != null ? version.trim() : "default";
        return normalizedName + ":" + normalizedVersion;
    }

    private String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase();
    }

    // ========== 内部类 ==========

    /**
     * 处理器信息封装
     */
    @Data
    public static class ProcessorInfo {
        private final TaskProcessor processor;
        private final String version;
        private final long registrationId;
        private final long registrationTime;

        public ProcessorInfo(TaskProcessor processor, String version, long registrationId) {
            this.processor = processor;
            this.version = version;
            this.registrationId = registrationId;
            this.registrationTime = System.currentTimeMillis();
        }

        public String getProcessorName() {
            return processor.getClass().getSimpleName();
        }
    }

    /**
     * 处理器统计信息
     */
    @Data
    public static class ProcessorStats {
        private final int registeredCount;
        private final long totalRegistered;
        private final long totalUnregistered;
        private final long cacheHitCount;
        private final long cacheMissCount;
        private final long evictionCount;

        public ProcessorStats(int registeredCount, long totalRegistered, long totalUnregistered,
                             long cacheHitCount, long cacheMissCount, long evictionCount) {
            this.registeredCount = registeredCount;
            this.totalRegistered = totalRegistered;
            this.totalUnregistered = totalUnregistered;
            this.cacheHitCount = cacheHitCount;
            this.cacheMissCount = cacheMissCount;
            this.evictionCount = evictionCount;
        }

        public double getCacheHitRate() {
            long total = cacheHitCount + cacheMissCount;
            return total > 0 ? (double) cacheHitCount / total * 100 : 0;
        }
    }
}
