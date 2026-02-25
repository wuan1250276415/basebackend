package com.basebackend.service.client.fallback;

import com.basebackend.common.model.Result;
import com.basebackend.service.client.SysRoleResourceServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 角色资源服务客户端降级实现
 *
 * @author Claude Code
 * @since 2025-11-08
 */
@Component
public class SysRoleResourceServiceClientFallback implements SysRoleResourceServiceClient {

    private static final Logger log = LoggerFactory.getLogger(SysRoleResourceServiceClientFallback.class);

    @Override
    public Result<String> assignMenus(Long roleId, List<Long> menuIds) {
        log.error("[服务降级] 分配角色菜单失败: roleId={}", roleId);
        return Result.error("角色资源服务不可用，分配菜单失败");
    }

    @Override
    public Result<List<Long>> getRoleMenus(Long roleId) {
        log.error("[服务降级] 获取角色菜单失败: roleId={}", roleId);
        return Result.success("角色资源服务降级，返回空列表", Collections.emptyList());
    }
}
