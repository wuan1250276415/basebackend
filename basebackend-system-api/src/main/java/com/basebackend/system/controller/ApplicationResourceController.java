package com.basebackend.system.controller;

import com.basebackend.common.context.UserContextHolder;
import com.basebackend.system.dto.ApplicationResourceDTO;
import com.basebackend.system.dto.MenuDTO;
import com.basebackend.system.service.ApplicationResourceService;
import com.basebackend.common.constant.CommonConstants;
import com.basebackend.common.model.Result;
import com.basebackend.jwt.JwtUtil;
import com.basebackend.logging.annotation.OperationLog;
import com.basebackend.logging.annotation.OperationLog.BusinessType;

import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 应用资源管理Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/system/application/resource")
@RequiredArgsConstructor
@Validated
@Tag(name = "应用资源管理", description = "应用资源管理相关接口")
public class ApplicationResourceController {

    private final ApplicationResourceService resourceService;
    private final JwtUtil jwtUtil;

    @GetMapping("/tree/{appId}")
    @Operation(summary = "查询应用的资源树")
    @OperationLog(operation="查询应用的资源树",businessType = BusinessType.SELECT)
    public Result<List<ApplicationResourceDTO>> getResourceTree(@PathVariable Long appId) {
        List<ApplicationResourceDTO> tree = resourceService.getResourceTree(appId);
        return Result.success(tree);
    }

