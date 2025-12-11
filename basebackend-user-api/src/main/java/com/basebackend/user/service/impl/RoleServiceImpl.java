package com.basebackend.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.user.dto.RoleDTO;
import com.basebackend.user.entity.*;
import com.basebackend.user.mapper.*;
import com.basebackend.user.service.RoleService;
import com.basebackend.user.util.AuditHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 角色服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysRoleResourceMapper roleResourceMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleListOperationMapper roleListOperationMapper;
    private final SysRoleDataPermissionMapper roleDataPermissionMapper;
    private final AuditHelper auditHelper;

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
        // 使用AuditHelper设置审计字段，从UserContextHolder获取当前用户ID
        auditHelper.setCreateAuditFields(role);

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
        // 使用AuditHelper设置审计字段
        auditHelper.setUpdateAuditFields(role);

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
        LambdaQueryWrapper<SysRoleResource> menuWrapper = new LambdaQueryWrapper<>();
        menuWrapper.eq(SysRoleResource::getRoleId, id);
        roleResourceMapper.delete(menuWrapper);

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
        LambdaQueryWrapper<SysRoleResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleResource::getRoleId, roleId);
        roleResourceMapper.delete(wrapper);

        // 添加新菜单关联
        if (menuIds != null && !menuIds.isEmpty()) {
            for (Long menuId : menuIds) {
                SysRoleResource roleMenu = new SysRoleResource();
                roleMenu.setRoleId(roleId);
                roleMenu.setResourceId(menuId);
                roleMenu.setCreateTime(LocalDateTime.now());
                roleResourceMapper.insert(roleMenu);
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
            Long currentUserId = auditHelper.getCurrentUserId();
            for (Long permissionId : permissionIds) {
                SysRolePermission rolePermission = new SysRolePermission();
                rolePermission.setRoleId(roleId);
                rolePermission.setPermissionId(permissionId);
                rolePermission.setCreateTime(LocalDateTime.now());
                rolePermission.setCreateBy(currentUserId);
                rolePermissionMapper.insert(rolePermission);
            }
        }

        log.info("角色权限分配成功");
    }

    @Override
    public List<Long> getRoleMenus(Long roleId) {
        LambdaQueryWrapper<SysRoleResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleResource::getRoleId, roleId);
        List<SysRoleResource> roleResources = roleResourceMapper.selectList(wrapper);

        return roleResources.stream()
                .map(SysRoleResource::getResourceId)
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

    @Override
    public List<SysRole> getRoleTree(Long appId) {
        log.info("获取角色树: appId={}", appId);

        // 查询所有角色
        List<SysRole> allRoles = roleMapper.selectRolesByAppId(appId);

        // 构建树形结构
        return buildRoleTree(allRoles, 0L);
    }

    @Override
    @Transactional
    public void assignResources(Long roleId, List<Long> resourceIds) {
        log.info("分配角色资源: roleId={}, resourceIds={}", roleId, resourceIds);

        // 删除原有资源关联
        LambdaQueryWrapper<SysRoleResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleResource::getRoleId, roleId);
        roleResourceMapper.delete(wrapper);

        // 添加新资源关联
        if (resourceIds != null && !resourceIds.isEmpty()) {
            for (Long resourceId : resourceIds) {
                SysRoleResource roleResource = new SysRoleResource();
                roleResource.setRoleId(roleId);
                roleResource.setResourceId(resourceId);
                roleResource.setCreateTime(LocalDateTime.now());
                roleResourceMapper.insert(roleResource);
            }
        }

        log.info("角色资源分配成功");
    }

    @Override
    @Transactional
    public void configureListOperations(Long roleId, String resourceType, List<Long> operationIds) {
        log.info("配置列表操作权限: roleId={}, resourceType={}, operationIds={}", roleId, resourceType, operationIds);

        // 删除该角色该资源类型的原有列表操作权限
        LambdaQueryWrapper<SysRoleListOperation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleListOperation::getRoleId, roleId)
                .eq(SysRoleListOperation::getResourceType, resourceType);
        roleListOperationMapper.delete(wrapper);

        // 添加新列表操作权限
        if (operationIds != null && !operationIds.isEmpty()) {
            for (Long operationId : operationIds) {
                SysRoleListOperation roleListOperation = new SysRoleListOperation();
                roleListOperation.setRoleId(roleId);
                roleListOperation.setOperationId(operationId);
                roleListOperation.setResourceType(resourceType);
                roleListOperation.setCreateTime(LocalDateTime.now());
                roleListOperationMapper.insert(roleListOperation);
            }
        }

        log.info("列表操作权限配置成功");
    }

    @Override
    @Transactional
    public void configureDataPermissions(Long roleId, String filterRule) {
        log.info("配置数据权限: roleId={}, filterRule={}", roleId, filterRule);

        // 查询或创建数据权限配置
        LambdaQueryWrapper<SysRoleDataPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleDataPermission::getRoleId, roleId);

        SysRoleDataPermission dataPermission = roleDataPermissionMapper.selectOne(wrapper);
        if (dataPermission == null) {
            dataPermission = new SysRoleDataPermission();
            dataPermission.setRoleId(roleId);
            dataPermission.setResourceType("all");
            dataPermission.setPermissionName("数据权限");
            dataPermission.setFilterType("custom");
            dataPermission.setFilterRule(filterRule);
            dataPermission.setStatus(1);
            dataPermission.setCreateTime(LocalDateTime.now());
            roleDataPermissionMapper.insert(dataPermission);
        } else {
            dataPermission.setFilterRule(filterRule);
            dataPermission.setUpdateTime(LocalDateTime.now());
            roleDataPermissionMapper.updateById(dataPermission);
        }

        log.info("数据权限配置成功");
    }

    @Override
    public List<SysUser> getRoleUsers(Long roleId, String username) {
        log.info("获取角色用户列表: roleId={}, username={}", roleId, username);
        return roleMapper.selectUsersByRoleId(roleId, username);
    }

    @Override
    @Transactional
    public void assignUsersToRole(Long roleId, List<Long> userIds) {
        log.info("批量关联用户到角色: roleId={}, userIds={}", roleId, userIds);

        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        for (Long userId : userIds) {
            // 检查是否已存在关联
            LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUserRole::getUserId, userId)
                    .eq(SysUserRole::getRoleId, roleId);

            if (userRoleMapper.selectCount(wrapper) == 0) {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(userId);
                userRole.setRoleId(roleId);
                userRole.setCreateTime(LocalDateTime.now());
                userRole.setCreateBy(auditHelper.getCurrentUserId());
                userRoleMapper.insert(userRole);
            }
        }

        log.info("用户关联成功");
    }

    @Override
    @Transactional
    public void removeUserFromRole(Long roleId, Long userId) {
        log.info("取消用户角色关联: roleId={}, userId={}", roleId, userId);

        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getUserId, userId)
                .eq(SysUserRole::getRoleId, roleId);
        userRoleMapper.delete(wrapper);

        log.info("用户角色关联已取消");
    }

    @Override
    public List<Long> getRoleResources(Long roleId) {
        log.info("获取角色资源列表: roleId={}", roleId);

        return roleMapper.selectResourceIdsByRoleId(roleId);
    }

    @Override
    public List<Long> getRoleListOperations(Long roleId, String resourceType) {
        log.info("获取角色列表操作权限: roleId={}, resourceType={}", roleId, resourceType);

        return roleListOperationMapper.selectOperationIdsByRoleIdAndResourceType(roleId, resourceType);
    }

    /**
     * 构建角色树形结构
     */
    private List<SysRole> buildRoleTree(List<SysRole> allRoles, Long parentId) {
        List<SysRole> treeNodes = new ArrayList<>();

        for (SysRole role : allRoles) {
            Long roleParentId = role.getParentId() != null ? role.getParentId() : 0L;

            if (roleParentId.equals(parentId)) {
                // 递归查找子节点
                List<SysRole> children = buildRoleTree(allRoles, role.getId());
                role.setChildren(children);
                treeNodes.add(role);
            }
        }

        return treeNodes;
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
