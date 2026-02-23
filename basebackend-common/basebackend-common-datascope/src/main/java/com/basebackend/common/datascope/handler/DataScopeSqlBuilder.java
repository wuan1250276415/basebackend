package com.basebackend.common.datascope.handler;

import com.basebackend.common.context.UserContext;
import com.basebackend.common.context.UserContextHolder;
import com.basebackend.common.datascope.config.DataScopeProperties;
import com.basebackend.common.datascope.enums.DataScopeType;

import java.util.Set;
import java.util.stream.Collectors;

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
        UserContext user = UserContextHolder.get();
        if (user == null) {
            return "";
        }

        String deptTableName = properties != null ? properties.getDeptTableName() : "sys_dept";

        return switch (type) {
            case ALL -> "";
            case DEPT -> buildDeptCondition(deptAlias, deptField, user.getDeptId());
            case DEPT_AND_BELOW -> buildDeptAndBelowCondition(deptAlias, deptField, user.getDeptId(), deptTableName);
            case SELF -> buildSelfCondition(userAlias, userField, user.getUserId());
            case CUSTOM -> buildCustomCondition(deptAlias, deptField);
            case AUTO -> "";
        };
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
     * 本部门及以下: AND {deptAlias}.{deptField} IN (SELECT dept_id FROM sys_dept WHERE dept_id = {deptId} OR FIND_IN_SET({deptId}, ancestors))
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
     * 注：实际的部门ID集合需要从角色配置中获取，此处暂时通过 UserContext 的 roles 预留扩展点。
     * 使用者应通过自定义 DataScopeHandler 来实现具体的部门ID集合获取逻辑。
     * </p>
     */
    static String buildCustomCondition(String deptAlias, String deptField) {
        // CUSTOM 模式需要业务层提供角色关联的部门集合
        // 通过 DataScopeContext 预设或自定义 handler 注入
        String existingCondition = com.basebackend.common.datascope.context.DataScopeContext.get();
        if (existingCondition != null && !existingCondition.isEmpty()) {
            return existingCondition;
        }
        return "";
    }
}
