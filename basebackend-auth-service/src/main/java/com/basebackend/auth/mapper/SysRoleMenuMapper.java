package com.basebackend.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.auth.entity.SysRoleMenu;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色菜单关联Mapper接口
 */
@Mapper
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {
}
