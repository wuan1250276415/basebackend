package com.basebackend.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.basebackend.system.dto.PermissionDTO;
import com.basebackend.system.entity.SysPermission;
import com.basebackend.system.mapper.SysPermissionMapper;
import com.basebackend.system.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 权限服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final SysPermissionMapper permissionMapper;

    @Override
    public List<PermissionDTO> getPermissionList() {
        log.info("获取权限列表");
        List<SysPermission> permissions = permissionMapper.selectList(null);
        return permissions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PermissionDTO> getPermissionsByType(Integer permissionType) {
        log.info("根据权限类型获取权限列表: {}", permissionType);
        List<SysPermission> permissions = permissionMapper.selectPermissionsByType(permissionType);
        return permissions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public PermissionDTO getById(Long id) {
        log.info("根据ID查询权限: {}", id);
        SysPermission permission = permissionMapper.selectById(id);
        if (permission == null) {
            throw new RuntimeException("权限不存在");
        }
        return convertToDTO(permission);
    }

    @Override
    @Transactional
    public void create(PermissionDTO permissionDTO) {
        log.info("创建权限: {}", permissionDTO.getPermissionName());

        // 检查权限标识是否唯一
        if (!checkPermissionKeyUnique(permissionDTO.getPermissionKey(), null)) {
            throw new RuntimeException("权限标识已存在");
        }

        // 创建权限
        SysPermission permission = new SysPermission();
        BeanUtil.copyProperties(permissionDTO, permission);
        permission.setCreateTime(LocalDateTime.now());
        permission.setUpdateTime(LocalDateTime.now());
        permission.setCreateBy(1L); // 临时硬编码
        permission.setUpdateBy(1L); // 临时硬编码

        permissionMapper.insert(permission);

        log.info("权限创建成功: {}", permission.getPermissionName());
    }

    @Override
    @Transactional
    public void update(PermissionDTO permissionDTO) {
        log.info("更新权限: {}", permissionDTO.getId());

        SysPermission permission = permissionMapper.selectById(permissionDTO.getId());
        if (permission == null) {
            throw new RuntimeException("权限不存在");
        }

        // 检查权限标识是否唯一
        if (!checkPermissionKeyUnique(permissionDTO.getPermissionKey(), permissionDTO.getId())) {
            throw new RuntimeException("权限标识已存在");
        }

        // 更新权限信息
        BeanUtil.copyProperties(permissionDTO, permission);
        permission.setUpdateTime(LocalDateTime.now());
        permission.setUpdateBy(1L); // 临时硬编码

        permissionMapper.updateById(permission);

        log.info("权限更新成功: {}", permission.getPermissionName());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.info("删除权限: {}", id);

        SysPermission permission = permissionMapper.selectById(id);
        if (permission == null) {
            throw new RuntimeException("权限不存在");
        }

        // 逻辑删除
        permissionMapper.deleteById(id);

        log.info("权限删除成功: {}", permission.getPermissionName());
    }

    @Override
    public List<PermissionDTO> getPermissionsByUserId(Long userId) {
        log.info("根据用户ID获取权限列表: {}", userId);
        List<SysPermission> permissions = permissionMapper.selectPermissionsByUserId(userId);
        return permissions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PermissionDTO> getPermissionsByRoleId(Long roleId) {
        log.info("根据角色ID获取权限列表: {}", roleId);
        List<SysPermission> permissions = permissionMapper.selectPermissionsByRoleId(roleId);
        return permissions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public boolean checkPermissionKeyUnique(String permissionKey, Long permissionId) {
        return permissionMapper.checkPermissionKeyUnique(permissionKey, permissionId) == 0;
    }

    /**
     * 转换为DTO
     */
    private PermissionDTO convertToDTO(SysPermission permission) {
        PermissionDTO dto = new PermissionDTO();
        BeanUtil.copyProperties(permission, dto);
        return dto;
    }
}
