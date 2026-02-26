package com.basebackend.workflow.engine;

import com.basebackend.workflow.model.ConditionBranch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ConditionEvaluator 测试")
class ConditionEvaluatorTest {

    private ConditionEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new ConditionEvaluator();
    }

    @Test
    @DisplayName("数值小于等于比较")
    void numericLessOrEqual() {
        assertThat(evaluator.evaluateExpression("amount <= 5000", Map.of("amount", 3000))).isTrue();
        assertThat(evaluator.evaluateExpression("amount <= 5000", Map.of("amount", 5000))).isTrue();
        assertThat(evaluator.evaluateExpression("amount <= 5000", Map.of("amount", 8000))).isFalse();
    }

    @Test
    @DisplayName("数值大于比较")
    void numericGreaterThan() {
        assertThat(evaluator.evaluateExpression("amount > 5000", Map.of("amount", 8000))).isTrue();
        assertThat(evaluator.evaluateExpression("amount > 5000", Map.of("amount", 5000))).isFalse();
    }

    @Test
    @DisplayName("数值等于比较")
    void numericEquals() {
        assertThat(evaluator.evaluateExpression("level == 3", Map.of("level", 3))).isTrue();
        assertThat(evaluator.evaluateExpression("level == 3", Map.of("level", 5))).isFalse();
    }

    @Test
    @DisplayName("数值不等于比较")
    void numericNotEquals() {
        assertThat(evaluator.evaluateExpression("status != 0", Map.of("status", 1))).isTrue();
        assertThat(evaluator.evaluateExpression("status != 0", Map.of("status", 0))).isFalse();
    }

    @Test
    @DisplayName("字符串等于比较")
    void stringEquals() {
        assertThat(evaluator.evaluateExpression("dept == \"技术部\"", Map.of("dept", "技术部"))).isTrue();
        assertThat(evaluator.evaluateExpression("dept == \"市场部\"", Map.of("dept", "技术部"))).isFalse();
    }

    @Test
    @DisplayName("变量不存在返回 false")
    void variableNotExists() {
        assertThat(evaluator.evaluateExpression("unknown > 0", Map.of())).isFalse();
    }

    @Test
    @DisplayName("无法解析的表达式返回 false")
    void invalidExpression() {
        assertThat(evaluator.evaluateExpression("invalid expression", Map.of())).isFalse();
    }

    @Test
    @DisplayName("evaluate 选择第一个匹配的分支")
    void evaluateFirstMatch() {
        List<ConditionBranch> branches = List.of(
                ConditionBranch.of("amount <= 5000", "leader"),
                ConditionBranch.of("amount > 5000", "director")
        );

        String target = evaluator.evaluate(branches, Map.of("amount", 3000));
        assertThat(target).isEqualTo("leader");
    }

    @Test
    @DisplayName("evaluate 使用默认分支")
    void evaluateDefaultBranch() {
        List<ConditionBranch> branches = List.of(
                ConditionBranch.of("amount > 10000", "ceo"),
                ConditionBranch.of("default", "leader")
        );

        String target = evaluator.evaluate(branches, Map.of("amount", 500));
        assertThat(target).isEqualTo("leader");
    }

    @Test
    @DisplayName("evaluate 无匹配且无默认返回 null")
    void evaluateNoMatch() {
        List<ConditionBranch> branches = List.of(
                ConditionBranch.of("amount > 10000", "ceo")
        );

        String target = evaluator.evaluate(branches, Map.of("amount", 500));
        assertThat(target).isNull();
    }

    @Test
    @DisplayName("evaluate 通配符 * 作为默认分支")
    void evaluateWildcardDefault() {
        List<ConditionBranch> branches = List.of(
                ConditionBranch.of("amount > 10000", "ceo"),
                ConditionBranch.of("*", "fallback")
        );

        String target = evaluator.evaluate(branches, Map.of("amount", 100));
        assertThat(target).isEqualTo("fallback");
    }

    @Test
    @DisplayName("浮点数比较")
    void floatComparison() {
        assertThat(evaluator.evaluateExpression("score >= 90.5", Map.of("score", 95.0))).isTrue();
        assertThat(evaluator.evaluateExpression("score >= 90.5", Map.of("score", 80.0))).isFalse();
    }
}
