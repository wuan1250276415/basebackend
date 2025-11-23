package com.basebackend.scheduler.workflow;

import com.basebackend.scheduler.core.RetryPolicy;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 工作流节点定义，描述处理器、参数、重试与并行度等关键元数据。
 * 不可变对象便于在多线程场景下安全复用。
 */
public final class WorkflowNode {

    private final String id;
    private final String name;
    private final String processorType;
    private final Map<String, Object> inputParameters;
    private final Map<String, Object> outputParameters;
    private final RetryPolicy retryPolicy;
    private final Duration timeout;
    private final int parallelism;
    private final Map<String, String> labels;
    private final Map<String, Object> metadata;

    private WorkflowNode(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id");
        this.name = Objects.requireNonNull(builder.name, "name");
        this.processorType = Objects.requireNonNull(builder.processorType, "processorType");
        this.inputParameters = unmodifiableCopy(builder.inputParameters);
        this.outputParameters = unmodifiableCopy(builder.outputParameters);
        this.retryPolicy = Objects.requireNonNull(builder.retryPolicy, "retryPolicy");
        this.timeout = builder.timeout != null ? builder.timeout : Duration.ZERO;
        this.parallelism = Math.max(1, builder.parallelism);
        this.labels = unmodifiableCopy(builder.labels);
        this.metadata = unmodifiableCopy(builder.metadata);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getProcessorType() {
        return processorType;
    }

    public Map<String, Object> getInputParameters() {
        return inputParameters;
    }

    public Map<String, Object> getOutputParameters() {
        return outputParameters;
    }

    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public int getParallelism() {
        return parallelism;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static Builder builder(String id, String name, String processorType) {
        return new Builder(id, name, processorType);
    }

    /**
     * 构建器，提供生产级可读性与校验。
     */
    public static final class Builder {
        private final String id;
        private final String name;
        private final String processorType;
        private Map<String, Object> inputParameters = Collections.emptyMap();
        private Map<String, Object> outputParameters = Collections.emptyMap();
        private RetryPolicy retryPolicy = RetryPolicy.noRetry();
        private Duration timeout = Duration.ZERO;
        private int parallelism = 1;
        private Map<String, String> labels = Collections.emptyMap();
        private Map<String, Object> metadata = Collections.emptyMap();

        private Builder(String id, String name, String processorType) {
            this.id = id;
            this.name = name;
            this.processorType = processorType;
        }

        private Builder(WorkflowNode source) {
            this.id = source.id;
            this.name = source.name;
            this.processorType = source.processorType;
            this.inputParameters = source.inputParameters;
            this.outputParameters = source.outputParameters;
            this.retryPolicy = source.retryPolicy;
            this.timeout = source.timeout;
            this.parallelism = source.parallelism;
            this.labels = source.labels;
            this.metadata = source.metadata;
        }

        public Builder inputParameters(Map<String, Object> inputParameters) {
            if (inputParameters != null) {
                this.inputParameters = new LinkedHashMap<>(inputParameters);
            }
            return this;
        }

        public Builder outputParameters(Map<String, Object> outputParameters) {
            if (outputParameters != null) {
                this.outputParameters = new LinkedHashMap<>(outputParameters);
            }
            return this;
        }

        public Builder retryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = Objects.requireNonNull(retryPolicy, "retryPolicy");
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder parallelism(int parallelism) {
            this.parallelism = parallelism;
            return this;
        }

        public Builder labels(Map<String, String> labels) {
            if (labels != null) {
                this.labels = new LinkedHashMap<>(labels);
            }
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            if (metadata != null) {
                this.metadata = new LinkedHashMap<>(metadata);
            }
            return this;
        }

        public WorkflowNode build() {
            return new WorkflowNode(this);
        }
    }

    private static <K, V> Map<K, V> unmodifiableCopy(Map<K, V> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }
}
