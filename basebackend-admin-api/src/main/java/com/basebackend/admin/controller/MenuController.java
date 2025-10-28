package com.basebackend.admin.controller;

import com.basebackend.admin.dto.ApplicationResourceDTO;
import com.basebackend.admin.dto.MenuDTO;
import com.basebackend.admin.service.ApplicationResourceService;
import com.basebackend.admin.service.MenuService;
import com.basebackend.common.constant.CommonConstants;
import com.basebackend.common.model.Result;
import com.basebackend.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜单管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/menus")
@RequiredArgsConstructor
@Validated
@Tag(name = "菜单管理", description = "菜单管理相关接口")
public class MenuController {

    private final MenuService menuService;
    private final ApplicationResourceService applicationResourceService;
    private final JwtUtil jwtUtil;

    /**
     * 获取菜单树
     */
    @GetMapping("/tree")
    @Operation(summary = "获取菜单树", description = "获取菜单树形结构")
    public Result<List<MenuDTO>> getMenuTree() {
        log.info("获取菜单树");
        try {
            // 从sys_application_resource表查询所有资源
            List<ApplicationResourceDTO> allResources = applicationResourceService.getResourceTree(null);

            // 转换为MenuDTO格式
            List<MenuDTO> menuTree = convertResourceToMenu(allResources);

            return Result.success("查询成功", menuTree);
        } catch (Exception e) {
            log.error("获取菜单树失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取菜单列表
     */
    @GetMapping
    @Operation(summary = "获取菜单列表", description = "获取菜单列表")
    public Result<List<MenuDTO>> getMenuList() {
        log.info("获取菜单列表");
        try {
            // 从sys_application_resource表查询所有资源（平铺列表）
            List<ApplicationResourceDTO> allResources = applicationResourceService.getResourceTree(null);

            // 转换为MenuDTO格式
            List<MenuDTO> menuList = convertResourceToMenuList(allResources);

            return Result.success("查询成功", menuList);
        } catch (Exception e) {
            log.error("获取菜单列表失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据ID查询菜单
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询菜单", description = "根据ID查询菜单详情")
    public Result<MenuDTO> getById(@Parameter(description = "菜单ID") @PathVariable Long id) {
        log.info("根据ID查询菜单: {}", id);
        try {
            ApplicationResourceDTO resource = applicationResourceService.getResourceById(id);
            if (resource == null) {
                return Result.error("菜单不存在");
            }
            MenuDTO menu = toMenuDTO(resource);
            return Result.success("查询成功", menu);
        } catch (Exception e) {
            log.error("根据ID查询菜单失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 创建菜单
     */
    @PostMapping
    @Operation(summary = "创建菜单", description = "创建新菜单")
    public Result<String> create(@Validated @RequestBody MenuDTO menuDTO) {
        log.info("创建菜单: {}", menuDTO.getMenuName());
        try {
            // 转换MenuDTO为ApplicationResourceDTO
            ApplicationResourceDTO resourceDTO = convertMenuToResource(menuDTO);
            boolean success = applicationResourceService.createResource(resourceDTO);
            return success ? Result.success("菜单创建成功") : Result.error("菜单创建失败");
        } catch (Exception e) {
            log.error("创建菜单失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新菜单
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新菜单", description = "更新菜单信息")
    public Result<String> update(
            @Parameter(description = "菜单ID") @PathVariable Long id,
            @Validated @RequestBody MenuDTO menuDTO) {
        log.info("更新菜单: {}", id);
        try {
            menuDTO.setId(id);
            // 转换MenuDTO为ApplicationResourceDTO
            ApplicationResourceDTO resourceDTO = convertMenuToResource(menuDTO);
            boolean success = applicationResourceService.updateResource(resourceDTO);
            return success ? Result.success("菜单更新成功") : Result.error("菜单更新失败");
        } catch (Exception e) {
            log.error("更新菜单失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除菜单
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除菜单", description = "删除菜单")
    public Result<String> delete(@Parameter(description = "菜单ID") @PathVariable Long id) {
        log.info("删除菜单: {}", id);
        try {
            boolean success = applicationResourceService.deleteResource(id);
            return success ? Result.success("菜单删除成功") : Result.error("菜单删除失败");
        } catch (Exception e) {
            log.error("删除菜单失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取前端路由
     */
    @GetMapping("/routes")
    @Operation(summary = "获取前端路由", description = "获取前端路由配置")
    public Result<List<MenuDTO>> getRoutes() {
        log.info("获取前端路由");
        try {
            List<MenuDTO> routes = menuService.getRoutes();
            return Result.success("查询成功", routes);
        } catch (Exception e) {
            log.error("获取前端路由失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据用户ID获取菜单树
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "获取用户菜单", description = "根据用户ID获取菜单树")
    public Result<List<MenuDTO>> getMenuTreeByUserId(@Parameter(description = "用户ID") @PathVariable Long userId) {
        log.info("根据用户ID获取菜单树: {}", userId);
        try {
            List<MenuDTO> menuTree = menuService.getMenuTreeByUserId(userId);
            return Result.success("查询成功", menuTree);
        } catch (Exception e) {
            log.error("根据用户ID获取菜单树失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
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
            Long currentUserId = getCurrentUserId();

            // 从sys_application_resource表中获取用户的资源树
            List<ApplicationResourceDTO> resourceTree = applicationResourceService.getUserResourceTreeByUserId(currentUserId);

            // 转换为MenuDTO格式
            List<MenuDTO> menuTree = convertResourceToMenu(resourceTree);

            return Result.success("查询成功", menuTree);
        } catch (Exception e) {
            log.error("获取当前用户菜单树失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new IllegalStateException("无法获取请求上下文");
        }

        HttpServletRequest request = attributes.getRequest();
        if (request == null) {
            throw new IllegalStateException("无法获取请求信息");
        }

        String bearerToken = request.getHeader(CommonConstants.TOKEN_HEADER);
        if (!StringUtils.hasText(bearerToken)) {
            throw new IllegalStateException("缺少认证信息");
        }

        String token = bearerToken.startsWith(CommonConstants.TOKEN_PREFIX)
                ? bearerToken.substring(CommonConstants.TOKEN_PREFIX.length())
                : bearerToken;

        Claims claims = jwtUtil.getClaimsFromToken(token);
        if (claims == null || claims.get("userId") == null) {
            throw new IllegalStateException("认证信息无效");
        }

        try {
            return Long.parseLong(claims.get("userId").toString());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("用户ID格式错误");
        }
    }

    /**
     * 检查菜单名称是否唯一
     */
    @GetMapping("/check-menu-name")
    @Operation(summary = "检查菜单名称唯一性", description = "检查菜单名称是否唯一")
    public Result<Boolean> checkMenuNameUnique(
            @Parameter(description = "菜单名称") @RequestParam String menuName,
            @Parameter(description = "父菜单ID") @RequestParam Long parentId,
            @Parameter(description = "菜单ID") @RequestParam(required = false) Long menuId) {
        try {
            boolean unique = menuService.checkMenuNameUnique(menuName, parentId, menuId);
            return Result.success("检查完成", unique);
        } catch (Exception e) {
            log.error("检查菜单名称唯一性失败: {}", e.getMessage());
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
