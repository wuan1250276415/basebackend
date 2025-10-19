package com.basebackend.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.admin.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统角色Mapper接口
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 根据角色标识查询角色
     */
    SysRole selectByRoleKey(@Param("roleKey") String roleKey);

    /**
     * 根据用户ID查询角色列表
     */
    List<SysRole> selectRolesByUserId(@Param("userId") Long userId);

    /**
     * 根据角色ID查询菜单ID列表
     */
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据角色ID查询权限ID列表
     */
    List<Long> selectPermissionIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据角色ID查询用户ID列表
     */
    List<Long> selectUserIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 检查角色名称是否唯一
     */
    int checkRoleNameUnique(@Param("roleName") String roleName, @Param("roleId") Long roleId);

    /**
     * 检查角色标识是否唯一
     */
    int checkRoleKeyUnique(@Param("roleKey") String roleKey, @Param("roleId") Long roleId);
}
