package com.basebackend.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.user.entity.SysRoleListOperation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色列表操作关联Mapper接口
 */
@Mapper
public interface SysRoleListOperationMapper extends BaseMapper<SysRoleListOperation> {

    /**
     * 根据角色ID和资源类型查询列表操作ID列表
     */
    List<Long> selectOperationIdsByRoleIdAndResourceType(@Param("roleId") Long roleId,
                                                           @Param("resourceType") String resourceType);
}
