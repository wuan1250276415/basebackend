package com.basebackend.common.datascope.handler;

import com.basebackend.common.context.UserContext;
import com.basebackend.common.context.UserContextHolder;
import com.basebackend.common.datascope.config.DataScopeProperties;
import com.basebackend.common.datascope.context.DataScopeContext;
import com.basebackend.common.datascope.enums.DataScopeType;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

/**
 * 数据权限 SQL 条件构建器
 * <p>
 * 根据数据范围类型和当前用户信息，生成对应的 SQL 过滤条件。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
public final class DataScopeSqlBuilder {

    private static final String DENY_ALL_CONDITION = "1 = 0";
    private static final String DEFAULT_DEPT_TABLE = "sys_dept";

    private static final Pattern IDENTIFIER_PATTERN =
            Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");
    private static final Pattern TABLE_NAME_PATTERN =
            Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)?$");
    private static final Pattern DANGEROUS_SQL_TOKENS =
            Pattern.compile("(?is)(;|--|/\\*|\\*/|\\b(select|insert|update|delete|drop|alter|create|" +
                    "truncate|merge|call|exec|execute|union|from|join)\\b)");
    private static final Pattern SAFE_CUSTOM_COMPARE_PATTERN = Pattern.compile(
            "(?is)^\\s*[A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)?\\s*" +
                    "(?:=|<>|!=|>|>=|<|<=)\\s*(?:-?\\d+|'[^']*')\\s*" +
                    "(?:\\s+(?:AND|OR)\\s+[A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)?\\s*" +
                    "(?:=|<>|!=|>|>=|<|<=)\\s*(?:-?\\d+|'[^']*')\\s*)*$");
    private static final Pattern SAFE_CUSTOM_IN_PATTERN = Pattern.compile(
            "(?is)^\\s*[A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)?\\s+IN\\s*\\(\\s*" +
                    "(?:-?\\d+|'[^']*')(?:\\s*,\\s*(?:-?\\d+|'[^']*'))*\\s*\\)\\s*$");

    private DataScopeSqlBuilder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 构建数据权限 SQL 条件
     *
     * @param type       数据范围类型
     * @param deptAlias  部门表别名
     * @param deptField  部门ID字段名
     * @param userAlias  用户表别名
     * @param userField  创建者字段名
     * @param properties 配置属性（可为 null，仅 DEPT_AND_BELOW 使用）
     * @return SQL 条件片段（不含前导 AND），ALL 类型返回空字符串
     */
    public static String buildCondition(DataScopeType type, String deptAlias, String deptField,
                                        String userAlias, String userField,
                                        DataScopeProperties properties) {
        if (type == DataScopeType.ALL) {
            return "";
        }

        UserContext user = UserContextHolder.get();
        if (user == null) {
            return DENY_ALL_CONDITION;
        }

        String deptTableName = properties != null ? properties.getDeptTableName() : DEFAULT_DEPT_TABLE;

        String condition = switch (type) {
            case DEPT -> buildDeptConditionSafely(type, deptAlias, deptField, user.getDeptId());
            case DEPT_AND_BELOW -> buildDeptAndBelowConditionSafely(
                    type, deptAlias, deptField, user.getDeptId(), deptTableName);
            case SELF -> buildSelfConditionSafely(type, userAlias, userField, user.getUserId());
            case CUSTOM -> buildCustomCondition(deptAlias, deptField);
            // AUTO 依赖业务侧角色映射，未配置时默认拒绝，避免数据越权
            case AUTO -> DENY_ALL_CONDITION;
            case ALL -> "";
        };

        return (condition == null || condition.isEmpty()) ? DENY_ALL_CONDITION : condition;
    }

    private static String buildDeptConditionSafely(DataScopeType type, String deptAlias,
                                                   String deptField, Long deptId) {
        if (!isSafeIdentifier(deptAlias)) {
            return denyForUnsafeIdentifier(type, "deptAlias", deptAlias);
        }
        if (!isSafeIdentifier(deptField)) {
            return denyForUnsafeIdentifier(type, "deptField", deptField);
        }
        return buildDeptCondition(deptAlias, deptField, deptId);
    }

    private static String buildDeptAndBelowConditionSafely(DataScopeType type, String deptAlias, String deptField,
                                                           Long deptId, String deptTableName) {
        if (!isSafeIdentifier(deptAlias)) {
            return denyForUnsafeIdentifier(type, "deptAlias", deptAlias);
        }
        if (!isSafeIdentifier(deptField)) {
            return denyForUnsafeIdentifier(type, "deptField", deptField);
        }
        if (!isSafeTableName(deptTableName)) {
            return denyForUnsafeIdentifier(type, "deptTableName", deptTableName);
        }
        return buildDeptAndBelowCondition(deptAlias, deptField, deptId, deptTableName);
    }

    private static String buildSelfConditionSafely(DataScopeType type, String userAlias,
                                                   String userField, Long userId) {
        if (!isSafeIdentifier(userAlias)) {
            return denyForUnsafeIdentifier(type, "userAlias", userAlias);
        }
        if (!isSafeIdentifier(userField)) {
            return denyForUnsafeIdentifier(type, "userField", userField);
        }
        return buildSelfCondition(userAlias, userField, userId);
    }

    /**
     * 本部门: AND {deptAlias}.{deptField} = {currentDeptId}
     */
    static String buildDeptCondition(String deptAlias, String deptField, Long deptId) {
        if (deptId == null) {
            return "";
        }
        return String.format("%s.%s = %d", deptAlias, deptField, deptId);
    }

    /**
     * 本部门及以下: AND {deptAlias}.{deptField} IN
     * (SELECT dept_id FROM sys_dept WHERE dept_id = {deptId} OR FIND_IN_SET({deptId}, ancestors))
     */
    static String buildDeptAndBelowCondition(String deptAlias, String deptField,
                                             Long deptId, String deptTableName) {
        if (deptId == null) {
            return "";
        }
        return String.format(
                "%s.%s IN (SELECT dept_id FROM %s WHERE dept_id = %d OR FIND_IN_SET(%d, ancestors))",
                deptAlias, deptField, deptTableName, deptId, deptId
        );
    }

    /**
     * 仅本人: AND {userAlias}.{userField} = {currentUserId}
     */
    static String buildSelfCondition(String userAlias, String userField, Long userId) {
        if (userId == null) {
            return "";
        }
        return String.format("%s.%s = %d", userAlias, userField, userId);
    }

    /**
     * 自定义: AND {deptAlias}.{deptField} IN ({角色关联的部门ID集合})
     * <p>
     * 注：实际的部门ID集合需要从角色配置中获取，
     * 此处暂时通过 UserContext 的 roles 预留扩展点。
     * 使用者应通过自定义 DataScopeHandler 来实现具体的部门ID集合获取逻辑。
     * </p>
     */
    static String buildCustomCondition(String deptAlias, String deptField) {
        // CUSTOM 模式需要业务层提供角色关联的部门集合
        // 通过 DataScopeContext 预设或自定义 handler 注入
        String existingCondition = DataScopeContext.get();
        if (existingCondition == null || existingCondition.isBlank()) {
            return "";
        }
        String normalizedCondition = existingCondition.trim();
        if (!isSafeCustomCondition(normalizedCondition)) {
            log.warn("检测到不安全的 CUSTOM 数据权限条件，已按拒绝策略处理: {}",
                    normalizedCondition);
            return DENY_ALL_CONDITION;
        }
        return normalizedCondition;
    }

    private static String denyForUnsafeIdentifier(DataScopeType type, String identifierName, String identifierValue) {
        log.warn("检测到非法数据权限标识符，已拒绝访问: type={}, {}={}",
                type, identifierName, identifierValue);
        return DENY_ALL_CONDITION;
    }

    private static boolean isSafeIdentifier(String identifier) {
        return identifier != null && IDENTIFIER_PATTERN.matcher(identifier).matches();
    }

    private static boolean isSafeTableName(String tableName) {
        return tableName != null && TABLE_NAME_PATTERN.matcher(tableName).matches();
    }

    private static boolean isSafeCustomCondition(String condition) {
        if (DANGEROUS_SQL_TOKENS.matcher(condition).find()) {
            return false;
        }
        return SAFE_CUSTOM_COMPARE_PATTERN.matcher(condition).matches()
                || SAFE_CUSTOM_IN_PATTERN.matcher(condition).matches();
    }
}
