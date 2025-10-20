package com.basebackend.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.admin.dto.ApplicationResourceDTO;
import com.basebackend.admin.entity.SysApplicationResource;
import com.basebackend.admin.mapper.SysApplicationResourceMapper;
import com.basebackend.admin.mapper.SysRoleResourceMapper;
import com.basebackend.admin.mapper.SysRoleMenuMapper;
import com.basebackend.admin.service.ApplicationResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 应用资源管理Service实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationResourceServiceImpl implements ApplicationResourceService {

    private final SysApplicationResourceMapper resourceMapper;
    private final SysRoleResourceMapper roleResourceMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    @Override
    public List<ApplicationResourceDTO> getResourceTree(Long appId) {
        List<SysApplicationResource> resources = resourceMapper.selectResourceTree(appId);
        return buildTree(resources);
    }

    @Override
    public List<ApplicationResourceDTO> getUserResourceTree(Long appId, Long userId) {
        List<SysApplicationResource> resources = resourceMapper.selectResourcesByAppIdAndUserId(appId, userId);
        return buildTree(resources);
    }

    @Override
    public ApplicationResourceDTO getResourceById(Long id) {
        SysApplicationResource resource = resourceMapper.selectById(id);
        if (resource != null && resource.getDeleted() == 0) {
            return convertToDTO(resource);
        }
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createResource(ApplicationResourceDTO dto) {
        SysApplicationResource resource = new SysApplicationResource();
        BeanUtils.copyProperties(dto, resource);

        // 设置默认值
        if (resource.getParentId() == null) {
            resource.setParentId(0L);
        }
        if (resource.getVisible() == null) {
            resource.setVisible(1);
        }
        if (resource.getStatus() == null) {
            resource.setStatus(1);
        }
        if (resource.getOpenType() == null) {
            resource.setOpenType("current");
        }

        return resourceMapper.insert(resource) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateResource(ApplicationResourceDTO dto) {
        if (dto.getId() == null) {
            throw new RuntimeException("资源ID不能为空");
        }

        SysApplicationResource resource = new SysApplicationResource();
        BeanUtils.copyProperties(dto, resource);

        return resourceMapper.updateById(resource) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteResource(Long id) {
        // 检查是否有子资源
        LambdaQueryWrapper<SysApplicationResource> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysApplicationResource::getParentId, id)
                .eq(SysApplicationResource::getDeleted, 0);

        Long count = resourceMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new RuntimeException("请先删除子资源");
        }

        SysApplicationResource resource = resourceMapper.selectById(id);
        if (resource == null) {
            throw new RuntimeException("资源不存在");
        }

        // 删除角色资源关联
        roleResourceMapper.deleteByResourceId(id);

        // 软删除资源
        resource.setDeleted(1);
        return resourceMapper.updateById(resource) > 0;
    }

    @Override
    public List<Long> getResourceIdsByRoleId(Long roleId) {
        return resourceMapper.selectResourceIdsByRoleId(roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignRoleResources(Long roleId, List<Long> resourceIds) {
        // 先删除原有关联
        roleResourceMapper.deleteByRoleId(roleId);
        roleMenuMapper.deleteByRoleId(roleId);

        // 批量插入新关联
        if (resourceIds != null && !resourceIds.isEmpty()) {
            // 插入角色资源关联
            roleResourceMapper.batchInsert(roleId, resourceIds);
            
            // 根据资源ID查询对应的菜单ID
            List<Long> menuIds = resourceMapper.selectMenuIdsByResourceIds(resourceIds);
            if (menuIds != null && !menuIds.isEmpty()) {
                // 插入角色菜单关联
                roleMenuMapper.batchInsert(roleId, menuIds);
            }
        }

        return true;
    }

    /**
     * 构建树形结构
     */
    private List<ApplicationResourceDTO> buildTree(List<SysApplicationResource> resources) {
        if (resources == null || resources.isEmpty()) {
            return new ArrayList<>();
        }

        List<ApplicationResourceDTO> dtoList = resources.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        // 将列表转为Map，以ID为键
        Map<Long, ApplicationResourceDTO> resourceMap = dtoList.stream()
                .collect(Collectors.toMap(ApplicationResourceDTO::getId, dto -> dto));

        // 构建树形结构
        List<ApplicationResourceDTO> tree = new ArrayList<>();
        for (ApplicationResourceDTO dto : dtoList) {
            if (dto.getParentId() == null || dto.getParentId() == 0) {
                // 顶级资源
                tree.add(dto);
            } else {
                // 子资源，添加到父资源的children列表
                ApplicationResourceDTO parent = resourceMap.get(dto.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(dto);
                }
            }
        }

        return tree;
    }

    /**
     * 转换为DTO
     */
    private ApplicationResourceDTO convertToDTO(SysApplicationResource entity) {
        ApplicationResourceDTO dto = new ApplicationResourceDTO();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }
}
