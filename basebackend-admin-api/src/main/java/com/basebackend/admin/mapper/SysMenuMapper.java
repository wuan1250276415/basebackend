package com.basebackend.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.admin.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统菜单Mapper接口
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 根据用户ID查询菜单列表
     */
    List<SysMenu> selectMenusByUserId(@Param("userId") Long userId);

    /**
     * 根据角色ID查询菜单列表
     */
    List<SysMenu> selectMenusByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据父菜单ID查询子菜单数量
     */
    int selectCountByParentId(@Param("parentId") Long parentId);

    /**
     * 查询菜单树列表
     */
    List<SysMenu> selectMenuTreeList();

    /**
     * 根据用户ID查询权限列表
     */
    List<String> selectPermsByUserId(@Param("userId") Long userId);

    /**
     * 根据角色ID查询权限列表
     */
    List<String> selectPermsByRoleId(@Param("roleId") Long roleId);

    /**
     * 检查菜单名称是否唯一
     */
    int checkMenuNameUnique(@Param("menuName") String menuName, @Param("parentId") Long parentId, @Param("menuId") Long menuId);

    /**
     * 根据菜单ID查询子菜单
     */
    List<SysMenu> selectChildrenByMenuId(@Param("menuId") Long menuId);
}
