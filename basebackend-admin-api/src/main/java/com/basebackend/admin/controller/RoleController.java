package com.basebackend.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.admin.dto.RoleDTO;
import com.basebackend.admin.service.RoleService;
import com.basebackend.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
@Tag(name = "角色管理", description = "角色管理相关接口")
public class RoleController {

    private final RoleService roleService;

    /**
     * 分页查询角色列表
     */
    @GetMapping
    @Operation(summary = "分页查询角色列表", description = "分页查询角色列表")
    public Result<Page<RoleDTO>> page(
            @Parameter(description = "当前页", example = "1") @RequestParam(defaultValue = "1") int current,
            @Parameter(description = "每页大小", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "角色名称") @RequestParam(required = false) String roleName,
            @Parameter(description = "角色标识") @RequestParam(required = false) String roleKey,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        log.info("分页查询角色列表: current={}, size={}", current, size);
        try {
            Page<RoleDTO> result = roleService.page(roleName, roleKey, status, current, size);
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("分页查询角色列表失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据ID查询角色
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询角色", description = "根据ID查询角色详情")
    public Result<RoleDTO> getById(@Parameter(description = "角色ID") @PathVariable Long id) {
        log.info("根据ID查询角色: {}", id);
        try {
            RoleDTO role = roleService.getById(id);
            return Result.success("查询成功", role);
        } catch (Exception e) {
            log.error("根据ID查询角色失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 创建角色
     */
    @PostMapping
    @Operation(summary = "创建角色", description = "创建新角色")
    public Result<String> create(@Validated @RequestBody RoleDTO roleDTO) {
        log.info("创建角色: {}", roleDTO.getRoleName());
        try {
            roleService.create(roleDTO);
            return Result.success("角色创建成功");
        } catch (Exception e) {
            log.error("创建角色失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新角色
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新角色", description = "更新角色信息")
    public Result<String> update(
            @Parameter(description = "角色ID") @PathVariable Long id,
            @Validated @RequestBody RoleDTO roleDTO) {
        log.info("更新角色: {}", id);
        try {
            roleDTO.setId(id);
            roleService.update(roleDTO);
            return Result.success("角色更新成功");
        } catch (Exception e) {
            log.error("更新角色失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除角色", description = "删除角色")
    public Result<String> delete(@Parameter(description = "角色ID") @PathVariable Long id) {
        log.info("删除角色: {}", id);
        try {
            roleService.delete(id);
            return Result.success("角色删除成功");
        } catch (Exception e) {
            log.error("删除角色失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 分配菜单
     */
    @PutMapping("/{id}/menus")
    @Operation(summary = "分配菜单", description = "为角色分配菜单")
    public Result<String> assignMenus(
            @Parameter(description = "角色ID") @PathVariable Long id,
            @RequestBody List<Long> menuIds) {
        log.info("分配角色菜单: roleId={}, menuIds={}", id, menuIds);
        try {
            roleService.assignMenus(id, menuIds);
            return Result.success("菜单分配成功");
        } catch (Exception e) {
            log.error("分配菜单失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 分配权限
     */
    @PutMapping("/{id}/permissions")
    @Operation(summary = "分配权限", description = "为角色分配权限")
    public Result<String> assignPermissions(
            @Parameter(description = "角色ID") @PathVariable Long id,
            @RequestBody List<Long> permissionIds) {
        log.info("分配角色权限: roleId={}, permissionIds={}", id, permissionIds);
        try {
            roleService.assignPermissions(id, permissionIds);
            return Result.success("权限分配成功");
        } catch (Exception e) {
            log.error("分配权限失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取角色菜单列表
     */
    @GetMapping("/{id}/menus")
    @Operation(summary = "获取角色菜单", description = "获取角色菜单列表")
    public Result<List<Long>> getRoleMenus(@Parameter(description = "角色ID") @PathVariable Long id) {
        log.info("获取角色菜单: {}", id);
        try {
            List<Long> menuIds = roleService.getRoleMenus(id);
            return Result.success("查询成功", menuIds);
        } catch (Exception e) {
            log.error("获取角色菜单失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取角色权限列表
     */
    @GetMapping("/{id}/permissions")
    @Operation(summary = "获取角色权限", description = "获取角色权限列表")
    public Result<List<Long>> getRolePermissions(@Parameter(description = "角色ID") @PathVariable Long id) {
        log.info("获取角色权限: {}", id);
        try {
            List<Long> permissionIds = roleService.getRolePermissions(id);
            return Result.success("查询成功", permissionIds);
        } catch (Exception e) {
            log.error("获取角色权限失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 检查角色名称是否唯一
     */
    @GetMapping("/check-role-name")
    @Operation(summary = "检查角色名称唯一性", description = "检查角色名称是否唯一")
    public Result<Boolean> checkRoleNameUnique(
            @Parameter(description = "角色名称") @RequestParam String roleName,
            @Parameter(description = "角色ID") @RequestParam(required = false) Long roleId) {
        try {
            boolean unique = roleService.checkRoleNameUnique(roleName, roleId);
            return Result.success("检查完成", unique);
        } catch (Exception e) {
            log.error("检查角色名称唯一性失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 检查角色标识是否唯一
     */
    @GetMapping("/check-role-key")
    @Operation(summary = "检查角色标识唯一性", description = "检查角色标识是否唯一")
    public Result<Boolean> checkRoleKeyUnique(
            @Parameter(description = "角色标识") @RequestParam String roleKey,
            @Parameter(description = "角色ID") @RequestParam(required = false) Long roleId) {
        try {
            boolean unique = roleService.checkRoleKeyUnique(roleKey, roleId);
            return Result.success("检查完成", unique);
        } catch (Exception e) {
            log.error("检查角色标识唯一性失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
