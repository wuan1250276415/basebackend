package com.basebackend.database.security.context;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 权限上下文
 * 用于存储当前用户的权限信息，支持基于权限的数据可见性控制
 * 
 * 使用示例：
 * <pre>
 * // 设置用户权限
 * PermissionContext.setPermissions(Set.of("VIEW_SENSITIVE_DATA", "VIEW_PHONE"));
 * 
 * // 检查权限
 * if (PermissionContext.hasPermission("VIEW_SENSITIVE_DATA")) {
 *     // 显示敏感数据
 * }
 * 
 * // 清除权限上下文
 * PermissionContext.clear();
 * </pre>
 */
@Slf4j
public class PermissionContext {
    
    /**
     * 查看所有敏感数据的权限
     */
    public static final String VIEW_SENSITIVE_DATA = "VIEW_SENSITIVE_DATA";
    
    /**
     * 查看手机号的权限
     */
    public static final String VIEW_PHONE = "VIEW_PHONE";
    
    /**
     * 查看身份证号的权限
     */
    public static final String VIEW_ID_CARD = "VIEW_ID_CARD";
    
    /**
     * 查看银行卡号的权限
     */
    public static final String VIEW_BANK_CARD = "VIEW_BANK_CARD";
    
    /**
     * 查看邮箱的权限
     */
    public static final String VIEW_EMAIL = "VIEW_EMAIL";
    
    /**
     * 查看地址的权限
     */
    public static final String VIEW_ADDRESS = "VIEW_ADDRESS";
    
    /**
     * 存储当前线程的权限集合
     */
    private static final ThreadLocal<Set<String>> PERMISSIONS = ThreadLocal.withInitial(HashSet::new);
    
    /**
     * 存储当前线程的用户ID
     */
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    
    /**
     * 设置当前用户的权限
     * 
     * @param permissions 权限集合
     */
    public static void setPermissions(Set<String> permissions) {
        if (permissions == null) {
            PERMISSIONS.set(new HashSet<>());
        } else {
            PERMISSIONS.set(new HashSet<>(permissions));
        }
        log.debug("Set permissions for current thread: {}", permissions);
    }
    
    /**
     * 添加权限
     * 
     * @param permission 权限代码
     */
    public static void addPermission(String permission) {
        if (permission != null && !permission.isEmpty()) {
            PERMISSIONS.get().add(permission);
            log.debug("Added permission: {}", permission);
        }
    }
    
    /**
     * 移除权限
     * 
     * @param permission 权限代码
     */
    public static void removePermission(String permission) {
        if (permission != null) {
            PERMISSIONS.get().remove(permission);
            log.debug("Removed permission: {}", permission);
        }
    }
    
    /**
     * 检查是否拥有指定权限
     * 
     * @param permission 权限代码
     * @return true表示拥有权限，false表示没有权限
     */
    public static boolean hasPermission(String permission) {
        if (permission == null || permission.isEmpty()) {
            return false;
        }
        
        Set<String> currentPermissions = PERMISSIONS.get();
        
        // 如果拥有查看所有敏感数据的权限，则返回true
        if (currentPermissions.contains(VIEW_SENSITIVE_DATA)) {
            return true;
        }
        
        // 检查是否拥有特定权限
        return currentPermissions.contains(permission);
    }
    
    /**
     * 检查是否拥有任意一个权限
     * 
     * @param permissions 权限代码数组
     * @return true表示至少拥有一个权限，false表示没有任何权限
     */
    public static boolean hasAnyPermission(String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return false;
        }
        
        for (String permission : permissions) {
            if (hasPermission(permission)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 检查是否拥有所有权限
     * 
     * @param permissions 权限代码数组
     * @return true表示拥有所有权限，false表示缺少某些权限
     */
    public static boolean hasAllPermissions(String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return true;
        }
        
        for (String permission : permissions) {
            if (!hasPermission(permission)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 获取当前用户的所有权限
     * 
     * @return 权限集合（不可修改）
     */
    public static Set<String> getPermissions() {
        return Collections.unmodifiableSet(PERMISSIONS.get());
    }
    
    /**
     * 设置当前用户ID
     * 
     * @param userId 用户ID
     */
    public static void setUserId(Long userId) {
        USER_ID.set(userId);
        log.debug("Set user ID for current thread: {}", userId);
    }
    
    /**
     * 获取当前用户ID
     * 
     * @return 用户ID，如果未设置则返回null
     */
    public static Long getUserId() {
        return USER_ID.get();
    }
    
    /**
     * 清除权限上下文
     * 注意：在请求结束时必须调用此方法，避免内存泄漏
     */
    public static void clear() {
        PERMISSIONS.remove();
        USER_ID.remove();
        log.debug("Cleared permission context for current thread");
    }
    
    /**
     * 检查权限上下文是否为空
     * 
     * @return true表示没有设置任何权限，false表示已设置权限
     */
    public static boolean isEmpty() {
        return PERMISSIONS.get().isEmpty();
    }
}
