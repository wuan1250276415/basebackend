package com.basebackend.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.admin.dto.RoleDTO;
import com.basebackend.admin.entity.SysRole;
import com.basebackend.admin.entity.SysRoleMenu;
import com.basebackend.admin.entity.SysRolePermission;
import com.basebackend.admin.mapper.SysRoleMapper;
import com.basebackend.admin.mapper.SysRoleMenuMapper;
import com.basebackend.admin.mapper.SysRolePermissionMapper;
import com.basebackend.admin.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysRolePermissionMapper rolePermissionMapper;

    @Override
    public Page<RoleDTO> page(String roleName, String roleKey, Integer status, int current, int size) {
        log.info("分页查询角色列表: current={}, size={}", current, size);

        Page<SysRole> page = new Page<>(current, size);
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();

        // 构建查询条件
        if (StrUtil.isNotBlank(roleName)) {
            wrapper.like(SysRole::getRoleName, roleName);
        }
        if (StrUtil.isNotBlank(roleKey)) {
            wrapper.like(SysRole::getRoleKey, roleKey);
        }
        if (status != null) {
            wrapper.eq(SysRole::getStatus, status);
        }

        wrapper.orderByAsc(SysRole::getRoleSort);
        Page<SysRole> rolePage = roleMapper.selectPage(page, wrapper);

        // 转换为DTO
        List<RoleDTO> roleDTOs = rolePage.getRecords().stream()
                .map(this::convertToDTO)
                .collect(java.util.stream.Collectors.toList());

        Page<RoleDTO> result = new Page<>(current, size);
        result.setRecords(roleDTOs);
        result.setTotal(rolePage.getTotal());
        result.setPages(rolePage.getPages());

        return result;
    }

    @Override
    public RoleDTO getById(Long id) {
        log.info("根据ID查询角色: {}", id);
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }
        return convertToDTO(role);
    }

    @Override
    @Transactional
    public void create(RoleDTO roleDTO) {
        log.info("创建角色: {}", roleDTO.getRoleName());

        // 检查角色名称是否唯一
        if (!checkRoleNameUnique(roleDTO.getRoleName(), null)) {
            throw new RuntimeException("角色名称已存在");
        }

        // 检查角色标识是否唯一
        if (!checkRoleKeyUnique(roleDTO.getRoleKey(), null)) {
            throw new RuntimeException("角色标识已存在");
        }

        // 创建角色
        SysRole role = new SysRole();
        BeanUtil.copyProperties(roleDTO, role);
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        role.setCreateBy(1L); // 临时硬编码
        role.setUpdateBy(1L); // 临时硬编码

        roleMapper.insert(role);

        // 分配菜单
        if (roleDTO.getMenuIds() != null && !roleDTO.getMenuIds().isEmpty()) {
            assignMenus(role.getId(), roleDTO.getMenuIds());
        }

        // 分配权限
        if (roleDTO.getPermissionIds() != null && !roleDTO.getPermissionIds().isEmpty()) {
            assignPermissions(role.getId(), roleDTO.getPermissionIds());
        }

        log.info("角色创建成功: {}", role.getRoleName());
    }

    @Override
    @Transactional
    public void update(RoleDTO roleDTO) {
        log.info("更新角色: {}", roleDTO.getId());

        SysRole role = roleMapper.selectById(roleDTO.getId());
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }

        // 检查角色名称是否唯一
        if (!checkRoleNameUnique(roleDTO.getRoleName(), roleDTO.getId())) {
            throw new RuntimeException("角色名称已存在");
        }

        // 检查角色标识是否唯一
        if (!checkRoleKeyUnique(roleDTO.getRoleKey(), roleDTO.getId())) {
            throw new RuntimeException("角色标识已存在");
        }

        // 更新角色信息
        BeanUtil.copyProperties(roleDTO, role);
        role.setUpdateTime(LocalDateTime.now());
        role.setUpdateBy(1L); // 临时硬编码

        roleMapper.updateById(role);

        // 更新菜单
        if (roleDTO.getMenuIds() != null) {
            assignMenus(role.getId(), roleDTO.getMenuIds());
        }

        // 更新权限
        if (roleDTO.getPermissionIds() != null) {
            assignPermissions(role.getId(), roleDTO.getPermissionIds());
        }

        log.info("角色更新成功: {}", role.getRoleName());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.info("删除角色: {}", id);

        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }

        // 逻辑删除
        roleMapper.deleteById(id);

        // 删除角色菜单关联
        LambdaQueryWrapper<SysRoleMenu> menuWrapper = new LambdaQueryWrapper<>();
        menuWrapper.eq(SysRoleMenu::getRoleId, id);
        roleMenuMapper.delete(menuWrapper);

        // 删除角色权限关联
        LambdaQueryWrapper<SysRolePermission> permissionWrapper = new LambdaQueryWrapper<>();
        permissionWrapper.eq(SysRolePermission::getRoleId, id);
        rolePermissionMapper.delete(permissionWrapper);

        log.info("角色删除成功: {}", role.getRoleName());
    }

    @Override
    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        log.info("分配角色菜单: roleId={}, menuIds={}", roleId, menuIds);

        // 删除原有菜单关联
        LambdaQueryWrapper<SysRoleMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleMenu::getRoleId, roleId);
        roleMenuMapper.delete(wrapper);

        // 添加新菜单关联
        if (menuIds != null && !menuIds.isEmpty()) {
            for (Long menuId : menuIds) {
                SysRoleMenu roleMenu = new SysRoleMenu();
                roleMenu.setRoleId(roleId);
                roleMenu.setMenuId(menuId);
                roleMenu.setCreateTime(LocalDateTime.now());
                roleMenu.setCreateBy(1L); // 临时硬编码
                roleMenuMapper.insert(roleMenu);
            }
        }

        log.info("角色菜单分配成功");
    }

    @Override
    @Transactional
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        log.info("分配角色权限: roleId={}, permissionIds={}", roleId, permissionIds);

        // 删除原有权限关联
        LambdaQueryWrapper<SysRolePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRolePermission::getRoleId, roleId);
        rolePermissionMapper.delete(wrapper);

        // 添加新权限关联
        if (permissionIds != null && !permissionIds.isEmpty()) {
            for (Long permissionId : permissionIds) {
                SysRolePermission rolePermission = new SysRolePermission();
                rolePermission.setRoleId(roleId);
                rolePermission.setPermissionId(permissionId);
                rolePermission.setCreateTime(LocalDateTime.now());
                rolePermission.setCreateBy(1L); // 临时硬编码
                rolePermissionMapper.insert(rolePermission);
            }
        }

        log.info("角色权限分配成功");
    }

    @Override
    public List<Long> getRoleMenus(Long roleId) {
        LambdaQueryWrapper<SysRoleMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleMenu::getRoleId, roleId);
        List<SysRoleMenu> roleMenus = roleMenuMapper.selectList(wrapper);

        return roleMenus.stream()
                .map(SysRoleMenu::getMenuId)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<Long> getRolePermissions(Long roleId) {
        LambdaQueryWrapper<SysRolePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRolePermission::getRoleId, roleId);
        List<SysRolePermission> rolePermissions = rolePermissionMapper.selectList(wrapper);

        return rolePermissions.stream()
                .map(SysRolePermission::getPermissionId)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public boolean checkRoleNameUnique(String roleName, Long roleId) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getRoleName, roleName);
        if (roleId != null) {
            wrapper.ne(SysRole::getId, roleId);
        }
        return roleMapper.selectCount(wrapper) == 0;
    }

    @Override
    public boolean checkRoleKeyUnique(String roleKey, Long roleId) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getRoleKey, roleKey);
        if (roleId != null) {
            wrapper.ne(SysRole::getId, roleId);
        }
        return roleMapper.selectCount(wrapper) == 0;
    }

    /**
     * 转换为DTO
     */
    private RoleDTO convertToDTO(SysRole role) {
        RoleDTO dto = new RoleDTO();
        BeanUtil.copyProperties(role, dto);

        // 设置菜单ID列表
        List<Long> menuIds = getRoleMenus(role.getId());
        dto.setMenuIds(menuIds);

        // 设置权限ID列表
        List<Long> permissionIds = getRolePermissions(role.getId());
        dto.setPermissionIds(permissionIds);

        return dto;
    }
}
