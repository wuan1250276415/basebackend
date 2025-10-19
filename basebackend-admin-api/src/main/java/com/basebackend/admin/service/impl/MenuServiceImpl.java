package com.basebackend.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.basebackend.admin.dto.MenuDTO;
import com.basebackend.admin.entity.SysMenu;
import com.basebackend.admin.mapper.SysMenuMapper;
import com.basebackend.admin.service.MenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜单服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final SysMenuMapper menuMapper;

    @Override
    public List<MenuDTO> getMenuTree() {
        log.info("获取菜单树");
        List<SysMenu> menus = menuMapper.selectMenuTreeList();
        return buildMenuTree(menus, 0L);
    }

    @Override
    public List<MenuDTO> getMenuList() {
        log.info("获取菜单列表");
        List<SysMenu> menus = menuMapper.selectList(null);
        return menus.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public MenuDTO getById(Long id) {
        log.info("根据ID查询菜单: {}", id);
        SysMenu menu = menuMapper.selectById(id);
        if (menu == null) {
            throw new RuntimeException("菜单不存在");
        }
        return convertToDTO(menu);
    }

    @Override
    @Transactional
    public void create(MenuDTO menuDTO) {
        log.info("创建菜单: {}", menuDTO.getMenuName());

        // 检查菜单名称是否唯一
        if (!checkMenuNameUnique(menuDTO.getMenuName(), menuDTO.getParentId(), null)) {
            throw new RuntimeException("菜单名称已存在");
        }

        // 创建菜单
        SysMenu menu = new SysMenu();
        BeanUtil.copyProperties(menuDTO, menu);
        menu.setCreateTime(LocalDateTime.now());
        menu.setUpdateTime(LocalDateTime.now());
        menu.setCreateBy(1L); // 临时硬编码
        menu.setUpdateBy(1L); // 临时硬编码

        menuMapper.insert(menu);

        log.info("菜单创建成功: {}", menu.getMenuName());
    }

    @Override
    @Transactional
    public void update(MenuDTO menuDTO) {
        log.info("更新菜单: {}", menuDTO.getId());

        SysMenu menu = menuMapper.selectById(menuDTO.getId());
        if (menu == null) {
            throw new RuntimeException("菜单不存在");
        }

        // 检查菜单名称是否唯一
        if (!checkMenuNameUnique(menuDTO.getMenuName(), menuDTO.getParentId(), menuDTO.getId())) {
            throw new RuntimeException("菜单名称已存在");
        }

        // 更新菜单信息
        BeanUtil.copyProperties(menuDTO, menu);
        menu.setUpdateTime(LocalDateTime.now());
        menu.setUpdateBy(1L); // 临时硬编码

        menuMapper.updateById(menu);

        log.info("菜单更新成功: {}", menu.getMenuName());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.info("删除菜单: {}", id);

        SysMenu menu = menuMapper.selectById(id);
        if (menu == null) {
            throw new RuntimeException("菜单不存在");
        }

        // 检查是否有子菜单
        if (hasChildren(id)) {
            throw new RuntimeException("存在子菜单，不允许删除");
        }

        // 逻辑删除
        menuMapper.deleteById(id);

        log.info("菜单删除成功: {}", menu.getMenuName());
    }

    @Override
    public List<MenuDTO> getRoutes() {
        log.info("获取前端路由");
        List<SysMenu> menus = menuMapper.selectMenuTreeList();
        return buildMenuTree(menus, 0L);
    }

    @Override
    public List<MenuDTO> getMenuTreeByUserId(Long userId) {
        log.info("根据用户ID获取菜单树: {}", userId);
        List<SysMenu> menus = menuMapper.selectMenusByUserId(userId);
        return buildMenuTree(menus, 0L);
    }

    @Override
    public boolean checkMenuNameUnique(String menuName, Long parentId, Long menuId) {
        // 这里简化处理，实际应该查询数据库
        return true;
    }

    @Override
    public boolean hasChildren(Long menuId) {
        return menuMapper.selectCountByParentId(menuId) > 0;
    }

    /**
     * 构建菜单树
     */
    private List<MenuDTO> buildMenuTree(List<SysMenu> menus, Long parentId) {
        List<MenuDTO> tree = new ArrayList<>();
        
        for (SysMenu menu : menus) {
            if (parentId.equals(menu.getParentId())) {
                MenuDTO dto = convertToDTO(menu);
                List<MenuDTO> children = buildMenuTree(menus, menu.getId());
                if (!children.isEmpty()) {
                    dto.setChildren(children);
                }
                tree.add(dto);
            }
        }
        
        return tree;
    }

    /**
     * 转换为DTO
     */
    private MenuDTO convertToDTO(SysMenu menu) {
        MenuDTO dto = new MenuDTO();
        BeanUtil.copyProperties(menu, dto);
        return dto;
    }
}
