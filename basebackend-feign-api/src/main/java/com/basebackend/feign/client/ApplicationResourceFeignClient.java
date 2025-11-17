package com.basebackend.feign.client;

import com.basebackend.common.model.Result;
import com.basebackend.feign.constant.FeignServiceConstants;
import com.basebackend.feign.fallback.ApplicationResourceFeignFallbackFactory;
import com.basebackend.admin.dto.ApplicationResourceDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 应用资源服务 Feign 客户端
 * 用于 application-service 调用 admin-api 的应用资源服务
 *
 * @author BaseBackend Team
 * @since 2025-11-14
 */
@FeignClient(
        name = FeignServiceConstants.ADMIN_SERVICE,
        contextId = "applicationResourceFeignClient",
        path = "/api/admin/application/resource",
        fallbackFactory = ApplicationResourceFeignFallbackFactory.class
)
public interface ApplicationResourceFeignClient {

    /**
     * 查询应用的资源树
     *
     * @param appId 应用ID
     * @return 资源树
     */
    @GetMapping("/tree/{appId}")
    @Operation(summary = "查询应用的资源树", description = "根据应用ID查询资源树")
    Result<List<ApplicationResourceDTO>> getResourceTree(@Parameter(description = "应用ID") @PathVariable("appId") Long appId);

    /**
     * 查询用户的资源树
     *
     * @param appId 应用ID
     * @param userId 用户ID
     * @return 用户资源树
     */
    @GetMapping("/user/tree/{appId}")
    @Operation(summary = "查询用户的资源树", description = "根据应用ID和用户ID查询用户的资源树")
    Result<List<ApplicationResourceDTO>> getUserResourceTree(
            @Parameter(description = "应用ID") @PathVariable("appId") Long appId,
            @Parameter(description = "用户ID") @RequestParam("userId") Long userId
    );

    /**
     * 根据ID查询资源
     *
     * @param id 资源ID
     * @return 资源详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询资源", description = "根据资源ID查询资源详情")
    Result<ApplicationResourceDTO> getResourceById(@Parameter(description = "资源ID") @PathVariable("id") Long id);
}
