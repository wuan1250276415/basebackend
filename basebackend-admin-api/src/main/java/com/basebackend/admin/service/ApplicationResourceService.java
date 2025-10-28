package com.basebackend.admin.service;

import com.basebackend.admin.dto.ApplicationResourceDTO;
import com.basebackend.admin.entity.SysApplicationResource;

import java.util.List;

/**
 * 应用资源管理Service接口
 */
public interface ApplicationResourceService {

    /**
     * 查询应用的资源树
     *
     * @param appId 应用ID
     * @return 资源树
     */
    List<ApplicationResourceDTO> getResourceTree(Long appId);

    /**
     * 根据应用ID和用户ID查询用户有权限的资源树
     *
     * @param appId  应用ID
     * @param userId 用户ID
     * @return 资源树
     */
    List<ApplicationResourceDTO> getUserResourceTree(Long appId, Long userId);

    /**
     * 根据ID查询资源
     *
     * @param id 资源ID
     * @return 资源信息
     */
    ApplicationResourceDTO getResourceById(Long id);

    /**
     * 创建资源
     *
     * @param dto 资源信息
     * @return 是否成功
     */
    boolean createResource(ApplicationResourceDTO dto);

    /**
     * 更新资源
     *
     * @param dto 资源信息
     * @return 是否成功
     */
    boolean updateResource(ApplicationResourceDTO dto);

    /**
     * 删除资源
     *
     * @param id 资源ID
     * @return 是否成功
     */
    boolean deleteResource(Long id);

    /**
     * 查询角色的资源ID列表
     *
     * @param roleId 角色ID
     * @return 资源ID列表
     */
    List<Long> getResourceIdsByRoleId(Long roleId);

    /**
     * 分配角色资源
     *
     * @param roleId      角色ID
     * @param resourceIds 资源ID列表
     * @return 是否成功
     */
    boolean assignRoleResources(Long roleId, List<Long> resourceIds);

    /**
     * 根据用户ID获取用户有权限的资源树（不限应用ID）
     *
     * @param userId 用户ID
     * @return 资源树
     */
    List<ApplicationResourceDTO> getUserResourceTreeByUserId(Long userId);
}
