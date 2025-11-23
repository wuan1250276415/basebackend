package com.basebackend.scheduler.processor;

import com.basebackend.scheduler.core.TaskProcessor;
import com.basebackend.scheduler.core.TaskContext;
import com.basebackend.scheduler.core.TaskResult;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 线程安全的处理器注册表，统一管理 TaskProcessor 生命周期。
 * 支持动态注册/注销与优雅关闭。
 */
@Component
@ConditionalOnProperty(prefix = "scheduler.processor", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ProcessorRegistry {

    private static final Logger log = LoggerFactory.getLogger(ProcessorRegistry.class);

    private final Map<String, TaskProcessor> processors = new ConcurrentHashMap<>();

    /**
     * 注册处理器。后注册的同名处理器会覆盖旧实例。
     */
    public void register(String name, TaskProcessor processor) {
        String key = normalize(name);
        processors.put(key, processor);
        log.info("Registered processor [{}] -> {}", key, processor.getClass().getSimpleName());
    }

    /**
     * 查询处理器。
     */
    public Optional<TaskProcessor> find(String name) {
        return Optional.ofNullable(processors.get(normalize(name)));
    }

    /**
     * 注销处理器。
     */
    public boolean unregister(String name) {
        String key = normalize(name);
        TaskProcessor removed = processors.remove(key);
        if (removed != null) {
            log.info("Unregistered processor [{}]", key);
            return true;
        }
        return false;
    }

    /**
     * 获取所有处理器的只读视图。
     */
    public Collection<TaskProcessor> list() {
        return Collections.unmodifiableCollection(processors.values());
    }

    private String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase();
    }

    /**
     * 优雅关闭：清理注册表。
     */
    @PreDestroy
    public void shutdown() {
        processors.clear();
        log.info("ProcessorRegistry cleared");
    }
}
