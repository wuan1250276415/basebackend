package com.basebackend.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.system.entity.SysRoleResource;
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
     */
    int batchInsert(@Param("roleId") Long roleId, @Param("resourceIds") List<Long> resourceIds);

    /**
     * 删除角色的所有资源关联
     */
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据资源ID删除关联
     */
    int deleteByResourceId(@Param("resourceId") Long resourceId);
}
