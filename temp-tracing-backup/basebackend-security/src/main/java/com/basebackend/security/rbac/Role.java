package com.basebackend.security.rbac;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 角色实体类
 * 支持角色继承、角色分类和动态权限
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Role extends BaseEntity {

    /**
     * 角色ID
     */
    private Long id;

    /**
     * 角色名称
     */
    private String name;

    /**
     * 角色编码
     */
    private String code;

    /**
     * 角色类型
     */
    private RoleType type;

    /**
     * 角色描述
     */
    private String description;

    /**
     * 父角色ID (支持角色继承)
     */
    private Long parentId;

    /**
     * 角色层级
     */
    private Integer level;

    /**
     * 排序号
     */
    private Integer sort;

    /**
     * 是否启用
     */
    private Boolean enabled = true;

    /**
     * 是否为系统角色 (系统角色不可删除)
     */
    private Boolean system = false;

    /**
     * 数据范围
     */
    private PermissionContext.DataScope dataScope;

    /**
     * 部门ID列表 (用于数据权限限制)
     */
    private Set<Long> deptIds;

    /**
     * 权限ID列表
     */
    private Set<Long> permissionIds;

    /**
     * 角色下的用户数量
     */
    private Integer userCount = 0;

    /**
     * 角色标签
     */
    private Set<String> tags;

    /**
     * 有效期开始时间
     */
    private LocalDateTime validFrom;

    /**
     * 有效期结束时间
     */
    private LocalDateTime validTo;

    /**
     * 子角色列表
     */
    private List<Role> children;

    /**
     * 继承的权限列表
     */
    private Set<String> inheritedPermissions;

    /**
     * 直接权限列表
     */
    private Set<String> directPermissions;

    /**
     * 角色类型枚举
     */
    public enum RoleType {
        /**
         * 系统角色
         */
        SYSTEM,
        /**
         * 业务角色
         */
        BUSINESS,
        /**
         * 岗位角色
         */
        POSITION,
        /**
         * 项目角色
         */
        PROJECT,
        /**
         * 临时角色
         */
        TEMPORARY,
        /**
         * 外部角色
         */
        EXTERNAL
    }

    /**
     * 检查角色是否有效
     */
    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();

        if (validFrom != null && now.isBefore(validFrom)) {
            return false;
        }

        if (validTo != null && now.isAfter(validTo)) {
            return false;
        }

        return enabled;
    }

    /**
     * 检查是否过期
     */
    public boolean isExpired() {
        return validTo != null && LocalDateTime.now().isAfter(validTo);
    }

    /**
     * 检查是否即将过期
     */
    public boolean isExpiringSoon(int daysThreshold) {
        if (validTo == null) {
            return false;
        }

        LocalDateTime threshold = LocalDateTime.now().plusDays(daysThreshold);
        return validTo.isBefore(threshold);
    }

    /**
     * 获取角色有效期剩余天数
     */
    public long getRemainingDays() {
        if (validTo == null) {
            return -1; // 表示永久有效
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(validTo)) {
            return 0; // 已过期
        }

        return now.until(validTo, java.time.temporal.ChronoUnit.DAYS);
    }

    /**
     * 检查是否为父角色
     */
    public boolean isParentRole() {
        return children != null && !children.isEmpty();
    }

    /**
     * 检查是否为叶子角色
     */
    public boolean isLeafRole() {
        return children == null || children.isEmpty();
    }

    /**
     * 获取所有权限 (包括继承的权限)
     */
    public Set<String> getAllPermissions() {
        Set<String> allPermissions = new java.util.HashSet<>();

        if (directPermissions != null) {
            allPermissions.addAll(directPermissions);
        }

        if (inheritedPermissions != null) {
            allPermissions.addAll(inheritedPermissions);
        }

        return allPermissions;
    }

    /**
     * 检查是否拥有指定权限
     */
    public boolean hasPermission(String permissionCode) {
        Set<String> allPermissions = getAllPermissions();
        return allPermissions.contains(permissionCode);
    }

    /**
     * 获取数据范围描述
     */
    public String getDataScopeDescription() {
        if (dataScope == null) {
            return "未知";
        }

        switch (dataScope) {
            case ALL:
                return "全部数据";
            case DEPT_AND_CHILD:
                return "本部门及以下数据";
            case DEPT:
                return "本部门数据";
            case DEPT_AND_SUB:
                return "本部门及子部门数据";
            case SELF:
                return "仅本人数据";
            case CUSTOM:
                return "自定义数据范围";
            default:
                return "未知";
        }
    }

    /**
     * 获取角色类型描述
     */
    public String getTypeDescription() {
        if (type == null) {
            return "未知";
        }

        switch (type) {
            case SYSTEM:
                return "系统角色";
            case BUSINESS:
                return "业务角色";
            case POSITION:
                return "岗位角色";
            case PROJECT:
                return "项目角色";
            case TEMPORARY:
                return "临时角色";
            case EXTERNAL:
                return "外部角色";
            default:
                return "未知";
        }
    }

    /**
     * 获取角色状态描述
     */
    public String getStatusDescription() {
        if (!enabled) {
            return "已禁用";
        }

        if (!isValid()) {
            return "已失效";
        }

        if (isExpired()) {
            return "已过期";
        }

        if (isExpiringSoon(30)) {
            return "即将过期";
        }

        return "正常";
    }

    /**
     * 检查是否需要续期提醒
     */
    public boolean needsRenewalReminder(int daysThreshold) {
        return isExpiringSoon(daysThreshold) && !isExpired();
    }
}

/**
 * 角色权限关联实体
 */
@Data
class RolePermission {
    private Long id;
    private Long roleId;
    private Long permissionId;
    private String permissionCode;
    private Boolean granted;
    private LocalDateTime grantedTime;
    private LocalDateTime revokedTime;
    private Long grantedBy;
    private Long revokedBy;
    private String remark;
}

/**
 * 用户角色关联实体
 */
@Data
class UserRole {
    private Long id;
    private Long userId;
    private Long roleId;
    private String roleCode;
    private LocalDateTime assignedTime;
    private LocalDateTime removedTime;
    private Long assignedBy;
    private Long removedBy;
    private Boolean enabled = true;
    private String remark;
}