    @GetMapping("/user/tree/{appId}")
    @Operation(summary = "查询用户的资源树")
    @OperationLog(operation="查询用户的资源树", businessType = BusinessType.SELECT)
    public Result<List<ApplicationResourceDTO>> getUserResourceTree(
            @PathVariable Long appId,
            HttpServletRequest request) {
        // 从Token中获取用户ID - 添加安全验证
        String token = request.getHeader(CommonConstants.TOKEN_HEADER);
        if (!StringUtils.hasText(token)) {
            log.warn("获取用户资源树失败: 缺少Token");
            return Result.error("未授权访问");
        }
        if (token.startsWith(CommonConstants.TOKEN_PREFIX)) {
            token = token.substring(CommonConstants.TOKEN_PREFIX.length());
        }

        Claims claims;
        try {
            claims = jwtUtil.getClaimsFromToken(token);
        } catch (Exception e) {
            log.warn("解析Token失败: {}", e.getMessage());
            return Result.error("认证信息无效");
        }

        Object userIdClaim = claims != null ? claims.get("userId") : null;
        if (userIdClaim == null) {
            log.warn("Token缺少userId声明");
            return Result.error("认证信息无效");
        }

        long userId;
        try {
            userId = Long.parseLong(userIdClaim.toString());
        } catch (NumberFormatException e) {
            log.warn("Token中的userId非法: {}", userIdClaim);
            return Result.error("认证信息无效");
        }

        List<ApplicationResourceDTO> tree = resourceService.getUserResourceTree(appId, userId);
        return Result.success(tree);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询资源")
    @OperationLog(operation="根据ID查询资源", businessType = BusinessType.SELECT)
    public Result<ApplicationResourceDTO> getResourceById(@PathVariable Long id) {
        ApplicationResourceDTO dto = resourceService.getResourceById(id);
        if (dto == null) {
            return Result.error("资源不存在");
        }
        return Result.success(dto);
    }

    @PostMapping
    @Operation(summary = "创建资源")
    @OperationLog(operation="创建资源", businessType = BusinessType.INSERT)
    public Result<Void> createResource(@Validated @RequestBody ApplicationResourceDTO dto) {
        log.info("创建资源: {}", dto.getResourceName());
        boolean success = resourceService.createResource(dto);
        return success ? Result.success() : Result.error("创建失败");
    }

    @PutMapping
    @Operation(summary = "更新资源")
    @OperationLog(operation="更新资源", businessType = BusinessType.UPDATE)
    public Result<Void> updateResource(@Validated @RequestBody ApplicationResourceDTO dto) {
        log.info("更新资源: {}", dto.getId());
        boolean success = resourceService.updateResource(dto);
        return success ? Result.success() : Result.error("更新失败");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除资源")
    @OperationLog(operation="删除资源", businessType = BusinessType.DELETE)
    public Result<Void> deleteResource(@PathVariable Long id) {
        log.info("删除资源: {}", id);
        boolean success = resourceService.deleteResource(id);
        return success ? Result.success() : Result.error("删除失败");
    }

    @GetMapping("/role/{roleId}")
    @Operation(summary = "查询角色的资源ID列表")
    @OperationLog(operation="查询角色的资源ID列表", businessType = BusinessType.SELECT)
    public Result<List<Long>> getResourceIdsByRoleId(@PathVariable Long roleId) {
        List<Long> resourceIds = resourceService.getResourceIdsByRoleId(roleId);
        return Result.success(resourceIds);
    }

    @PostMapping("/role/{roleId}/assign")
    @Operation(summary = "分配角色资源")
    @OperationLog(operation="分配角色资源", businessType = BusinessType.UPDATE)
    public Result<Void> assignRoleResources(
            @PathVariable Long roleId,
            @RequestBody List<Long> resourceIds) {
        log.info("分配角色资源: roleId={}, resourceIds={}", roleId, resourceIds);
        boolean success = resourceService.assignRoleResources(roleId, resourceIds);
        return success ? Result.success() : Result.error("分配失败");
    }
    /**
     * 获取当前登录用户的菜单树（用于前端动态路由）
     */
    @GetMapping("/current-user")
    @Operation(summary = "获取当前用户菜单", description = "获取当前登录用户的菜单树，用于前端动态菜单")
    public Result<List<MenuDTO>> getCurrentUserMenuTree() {
        log.info("获取当前用户菜单树");
        try {
            // 从SecurityContext获取当前用户ID
            Long currentUserId = UserContextHolder.getUserId();

            // 从sys_application_resource表中获取用户的资源树
            List<ApplicationResourceDTO> resourceTree = resourceService.getUserResourceTreeByUserId(currentUserId);

            // 转换为MenuDTO格式
            List<MenuDTO> menuTree = convertResourceToMenu(resourceTree);

            return Result.success("查询成功", menuTree);
        } catch (Exception e) {
            log.error("获取当前用户菜单树失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 将ApplicationResourceDTO转换为MenuDTO
     */
    private List<MenuDTO> convertResourceToMenu(List<ApplicationResourceDTO> resources) {
        if (resources == null || resources.isEmpty()) {
            return List.of();
        }

        return resources.stream()
                .map(this::toMenuDTO)
                .collect(Collectors.toList());
    }


    /**
     * 将资源树平铺为列表
     */
    private List<MenuDTO> convertResourceToMenuList(List<ApplicationResourceDTO> resources) {
        List<MenuDTO> result = new ArrayList<>();
        flattenResourceTree(resources, result);
        return result;
    }

    private void flattenResourceTree(List<ApplicationResourceDTO> resources, List<MenuDTO> result) {
        if (resources == null || resources.isEmpty()) {
            return;
        }
        for (ApplicationResourceDTO resource : resources) {
            result.add(toMenuDTO(resource));
            if (resource.getChildren() != null && !resource.getChildren().isEmpty()) {
                flattenResourceTree(resource.getChildren(), result);
            }
        }
    }

    /**
     * 单个资源转换为菜单DTO
     */
    private MenuDTO toMenuDTO(ApplicationResourceDTO resource) {
        MenuDTO menu = new MenuDTO();
        menu.setId(resource.getId());
        menu.setAppId(resource.getAppId());
        menu.setMenuName(resource.getResourceName());
        menu.setParentId(resource.getParentId());
        menu.setOrderNum(resource.getOrderNum());
        menu.setPath(resource.getPath());
        menu.setComponent(resource.getComponent());
        menu.setMenuType(resource.getResourceType());
        menu.setVisible(resource.getVisible());
        menu.setStatus(resource.getStatus());
        menu.setPerms(resource.getPerms());
        menu.setIcon(resource.getIcon());
        menu.setRemark(resource.getRemark());

        // 转换子资源
        if (resource.getChildren() != null && !resource.getChildren().isEmpty()) {
            menu.setChildren(convertResourceToMenu(resource.getChildren()));
        }

        return menu;
    }

    /**
     * 将MenuDTO转换为ApplicationResourceDTO
     */
    private ApplicationResourceDTO convertMenuToResource(MenuDTO menu) {
        ApplicationResourceDTO resource = new ApplicationResourceDTO();
        resource.setId(menu.getId());
        resource.setAppId(menu.getAppId());
        resource.setResourceName(menu.getMenuName());
        resource.setParentId(menu.getParentId());
        resource.setResourceType(menu.getMenuType());
        resource.setPath(menu.getPath());
        resource.setComponent(menu.getComponent());
        resource.setPerms(menu.getPerms());
        resource.setIcon(menu.getIcon());
        resource.setVisible(menu.getVisible());
        resource.setOrderNum(menu.getOrderNum());
        resource.setStatus(menu.getStatus());
        resource.setRemark(menu.getRemark());

        return resource;
    }
}
