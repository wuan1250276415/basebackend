package com.basebackend.common.datascope.handler;

import com.basebackend.common.context.UserContext;
import com.basebackend.common.context.UserContextHolder;
import com.basebackend.common.datascope.config.DataScopeProperties;
import com.basebackend.common.datascope.context.DataScopeContext;
import com.basebackend.common.datascope.enums.DataScopeType;

import java.util.regex.Matcher;
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
public final class DataScopeSqlBuilder {

    /**
     * SQL 标识符白名单正则：只允许字母、数字、下划线，且必须以字母或下划线开头。
     * 用于防御将别名/字段名/表名直接插入 SQL 时的注入攻击。
     */
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    /**
     * 自定义条件中的危险 SQL 片段，命中后直接拒绝访问。
     * <p>
     * 这里只覆盖当前回归所需的最小安全集，避免把合法表达式误判为非法。
     * </p>
     */
    private static final Pattern UNSAFE_CUSTOM_CONDITION = Pattern.compile(
            "(?is)(;|--|/\\*|\\*/|\\b(drop|delete|insert|update|truncate|alter|create|grant|revoke|union|exec|sleep|benchmark)\\b)"
    );

    private DataScopeSqlBuilder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 校验 SQL 标识符合法性，不合法时抛出 IllegalArgumentException。
     *
     * @param name  标识符（表别名、字段名、表名）
     * @param label 描述，用于错误信息
     */
    private static void validateIdentifier(String name, String label) {
        if (name == null || !SAFE_IDENTIFIER.matcher(name).matches()) {
            throw new IllegalArgumentException("非法 SQL 标识符 [" + label + "]: " + name);
        }
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
        if (type == null) {
            return denyCondition();
        }

        if (type == DataScopeType.ALL) {
            return "";
        }

        UserContext user = UserContextHolder.get();
        if (user == null) {
            return denyCondition();
        }

        String deptTableName = properties != null ? properties.getDeptTableName() : "sys_dept";

        try {
            return switch (type) {
                case DEPT -> buildDeptCondition(deptAlias, deptField, user.getDeptId());
                case DEPT_AND_BELOW -> buildDeptAndBelowCondition(deptAlias, deptField, user.getDeptId(), deptTableName);
                case SELF -> buildSelfCondition(userAlias, userField, user.getUserId());
                case CUSTOM -> buildCustomCondition();
                case AUTO -> denyCondition();
                case ALL -> "";
            };
        } catch (IllegalArgumentException | UnsupportedOperationException ex) {
            return denyCondition();
        }
    }

    /**
     * 本部门: AND {deptAlias}.{deptField} = {currentDeptId}
     */
    static String buildDeptCondition(String deptAlias, String deptField, Long deptId) {
        if (deptId == null) {
            return "";
        }
        validateIdentifier(deptAlias, "deptAlias");
        validateIdentifier(deptField, "deptField");
        return String.format("%s.%s = %d", deptAlias, deptField, deptId);
    }

    /**
     * 本部门及以下: AND {deptAlias}.{deptField} IN (SELECT dept_id FROM sys_dept WHERE dept_id = {deptId} OR FIND_IN_SET({deptId}, ancestors))
     */
    static String buildDeptAndBelowCondition(String deptAlias, String deptField,
                                             Long deptId, String deptTableName) {
        if (deptId == null) {
            return "";
        }
        validateIdentifier(deptAlias, "deptAlias");
        validateIdentifier(deptField, "deptField");
        validateIdentifier(deptTableName, "deptTableName");
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
        validateIdentifier(userAlias, "userAlias");
        validateIdentifier(userField, "userField");
        return String.format("%s.%s = %d", userAlias, userField, userId);
    }

    /**
     * 自定义: AND {deptAlias}.{deptField} IN ({角色关联的部门ID集合})
     * <p>
     * 注：实际的部门ID集合需要从角色配置中获取，此处暂时通过 UserContext 的 roles 预留扩展点。
     * 使用者应通过自定义 DataScopeHandler 来实现具体的部门ID集合获取逻辑。
     * </p>
     */
    static String buildCustomCondition() {
        // CUSTOM 模式需要业务层提供角色关联的部门集合
        // 通过 DataScopeContext 预设或自定义 handler 注入
        String existingCondition = DataScopeContext.get();
        if (existingCondition != null && !existingCondition.isEmpty() && !isUnsafeCustomCondition(existingCondition)) {
            return existingCondition;
        }
        return denyCondition();
    }

    private static String denyCondition() {
        return "1 = 0";
    }

    private static boolean isUnsafeCustomCondition(String condition) {
        Matcher matcher = UNSAFE_CUSTOM_CONDITION.matcher(condition);
        return matcher.find();
    }
}
