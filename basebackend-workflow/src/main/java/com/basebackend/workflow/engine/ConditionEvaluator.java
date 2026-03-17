package com.basebackend.workflow.engine;

import com.basebackend.workflow.model.ConditionBranch;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 条件表达式求值器
 * <p>
 * 支持简单的比较表达式：{@code variable op value}
 * <ul>
 *   <li>{@code days <= 3}</li>
 *   <li>{@code amount > 5000}</li>
 *   <li>{@code department == "技术部"}</li>
 *   <li>{@code level != "senior"}</li>
 *   <li>{@code default} — 默认分支（始终匹配）</li>
 * </ul>
 *
 * <h3>安全说明</h3>
 * <p>
 * 当前实现基于简单字符串/数值比较，不涉及脚本执行，不存在代码注入风险。
 * <strong>但条件表达式必须由开发人员在流程定义阶段写入，严禁将终端用户的输入直接作为表达式使用。</strong>
 * 若未来需要支持用户自定义表达式，须引入沙箱求值器（如 Spring Expression Language 的只读上下文），
 * 并对变量名和值做严格白名单校验。
 */
@Slf4j
public class ConditionEvaluator {

    private static final Pattern EXPR_PATTERN = Pattern.compile(
            "(\\w+)\\s*(==|!=|>=|<=|>|<)\\s*(.+)"
    );

    /** 浮点数等值比较容差，避免 0.1 + 0.2 != 0.3 类精度问题 */
    private static final double EPSILON = 1e-9;

    /**
     * 从分支列表中选择第一个满足条件的目标节点
     *
     * @param branches  条件分支列表
     * @param variables 流程变量
     * @return 目标节点 ID，无匹配返回 null
     */
    public String evaluate(List<ConditionBranch> branches, Map<String, Object> variables) {
        String defaultTarget = null;

        for (ConditionBranch branch : branches) {
            String expr = branch.expression().trim();

            // 默认分支
            if ("default".equalsIgnoreCase(expr) || "*".equals(expr)) {
                defaultTarget = branch.targetNodeId();
                continue;
            }

            if (evaluateExpression(expr, variables)) {
                log.debug("条件匹配: expr='{}', target='{}'", expr, branch.targetNodeId());
                return branch.targetNodeId();
            }
        }

        // 无匹配时使用默认分支
        if (defaultTarget != null) {
            log.debug("使用默认分支: target='{}'", defaultTarget);
        }
        return defaultTarget;
    }

    /**
     * 求值单个表达式
     */
    public boolean evaluateExpression(String expression, Map<String, Object> variables) {
        Matcher matcher = EXPR_PATTERN.matcher(expression.trim());
        if (!matcher.matches()) {
            log.warn("无法解析条件表达式: '{}'", expression);
            return false;
        }

        String varName = matcher.group(1);
        String operator = matcher.group(2);
        String valueStr = matcher.group(3).trim().replaceAll("^\"|\"$", ""); // 去除引号

        Object varValue = variables.get(varName);
        if (varValue == null) {
            log.debug("变量不存在: '{}'", varName);
            return false;
        }

        return compare(varValue, operator, valueStr);
    }

    private boolean compare(Object varValue, String operator, String valueStr) {
        // 数值比较
        if (varValue instanceof Number numVar) {
            try {
                double numValue = Double.parseDouble(valueStr);
                double varDouble = numVar.doubleValue();
                return switch (operator) {
                    case "==" -> Math.abs(varDouble - numValue) <= EPSILON;
                    case "!=" -> Math.abs(varDouble - numValue) > EPSILON;
                    case ">" -> varDouble > numValue;
                    case ">=" -> varDouble >= numValue;
                    case "<" -> varDouble < numValue;
                    case "<=" -> varDouble <= numValue;
                    default -> false;
                };
            } catch (NumberFormatException e) {
                // 值不是数字，回退到字符串比较
            }
        }

        // 字符串比较
        String varStr = String.valueOf(varValue);
        return switch (operator) {
            case "==" -> varStr.equals(valueStr);
            case "!=" -> !varStr.equals(valueStr);
            case ">" -> varStr.compareTo(valueStr) > 0;
            case ">=" -> varStr.compareTo(valueStr) >= 0;
            case "<" -> varStr.compareTo(valueStr) < 0;
            case "<=" -> varStr.compareTo(valueStr) <= 0;
            default -> false;
        };
    }
}
