package com.basebackend.system.service;

import com.basebackend.system.dto.ApplicationResourceDTO;

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

    /**
     * 获取所有资源树（不限应用ID）
     *
     * @return 资源树
     */
    List<ApplicationResourceDTO> getAllResourceTree();

    /**
     * 获取所有资源列表（扁平化）
     *
     * @return 资源列表
     */
    List<ApplicationResourceDTO> getAllResourceList();

    /**
     * 检查资源名称是否唯一
     *
     * @param resourceName 资源名称
     * @param parentId     父资源ID
     * @param resourceId   资源ID（更新时排除自身）
     * @return true-唯一，false-不唯一
     */
    boolean checkResourceNameUnique(String resourceName, Long parentId, Long resourceId);
}
