package com.basebackend.scheduler.core;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 任务执行上下文，携带幂等键、标签、参数以及重试与超时信息。
 * <p>
 * 不可变设计保证线程安全，提供 with 方法便于衍生上下文。
 */
public final class TaskContext {

    private final String taskId;
    private final String idempotentKey;
    private final Map<String, String> labels;
    private final Map<String, Object> parameters;
    private final int retryCount;
    private final Duration timeout;
    private final Map<String, String> traceContext;

    private TaskContext(Builder builder) {
        this.taskId = Objects.requireNonNull(builder.taskId, "taskId");
        this.idempotentKey = builder.idempotentKey;
        this.labels = unmodifiableCopy(builder.labels);
        this.parameters = unmodifiableCopy(builder.parameters);
        this.retryCount = Math.max(0, builder.retryCount);
        this.timeout = builder.timeout;
        this.traceContext = unmodifiableCopy(builder.traceContext);
    }

    public String getTaskId() {
        return taskId;
    }

    public String getIdempotentKey() {
        return idempotentKey;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public Map<String, String> getTraceContext() {
        return traceContext;
    }

    /**
     * 生成新的上下文，更新重试次数。
     *
     * @param retryCount 新的重试次数
     * @return 新上下文
     */
    public TaskContext withRetryCount(int retryCount) {
        return toBuilder().retryCount(retryCount).build();
    }

    /**
     * 生成新的上下文，重试次数自增。
     *
     * @return 新上下文
     */
    public TaskContext incrementRetryCount() {
        return withRetryCount(this.retryCount + 1);
    }

    /**
     * 生成新的上下文，更新超时时间。
     *
     * @param timeout 新超时时间
     * @return 新上下文
     */
    public TaskContext withTimeout(Duration timeout) {
        return toBuilder().timeout(timeout).build();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static Builder builder(String taskId) {
        return new Builder(taskId);
    }

    private static <K, V> Map<K, V> unmodifiableCopy(Map<K, V> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    /**
     * 构建器，提供不可变上下文的友好创建方式。
     */
    public static final class Builder {
        private String taskId;
        private String idempotentKey;
        private Map<String, String> labels = Collections.emptyMap();
        private Map<String, Object> parameters = Collections.emptyMap();
        private int retryCount;
        private Duration timeout = Duration.ofSeconds(30);
        private Map<String, String> traceContext = Collections.emptyMap();

        private Builder(String taskId) {
            this.taskId = taskId;
        }

        private Builder(TaskContext original) {
            this.taskId = original.taskId;
            this.idempotentKey = original.idempotentKey;
            this.labels = original.labels;
            this.parameters = original.parameters;
            this.retryCount = original.retryCount;
            this.timeout = original.timeout;
            this.traceContext = original.traceContext;
        }

        public Builder taskId(String taskId) {
            this.taskId = taskId;
            return this;
        }

        public Builder idempotentKey(String idempotentKey) {
            this.idempotentKey = idempotentKey;
            return this;
        }

        public Builder labels(Map<String, String> labels) {
            if (labels != null) {
                this.labels = new LinkedHashMap<>(labels);
            }
            return this;
        }

        public Builder parameters(Map<String, Object> parameters) {
            if (parameters != null) {
                this.parameters = new LinkedHashMap<>(parameters);
            }
            return this;
        }

        public Builder retryCount(int retryCount) {
            this.retryCount = Math.max(0, retryCount);
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder traceContext(Map<String, String> traceContext) {
            if (traceContext != null) {
                this.traceContext = new LinkedHashMap<>(traceContext);
            }
            return this;
        }

        public TaskContext build() {
            return new TaskContext(this);
        }
    }
}
