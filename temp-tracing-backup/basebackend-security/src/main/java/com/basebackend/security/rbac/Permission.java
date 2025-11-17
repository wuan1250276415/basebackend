package com.basebackend.security.rbac;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 权限实体类
 * 支持资源权限、操作权限和条件权限
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Permission extends BaseEntity {

    /**
     * 权限ID
     */
    private Long id;

    /**
     * 权限名称
     */
    private String name;

    /**
     * 权限编码 (格式: 模块:操作:资源)
     * 例如: user:read:user_info, user:write:user_info
     */
    private String code;

    /**
     * 权限类型
     */
    private PermissionType type;

    /**
     * 权限描述
     */
    private String description;

    /**
     * 资源类型 (URL、功能模块、数据类型等)
     */
    private String resourceType;

    /**
     * 资源标识 (具体的资源ID、URL路径等)
     */
    private String resource;

    /**
     * 操作类型 (read、write、delete、execute等)
     */
    private String action;

    /**
     * 权限条件 (JSON格式)
     * 用于表达复杂的权限条件，如数据范围限制
     * 例如: {"ownerId": "${userId}", "deptId": [1,2,3]}
     */
    private String condition;

    /**
     * 父权限ID (支持权限层级)
     */
    private Long parentId;

    /**
     * 是否为叶子节点
     */
    private Boolean isLeaf = true;

    /**
     * 排序号
     */
    private Integer sort;

    /**
     * 是否启用
     */
    private Boolean enabled = true;

    /**
     * 是否为系统权限 (系统权限不可删除)
     */
    private Boolean system = false;

    /**
     * 权限标签 (用于分组和标识)
     */
    private Set<String> tags;

    /**
     * 权限的子权限列表
     */
    private List<Permission> children;

    /**
     * 权限类型枚举
     */
    public enum PermissionType {
        /**
         * 菜单权限
         */
        MENU,
        /**
         * 按钮权限
         */
        BUTTON,
        /**
         * API权限
         */
        API,
        /**
         * 数据权限
         */
        DATA,
        /**
         * 字段权限
         */
        FIELD,
        /**
         * 功能权限
         */
        FUNCTION,
        /**
         * 资源权限
         */
        RESOURCE
    }

    /**
     * 检查权限条件是否满足
     *
     * @param context 权限上下文
     * @return 是否满足
     */
    public boolean checkCondition(PermissionContext context) {
        if (condition == null || condition.isEmpty()) {
            return true;
        }

        try {
            // 这里可以解析JSON格式的条件表达式
            // 并根据context中的数据进行判断
            // 例如: {"ownerId": "${userId}"} 表示只能访问自己创建的数据

            // 简化实现，实际项目中应使用更强大的条件引擎
            return evaluateCondition(condition, context);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 评估权限条件
     */
    private boolean evaluateCondition(String condition, PermissionContext context) {
        // TODO: 实现条件表达式评估器
        // 可以使用Spring Expression Language (SpEL) 或自定义条件引擎
        return true;
    }

    /**
     * 获取权限的完整路径
     */
    public String getFullPath() {
        if (parentId == null) {
            return code;
        }
        // 递归获取父权限路径
        // 简化实现，实际需要查询数据库
        return code;
    }

    /**
     * 检查是否为父权限
     */
    public boolean isParentPermission() {
        return !isLeaf && (children != null && !children.isEmpty());
    }

    /**
     * 检查是否为叶子权限
     */
    public boolean isLeafPermission() {
        return isLeaf || (children == null || children.isEmpty());
    }
}

/**
 * 权限上下文
 * 用于权限计算和验证的上下文信息
 */
@Data
class PermissionContext {
    /**
     * 当前用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 角色ID列表
     */
    private List<Long> roleIds;

    /**
     * 角色编码列表
     */
    private List<String> roleCodes;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 部门路径
     */
    private String deptPath;

    /**
     * 请求URL
     */
    private String requestUrl;

    /**
     * HTTP方法
     */
    private String httpMethod;

    /**
     * 请求参数
     */
    private Object requestParams;

    /**
     * 资源ID
     */
    private Object resourceId;

    /**
     * 资源Owner ID
     */
    private Long resourceOwnerId;

    /**
     * 数据范围
     */
    private DataScope dataScope;

    /**
     * 是否为管理员
     */
    private Boolean isAdmin = false;

    /**
     * 扩展属性
     */
    private java.util.Map<String, Object> attributes;

    /**
     * 数据范围枚举
     */
    public enum DataScope {
        /**
         * 全部数据
         */
        ALL,
        /**
         * 本部门及以下数据
         */
        DEPT_AND_CHILD,
        /**
         * 本部门数据
         */
        DEPT,
        /**
         * 本部门及子部门数据
         */
        DEPT_AND_SUB,
        /**
         * 仅本人数据
         */
        SELF,
        /**
         * 自定义数据范围
         */
        CUSTOM
    }
}

/**
 * 基础实体类
 */
@Data
class BaseEntity {
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    private Long createdBy;
    private Long updatedBy;
}
