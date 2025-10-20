package com.basebackend.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.admin.entity.SysRoleResource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色资源关联Mapper
 */
@Mapper
public interface SysRoleResourceMapper extends BaseMapper<SysRoleResource> {

    /**
     * 批量插入角色资源关联
     *
     * @param roleId      角色ID
     * @param resourceIds 资源ID列表
     * @return 插入条数
     */
    int batchInsert(@Param("roleId") Long roleId, @Param("resourceIds") List<Long> resourceIds);

    /**
     * 删除角色的所有资源关联
     *
     * @param roleId 角色ID
     * @return 删除条数
     */
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据资源ID删除关联
     *
     * @param resourceId 资源ID
     * @return 删除条数
     */
    int deleteByResourceId(@Param("resourceId") Long resourceId);
}
