package com.basebackend.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.user.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统用户Mapper接口
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 根据用户名查询用"
     */
    SysUser selectByUsername(@Param("username") String username);

    /**
     * 根据用户ID查询用户角色列表
     */
    List<String> selectUserRoles(@Param("userId") Long userId);

    /**
     * 根据用户ID查询用户权限列表
     */
    List<String> selectUserPermissions(@Param("userId") Long userId);

    /**
     * 根据用户ID查询用户菜单列表
     */
    List<String> selectUserMenus(@Param("userId") Long userId);

    /**
     * 根据部门ID查询用户列表
     */
    List<SysUser> selectUsersByDeptId(@Param("deptId") Long deptId);

    /**
     * 根据角色ID查询用户列表
     */
    List<SysUser> selectUsersByRoleId(@Param("roleId") Long roleId);
}
