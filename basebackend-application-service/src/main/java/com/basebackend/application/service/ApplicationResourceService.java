package com.basebackend.application.service;

import com.basebackend.admin.dto.ApplicationResourceDTO;
import com.basebackend.feign.client.ApplicationResourceFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 应用资源服务
 * 通过 Feign 调用 admin-api 的应用资源服务
 *
 * @author BaseBackend Team
 * @since 2025-11-14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationResourceService {

    private final ApplicationResourceFeignClient feignClient;

    /**
     * 查询应用的资源树
     *
     * @param appId 应用ID
     * @return 资源树
     */
    public List<ApplicationResourceDTO> getResourceTree(Long appId) {
        log.debug("查询应用资源树: appId={}", appId);
        var result = feignClient.getResourceTree(appId);
        return result.getData() != null ? result.getData() : List.of();
    }

    /**
     * 查询用户的资源树
     *
     * @param appId 应用ID
     * @param userId 用户ID
     * @return 用户资源树
     */
    public List<ApplicationResourceDTO> getUserResourceTree(Long appId, Long userId) {
        log.debug("查询用户资源树: appId={}, userId={}", appId, userId);
        var result = feignClient.getUserResourceTree(appId, userId);
        return result.getData() != null ? result.getData() : List.of();
    }

    /**
     * 根据ID查询资源
     *
     * @param id 资源ID
     * @return 资源详情
     */
    public ApplicationResourceDTO getResourceById(Long id) {
        log.debug("查询资源详情: id={}", id);
        var result = feignClient.getResourceById(id);
        return result.getData();
    }
}
