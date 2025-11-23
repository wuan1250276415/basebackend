package com.basebackend.scheduler.registry;

import com.basebackend.scheduler.config.JobConfigProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 任务注册服务实现
 * <p>
 * 实现了JobRegistry接口，提供PowerJob任务处理器的注册、管理、查询等功能。
 * </p>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-11-25
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "scheduler.powerjob", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class JobRegisterService implements JobRegistry {

    private final JobConfigProperties jobConfigProperties;

    private final ConcurrentHashMap<String, Class<?>> processorRegistry =
            new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Object> processorInstanceRegistry =
            new ConcurrentHashMap<>();

    private final AtomicLong totalRegistrations = new AtomicLong(0);
    private final AtomicLong totalUnregistrations = new AtomicLong(0);
    private volatile long lastRegistrationTime = 0L;

    @PostConstruct
    public void init() {
        log.info("[JobRegister] Service initialized");
        validate();
        if (jobConfigProperties.isAutoRegister()) {
            log.info("[JobRegister] Auto-register is enabled");
        }
    }

    @PreDestroy
    public void destroy() {
        int cleared = clearAll();
        log.info("[JobRegister] Service destroyed, cleared count={}", cleared);
    }

    @Override
    public void registerProcessor(String jobName, Class<?> processorClass) {
        validateJobName(jobName);
        validateProcessorClass(processorClass);

        Class<?> existing = processorRegistry.putIfAbsent(jobName, processorClass);
        if (existing != null) {
            throw new IllegalArgumentException(
                    String.format("Job name already exists: %s", jobName));
        }

        totalRegistrations.incrementAndGet();
        lastRegistrationTime = System.currentTimeMillis();

        log.info("[JobRegister] Processor registered: jobName={}, class={}",
                jobName, processorClass.getSimpleName());
    }

    @Override
    public void registerProcessorInstance(String jobName, Object processor) {
        validateJobName(jobName);
        validateProcessorInstance(processor);

        Object existing = processorInstanceRegistry.putIfAbsent(jobName, processor);
        if (existing != null) {
            throw new IllegalArgumentException(
                    String.format("Job name already exists: %s", jobName));
        }

        totalRegistrations.incrementAndGet();
        lastRegistrationTime = System.currentTimeMillis();

        log.info("[JobRegister] Processor instance registered: jobName={}, processor={}",
                jobName, processor.getClass().getSimpleName());
    }

    @Override
    public Class<?> getProcessorClass(String jobName) {
        validateJobName(jobName);
        return processorRegistry.get(jobName);
    }

    @Override
    public Object getProcessorInstance(String jobName) {
        validateJobName(jobName);
        return processorInstanceRegistry.get(jobName);
    }

    @Override
    public Set<String> getRegisteredJobs() {
        Set<String> allJobs = new HashSet<>();
        allJobs.addAll(processorRegistry.keySet());
        allJobs.addAll(processorInstanceRegistry.keySet());
        return Collections.unmodifiableSet(allJobs);
    }

    @Override
    public boolean isRegistered(String jobName) {
        validateJobName(jobName);
        return processorRegistry.containsKey(jobName) ||
                processorInstanceRegistry.containsKey(jobName);
    }

    @Override
    public boolean unregisterProcessor(String jobName) {
        validateJobName(jobName);

        boolean removed = false;

        if (processorRegistry.remove(jobName) != null) {
            removed = true;
        }

        if (processorInstanceRegistry.remove(jobName) != null) {
            removed = true;
        }

        if (removed) {
            totalUnregistrations.incrementAndGet();
            log.info("[JobRegister] Processor unregistered: jobName={}", jobName);
        }

        return removed;
    }

    @Override
    public BatchRegisterResult registerAll(Map<String, Class<?>> processors) {
        if (processors == null || processors.isEmpty()) {
            return new BatchRegisterResult(0, 0, 0, Collections.emptyList());
        }

        int total = processors.size();
        int successCount = 0;
        int failureCount = 0;
        List<String> failureReasons = new ArrayList<>();

        for (Map.Entry<String, Class<?>> entry : processors.entrySet()) {
            String jobName = entry.getKey();
            Class<?> processorClass = entry.getValue();

            try {
                registerProcessor(jobName, processorClass);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                String reason = String.format("jobName=%s, error=%s", jobName, e.getMessage());
                failureReasons.add(reason);
                log.error("[JobRegister] Batch register failed: {}", reason, e);
            }
        }

        log.info("[JobRegister] Batch register completed: total={}, success={}, failure={}",
                total, successCount, failureCount);

        return new BatchRegisterResult(total, successCount, failureCount, failureReasons);
    }

    @Override
    public int clearAll() {
        int count = processorRegistry.size() + processorInstanceRegistry.size();

        processorRegistry.clear();
        processorInstanceRegistry.clear();

        log.info("[JobRegister] Cleared all processors, count={}", count);

        return count;
    }

    @Override
    public int getRegisteredJobCount() {
        return processorRegistry.size() + processorInstanceRegistry.size();
    }

    @Override
    public RegistryStatistics getStatistics() {
        Runtime runtime = Runtime.getRuntime();
        long memoryUsage = runtime.totalMemory() - runtime.freeMemory();

        return new RegistryStatistics(
                getRegisteredJobCount(),
                memoryUsage,
                lastRegistrationTime,
                (int) totalRegistrations.get(),
                (int) totalUnregistrations.get()
        );
    }

    private void validate() {
        try {
            jobConfigProperties.validate();
        } catch (Exception e) {
            log.error("[JobRegister] Configuration validation failed", e);
            throw e;
        }
    }

    private void validateJobName(String jobName) {
        if (jobName == null || jobName.trim().isEmpty()) {
            throw new IllegalArgumentException("Job name cannot be empty");
        }
        if (jobName.length() > 100) {
            throw new IllegalArgumentException(
                    String.format("Job name too long: %s (max 100 chars)", jobName));
        }
        if (!jobName.matches("^[a-zA-Z0-9_-]+$")) {
            throw new IllegalArgumentException(
                    String.format("Job name contains invalid characters: %s", jobName));
        }
    }

    private void validateProcessorClass(Class<?> processorClass) {
        if (processorClass == null) {
            throw new IllegalArgumentException("Processor class cannot be null");
        }
    }

    private void validateProcessorInstance(Object processor) {
        if (processor == null) {
            throw new IllegalArgumentException("Processor instance cannot be null");
        }
    }

    public boolean isEmpty() {
        return processorRegistry.isEmpty() && processorInstanceRegistry.isEmpty();
    }

    public int getClassRegistrySize() {
        return processorRegistry.size();
    }

    public int getInstanceRegistrySize() {
        return processorInstanceRegistry.size();
    }
}
