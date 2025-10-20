package com.basebackend.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.admin.entity.SysApplicationResource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 应用资源Mapper
 */
@Mapper
public interface SysApplicationResourceMapper extends BaseMapper<SysApplicationResource> {

    /**
     * 根据应用ID查询资源列表
     *
     * @param appId 应用ID
     * @return 资源列表
     */
    List<SysApplicationResource> selectByAppId(@Param("appId") Long appId);

    /**
     * 根据应用ID和用户ID查询用户有权限的资源
     *
     * @param appId  应用ID
     * @param userId 用户ID
     * @return 资源列表
     */
    List<SysApplicationResource> selectResourcesByAppIdAndUserId(@Param("appId") Long appId, @Param("userId") Long userId);

    /**
     * 根据角色ID查询资源ID列表
     *
     * @param roleId 角色ID
     * @return 资源ID列表
     */
    List<Long> selectResourceIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 查询资源树（包含子资源）
     *
     * @param appId 应用ID
     * @return 资源树
     */
    List<SysApplicationResource> selectResourceTree(@Param("appId") Long appId);

    /**
     * 根据资源ID列表查询对应的菜单ID列表
     *
     * @param resourceIds 资源ID列表
     * @return 菜单ID列表
     */
    List<Long> selectMenuIdsByResourceIds(@Param("resourceIds") List<Long> resourceIds);
}
