package com.basebackend.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.user.entity.SysRoleDataPermission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色数据权限配置Mapper接口
 */
@Mapper
public interface SysRoleDataPermissionMapper extends BaseMapper<SysRoleDataPermission> {
}
