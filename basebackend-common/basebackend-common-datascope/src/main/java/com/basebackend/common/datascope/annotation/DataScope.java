package com.basebackend.common.datascope.annotation;

import com.basebackend.common.datascope.enums.DataScopeType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据权限注解
 * <p>
 * 标注在 Mapper 方法或 Service 方法上，自动在 SQL 查询中追加数据权限过滤条件。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 自动根据用户角色决定数据范围
 * @DataScope
 * List<SysUser> selectUserList(SysUser user);
 *
 * // 指定仅查看本部门数据
 * @DataScope(type = DataScopeType.DEPT, deptAlias = "d")
 * List<SysUser> selectDeptUserList(SysUser user);
 *
 * // 仅查看本人数据
 * @DataScope(type = DataScopeType.SELF, userAlias = "u", userField = "create_by")
 * List<SysOrder> selectMyOrders(SysOrder order);
 * }</pre>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DataScope {

    /**
     * 部门表别名
     */
    String deptAlias() default "d";

    /**
     * 用户表别名
     */
    String userAlias() default "u";

    /**
     * 部门ID字段名
     */
    String deptField() default "dept_id";

    /**
     * 创建者字段名
     */
    String userField() default "create_by";

    /**
     * 数据范围类型
     */
    DataScopeType type() default DataScopeType.AUTO;
}
