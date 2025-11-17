package com.basebackend.system.service;

import com.basebackend.system.dto.MenuDTO;

import java.util.List;

/**
 * 菜单服务接口
 */
public interface MenuService {

    /**
     * 获取菜单树
     */
    List<MenuDTO> getMenuTree();

    /**
     * 获取菜单列表
     */
    List<MenuDTO> getMenuList();

    /**
     * 根据ID查询菜单
     */
    MenuDTO getById(Long id);

    /**
     * 创建菜单
     */
    void create(MenuDTO menuDTO);

    /**
     * 更新菜单
     */
    void update(MenuDTO menuDTO);

    /**
     * 删除菜单
     */
    void delete(Long id);

    /**
     * 获取前端路由
     */
    List<MenuDTO> getRoutes();

    /**
     * 根据用户ID获取菜单树
     */
    List<MenuDTO> getMenuTreeByUserId(Long userId);

    /**
     * 检查菜单名称是否唯一
     */
    boolean checkMenuNameUnique(String menuName, Long parentId, Long menuId);
}
