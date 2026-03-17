package com.basebackend.workflow.engine;

/**
 * 审批权限校验接口
 * <p>
 * 由消费方实现，提供"用户是否具有某角色"的判断逻辑。
 * 若 {@link ProcessEngine} 未注入此接口，审批操作将跳过权限校验。
 *
 * <h3>典型用法（Spring 环境）</h3>
 * <pre>
 * {@code
 * @Bean
 * public RoleChecker roleChecker(UserRoleService userRoleService) {
 *     return (user, role) -> userRoleService.hasRole(user, role);
 * }
 * }
 * </pre>
 */
@FunctionalInterface
public interface RoleChecker {

    /**
     * 检查用户是否具有指定角色
     *
     * @param user 用户名（与 {@code ProcessEngine#approve} 等方法中的 approver 一致）
     * @param role 节点要求的角色（与 {@link com.basebackend.workflow.model.ProcessNode#getAssigneeRole()} 一致）
     * @return 具有角色返回 {@code true}，否则返回 {@code false}
     */
    boolean hasRole(String user, String role);
}
