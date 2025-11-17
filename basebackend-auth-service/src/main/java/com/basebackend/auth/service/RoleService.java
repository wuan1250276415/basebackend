package com.basebackend.auth.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.auth.dto.RoleDTO;
import com.basebackend.auth.entity.SysRole;

import java.util.List;

/**
 * 角色服务接口
 */
public interface RoleService {

    /**
     * 分页查询角色列表
     */
    Page<RoleDTO> page(String roleName, String roleKey, Integer status, int current, int size);

    /**
     * 根据ID查询角色
     */
    RoleDTO getById(Long id);

    /**
     * 根据应用ID获取角色树
     */
    List<SysRole> getRoleTree(Long appId);

    /**
     * 创建角色
     */
    void create(RoleDTO roleDTO);

    /**
     * 更新角色
     */
    void update(RoleDTO roleDTO);

    /**
     * 删除角色
     */
    void delete(Long id);

    /**
     * 分配菜单
     */
    void assignMenus(Long roleId, List<Long> menuIds);

    /**
     * 分配权限
     */
    void assignPermissions(Long roleId, List<Long> permissionIds);

    /**
     * 批量关联用户到角色
     */
    void assignUsersToRole(Long roleId, List<Long> userIds);

    /**
     * 取消用户角色关联
     */
    void removeUserFromRole(Long roleId, Long userId);

    /**
     * 获取角色菜单列表
     */
    List<Long> getRoleMenus(Long roleId);

    /**
     * 获取角色权限列表
     */
    List<Long> getRolePermissions(Long roleId);

    /**
     * 检查角色名称是否唯一
     */
    boolean checkRoleNameUnique(String roleName, Long roleId);

    /**
     * 检查角色标识是否唯一
     */
    boolean checkRoleKeyUnique(String roleKey, Long roleId);
}
