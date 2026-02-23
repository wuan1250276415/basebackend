package com.basebackend.scheduler.core;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 统一的任务结果封装，承载状态、耗时、异常与输出数据。
 * 不可变设计便于跨线程安全传递。
 */
public final class TaskResult {

    /**
     * 任务状态集合。
     */
    public enum Status {
        SUCCESS,
        FAILED,
        RETRYING,
        CANCELLED
    }

    private final Status status;
    private final Instant startTime;
    private final Duration duration;
    private final String errorMessage;
    private final Throwable error;
    private final Map<String, Object> output;
    private final String idempotentKey;
    private final boolean idempotentHit;

    private TaskResult(Builder builder) {
        this.status = Objects.requireNonNull(builder.status, "status");
        this.startTime = builder.startTime;
        this.duration = builder.duration;
        this.errorMessage = builder.errorMessage;
        this.error = builder.error;
        this.output = unmodifiableCopy(builder.output);
        this.idempotentKey = builder.idempotentKey;
        this.idempotentHit = builder.idempotentHit;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Duration getDuration() {
        return duration;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Throwable getError() {
        return error;
    }

    public Map<String, Object> getOutput() {
        return output;
    }

    public String getIdempotentKey() {
        return idempotentKey;
    }

    public boolean isIdempotentHit() {
        return idempotentHit;
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    /**
     * 为结果补全时间信息，便于模板侧统一记录耗时。
     *
     * @param startTime 开始时间
     * @param duration  耗时
     * @return 新的结果实例
     */
    public TaskResult withTiming(Instant startTime, Duration duration) {
        return TaskResult.builder(this.status)
                .startTime(startTime)
                .duration(duration)
                .errorMessage(this.errorMessage)
                .error(this.error)
                .output(this.output)
                .idempotentKey(this.idempotentKey)
                .idempotentHit(this.idempotentHit)
                .build();
    }

    public static Builder builder(Status status) {
        return new Builder(status);
    }

    private static <K, V> Map<K, V> unmodifiableCopy(Map<K, V> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    /**
     * 结果构建器，提供生产级的可读创建方式。
     */
    public static final class Builder {
        private final Status status;
        private Instant startTime;
        private Duration duration;
        private String errorMessage;
        private Throwable error;
        private Map<String, Object> output = Collections.emptyMap();
        private String idempotentKey;
        private boolean idempotentHit;

        private Builder(Status status) {
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

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder error(Throwable error) {
            this.error = error;
            return this;
        }

        public Builder output(Map<String, Object> output) {
            if (output != null) {
                this.output = new LinkedHashMap<>(output);
            }
            return this;
        }

        public Builder idempotentKey(String idempotentKey) {
            this.idempotentKey = idempotentKey;
            return this;
        }

        public Builder idempotentHit(boolean idempotentHit) {
            this.idempotentHit = idempotentHit;
            return this;
        }

        public TaskResult build() {
            return new TaskResult(this);
        }
    }
}
