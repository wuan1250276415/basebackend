package com.basebackend.scheduler.workflow;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 工作流实例状态，包含当前节点、上下文与时间信息。
 * 采用不可变模式确保线程安全。
 */
public final class WorkflowInstance {

    /**
     * 实例状态集合。
     */
    public enum Status {
        PENDING,
        RUNNING,
        SUCCEEDED,
        FAILED,
        CANCELLED,
        PAUSED
    }

    private final String id;
    private final String definitionId;
    private final Status status;
    private final Set<String> activeNodes;
    private final Map<String, Object> context;
    private final Instant startTime;
    private final Instant endTime;
    private final Duration duration;
    private final String errorMessage;

    private WorkflowInstance(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id");
        this.definitionId = Objects.requireNonNull(builder.definitionId, "definitionId");
        this.status = Objects.requireNonNull(builder.status, "status");
        this.activeNodes = unmodifiableSet(builder.activeNodes);
        this.context = unmodifiableMap(builder.context);
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.duration = builder.duration != null ? builder.duration : calculateDuration(builder.startTime, builder.endTime);
        this.errorMessage = builder.errorMessage;
    }

    public String getId() {
        return id;
    }

    public String getDefinitionId() {
        return definitionId;
    }

    public Status getStatus() {
        return status;
    }

    public Set<String> getActiveNodes() {
        return activeNodes;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public Duration getDuration() {
        return duration;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * 创建新的实例状态，用于状态迁移。
     *
     * @param newStatus     新状态
     * @param errorMessage  错误信息
     * @param endTime       结束时间
     * @param activeNodes   当前节点集合
     * @return 新的实例
     */
    public WorkflowInstance withStatus(Status newStatus, String errorMessage, Instant endTime, Set<String> activeNodes) {
        return WorkflowInstance.builder(this.id, this.definitionId)
                .status(newStatus)
                .context(this.context)
                .startTime(this.startTime)
                .endTime(endTime)
                .activeNodes(activeNodes)
                .errorMessage(errorMessage)
                .build();
    }

    public boolean isTerminal() {
        return status == Status.SUCCEEDED || status == Status.FAILED || status == Status.CANCELLED;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static Builder builder(String id, String definitionId) {
        return new Builder(id, definitionId);
    }

    private static Duration calculateDuration(Instant start, Instant end) {
        if (start != null && end != null) {
            return Duration.between(start, end);
        }
        return null;
    }

    private static Set<String> unmodifiableSet(Set<String> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new LinkedHashSet<>(source));
    }

    private static Map<String, Object> unmodifiableMap(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    /**
     * 构建器，负责状态变更的不可变创建。
     */
    public static final class Builder {
        private final String id;
        private final String definitionId;
        private Status status = Status.PENDING;
        private Set<String> activeNodes = Collections.emptySet();
        private Map<String, Object> context = Collections.emptyMap();
        private Instant startTime;
        private Instant endTime;
        private Duration duration;
        private String errorMessage;

        private Builder(String id, String definitionId) {
            this.id = id;
            this.definitionId = definitionId;
        }

        private Builder(WorkflowInstance source) {
            this.id = source.id;
            this.definitionId = source.definitionId;
            this.status = source.status;
            this.activeNodes = source.activeNodes;
            this.context = source.context;
            this.startTime = source.startTime;
            this.endTime = source.endTime;
            this.duration = source.duration;
            this.errorMessage = source.errorMessage;
        }

        public Builder status(Status status) {
            this.status = status;
            return this;
        }

        public Builder activeNodes(Set<String> activeNodes) {
            if (activeNodes != null) {
                this.activeNodes = new LinkedHashSet<>(activeNodes);
            }
            return this;
        }

        public Builder context(Map<String, Object> context) {
            if (context != null) {
                this.context = new LinkedHashMap<>(context);
            }
            return this;
        }

        public Builder startTime(Instant startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder endTime(Instant endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder duration(Duration duration) {
            this.duration = duration;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public WorkflowInstance build() {
            return new WorkflowInstance(this);
        }
    }
}
