package com.basebackend.generator.core.strategy;

import cn.hutool.core.util.StrUtil;

/**
 * 命名策略工具类
 */
public class NamingStrategy {

    /**
     * 表名转类名（UpperCamelCase）
     * 例：sys_user -> SysUser
     */
    public static String tableToClassName(String tableName, String tablePrefix) {
        if (StrUtil.isNotBlank(tablePrefix) && tableName.startsWith(tablePrefix)) {
            tableName = tableName.substring(tablePrefix.length());
        }
        return StrUtil.upperFirst(StrUtil.toCamelCase(tableName));
    }

    /**
     * 表名转变量名（lowerCamelCase）
     * 例：sys_user -> sysUser
     */
    public static String tableToVariableName(String tableName, String tablePrefix) {
        if (StrUtil.isNotBlank(tablePrefix) && tableName.startsWith(tablePrefix)) {
            tableName = tableName.substring(tablePrefix.length());
        }
        return StrUtil.toCamelCase(tableName);
    }

    /**
     * 表名转URL路径（kebab-case）
     * 例：sys_user -> sys-user
     */
    public static String tableToUrlPath(String tableName, String tablePrefix) {
        if (StrUtil.isNotBlank(tablePrefix) && tableName.startsWith(tablePrefix)) {
            tableName = tableName.substring(tablePrefix.length());
        }
        return tableName.replace("_", "-");
    }

    /**
     * 列名转Java字段名（lowerCamelCase）
     * 例：user_name -> userName
     */
    public static String columnToJavaField(String columnName) {
        return StrUtil.toCamelCase(columnName);
    }

    /**
     * 列名转常量名（UPPER_SNAKE_CASE）
     * 例：user_name -> USER_NAME
     */
    public static String columnToConstant(String columnName) {
        return columnName.toUpperCase();
    }
}
