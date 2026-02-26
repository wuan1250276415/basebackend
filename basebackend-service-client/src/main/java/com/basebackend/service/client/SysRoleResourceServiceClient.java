package com.basebackend.service.client;

import com.basebackend.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PutExchange;

import java.util.List;

/**
 * 角色资源服务客户端
 *
 * @author Claude Code
 * @since 2025-11-08
 */
@HttpExchange("/api/user/roles")
public interface SysRoleResourceServiceClient {

    @PutExchange("/{roleId}/menus")
    @Operation(summary = "分配角色菜单")
    Result<String> assignMenus(@PathVariable("roleId") Long roleId, @RequestBody List<Long> menuIds);

    @GetExchange("/{roleId}/menus")
    @Operation(summary = "获取角色菜单")
    Result<List<Long>> getRoleMenus(@PathVariable("roleId") Long roleId);
}
