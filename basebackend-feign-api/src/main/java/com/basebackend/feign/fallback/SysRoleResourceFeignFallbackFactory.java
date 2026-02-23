package com.basebackend.feign.fallback;

import com.basebackend.common.model.Result;
import com.basebackend.feign.client.SysRoleResourceFeignClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 角色菜单服务 Feign 降级处理工厂
 *
 * @author Kiro AI
 * @since 2025-11-23
 */
@Component
public class SysRoleResourceFeignFallbackFactory implements FallbackFactory<SysRoleResourceFeignClient> {

    private static final Logger log = LoggerFactory.getLogger(SysRoleResourceFeignFallbackFactory.class);

    @Override
    public SysRoleResourceFeignClient create(Throwable cause) {
        return new SysRoleResourceFeignClient() {

            @Override
            public Result<String> assignMenus(Long roleId, List<Long> menuIds) {
                log.error("[Feign降级] 分配角色菜单失败: roleId={}, menuIds={}, error={}", 
                        roleId, menuIds, cause.getMessage(), cause);
                return Result.error("角色菜单服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<List<Long>> getRoleMenus(Long roleId) {
                log.error("[Feign降级] 获取角色菜单失败: roleId={}, error={}", 
                        roleId, cause.getMessage(), cause);
                return Result.success("角色菜单服务暂时不可用，返回空列表", Collections.emptyList());
            }
        };
    }
}
