package com.basebackend.scheduler.workflow;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 节点执行日志，记录输入输出、状态与耗时。
 * 不可变设计便于审计与追踪。
 */
public final class WorkflowExecutionLog {

    /**
     * 节点执行状态。
     */
    public enum Status {
        SUCCESS,
        FAILED,
        SKIPPED
    }

    private final String nodeId;
    private final String instanceId;
    private final Instant startTime;
    private final Duration duration;
    private final Status status;
    private final Map<String, Object> inputParameters;
    private final Map<String, Object> outputResult;
    private final String errorMessage;

    private WorkflowExecutionLog(Builder builder) {
        this.nodeId = Objects.requireNonNull(builder.nodeId, "nodeId");
        this.instanceId = Objects.requireNonNull(builder.instanceId, "instanceId");
        this.startTime = builder.startTime != null ? builder.startTime : Instant.now();
        this.duration = builder.duration;
        this.status = Objects.requireNonNull(builder.status, "status");
        this.inputParameters = unmodifiable(builder.inputParameters);
        this.outputResult = unmodifiable(builder.outputResult);
        this.errorMessage = builder.errorMessage;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Duration getDuration() {
        return duration;
    }

    public Status getStatus() {
        return status;
    }

    public Map<String, Object> getInputParameters() {
        return inputParameters;
    }

    public Map<String, Object> getOutputResult() {
        return outputResult;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public static Builder builder(String nodeId, String instanceId, Status status) {
        return new Builder(nodeId, instanceId, status);
    }

    private static Map<String, Object> unmodifiable(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    /**
     * 构建器，便于日志采集侧按需补充字段。
     */
    public static final class Builder {
        private final String nodeId;
        private final String instanceId;
        private final Status status;
        private Instant startTime;
        private Duration duration;
        private Map<String, Object> inputParameters = Collections.emptyMap();
        private Map<String, Object> outputResult = Collections.emptyMap();
        private String errorMessage;

        private Builder(String nodeId, String instanceId, Status status) {
            this.nodeId = nodeId;
            this.instanceId = instanceId;
            this.status = status;
        }

        public Builder startTime(Instant startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder duration(Duration duration) {
            this.duration = duration;
            return this;
        }

        public Builder inputParameters(Map<String, Object> inputParameters) {
            if (inputParameters != null) {
                this.inputParameters = new LinkedHashMap<>(inputParameters);
            }
            return this;
        }

        public Builder outputResult(Map<String, Object> outputResult) {
            if (outputResult != null) {
                this.outputResult = new LinkedHashMap<>(outputResult);
            }
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public WorkflowExecutionLog build() {
            return new WorkflowExecutionLog(this);
        }
    }
}
