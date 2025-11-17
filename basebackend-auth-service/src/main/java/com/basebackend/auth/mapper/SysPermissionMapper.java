package com.basebackend.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.auth.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统权限Mapper接口
 */
@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    /**
     * 根据权限标识查询权限
     */
    SysPermission selectByPermissionKey(@Param("permissionKey") String permissionKey);

    /**
     * 根据用户ID查询权限列表
     */
    List<SysPermission> selectPermissionsByUserId(@Param("userId") Long userId);

    /**
     * 根据角色ID查询权限列表
     */
    List<SysPermission> selectPermissionsByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据权限类型查询权限列表
     */
    List<SysPermission> selectPermissionsByType(@Param("permissionType") Integer permissionType);

    /**
     * 检查权限标识是否唯一
     */
    int checkPermissionKeyUnique(@Param("permissionKey") String permissionKey, @Param("permissionId") Long permissionId);
}
