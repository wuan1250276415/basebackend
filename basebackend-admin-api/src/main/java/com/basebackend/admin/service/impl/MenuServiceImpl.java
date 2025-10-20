package com.basebackend.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.basebackend.admin.dto.MenuDTO;
import com.basebackend.admin.entity.SysMenu;
import com.basebackend.admin.entity.SysRoleMenu;
import com.basebackend.admin.mapper.SysMenuMapper;
import com.basebackend.admin.mapper.SysRoleMenuMapper;
import com.basebackend.admin.service.MenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 菜单服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;

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

        // 自动为管理员角色（ID=1）分配新创建的菜单权限
        try {
            SysRoleMenu roleMenu = new SysRoleMenu();
            roleMenu.setRoleId(1L); // 管理员角色ID
            roleMenu.setMenuId(menu.getId());
            roleMenu.setCreateTime(LocalDateTime.now());
            roleMenu.setCreateBy(1L);
            roleMenuMapper.insert(roleMenu);
            log.info("已自动为管理员角色分配菜单权限: menuId={}", menu.getId());
        } catch (Exception e) {
            log.warn("为管理员角色分配菜单权限失败（可能已存在）: {}", e.getMessage());
            // 不影响菜单创建，只记录警告
        }

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
        List<SysMenu> userMenus = menuMapper.selectMenusByUserId(userId);
        if (userMenus == null || userMenus.isEmpty()) {
            return Collections.emptyList();
        }

        List<SysMenu> allMenus = menuMapper.selectMenuTreeList();
        Map<Long, SysMenu> menuMap = allMenus.stream()
                .collect(Collectors.toMap(SysMenu::getId, menu -> menu, (a, b) -> a, HashMap::new));

        Set<Long> requiredMenuIds = userMenus.stream()
                .map(SysMenu::getId)
                .collect(Collectors.toCollection(HashSet::new));

        for (SysMenu menu : userMenus) {
            collectParentIds(menu.getParentId(), requiredMenuIds, menuMap);
        }

        List<SysMenu> filteredMenus = allMenus.stream()
                .filter(menu -> requiredMenuIds.contains(menu.getId()))
                .collect(Collectors.toList());

        return buildMenuTree(filteredMenus, 0L);
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

    /**
     * 递归收集父节点ID，确保菜单树包含目录节点
     */
    private void collectParentIds(Long parentId, Set<Long> requiredMenuIds, Map<Long, SysMenu> menuMap) {
        if (parentId == null || parentId == 0) {
            return;
        }
        if (requiredMenuIds.add(parentId)) {
            SysMenu parentMenu = menuMap.get(parentId);
            if (parentMenu != null) {
                collectParentIds(parentMenu.getParentId(), requiredMenuIds, menuMap);
            }
        }
    }
}
