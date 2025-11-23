package com.basebackend.scheduler.workflow;

import java.util.Objects;

/**
 * 工作流边定义，描述节点间依赖与条件。
 * 不可变设计便于安全共享。
 */
public final class WorkflowEdge {

    /**
     * 节点执行失败后的处理策略。
     */
    public enum FailureStrategy {
        CONTINUE,
        FAIL,
        STOP
    }

    private final String from;
    private final String to;
    private final String conditionExpression;
    private final double weight;
    private final FailureStrategy failureStrategy;

    private WorkflowEdge(Builder builder) {
        this.from = Objects.requireNonNull(builder.from, "from");
        this.to = Objects.requireNonNull(builder.to, "to");
        this.conditionExpression = builder.conditionExpression;
        this.weight = builder.weight;
        this.failureStrategy = Objects.requireNonNull(builder.failureStrategy, "failureStrategy");
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String getConditionExpression() {
        return conditionExpression;
    }

    public double getWeight() {
        return weight;
    }

    public FailureStrategy getFailureStrategy() {
        return failureStrategy;
    }

    /**
     * 是否存在条件表达式。
     *
     * @return true 表示存在条件，需要外部评估
     */
    public boolean isConditional() {
        return conditionExpression != null && !conditionExpression.trim().isEmpty();
    }

    public static Builder builder(String from, String to) {
        return new Builder(from, to);
    }

    /**
     * 构建器，封装边的校验与默认值。
     */
    public static final class Builder {
        private final String from;
        private final String to;
        private String conditionExpression;
        private double weight = 1.0d;
        private FailureStrategy failureStrategy = FailureStrategy.FAIL;

        private Builder(String from, String to) {
            this.from = from;
            this.to = to;
        }

        public Builder conditionExpression(String conditionExpression) {
            this.conditionExpression = conditionExpression;
            return this;
        }

        public Builder weight(double weight) {
            if (Double.isNaN(weight) || Double.isInfinite(weight) || weight <= 0) {
                throw new IllegalArgumentException("weight must be positive");
            }
            this.weight = weight;
            return this;
        }

        public Builder failureStrategy(FailureStrategy failureStrategy) {
            this.failureStrategy = Objects.requireNonNull(failureStrategy, "failureStrategy");
            return this;
        }

        public WorkflowEdge build() {
            return new WorkflowEdge(this);
        }
    }
}
