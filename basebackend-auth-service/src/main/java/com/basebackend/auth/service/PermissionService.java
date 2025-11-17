package com.basebackend.auth.service;

import com.basebackend.auth.dto.PermissionDTO;

import java.util.List;

/**
 * 权限服务接口
 */
public interface PermissionService {

    /**
     * 获取权限列表
     */
    List<PermissionDTO> getPermissionList();

    /**
     * 根据权限类型获取权限列表
     */
    List<PermissionDTO> getPermissionsByType(Integer permissionType);

    /**
     * 根据ID查询权限
     */
    PermissionDTO getById(Long id);

    /**
     * 创建权限
     */
    void create(PermissionDTO permissionDTO);

    /**
     * 更新权限
     */
    void update(PermissionDTO permissionDTO);

    /**
     * 删除权限
     */
    void delete(Long id);

    /**
     * 根据用户ID获取权限列表
     */
    List<PermissionDTO> getPermissionsByUserId(Long userId);

    /**
     * 根据角色ID获取权限列表
     */
    List<PermissionDTO> getPermissionsByRoleId(Long roleId);

    /**
     * 检查权限标识是否唯一
     */
    boolean checkPermissionKeyUnique(String permissionKey, Long permissionId);
}
