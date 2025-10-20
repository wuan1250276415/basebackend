package com.basebackend.admin.controller;

import com.basebackend.admin.dto.MenuDTO;
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

import java.util.List;

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
    private final JwtUtil jwtUtil;

    /**
     * 获取菜单树
     */
    @GetMapping("/tree")
    @Operation(summary = "获取菜单树", description = "获取菜单树形结构")
    public Result<List<MenuDTO>> getMenuTree() {
        log.info("获取菜单树");
        try {
            List<MenuDTO> menuTree = menuService.getMenuTree();
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
            List<MenuDTO> menuList = menuService.getMenuList();
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
            MenuDTO menu = menuService.getById(id);
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
            menuService.create(menuDTO);
            return Result.success("菜单创建成功");
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
            menuService.update(menuDTO);
            return Result.success("菜单更新成功");
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
            menuService.delete(id);
            return Result.success("菜单删除成功");
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
            List<MenuDTO> menuTree = menuService.getMenuTreeByUserId(currentUserId);
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
}
