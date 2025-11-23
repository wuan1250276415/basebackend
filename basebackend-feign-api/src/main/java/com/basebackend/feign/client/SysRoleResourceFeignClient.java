package com.basebackend.feign.client;

import com.basebackend.common.model.Result;
import com.basebackend.feign.constant.FeignServiceConstants;
import com.basebackend.feign.fallback.SysRoleResourceFeignFallbackFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色菜单服务 Feign 客户端
 *
 * @author Kiro AI
 * @since 2025-11-23
 */
@FeignClient(
        name = FeignServiceConstants.USER_SERVICE,
        contextId = "roleResourceFeignClient",
        path = "/api/user/roles",
        fallbackFactory = SysRoleResourceFeignFallbackFactory.class
)
public interface SysRoleResourceFeignClient {

    /**
     * 分配菜单给角色
     *
     * @param roleId  角色ID
     * @param menuIds 菜单ID列表
     * @return 操作结果
     */
    @PutMapping("/{roleId}/menus")
    @Operation(summary = "分配菜单", description = "为角色分配菜单")
    Result<String> assignMenus(
            @Parameter(description = "角色ID") @PathVariable("roleId") Long roleId,
            @RequestBody List<Long> menuIds
    );

    /**
     * 获取角色的菜单列表
     *
     * @param roleId 角色ID
     * @return 菜单ID列表
     */
    @GetMapping("/{roleId}/menus")
    @Operation(summary = "获取角色菜单", description = "获取角色菜单列表")
    Result<List<Long>> getRoleMenus(
            @Parameter(description = "角色ID") @PathVariable("roleId") Long roleId
    );
}
