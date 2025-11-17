package com.basebackend.system.service;

import com.basebackend.system.dto.ApplicationResourceDTO;

import java.util.List;

/**
 * 应用资源服务接口
 */
public interface ApplicationResourceService {

    /**
     * 获取资源树
     */
    List<ApplicationResourceDTO> getResourceTree(Long appId);

    /**
     * 根据ID获取资源
     */
    ApplicationResourceDTO getResourceById(Long id);

    /**
     * 创建资源
     */
    boolean createResource(ApplicationResourceDTO resourceDTO);

    /**
     * 更新资源
     */
    boolean updateResource(ApplicationResourceDTO resourceDTO);

    /**
     * 删除资源
     */
    boolean deleteResource(Long id);

    /**
     * 根据用户ID获取用户资源树
     */
    List<ApplicationResourceDTO> getUserResourceTreeByUserId(Long userId);
}
